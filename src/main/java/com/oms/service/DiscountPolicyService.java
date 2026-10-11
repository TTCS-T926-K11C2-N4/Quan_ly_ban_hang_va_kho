package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.DiscountPolicyDao;
import com.oms.dao.ProductCategoryDao;
import com.oms.dao.ProductDao;
import com.oms.model.DiscountPolicyForm;
import com.oms.model.DiscountPolicyRow;
import com.oms.model.DiscountPolicyStatus;
import com.oms.model.PageResult;
import com.oms.model.Product;
import com.oms.model.ProductCategory;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// Chính sách chiết khấu theo sản lượng (S3-01): bậc theo số lượng mua (đơn vị cơ sở) của một SKU hoặc một nhóm hàng
// (gồm nhóm con), chiết khấu % hoặc số tiền trên một đơn vị cơ sở. Nhiều chính sách cùng áp được cho một dòng hàng
// thì màn tạo đơn (SalesOrderService) lấy chính sách cho số tiền chiết khấu lớn nhất, xem docs/quy-tac-chiet-khau.md.
// Mọi thay đổi ghi nhật ký thao tác trong cùng transaction (S2-04).
public class DiscountPolicyService {

    public static final int PAGE_SIZE = 10;
    public static final int NAME_MAX_LENGTH = 150;
    public static final int MAX_TIERS = 10;
    private static final String ENTITY = "DISCOUNT_POLICY";
    private static final String CODE_PREFIX = "CK-";
    // Số lượng theo đơn vị cơ sở, tối đa 3 chữ số thập phân như cột min_qty_base; % tối đa 2 chữ số thập phân
    private static final Pattern QTY_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("\\d{1,3}([.,]\\d{1,2})?");
    // Số tiền VNĐ / đơn vị: số nguyên, cho gõ dấu chấm ngăn nghìn
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d{1,3}(\\.?\\d{3})*");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // Bấm thùng rác: chính sách chưa có đơn dùng thì xoá hẳn, đã có thì chỉ ngừng áp dụng
    public enum RemoveResult { DELETED, DEACTIVATED }

    private final DiscountPolicyDao policyDao = new DiscountPolicyDao();
    private final ProductDao productDao = new ProductDao();
    private final ProductCategoryDao categoryDao = new ProductCategoryDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public PageResult<DiscountPolicyRow> search(String keyword, Long groupId, DiscountPolicyStatus status,
                                                int requestedPage) throws SQLException {
        LocalDate today = DateTimeUtil.today();
        long total = policyDao.count(keyword, groupId, status, today);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(requestedPage, 1), totalPages);
        return new PageResult<>(policyDao.findPage(keyword, groupId, status, today, (page - 1) * PAGE_SIZE, PAGE_SIZE),
                page, PAGE_SIZE, total);
    }

    public DiscountPolicyRow find(long id) throws SQLException {
        return policyDao.findById(id);
    }

    public List<SelectOption> getProductOptions() throws SQLException {
        return policyDao.findProductOptions();
    }

    // Kết quả kiểm tra form: lỗi theo tên ô ("tiers" là lỗi chung của bảng bậc) và giá trị đã đọc được để lưu
    // target: SKU hoặc tên nhóm hàng được chiết khấu, để ghi nhật ký bằng chữ thay vì mã id
    public record Checked(Map<String, String> errors, DiscountPolicyDao.Values values,
                          List<DiscountPolicyRow.Tier> tiers, String target) {
        public boolean isValid() {
            return errors.isEmpty();
        }
    }

    public Checked validate(DiscountPolicyForm form, List<SelectOption> groups) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên chính sách.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên chính sách tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }

        Long groupId = parseId(form.getCustomerGroupId());
        if (form.getCustomerGroupId() != null && (groupId == null
                || groups.stream().noneMatch(group -> group.getId() == groupId))) {
            errors.put("customerGroupId", "Nhóm khách hàng không còn hoạt động.");
        }

        Long productId = null;
        Long categoryId = null;
        String target = null;
        String scope = form.getScopeType();
        if (DiscountPolicyRow.SCOPE_PRODUCT.equals(scope)) {
            if (form.getProductSku() == null) {
                errors.put("productSku", "Nhập mã SKU được chiết khấu.");
            } else {
                Product product = productDao.findBySkus(List.of(form.getProductSku()), false)
                        .get(form.getProductSku().toUpperCase());
                if (product == null) {
                    errors.put("productSku", "Không có sản phẩm mã " + form.getProductSku() + ".");
                } else {
                    productId = product.getId();
                    target = product.getSku();
                }
            }
        } else if (DiscountPolicyRow.SCOPE_CATEGORY.equals(scope)) {
            Long id = parseId(form.getCategoryId());
            ProductCategory category = id == null ? null : categoryDao.findById(id);
            if (category == null) {
                errors.put("categoryId", "Chọn nhóm hàng được chiết khấu.");
            } else {
                categoryId = category.getId();
                target = category.getName();
            }
        } else {
            errors.put("scopeType", "Chọn chiết khấu cho một SKU hay một nhóm hàng.");
        }

        boolean percent = DiscountPolicyRow.PERCENT.equals(form.getDiscountType());
        if (!percent && !DiscountPolicyRow.AMOUNT_PER_UNIT.equals(form.getDiscountType())) {
            errors.put("discountType", "Chọn cách tính chiết khấu.");
        }

        LocalDate validFrom = parseDate(form.getValidFrom());
        LocalDate validTo = parseDate(form.getValidTo());
        if (validFrom == null) {
            errors.put("validFrom", form.getValidFrom() == null ? "Chọn ngày bắt đầu." : "Ngày bắt đầu không hợp lệ.");
        }
        if (form.getValidTo() != null && validTo == null) {
            errors.put("validTo", "Ngày kết thúc không hợp lệ.");
        } else if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            errors.put("validTo", "Ngày kết thúc không được trước ngày bắt đầu.");
        }

        List<DiscountPolicyRow.Tier> tiers = parseTiers(form, percent, errors);
        DiscountPolicyDao.Values values = errors.isEmpty()
                ? new DiscountPolicyDao.Values(form.getName(), scope, productId, categoryId, form.getDiscountType(),
                groupId, validFrom, validTo, form.isActive())
                : null;
        return new Checked(errors, values, tiers, target);
    }

    // Bậc bỏ trống cả hai ô thì bỏ qua; còn lại phải đủ, số lượng không trùng; lưu theo số lượng tăng dần
    static List<DiscountPolicyRow.Tier> parseTiers(DiscountPolicyForm form, boolean percent,
                                                   Map<String, String> errors) {
        List<DiscountPolicyRow.Tier> tiers = new ArrayList<>();
        Set<BigDecimal> quantities = new HashSet<>();
        int index = 0;
        for (DiscountPolicyForm.TierLine line : form.getTiers()) {
            int number = ++index;
            if (line.minQty() == null && line.value() == null) {
                continue;
            }
            BigDecimal qty = line.minQty() != null && QTY_PATTERN.matcher(line.minQty()).matches()
                    ? new BigDecimal(line.minQty().replace(',', '.')) : null;
            BigDecimal value = parseValue(line.value(), percent);
            if (qty == null || qty.signum() <= 0) {
                errors.putIfAbsent("tiers", "Bậc " + number + ": số lượng tối thiểu phải là số lớn hơn 0.");
            } else if (!quantities.add(qty.stripTrailingZeros())) {
                errors.putIfAbsent("tiers", "Bậc " + number + ": trùng số lượng tối thiểu với một bậc khác.");
            }
            if (value == null || value.signum() <= 0) {
                errors.putIfAbsent("tiers", "Bậc " + number + ": " + (percent
                        ? "phần trăm chiết khấu phải lớn hơn 0 và không quá 100." : "số tiền chiết khấu phải lớn hơn 0."));
            } else if (percent && value.compareTo(HUNDRED) > 0) {
                errors.putIfAbsent("tiers", "Bậc " + number + ": phần trăm chiết khấu không quá 100.");
            }
            if (qty != null && value != null) {
                tiers.add(new DiscountPolicyRow.Tier(qty, value));
            }
        }
        if (tiers.isEmpty() && !errors.containsKey("tiers")) {
            errors.put("tiers", "Khai báo ít nhất một bậc chiết khấu.");
        } else if (tiers.size() > MAX_TIERS) {
            errors.put("tiers", "Một chính sách tối đa " + MAX_TIERS + " bậc.");
        }
        tiers.sort(Comparator.comparing(DiscountPolicyRow.Tier::minQtyBase));
        return tiers;
    }

    private static BigDecimal parseValue(String text, boolean percent) {
        if (text == null) {
            return null;
        }
        if (percent) {
            return PERCENT_PATTERN.matcher(text).matches() ? new BigDecimal(text.replace(',', '.')) : null;
        }
        return AMOUNT_PATTERN.matcher(text).matches() ? new BigDecimal(text.replace(".", "")) : null;
    }

    // Gọi validate trước. Trả về id chính sách mới.
    public long create(Checked checked, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String code = CODE_PREFIX + String.format("%04d", policyDao.findMaxCodeNumber() + 1);
                long id = policyDao.insert(connection, code, checked.values(), actorUserId);
                policyDao.replaceTiers(connection, id, checked.tiers());
                audit(connection, actorUserId, "DISCOUNT_POLICY_CREATE", id, null,
                        values(code, checked.values(), checked.tiers(), checked.target()), ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Đơn đã tạo lưu sẵn số tiền chiết khấu từng dòng nên sửa bậc chỉ ảnh hưởng đơn tạo sau đó
    public void update(DiscountPolicyRow old, Checked checked, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                policyDao.update(connection, old.getId(), checked.values(), actorUserId);
                policyDao.replaceTiers(connection, old.getId(), checked.tiers());
                audit(connection, actorUserId, "DISCOUNT_POLICY_UPDATE", old.getId(), values(old),
                        values(old.getCode(), checked.values(), checked.tiers(), checked.target()), ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public RemoveResult remove(DiscountPolicyRow policy, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                RemoveResult result;
                if (policy.isUsed()) {
                    policyDao.setActive(connection, policy.getId(), false, actorUserId);
                    audit(connection, actorUserId, "DISCOUNT_POLICY_DEACTIVATE", policy.getId(),
                            activeValues(policy.getCode(), policy.isActive()), activeValues(policy.getCode(), false),
                            ipAddress);
                    result = RemoveResult.DEACTIVATED;
                } else {
                    policyDao.delete(connection, policy.getId());
                    audit(connection, actorUserId, "DISCOUNT_POLICY_DELETE", policy.getId(), values(policy), null,
                            ipAddress);
                    result = RemoveResult.DELETED;
                }
                connection.commit();
                return result;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void setActive(DiscountPolicyRow policy, boolean active, long actorUserId, String ipAddress)
            throws SQLException {
        if (policy.isActive() == active) {
            return;
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                policyDao.setActive(connection, policy.getId(), active, actorUserId);
                audit(connection, actorUserId, active ? "DISCOUNT_POLICY_ACTIVATE" : "DISCOUNT_POLICY_DEACTIVATE",
                        policy.getId(), activeValues(policy.getCode(), !active), activeValues(policy.getCode(), active),
                        ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void audit(Connection connection, long actorUserId, String action, long id, Map<String, Object> oldValues,
                       Map<String, Object> newValues, String ipAddress) throws SQLException {
        auditLogDao.insert(connection, actorUserId, action, ENTITY, id,
                oldValues == null ? null : JsonUtil.object(oldValues),
                newValues == null ? null : JsonUtil.object(newValues), null, ipAddress);
    }

    static Map<String, Object> values(String code, DiscountPolicyDao.Values values, List<DiscountPolicyRow.Tier> tiers,
                                      String target) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("name", values.name());
        map.put("customerGroupId", values.customerGroupId());
        map.put("scopeType", DiscountPolicyRow.SCOPE_PRODUCT.equals(values.scopeType()) ? "Một SKU" : "Nhóm hàng");
        map.put("target", target);
        map.put("discountType", DiscountPolicyRow.PERCENT.equals(values.discountType()) ? "Phần trăm"
                : "Số tiền trên đơn vị");
        map.put("tiers", tiers.stream().map(DiscountPolicyService::tierText).toList());
        map.put("validFrom", values.validFrom().toString());
        map.put("validTo", values.validTo() == null ? null : values.validTo().toString());
        map.put("active", values.active());
        return map;
    }

    static Map<String, Object> values(DiscountPolicyRow policy) {
        return values(policy.getCode(), new DiscountPolicyDao.Values(policy.getName(), policy.getScopeType(), null,
                policy.getCategoryId(), policy.getDiscountType(), policy.getCustomerGroupId(), policy.getValidFrom(),
                policy.getValidTo(), policy.isActive()), policy.getTiers(),
                policy.isProductScope() ? policy.getTargetCode() : policy.getTargetName());
    }

    // Bậc dạng "24=2" (số lượng tối thiểu = mức chiết khấu) để so trước / sau trên trang Nhật ký
    static String tierText(DiscountPolicyRow.Tier tier) {
        return tier.minQtyBase().stripTrailingZeros().toPlainString() + "=" + tier.value().stripTrailingZeros()
                .toPlainString();
    }

    private static Map<String, Object> activeValues(String code, boolean active) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("active", active);
        return map;
    }

    private static Long parseId(String text) {
        try {
            return text == null ? null : Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return text == null ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

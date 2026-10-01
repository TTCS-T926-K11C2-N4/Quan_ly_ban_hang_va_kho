package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.ProductCategoryDao;
import com.oms.dao.ProductDao;
import com.oms.model.CategoryForm;
import com.oms.model.CategoryRow;
import com.oms.model.PageResult;
import com.oms.model.ProductCategory;
import com.oms.model.ProductSummary;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

// Quản lý nhóm hàng nhiều cấp (S2-06): cây tối đa MAX_LEVEL cấp, xoá chỉ khi nhóm trống,
// chuyển sản phẩm giữa các nhóm và chuyển cả nhánh sang nhóm cha khác. Mọi thay đổi ghi audit_logs.
public class ProductCategoryService {

    public static final int MAX_LEVEL = 5;
    public static final int SORT_ORDER_MAX = 9999;
    public static final int ROOTS_PER_PAGE = 10;

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9._-]{2,30}");
    private static final int NAME_MAX_LENGTH = 150;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final String ENTITY = "PRODUCT_CATEGORY";

    private final ProductCategoryDao categoryDao = new ProductCategoryDao();
    private final ProductDao productDao = new ProductDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Cả cây theo thứ tự hiển thị: cha đứng trước, các con theo sort_order rồi theo tên
    public List<ProductCategory> getTree() throws SQLException {
        return toTreeOrder(categoryDao.findAll());
    }

    // Màn hình cây có tìm kiếm/lọc: nhóm khớp hiện kèm các nhóm cha để không mất ngữ cảnh;
    // phân trang theo nhóm gốc (mỗi trang ROOTS_PER_PAGE nhóm gốc cùng toàn bộ nhánh đang hiện).
    // keyword tìm trong tên và mã; active = null là mọi trạng thái.
    public PageResult<CategoryRow> search(String keyword, Boolean active, int requestedPage) throws SQLException {
        return searchTree(getTree(), keyword, active, requestedPage);
    }

    static PageResult<CategoryRow> searchTree(List<ProductCategory> tree, String keyword, Boolean active,
                                              int requestedPage) {
        String needle = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        Map<Long, ProductCategory> byId = new HashMap<>();
        tree.forEach(category -> byId.put(category.getId(), category));

        Set<Long> matched = new HashSet<>();
        Set<Long> visible = new HashSet<>();
        for (ProductCategory category : tree) {
            boolean keywordOk = needle.isEmpty() || category.getName().toLowerCase(Locale.ROOT).contains(needle)
                    || category.getCode().toLowerCase(Locale.ROOT).contains(needle);
            if (keywordOk && (active == null || category.isActive() == active)) {
                matched.add(category.getId());
                // path /1/5/12/ chứa id mọi nhóm cha
                for (String id : category.getPath().split("/")) {
                    if (!id.isEmpty()) {
                        visible.add(Long.parseLong(id));
                    }
                }
            }
        }

        List<ProductCategory> roots = tree.stream()
                .filter(category -> category.getParentId() == null && visible.contains(category.getId()))
                .toList();
        int totalPages = Math.max(1, (roots.size() + ROOTS_PER_PAGE - 1) / ROOTS_PER_PAGE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        int firstRoot = (page - 1) * ROOTS_PER_PAGE;
        Set<Long> pageRoots = new HashSet<>();
        roots.subList(firstRoot, Math.min(firstRoot + ROOTS_PER_PAGE, roots.size()))
                .forEach(root -> pageRoots.add(root.getId()));

        // STT đánh theo các nhóm đang hiện: nhóm gốc thứ n trong toàn bộ kết quả, con thứ k của nó là n.k
        Map<Long, String> numbers = new HashMap<>();
        Map<Long, Integer> childCounters = new HashMap<>();
        int rootCounter = 0;
        List<CategoryRow> rows = new ArrayList<>();
        for (ProductCategory category : tree) {
            if (!visible.contains(category.getId())) {
                continue;
            }
            String number;
            if (category.getParentId() == null) {
                number = String.valueOf(++rootCounter);
            } else {
                int index = childCounters.merge(category.getParentId(), 1, Integer::sum);
                number = numbers.get(category.getParentId()) + "." + index;
            }
            numbers.put(category.getId(), number);
            if (pageRoots.contains(rootId(category))) {
                rows.add(new CategoryRow(category, number, matched.contains(category.getId())));
            }
        }
        return new PageResult<>(rows, page, ROOTS_PER_PAGE, roots.size());
    }

    private static long rootId(ProductCategory category) {
        return Long.parseLong(category.getPath().split("/")[1]);
    }

    public ProductCategory find(long id) throws SQLException {
        return categoryDao.findById(id);
    }

    public List<ProductSummary> getProducts(long categoryId) throws SQLException {
        return productDao.findByCategory(categoryId);
    }

    // Nhóm có thể chọn làm nhóm cha: đang dùng, không phải chính nhóm đang sửa hay nhóm con cháu của nó,
    // và đặt vào thì cả nhánh không vượt quá MAX_LEVEL cấp. editing = null khi thêm mới.
    public List<ProductCategory> getParentOptions(ProductCategory editing) throws SQLException {
        int branchHeight = editing == null ? 1 : branchHeight(editing);
        List<ProductCategory> options = new ArrayList<>();
        for (ProductCategory category : getTree()) {
            boolean insideEditing = editing != null && (category.getId() == editing.getId()
                    || category.isDescendantOf(editing));
            if (category.isActive() && !insideEditing && category.getLevel() + branchHeight <= MAX_LEVEL) {
                options.add(category);
            }
        }
        return options;
    }

    // Trả về lỗi theo tên ô trong form; rỗng nghĩa là hợp lệ. editing = null khi thêm mới.
    public Map<String, String> validate(CategoryForm form, ProductCategory editing) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        Long editingId = editing == null ? null : editing.getId();

        if (form.getCode() == null) {
            errors.put("code", "Vui lòng nhập mã nhóm hàng.");
        } else if (!CODE_PATTERN.matcher(form.getCode()).matches()) {
            errors.put("code", "Mã nhóm gồm 2–30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        } else if (categoryDao.existsByCode(form.getCode(), editingId)) {
            errors.put("code", "Mã nhóm hàng đã tồn tại.");
        }

        if (form.getParentId() != null) {
            ProductCategory parent = categoryDao.findById(form.getParentId());
            String parentError = parent == null ? "Nhóm cha không tồn tại."
                    : parentError(parent, editing, editing == null ? 1 : branchHeight(editing));
            if (parentError != null) {
                errors.put("parentId", parentError);
            }
        }

        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên nhóm hàng.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên nhóm hàng tối đa " + NAME_MAX_LENGTH + " ký tự.");
        } else if (!errors.containsKey("parentId")
                && categoryDao.existsNameInParent(form.getName(), form.getParentId(), editingId)) {
            errors.put("name", "Trong cùng nhóm cha đã có nhóm hàng tên này.");
        }

        if (form.getDescription() != null && form.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            errors.put("description", "Mô tả tối đa " + DESCRIPTION_MAX_LENGTH + " ký tự.");
        }

        if (parseSortOrder(form.getSortOrder()) == null) {
            errors.put("sortOrder", "Thứ tự hiển thị là số nguyên từ 0 đến " + SORT_ORDER_MAX + ".");
        }
        return errors;
    }

    // Gọi validate trước
    public long create(CategoryForm form, long actorUserId, String ipAddress) throws SQLException {
        ProductCategory parent = form.getParentId() == null ? null : categoryDao.findById(form.getParentId());
        int level = parent == null ? 1 : parent.getLevel() + 1;
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = categoryDao.insert(connection, form.getParentId(), form.getCode(), form.getName(),
                        form.getDescription(), level, parseSortOrder(form.getSortOrder()), actorUserId);
                categoryDao.updatePath(connection, id, childPath(parent == null ? null : parent.getPath(), id));
                auditLogDao.insert(connection, actorUserId, "CATEGORY_CREATE", ENTITY, id, null,
                        toJson(form), null, ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gọi validate trước. Đổi nhóm cha thì chuyển cả nhánh (cập nhật path và cấp của mọi nhóm con cháu).
    public void update(ProductCategory editing, CategoryForm form, long actorUserId, String ipAddress)
            throws SQLException {
        ProductCategory parent = form.getParentId() == null ? null : categoryDao.findById(form.getParentId());
        boolean parentChanged = !Objects.equals(editing.getParentId(), form.getParentId());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                categoryDao.update(connection, editing.getId(), form.getParentId(), form.getCode(), form.getName(),
                        form.getDescription(), parseSortOrder(form.getSortOrder()), actorUserId);
                if (parentChanged) {
                    String newPath = childPath(parent == null ? null : parent.getPath(), editing.getId());
                    int newLevel = parent == null ? 1 : parent.getLevel() + 1;
                    categoryDao.moveBranch(connection, editing.getPath(), newPath, newLevel - editing.getLevel());
                }
                auditLogDao.insert(connection, actorUserId, "CATEGORY_UPDATE", ENTITY, editing.getId(),
                        toJson(editing), toJson(form), null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu xoá được; ngược lại là lý do để hiện cho người dùng (S2-06: nhóm còn sản phẩm thì không xoá)
    public String validateDelete(ProductCategory category) {
        if (category.getProductCount() > 0) {
            return "Nhóm \"" + category.getName() + "\" còn " + category.getProductCount()
                    + " sản phẩm. Hãy chuyển sản phẩm sang nhóm khác trước khi xoá.";
        }
        if (category.getChildCount() > 0) {
            return "Nhóm \"" + category.getName() + "\" còn " + category.getChildCount()
                    + " nhóm con. Hãy xoá hoặc chuyển các nhóm con trước.";
        }
        return null;
    }

    // Trả về false nếu ngay lúc xoá nhóm lại có sản phẩm/nhóm con (người khác vừa thêm)
    public boolean delete(ProductCategory category, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!categoryDao.deleteIfEmpty(connection, category.getId())) {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId, "CATEGORY_DELETE", ENTITY, category.getId(),
                        toJson(category), null, null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Ngừng sử dụng cả nhánh: không chọn được khi thêm/sửa sản phẩm, sản phẩm đang có vẫn giữ nguyên
    public void deactivate(ProductCategory category, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                categoryDao.deactivateBranch(connection, category.getPath(), actorUserId);
                auditLogDao.insert(connection, actorUserId, "CATEGORY_DEACTIVATE", ENTITY, category.getId(),
                        null, null, null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu dùng lại được: nhóm cha phải đang dùng
    public String validateActivate(ProductCategory category) throws SQLException {
        if (category.getParentId() == null) {
            return null;
        }
        ProductCategory parent = categoryDao.findById(category.getParentId());
        return parent != null && parent.isActive() ? null
                : "Nhóm cha \"" + (parent == null ? "" : parent.getName()) + "\" đang ngừng sử dụng. Hãy dùng lại nhóm cha trước.";
    }

    public void activate(ProductCategory category, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                categoryDao.activate(connection, category.getId(), actorUserId);
                auditLogDao.insert(connection, actorUserId, "CATEGORY_ACTIVATE", ENTITY, category.getId(),
                        null, null, null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Lỗi theo tên ô: productIds, targetCategoryId
    public Map<String, String> validateMove(ProductCategory from, List<Long> productIds, Long targetCategoryId)
            throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (productIds.isEmpty()) {
            errors.put("productIds", "Vui lòng chọn ít nhất một sản phẩm.");
        }
        if (targetCategoryId == null) {
            errors.put("targetCategoryId", "Vui lòng chọn nhóm hàng muốn chuyển tới.");
        } else if (targetCategoryId == from.getId()) {
            errors.put("targetCategoryId", "Nhóm chuyển tới phải khác nhóm hiện tại.");
        } else {
            ProductCategory target = categoryDao.findById(targetCategoryId);
            if (target == null) {
                errors.put("targetCategoryId", "Nhóm chuyển tới không tồn tại.");
            } else if (!target.isActive()) {
                errors.put("targetCategoryId", "Nhóm \"" + target.getName() + "\" đang ngừng sử dụng.");
            }
        }
        return errors;
    }

    // Gọi validateMove trước. Trả về số sản phẩm đã chuyển (sản phẩm đã rời nhóm nguồn thì bỏ qua).
    public int moveProducts(ProductCategory from, List<Long> productIds, long targetCategoryId, long actorUserId,
                            String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int moved = productDao.moveToCategory(connection, productIds, from.getId(), targetCategoryId,
                        actorUserId);
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("productIds", productIds);
                change.put("fromCategoryId", from.getId());
                change.put("toCategoryId", targetCategoryId);
                auditLogDao.insert(connection, actorUserId, "PRODUCT_MOVE_CATEGORY", ENTITY, from.getId(),
                        null, JsonUtil.object(change), null, ipAddress);
                connection.commit();
                return moved;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Số cấp của nhánh tính từ chính nhóm này (nhóm không có con = 1)
    private int branchHeight(ProductCategory category) throws SQLException {
        return categoryDao.findMaxLevelInBranch(category.getPath()) - category.getLevel() + 1;
    }

    static String parentError(ProductCategory parent, ProductCategory editing, int branchHeight) {
        if (editing != null && (parent.getId() == editing.getId() || parent.isDescendantOf(editing))) {
            return "Không thể chọn chính nhóm này hoặc nhóm con của nó làm nhóm cha.";
        }
        if (!parent.isActive()) {
            return "Nhóm cha \"" + parent.getName() + "\" đang ngừng sử dụng.";
        }
        if (parent.getLevel() + branchHeight > MAX_LEVEL) {
            return "Cây nhóm hàng tối đa " + MAX_LEVEL + " cấp; đặt vào nhóm này sẽ vượt quá số cấp cho phép.";
        }
        return null;
    }

    static String childPath(String parentPath, long id) {
        return (parentPath == null ? "/" : parentPath) + id + "/";
    }

    // Ô trống = 0; null nếu không phải số nguyên trong khoảng cho phép
    static Integer parseSortOrder(String value) {
        if (value == null) {
            return 0;
        }
        try {
            int sortOrder = Integer.parseInt(value);
            return sortOrder >= 0 && sortOrder <= SORT_ORDER_MAX ? sortOrder : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static List<ProductCategory> toTreeOrder(List<ProductCategory> categories) {
        Map<Long, List<ProductCategory>> children = new HashMap<>();
        for (ProductCategory category : categories) {
            children.computeIfAbsent(category.getParentId() == null ? 0L : category.getParentId(),
                    key -> new ArrayList<>()).add(category);
        }
        Comparator<ProductCategory> siblingOrder = Comparator.comparingInt(ProductCategory::getSortOrder)
                .thenComparing(ProductCategory::getName, String.CASE_INSENSITIVE_ORDER);
        children.values().forEach(list -> list.sort(siblingOrder));
        List<ProductCategory> ordered = new ArrayList<>();
        appendChildren(0L, children, ordered);
        return ordered;
    }

    private static void appendChildren(long parentId, Map<Long, List<ProductCategory>> children,
                                       List<ProductCategory> ordered) {
        for (ProductCategory child : children.getOrDefault(parentId, List.of())) {
            ordered.add(child);
            appendChildren(child.getId(), children, ordered);
        }
    }

    private static String toJson(CategoryForm form) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", form.getCode());
        values.put("name", form.getName());
        values.put("description", form.getDescription());
        values.put("parentId", form.getParentId());
        values.put("sortOrder", parseSortOrder(form.getSortOrder()));
        return JsonUtil.object(values);
    }

    private static String toJson(ProductCategory category) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", category.getCode());
        values.put("name", category.getName());
        values.put("description", category.getDescription());
        values.put("parentId", category.getParentId());
        values.put("sortOrder", category.getSortOrder());
        return JsonUtil.object(values);
    }
}

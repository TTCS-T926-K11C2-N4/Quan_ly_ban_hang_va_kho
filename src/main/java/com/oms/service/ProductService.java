package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.FileObjectDao;
import com.oms.dao.ProductCategoryDao;
import com.oms.dao.ProductDao;
import com.oms.dao.UnitDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.PageResult;
import com.oms.model.Product;
import com.oms.model.ProductCategory;
import com.oms.model.ProductFilter;
import com.oms.model.ProductForm;
import com.oms.model.ProductListItem;
import com.oms.model.SelectOption;
import com.oms.model.StockStatus;
import com.oms.util.AvatarImages;
import com.oms.util.DbConnection;
import com.oms.util.FileStorage;
import com.oms.util.JsonUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// Danh mục sản phẩm (S2-05): SKU duy nhất, giá vốn chỉ người có quyền COST_PRICE_VIEW (Quản lý kinh doanh)
// xem và sửa, sản phẩm đã phát sinh giao dịch thì không xoá được, chỉ ngừng kinh doanh. Mọi thay đổi ghi audit_logs.
public class ProductService {

    public static final int PAGE_SIZE = 10;
    public static final long IMAGE_MAX_BYTES = 2L * 1024 * 1024;
    public static final Set<String> SORTS = Set.of("name", "cost_asc", "cost_desc", "stock_asc", "stock_desc");

    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Z0-9._-]{2,50}");
    private static final int NAME_MAX_LENGTH = 250;
    private static final int PACKAGING_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    // decimal(18,2): tối đa 16 chữ số phần nguyên
    private static final int COST_MAX_DIGITS = 16;
    private static final int IMAGE_SIZE = 512;
    private static final int IMAGE_THUMB_SIZE = 96;
    private static final String ENTITY = "PRODUCT";
    private static final String IMAGE_PURPOSE = "PRODUCT_IMAGE";

    private final ProductDao productDao = new ProductDao();
    private final ProductCategoryDao categoryDao = new ProductCategoryDao();
    private final ProductCategoryService categoryService = new ProductCategoryService();
    private final UnitDao unitDao = new UnitDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final FileObjectDao fileObjectDao = new FileObjectDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Ảnh đã kiểm tra và thu nhỏ, chưa lưu
    public static final class ProductImage {
        private final byte[] full;
        private final byte[] thumb;
        private final String originalName;

        private ProductImage(byte[] full, byte[] thumb, String originalName) {
            this.full = full;
            this.thumb = thumb;
            this.originalName = originalName;
        }
    }

    // categoryId: lọc theo nhóm và mọi nhóm con cháu. Người không xem được giá vốn thì không sắp xếp theo giá vốn.
    public PageResult<ProductListItem> search(String keyword, Long categoryId, Long warehouseId,
                                              StockStatus status, String sort, int requestedPage,
                                              boolean canViewCost) throws SQLException {
        ProductCategory category = categoryId == null ? null : categoryDao.findById(categoryId);
        if (categoryId != null && category == null) {
            // Lọc theo nhóm không tồn tại (sửa tay địa chỉ) thì không có sản phẩm nào, không phải mọi sản phẩm
            return new PageResult<>(List.of(), 1, PAGE_SIZE, 0);
        }
        // Set.of(...).contains(null) ném NullPointerException
        String effectiveSort = sort != null && SORTS.contains(sort) &&(canViewCost || !sort.startsWith("cost")) ? sort : "name";
        ProductFilter filter = new ProductFilter(keyword, category == null ? null : category.getPath(), warehouseId,
                status, effectiveSort);
        long total = productDao.count(filter);
        int totalPages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        List<ProductListItem> items = productDao.findPage(filter, canViewCost, (page - 1) * PAGE_SIZE, PAGE_SIZE);
        return new PageResult<>(items, page, PAGE_SIZE, total);
    }

    // Cả cây nhóm hàng (để lọc)
    public List<ProductCategory> getCategoryTree() throws SQLException {
        return categoryService.getTree();
    }

    // Nhóm chọn được khi thêm/sửa: đang dùng, cộng nhóm hiện tại của sản phẩm (kể cả khi nhóm đó đã ngừng)
    public List<ProductCategory> getCategoryOptions(Long currentCategoryId) throws SQLException {
        return categoryService.getTree().stream()
                .filter(category -> category.isActive() || Objects.equals(category.getId(), currentCategoryId))
                .toList();
    }

    public List<SelectOption> getUnits() throws SQLException {
        return unitDao.findAll();
    }

    public List<SelectOption> getWarehouses() throws SQLException {
        return warehouseDao.findActive();
    }

    public Product find(long id, boolean canViewCost) throws SQLException {
        return productDao.findById(id, canViewCost);
    }

    public boolean hasTransactions(long productId) throws SQLException {
        return productDao.hasTransactions(productId);
    }

    // Lỗi theo tên ô; rỗng nghĩa là hợp lệ. editing = null khi thêm mới. canEditCost = false thì bỏ qua ô giá vốn.
    public Map<String, String> validate(ProductForm form, Product editing, boolean canEditCost) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        Long editingId = editing == null ? null : editing.getId();

        String skuError = skuError(form.getSku());
        if (skuError != null) {
            errors.put("sku", skuError);
        } else if (productDao.existsBySku(form.getSku(), editingId)) {
            errors.put("sku", "Mã SKU đã tồn tại.");
        }

        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên sản phẩm.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên sản phẩm tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }

        if (form.getCategoryId() == null) {
            errors.put("categoryId", "Vui lòng chọn nhóm hàng.");
        } else {
            ProductCategory category = categoryDao.findById(form.getCategoryId());
            boolean unchanged = editing != null && editing.getCategoryId() == form.getCategoryId();
            if (category == null) {
                errors.put("categoryId", "Nhóm hàng không tồn tại.");
            } else if (!category.isActive() && !unchanged) {
                errors.put("categoryId", "Nhóm \"" + category.getName() + "\" đang ngừng hoạt động.");
            }
        }

        if (form.getBaseUnitId() == null) {
            errors.put("baseUnitId", "Vui lòng chọn đơn vị tính cơ sở.");
        } else if (!unitDao.exists(form.getBaseUnitId())) {
            errors.put("baseUnitId", "Đơn vị tính không tồn tại.");
        } else if (editing != null && editing.getBaseUnitId() != form.getBaseUnitId()
                && productDao.hasTransactions(editing.getId())) {
            // Sổ tồn và chứng từ cũ lưu số lượng theo đơn vị cơ sở cũ
            errors.put("baseUnitId", "Sản phẩm đã phát sinh giao dịch nên không đổi được đơn vị tính cơ sở.");
        }

        if (form.getPackagingSpec() != null && form.getPackagingSpec().length() > PACKAGING_MAX_LENGTH) {
            errors.put("packagingSpec", "Quy cách đóng gói tối đa " + PACKAGING_MAX_LENGTH + " ký tự.");
        }

        if (canEditCost && parseCostPrice(form.getCostPrice()) == null) {
            errors.put("costPrice", "Giá vốn là số tiền nguyên, không âm, tối đa " + COST_MAX_DIGITS + " chữ số.");
        }

        if (!Product.ACTIVE.equals(form.getStatus()) && !Product.DISCONTINUED.equals(form.getStatus())) {
            errors.put("status", "Vui lòng chọn trạng thái.");
        }

        if (form.getDescription() != null && form.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            errors.put("description", "Mô tả tối đa " + DESCRIPTION_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    // Kiểm tra và thu nhỏ ảnh trước khi ghi CSDL; content rỗng = không đổi ảnh (trả về null)
    public ProductImage prepareImage(byte[] content, String originalName)
            throws AvatarImages.InvalidImageException, IOException {
        if (content == null || content.length == 0) {
            return null;
        }
        if (content.length > IMAGE_MAX_BYTES) {
            throw new AvatarImages.InvalidImageException("Ảnh vượt quá dung lượng tối đa 2MB.");
        }
        BufferedImage square = AvatarImages.readSquare(content);
        return new ProductImage(AvatarImages.resizePng(square, IMAGE_SIZE),
                AvatarImages.resizePng(square, IMAGE_THUMB_SIZE), originalName);
    }

    // Gọi validate trước. canEditCost = false thì giá vốn để mặc định 0.
    public long create(ProductForm form, ProductImage image, boolean canEditCost, long actorUserId, String ipAddress)
            throws SQLException, IOException {
        BigDecimal costPrice = canEditCost ? parseCostPrice(form.getCostPrice()) : null;
        String imageKey = null;
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = productDao.insert(connection, form, costPrice, actorUserId);
                productDao.replaceBaseUnit(connection, id, form.getBaseUnitId(), actorUserId);
                if (image != null) {
                    imageKey = saveImage(connection, id, image, actorUserId);
                }
                auditLogDao.insert(connection, actorUserId, "PRODUCT_CREATE", ENTITY, id, null,
                        toJson(form, costPrice != null && costPrice.signum() != 0), null, ipAddress);
                connection.commit();
                return id;
            } catch (SQLException | IOException e) {
                connection.rollback();
                deleteImageQuietly(imageKey);
                throw e;
            }
        }
    }

    // Gọi validate trước. Trả về false nếu người khác vừa sửa sản phẩm (version đã đổi), khi đó không lưu gì.
    public boolean update(Product editing, ProductForm form, ProductImage image, boolean canEditCost,
                          long actorUserId, String ipAddress) throws SQLException, IOException {
        BigDecimal costPrice = canEditCost ? parseCostPrice(form.getCostPrice()) : null;
        boolean costChanged = costPrice != null && editing.getCostPrice() != null
                && costPrice.compareTo(editing.getCostPrice()) != 0;
        String imageKey = null;
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!productDao.update(connection, editing.getId(), form.getVersion(), form, costPrice, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                if (editing.getBaseUnitId() != form.getBaseUnitId()) {
                    productDao.replaceBaseUnit(connection, editing.getId(), form.getBaseUnitId(), actorUserId);
                }
                if (image != null) {
                    imageKey = saveImage(connection, editing.getId(), image, actorUserId);
                }
                Map<String, Object> newValues = toValues(form, costChanged);
                newValues.put("imageChanged", image != null);
                auditLogDao.insert(connection, actorUserId, "PRODUCT_UPDATE", ENTITY, editing.getId(),
                        JsonUtil.object(toValues(editing)), JsonUtil.object(newValues), null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException | IOException e) {
                connection.rollback();
                deleteImageQuietly(imageKey);
                throw e;
            }
        }
    }

    // null nếu xoá được; ngược lại là lý do hiện cho người dùng
    public String validateDelete(Product product) throws SQLException {
        return productDao.hasTransactions(product.getId())
                ? "Sản phẩm \"" + product.getName() + "\" đã phát sinh giao dịch nên không xoá được. "
                        + "Hãy chuyển sang Ngừng kinh doanh."
                : null;
    }

    // Gọi validateDelete trước. Ném SQLIntegrityConstraintViolationException nếu bảng giá/chiết khấu còn dùng sản phẩm.
    public void delete(Product product, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (productDao.delete(connection, product.getId())) {
                    auditLogDao.insert(connection, actorUserId, "PRODUCT_DELETE", ENTITY, product.getId(),
                            JsonUtil.object(toValues(product)), null, null, ipAddress);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // status: ACTIVE | DISCONTINUED. Trả về false nếu sản phẩm đã ở trạng thái đó.
    public boolean changeStatus(Product product, String status, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!productDao.updateStatus(connection, product.getId(), status, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId,
                        Product.DISCONTINUED.equals(status) ? "PRODUCT_DISCONTINUE" : "PRODUCT_ACTIVATE", ENTITY,
                        product.getId(), null, null, null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Đường dẫn tệp ảnh sản phẩm; null nếu chưa có ảnh hoặc tệp đã mất
    public Path findImagePath(long productId, boolean thumbnail) throws SQLException {
        String key = productDao.findImageKey(productId);
        if (key == null) {
            return null;
        }
        Path path = FileStorage.resolve(thumbnail ? AvatarService.thumbKey(key) : key);
        return Files.isRegularFile(path) ? path : null;
    }

    private String saveImage(Connection connection, long productId, ProductImage image, long actorUserId)
            throws SQLException, IOException {
        String key = "products/" + productId + "-" + UUID.randomUUID() + ".png";
        FileStorage.save(key, image.full);
        FileStorage.save(AvatarService.thumbKey(key), image.thumb);
        long fileId = fileObjectDao.insert(connection, key, image.originalName, "image/png", image.full.length,
                IMAGE_PURPOSE, actorUserId);
        productDao.updateImage(connection, productId, fileId, actorUserId);
        return key;
    }

    private static void deleteImageQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            Files.deleteIfExists(FileStorage.resolve(key));
            Files.deleteIfExists(FileStorage.resolve(AvatarService.thumbKey(key)));
        } catch (IOException e) {
            // Tệp mồ côi không ảnh hưởng chức năng, chỉ tốn chỗ
        }
    }

    static String skuError(String sku) {
        if (sku == null) {
            return "Vui lòng nhập mã SKU.";
        }
        return SKU_PATTERN.matcher(sku).matches() ? null
                : "Mã SKU gồm 2–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.";
    }

    // Tiền đồng, số nguyên; cho phép gõ dấu chấm/phẩy/khoảng trắng phân cách hàng nghìn (vd 30.000).
    // Ô trống = 0. null nếu không hợp lệ.
    static BigDecimal parseCostPrice(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String digits = value.replaceAll("[.,\\s]", "");
        if (!digits.matches("\\d{1," + COST_MAX_DIGITS + "}")) {
            return null;
        }
        return new BigDecimal(digits);
    }

    // Giá vốn không ghi vào nhật ký (Admin xem nhật ký nhưng không được xem giá vốn), chỉ ghi là có đổi hay không
    private static String toJson(ProductForm form, boolean costChanged) {
        return JsonUtil.object(toValues(form, costChanged));
    }

    private static Map<String, Object> toValues(ProductForm form, boolean costChanged) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sku", form.getSku());
        values.put("name", form.getName());
        values.put("categoryId", form.getCategoryId());
        values.put("baseUnitId", form.getBaseUnitId());
        values.put("packagingSpec", form.getPackagingSpec());
        values.put("status", form.getStatus());
        values.put("description", form.getDescription());
        values.put("costPriceChanged", costChanged);
        return values;
    }

    private static Map<String, Object> toValues(Product product) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sku", product.getSku());
        values.put("name", product.getName());
        values.put("categoryId", product.getCategoryId());
        values.put("baseUnitId", product.getBaseUnitId());
        values.put("packagingSpec", product.getPackagingSpec());
        values.put("status", product.getStatus());
        values.put("description", product.getDescription());
        return values;
    }
}

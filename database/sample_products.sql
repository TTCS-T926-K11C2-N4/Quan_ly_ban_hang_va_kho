-- =====================================================================
-- Đơn vị tính và sản phẩm mẫu (S2-05) để dựng và thử màn hình Danh mục sản phẩm.
-- Chạy SAU database/sample_categories.sql (sản phẩm gắn vào nhóm hàng theo mã nhóm).
-- Chạy lại lần 2 không lỗi: đơn vị và SKU đã có thì bỏ qua (INSERT IGNORE theo cột UNIQUE).
-- Chưa có tồn kho: tồn chỉ có khi nhập kho (EP-05), nên mọi sản phẩm hiện "Hết hàng".
-- =====================================================================

SET NAMES utf8mb4;

INSERT IGNORE INTO `units` (`code`, `name`, `created_at`, `updated_at`)
VALUES
  ('LON',   'Lon',   UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('CHAI',  'Chai',  UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('GOI',   'Gói',   UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('HOP',   'Hộp',   UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('LOC',   'Lốc',   UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('THUNG', 'Thùng', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('KG',    'Kg',    UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('CAI',   'Cái',   UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT IGNORE INTO `products` (`sku`, `name`, `category_id`, `base_unit_id`, `packaging_spec`, `cost_price`, `status`,
                               `created_at`, `updated_at`)
SELECT p.sku, p.name, c.id, u.id, p.packaging_spec, p.cost_price, p.status, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'SP001' AS sku, 'Bia Hà Nội lon 330ml' AS name, 'BIA-LON' AS category_code, 'LON' AS unit_code,
             'Thùng 24 lon x 330ml' AS packaging_spec, 9500 AS cost_price, 'ACTIVE' AS status
      UNION ALL SELECT 'SP002', 'Bia Sài Gòn Special lon 330ml', 'BIA-LON', 'LON', 'Thùng 24 lon x 330ml', 12000, 'ACTIVE'
      UNION ALL SELECT 'SP003', 'Bia Tiger lon 500ml', 'BIA-LON', 'LON', 'Thùng 12 lon x 500ml', 17500, 'ACTIVE'
      UNION ALL SELECT 'SP004', 'Bia Hà Nội chai 450ml', 'BIA-CHAI', 'CHAI', 'Két 20 chai x 450ml', 8500, 'ACTIVE'
      UNION ALL SELECT 'SP005', 'Bia Trúc Bạch chai 330ml', 'BIA-CHAI', 'CHAI', 'Két 20 chai x 330ml', 14000, 'DISCONTINUED'
      UNION ALL SELECT 'SP006', 'Coca-Cola lon 320ml', 'NN-CO-GA', 'LON', 'Thùng 24 lon x 320ml', 7200, 'ACTIVE'
      UNION ALL SELECT 'SP007', 'Pepsi chai 390ml', 'NN-CO-GA', 'CHAI', 'Thùng 24 chai x 390ml', 6800, 'ACTIVE'
      UNION ALL SELECT 'SP008', '7Up lon 320ml', 'NN-CO-GA', 'LON', 'Thùng 24 lon x 320ml', 7000, 'ACTIVE'
      UNION ALL SELECT 'SP009', 'Trà xanh Không Độ chai 455ml', 'NN-KHONG-GA', 'CHAI', 'Thùng 24 chai x 455ml', 7800, 'ACTIVE'
      UNION ALL SELECT 'SP010', 'Nước cam Teppy chai 327ml', 'NN-KHONG-GA', 'CHAI', 'Thùng 24 chai x 327ml', 6500, 'ACTIVE'
      UNION ALL SELECT 'SP011', 'Nước suối Lavie chai 500ml', 'NUOC-SUOI', 'CHAI', 'Thùng 24 chai x 500ml', 3200, 'ACTIVE'
      UNION ALL SELECT 'SP012', 'Nước khoáng Vĩnh Hảo chai 1.5L', 'NUOC-SUOI', 'CHAI', 'Thùng 12 chai x 1.5L', 7500, 'ACTIVE'
      UNION ALL SELECT 'SP013', 'Bánh quy Cosy hộp 288g', 'BANH-QUY', 'HOP', 'Thùng 12 hộp x 288g', 32000, 'ACTIVE'
      UNION ALL SELECT 'SP014', 'Bánh xốp Hello Panda gói 50g', 'BANH-QUY', 'GOI', 'Thùng 48 gói x 50g', 8500, 'ACTIVE'
      UNION ALL SELECT 'SP015', 'Kẹo Alpenliebe gói 120g', 'KEO', 'GOI', 'Thùng 24 gói x 120g', 16000, 'ACTIVE'
      UNION ALL SELECT 'SP016', 'Kẹo dẻo Chupa Chups gói 90g', 'KEO', 'GOI', 'Thùng 30 gói x 90g', 14500, 'ACTIVE'
      UNION ALL SELECT 'SP017', 'Snack Oishi tôm cay gói 40g', 'BANH-KEO', 'GOI', 'Thùng 50 gói x 40g', 4200, 'ACTIVE'
      UNION ALL SELECT 'SP018', 'Nước mắm Nam Ngư chai 500ml', 'GIA-VI', 'CHAI', 'Thùng 12 chai x 500ml', 28000, 'ACTIVE'
      UNION ALL SELECT 'SP019', 'Dầu ăn Neptune chai 1L', 'GIA-VI', 'CHAI', 'Thùng 12 chai x 1L', 52000, 'ACTIVE'
      UNION ALL SELECT 'SP020', 'Hạt nêm Knorr gói 400g', 'GIA-VI', 'GOI', 'Thùng 24 gói x 400g', 33000, 'ACTIVE'
      UNION ALL SELECT 'SP021', 'Tương ớt Chinsu chai 250g', 'GIA-VI', 'CHAI', 'Thùng 24 chai x 250g', 12500, 'ACTIVE'
      UNION ALL SELECT 'SP022', 'Đường kính trắng Biên Hòa', 'GIA-VI', 'KG', 'Bao 50kg', 21000, 'ACTIVE') p
JOIN `product_categories` c ON c.code = p.category_code
JOIN `units` u ON u.code = p.unit_code;

-- Đơn vị cơ sở cũng là dòng is_base của bảng quy đổi product_units (hệ số 1), giống khi thêm sản phẩm trên giao diện
INSERT IGNORE INTO `product_units` (`product_id`, `unit_id`, `factor_to_base`, `is_base`, `created_at`, `updated_at`)
SELECT p.id, p.base_unit_id, 1, true, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM `products` p
WHERE p.sku BETWEEN 'SP001' AND 'SP022';

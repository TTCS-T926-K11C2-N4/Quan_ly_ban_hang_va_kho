-- =====================================================================
-- Nhóm hàng mẫu 3 cấp (S2-06) để dựng và thử màn hình Quản lý nhóm hàng.
-- Chạy SAU OMS_schema_mysql.sql (đã có cột description, hoặc đã chạy
-- database/migrations/001_product_categories_description.sql). Chạy lại lần 2 sẽ lỗi trùng code.
-- path dạng /id_cha/id/ chỉ biết sau khi có id nên điền ở cuối file theo từng cấp.
-- =====================================================================

SET NAMES utf8mb4;

-- Cấp 1
INSERT INTO `product_categories` (`parent_id`, `code`, `name`, `description`, `level`, `path`, `sort_order`, `created_at`, `updated_at`)
VALUES
  (NULL, 'DO-UONG',  'Đồ uống',   'Bia, nước ngọt, nước suối, nước tăng lực', 1, '', 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (NULL, 'BANH-KEO', 'Bánh kẹo',  'Bánh quy, kẹo, snack',                      1, '', 2, UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (NULL, 'GIA-VI',   'Gia vị',    'Nước mắm, dầu ăn, bột nêm',                 1, '', 3, UTC_TIMESTAMP(), UTC_TIMESTAMP());

-- Cấp 2
INSERT INTO `product_categories` (`parent_id`, `code`, `name`, `description`, `level`, `path`, `sort_order`, `created_at`, `updated_at`)
SELECT p.id, c.code, c.name, c.description, 2, '', c.sort_order, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'DO-UONG' AS parent_code, 'BIA' AS code, 'Bia' AS name, 'Bia các loại' AS description, 1 AS sort_order
      UNION ALL SELECT 'DO-UONG', 'NUOC-NGOT', 'Nước ngọt', 'Nước ngọt có ga và không ga', 2
      UNION ALL SELECT 'DO-UONG', 'NUOC-SUOI', 'Nước suối', 'Nước khoáng, nước tinh khiết', 3
      UNION ALL SELECT 'BANH-KEO', 'BANH-QUY', 'Bánh quy', 'Bánh quy, bánh xốp', 1
      UNION ALL SELECT 'BANH-KEO', 'KEO', 'Kẹo', 'Kẹo cứng, kẹo mềm', 2) c
JOIN `product_categories` p ON p.code = c.parent_code;

-- Cấp 3
INSERT INTO `product_categories` (`parent_id`, `code`, `name`, `description`, `level`, `path`, `sort_order`, `created_at`, `updated_at`)
SELECT p.id, c.code, c.name, c.description, 3, '', c.sort_order, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'BIA' AS parent_code, 'BIA-LON' AS code, 'Bia lon' AS name, 'Bia đóng lon 330ml, 500ml' AS description, 1 AS sort_order
      UNION ALL SELECT 'BIA', 'BIA-CHAI', 'Bia chai', 'Bia chai thuỷ tinh', 2
      UNION ALL SELECT 'NUOC-NGOT', 'NN-CO-GA', 'Có ga', 'Nước ngọt có ga', 1
      UNION ALL SELECT 'NUOC-NGOT', 'NN-KHONG-GA', 'Không ga', 'Trà, nước trái cây', 2) c
JOIN `product_categories` p ON p.code = c.parent_code;

-- Điền path theo thứ tự cấp: cấp 1 trước rồi mới tới con của nó
UPDATE `product_categories` SET `path` = CONCAT('/', `id`, '/') WHERE `parent_id` IS NULL AND `path` = '';
UPDATE `product_categories` c JOIN `product_categories` p ON p.id = c.parent_id
SET c.`path` = CONCAT(p.`path`, c.`id`, '/') WHERE c.`level` = 2 AND c.`path` = '';
UPDATE `product_categories` c JOIN `product_categories` p ON p.id = c.parent_id
SET c.`path` = CONCAT(p.`path`, c.`id`, '/') WHERE c.`level` = 3 AND c.`path` = '';

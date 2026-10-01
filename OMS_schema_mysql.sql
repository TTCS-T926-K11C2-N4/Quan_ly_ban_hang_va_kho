-- =====================================================================
-- HỆ THỐNG BÁN HÀNG & KHO (OMS) - LƯỢC ĐỒ CƠ SỞ DỮ LIỆU MYSQL 8.0
-- Sinh từ CÙNG một mô hình (schema.py) với bản PostgreSQL và với file ERD draw.io
-- (OMS_ERD.drawio) — tên bảng/cột/khoá giống hệt nhau, chỉ khác cú pháp DDL.
-- 69 bảng / 9 epic. Đã chạy thử trên MySQL 8.0.46.
--
-- KHÁC BIỆT SO VỚI BẢN POSTGRESQL (đọc trước khi dùng)
--  * timestamptz -> DATETIME: MySQL DATETIME không tự quy đổi múi giờ. Ứng dụng PHẢI luôn
--    ghi và đọc giá trị theo UTC (Java: Instant/OffsetDateTime giờ UTC; Node: new Date().toISOString()).
--  * "Chỉ 1 dòng cờ=true" (vd 1 lô mặc định/SKU, 1 địa chỉ giao mặc định/đại lý, 1 giữ chỗ ACTIVE/dòng đơn)
--    dùng cột phụ *_key sinh tự động (NULL khi không thoả) + UNIQUE trên cột đó, vì MySQL không có
--    partial index như Postgres. Đừng tự ý ghi vào các cột *_key này - để DB tự tính.
--  * Bảng giá không chồng ngày hiệu lực (EXCLUDE constraint ở bản Postgres) -> 2 TRIGGER kiểm tra
--    khi INSERT/UPDATE, ở cuối file. Trigger này CÓ dùng DELIMITER - nếu chạy bằng Flyway/Liquibase,
--    nên tách 2 trigger đó ra 1 file migration riêng.
--  * Tìm SKU/đại lý gần đúng theo tên: dùng FULLTEXT INDEX (MySQL) thay vì pg_trgm; khác Postgres ở
--    chỗ khớp theo TỪ, không phải theo chuỗi con - vd "Coca 330" gõ "coca" ra kết quả, gõ "oca" thì không.
--  * Toàn bộ InnoDB + utf8mb4 để lưu được tiếng Việt có dấu trong dữ liệu lẫn phần COMMENT.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ---------------------------------------------------------------------
-- EP-01: Tài khoản, Phân quyền & Nền tảng dùng chung
-- ---------------------------------------------------------------------

CREATE TABLE `users` (
  `id` bigint AUTO_INCREMENT,
  `username` varchar(50) NOT NULL UNIQUE,
  `email` varchar(150) NOT NULL,
  `phone` varchar(20),
  `full_name` varchar(150) NOT NULL,
  `password_hash` varchar(100) NOT NULL COMMENT 'bcrypt',
  `avatar_file_id` bigint,
  `customer_id` bigint COMMENT 'Chỉ có giá trị với tài khoản vai trò CUSTOMER. 1 đại lý có thể có nhiều tài khoản.',
  `status` varchar(30) NOT NULL DEFAULT 'PENDING' CHECK (`status` IN ('PENDING','ACTIVE','LOCKED')) COMMENT 'PENDING | ACTIVE | LOCKED',
  `must_change_password` boolean NOT NULL DEFAULT true,
  `failed_login_count` integer NOT NULL DEFAULT 0 COMMENT 'Sai 5 lần liên tiếp -> locked_until = now()+15 phút',
  `locked_until` datetime,
  `lock_reason` text COMMENT 'Bắt buộc khi khoá tài khoản (S1-10)',
  `last_login_at` datetime,
  `password_changed_at` datetime,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_users_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_users_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_users_email ((LOWER(`email`))),
  KEY ix_users_avatar_file_id (`avatar_file_id`),
  KEY ix_users_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tài khoản đăng nhập của nhân viên nội bộ VÀ của đại lý (cổng đặt hàng). Không xoá cứng; khoá bằng status.';

CREATE TABLE `roles` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(100) NOT NULL,
  `description` text,
  `is_system` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_roles_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_roles_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='7 vai trò: CUSTOMER, SALES_REP, SALES_MANAGER, WAREHOUSE, WH_MANAGER, ACCOUNTANT, ADMIN.';

CREATE TABLE `permissions` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(60) NOT NULL UNIQUE,
  `module` varchar(40) NOT NULL COMMENT 'Sản phẩm & bảng giá, Đơn hàng, Tồn kho, ...',
  `action` varchar(20) NOT NULL COMMENT 'VIEW | CREATE | UPDATE | APPROVE | ...',
  `description` varchar(200),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Danh mục quyền chi tiết (vd ORDER_APPROVE). Ánh xạ từ ma trận F/W/R trong sheet User Roles.';

CREATE TABLE `role_permissions` (
  `role_id` bigint,
  CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`),
  `permission_id` bigint,
  CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (`permission_id`) REFERENCES `permissions`(`id`),
  `data_scope` varchar(30) NOT NULL DEFAULT 'ALL' CHECK (`data_scope` IN ('ALL','ASSIGNED','OWN')) COMMENT 'ALL | ASSIGNED | OWN',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY ix_role_permissions_role_id (`role_id`),
  KEY ix_role_permissions_permission_id (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Vai trò có quyền gì. data_scope thể hiện dấu * trong ma trận (chỉ dữ liệu của mình/nhóm mình phụ trách).';

CREATE TABLE `user_roles` (
  `user_id` bigint,
  CONSTRAINT fk_user_roles_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `role_id` bigint,
  CONSTRAINT fk_user_roles_role_id FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`),
  `assigned_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `assigned_by` bigint,
  CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (`assigned_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`user_id`, `role_id`),
  KEY ix_user_roles_user_id (`user_id`),
  KEY ix_user_roles_role_id (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Một người có thể giữ nhiều vai trò (S1-09).';

CREATE TABLE `user_warehouses` (
  `user_id` bigint,
  CONSTRAINT fk_user_warehouses_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `warehouse_id` bigint,
  PRIMARY KEY (`user_id`, `warehouse_id`),
  KEY ix_user_warehouses_user_id (`user_id`),
  KEY ix_user_warehouses_warehouse_id (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Thủ kho chỉ thao tác trên kho mình phụ trách (S1-09).';

CREATE TABLE `user_regions` (
  `user_id` bigint,
  CONSTRAINT fk_user_regions_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `region_id` bigint,
  PRIMARY KEY (`user_id`, `region_id`),
  KEY ix_user_regions_user_id (`user_id`),
  KEY ix_user_regions_region_id (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhân viên kinh doanh được gắn địa bàn (S1-09, S3-06).';

CREATE TABLE `refresh_tokens` (
  `id` bigint AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `token_hash` varchar(64) NOT NULL UNIQUE,
  `issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime NOT NULL,
  `revoked_at` datetime,
  `revoke_reason` varchar(30),
  `user_agent` varchar(255),
  `ip_address` varchar(45),
  PRIMARY KEY (`id`),
  KEY ix_refresh_tokens_user (`user_id`, `revoked_at`),
  KEY ix_refresh_tokens_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='JWT refresh token (lưu hash). Đăng xuất / đổi mật khẩu -> đặt revoked_at (S1-02, S1-04).';

CREATE TABLE `user_tokens` (
  `id` bigint AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  CONSTRAINT fk_user_tokens_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `purpose` varchar(30) NOT NULL CHECK (`purpose` IN ('PASSWORD_RESET','ACCOUNT_ACTIVATION')) COMMENT 'PASSWORD_RESET | ACCOUNT_ACTIVATION',
  `token_hash` varchar(64) NOT NULL UNIQUE,
  `expires_at` datetime NOT NULL,
  `used_at` datetime,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_user_tokens_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_user_tokens_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Token dùng một lần: đặt lại mật khẩu (30 phút) và kích hoạt tài khoản (S1-03, S1-08).';

CREATE TABLE `file_objects` (
  `id` bigint AUTO_INCREMENT,
  `storage_key` varchar(300) NOT NULL UNIQUE,
  `original_name` varchar(255) NOT NULL,
  `content_type` varchar(100) NOT NULL,
  `size_bytes` bigint NOT NULL,
  `purpose` varchar(30) NOT NULL CHECK (`purpose` IN ('AVATAR','PRODUCT_IMAGE','DELIVERY_PROOF','RECEIPT','DOCUMENT')) COMMENT 'AVATAR | PRODUCT_IMAGE | DELIVERY_PROOF | RECEIPT | DOCUMENT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_file_objects_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Metadata tệp lưu ở dịch vụ lưu trữ đối tượng: ảnh đại diện, ảnh sản phẩm, ảnh chứng từ giao hàng, biên lai.';

CREATE TABLE `notifications` (
  `id` bigint AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  CONSTRAINT fk_notifications_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `type` varchar(40) NOT NULL,
  `title` varchar(200) NOT NULL,
  `body` text,
  `ref_type` varchar(40),
  `ref_id` bigint,
  `read_at` datetime,
  `emailed_at` datetime,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_notifications_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_notifications_user_unread (`user_id`, `read_at`, `created_at`),
  KEY ix_notifications_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Thông báo trong ứng dụng (nhắc hạn thanh toán, cảnh báo bàn giao đại lý, ...).';

CREATE TABLE `document_sequences` (
  `doc_type` varchar(20) COMMENT 'ORDER | INVOICE | RECEIPT | DELIVERY | ...',
  `period_key` varchar(10) COMMENT '''2026'' hoặc ''202609''; '''' nếu không reset',
  `prefix` varchar(10) NOT NULL,
  `last_number` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`doc_type`, `period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cấp số chứng từ liên tục, không nhảy cóc (hoá đơn S7-01). Cấp số bằng SELECT ... FOR UPDATE trong cùng transaction với việc tạo chứng từ.';

CREATE TABLE `system_settings` (
  `key` varchar(80),
  `value` text NOT NULL,
  `description` varchar(300),
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_system_settings_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_system_settings_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cấu hình chạy được (ngưỡng khoá đăng nhập, ngưỡng lô sắp hết hạn, hành vi khi đại lý quá hạn: chặn hay chuyển duyệt, ...).';

CREATE TABLE `audit_logs` (
  `id` bigint AUTO_INCREMENT,
  `actor_user_id` bigint,
  CONSTRAINT fk_audit_logs_actor_user_id FOREIGN KEY (`actor_user_id`) REFERENCES `users`(`id`),
  `action` varchar(40) NOT NULL,
  `entity_type` varchar(60) NOT NULL,
  `entity_id` bigint,
  `old_values` json,
  `new_values` json,
  `reason` text,
  `ip_address` varchar(45),
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY ix_audit_entity (`entity_type`, `entity_id`),
  KEY ix_audit_actor_time (`actor_user_id`, `occurred_at`),
  KEY ix_audit_time (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhật ký thao tác trên tồn kho, giá, hạn mức công nợ, hoá đơn (S2-04). CHỈ INSERT.';

-- ---------------------------------------------------------------------
-- EP-02: Danh mục Sản phẩm & Bảng giá
-- ---------------------------------------------------------------------

CREATE TABLE `product_categories` (
  `id` bigint AUTO_INCREMENT,
  `parent_id` bigint,
  CONSTRAINT fk_product_categories_parent_id FOREIGN KEY (`parent_id`) REFERENCES `product_categories`(`id`),
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(150) NOT NULL,
  `description` varchar(500),
  `level` integer NOT NULL DEFAULT 1,
  `path` varchar(300) NOT NULL COMMENT 'Materialized path, vd ''/1/5/12/'' để lấy cả nhánh bằng LIKE',
  `sort_order` integer NOT NULL DEFAULT 0,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_product_categories_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_product_categories_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_product_categories_parent_id (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhóm hàng dạng cây, tối thiểu 3 cấp (S2-06).';

CREATE TABLE `units` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(20) NOT NULL UNIQUE,
  `name` varchar(50) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_units_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_units_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Danh mục tên đơn vị tính: lon, lốc, thùng, kg, ...';

CREATE TABLE `products` (
  `id` bigint AUTO_INCREMENT,
  `sku` varchar(50) NOT NULL UNIQUE,
  `name` varchar(250) NOT NULL,
  `category_id` bigint NOT NULL,
  CONSTRAINT fk_products_category_id FOREIGN KEY (`category_id`) REFERENCES `product_categories`(`id`),
  `base_unit_id` bigint NOT NULL COMMENT 'Đơn vị cơ sở - mọi sổ tồn/sổ nợ lưu theo đơn vị này',
  CONSTRAINT fk_products_base_unit_id FOREIGN KEY (`base_unit_id`) REFERENCES `units`(`id`),
  `packaging_spec` varchar(100),
  `cost_price` decimal(18,2) NOT NULL DEFAULT 0 COMMENT 'NHẠY CẢM: chỉ SALES_MANAGER được xem/sửa. Che ở tầng service, không trả ra DTO của vai trò khác.',
  `image_file_id` bigint,
  CONSTRAINT fk_products_image_file_id FOREIGN KEY (`image_file_id`) REFERENCES `file_objects`(`id`),
  `is_lot_tracked` boolean NOT NULL DEFAULT false COMMENT 'Bật quản lý lô/HSD (S6-01). SKU không bật dùng lô mặc định.',
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE' CHECK (`status` IN ('ACTIVE','DISCONTINUED')) COMMENT 'ACTIVE | DISCONTINUED',
  `description` text,
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'Khoá lạc quan (optimistic lock)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_products_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_products_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_products_1 CHECK (cost_price >= 0),
  KEY ix_products_category (`category_id`),
  FULLTEXT KEY ftx_products_name (`name`),
  KEY ix_products_category_id (`category_id`),
  KEY ix_products_base_unit_id (`base_unit_id`),
  KEY ix_products_image_file_id (`image_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU. Đã phát sinh giao dịch thì không xoá, chỉ chuyển DISCONTINUED (S2-05).';

CREATE TABLE `product_units` (
  `id` bigint AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_product_units_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_product_units_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `factor_to_base` decimal(18,4) NOT NULL COMMENT 'Số đơn vị cơ sở trong 1 đơn vị này',
  `is_base` boolean NOT NULL DEFAULT false,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_product_units_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_product_units_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_product_units_product_id_unit_id (`product_id`, `unit_id`),
  CONSTRAINT ck_product_units_1 CHECK (factor_to_base > 0),
  CONSTRAINT ck_product_units_2 CHECK (NOT is_base OR factor_to_base = 1),
  `base_key` bigint GENERATED ALWAYS AS (CASE WHEN is_base THEN product_id END) STORED COMMENT 'Cột phụ: có giá trị khi is_base=true, NULL khi ngược lại -> ép mỗi SKU chỉ 1 đơn vị cơ sở',
  UNIQUE KEY ux_product_units_one_base (`base_key`),
  KEY ix_product_units_unit_id (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Đơn vị quy đổi của từng SKU: 1 thùng = 24 lon (S2-07).';

CREATE TABLE `customer_groups` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(100) NOT NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_customer_groups_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_customer_groups_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhóm khách hàng: Đại lý cấp 1, cấp 2, khách lẻ. Nhóm quyết định bảng giá áp dụng.';

CREATE TABLE `price_lists` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(40) NOT NULL UNIQUE,
  `name` varchar(150) NOT NULL,
  `customer_group_id` bigint NOT NULL,
  CONSTRAINT fk_price_lists_customer_group_id FOREIGN KEY (`customer_group_id`) REFERENCES `customer_groups`(`id`),
  `version_no` integer NOT NULL DEFAULT 1,
  `previous_version_id` bigint,
  CONSTRAINT fk_price_lists_previous_version_id FOREIGN KEY (`previous_version_id`) REFERENCES `price_lists`(`id`),
  `valid_from` date NOT NULL,
  `valid_to` date,
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','ACTIVE','EXPIRED')) COMMENT 'DRAFT | ACTIVE | EXPIRED',
  `is_locked` boolean NOT NULL DEFAULT false COMMENT 'true khi đã có đơn dùng bảng giá này',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_price_lists_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_price_lists_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_price_lists_1 CHECK (valid_to IS NULL OR valid_to >= valid_from),
  KEY ix_price_lists_customer_group_id (`customer_group_id`),
  KEY ix_price_lists_previous_version_id (`previous_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Bảng giá theo nhóm khách + thời gian hiệu lực. Đã phát sinh đơn -> khoá, chỉ tạo phiên bản mới (S2-10).';

CREATE TABLE `price_list_items` (
  `id` bigint AUTO_INCREMENT,
  `price_list_id` bigint NOT NULL,
  CONSTRAINT fk_price_list_items_price_list_id FOREIGN KEY (`price_list_id`) REFERENCES `price_lists`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_price_list_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_price_list_items_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `price` decimal(18,2) NOT NULL DEFAULT 0 CHECK (`price` >= 0),
  `floor_price` decimal(18,2) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_price_list_items_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_price_list_items_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_price_list_items_price_list_id_product_id_unit_id (`price_list_id`, `product_id`, `unit_id`),
  CONSTRAINT ck_price_list_items_1 CHECK (floor_price >= 0 AND floor_price <= price),
  KEY ix_price_list_items_product_id (`product_id`),
  KEY ix_price_list_items_unit_id (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giá bán và GIÁ SÀN theo SKU + đơn vị. Bán dưới giá sàn -> bắt buộc duyệt.';

CREATE TABLE `price_history` (
  `id` bigint AUTO_INCREMENT,
  `price_list_id` bigint NOT NULL,
  CONSTRAINT fk_price_history_price_list_id FOREIGN KEY (`price_list_id`) REFERENCES `price_lists`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_price_history_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_price_history_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `old_price` decimal(18,2),
  `new_price` decimal(18,2) NOT NULL DEFAULT 0,
  `old_floor_price` decimal(18,2),
  `new_floor_price` decimal(18,2) NOT NULL DEFAULT 0,
  `effective_from` date NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_price_history_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_price_history_product (`product_id`, `created_at`),
  KEY ix_price_history_price_list_id (`price_list_id`),
  KEY ix_price_history_product_id (`product_id`),
  KEY ix_price_history_unit_id (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Lịch sử đổi giá (S3-02). CHỈ INSERT: created_by/created_at = người sửa/thời điểm sửa.';

CREATE TABLE `discount_policies` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(40) NOT NULL UNIQUE,
  `name` varchar(150) NOT NULL,
  `scope_type` varchar(30) NOT NULL CHECK (`scope_type` IN ('PRODUCT','CATEGORY')) COMMENT 'PRODUCT | CATEGORY',
  `product_id` bigint,
  CONSTRAINT fk_discount_policies_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `category_id` bigint,
  CONSTRAINT fk_discount_policies_category_id FOREIGN KEY (`category_id`) REFERENCES `product_categories`(`id`),
  `discount_type` varchar(30) NOT NULL CHECK (`discount_type` IN ('PERCENT','AMOUNT_PER_UNIT')) COMMENT 'PERCENT | AMOUNT_PER_UNIT',
  `customer_group_id` bigint COMMENT 'NULL = áp dụng cho mọi nhóm',
  CONSTRAINT fk_discount_policies_customer_group_id FOREIGN KEY (`customer_group_id`) REFERENCES `customer_groups`(`id`),
  `valid_from` date NOT NULL,
  `valid_to` date,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_discount_policies_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_discount_policies_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_discount_policies_1 CHECK ((scope_type = 'PRODUCT' AND product_id IS NOT NULL AND category_id IS NULL) OR (scope_type = 'CATEGORY' AND category_id IS NOT NULL AND product_id IS NULL)),
  CONSTRAINT ck_discount_policies_2 CHECK (valid_to IS NULL OR valid_to >= valid_from),
  KEY ix_discount_policies_product_id (`product_id`),
  KEY ix_discount_policies_category_id (`category_id`),
  KEY ix_discount_policies_customer_group_id (`customer_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Chính sách chiết khấu theo sản lượng của 1 SKU hoặc 1 nhóm hàng (S3-01).';

CREATE TABLE `discount_tiers` (
  `id` bigint AUTO_INCREMENT,
  `policy_id` bigint NOT NULL,
  CONSTRAINT fk_discount_tiers_policy_id FOREIGN KEY (`policy_id`) REFERENCES `discount_policies`(`id`),
  `min_qty_base` decimal(18,3) NOT NULL COMMENT 'Theo đơn vị cơ sở',
  `discount_value` decimal(18,4) NOT NULL COMMENT 'PERCENT: 0-100; AMOUNT_PER_UNIT: VND / 1 đơn vị cơ sở',
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_discount_tiers_policy_id_min_qty_base (`policy_id`, `min_qty_base`),
  CONSTRAINT ck_discount_tiers_1 CHECK (discount_value >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Bậc chiết khấu: mua từ min_qty_base trở lên thì được discount_value. Nhiều chính sách cùng khớp -> lấy có lợi nhất cho khách (quy tắc ở service).';

-- ---------------------------------------------------------------------
-- EP-03: Đại lý & Hạn mức công nợ
-- ---------------------------------------------------------------------

CREATE TABLE `regions` (
  `id` bigint AUTO_INCREMENT,
  `parent_id` bigint,
  CONSTRAINT fk_regions_parent_id FOREIGN KEY (`parent_id`) REFERENCES `regions`(`id`),
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(100) NOT NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_regions_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_regions_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_regions_parent_id (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Khu vực / địa bàn, có thể lồng nhau (tỉnh > quận > tuyến).';

CREATE TABLE `customers` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(200) NOT NULL,
  `tax_code` varchar(20),
  `customer_group_id` bigint NOT NULL,
  CONSTRAINT fk_customers_customer_group_id FOREIGN KEY (`customer_group_id`) REFERENCES `customer_groups`(`id`),
  `region_id` bigint NOT NULL,
  CONSTRAINT fk_customers_region_id FOREIGN KEY (`region_id`) REFERENCES `regions`(`id`),
  `sales_rep_id` bigint COMMENT 'NV kinh doanh phụ trách chính (S3-06)',
  CONSTRAINT fk_customers_sales_rep_id FOREIGN KEY (`sales_rep_id`) REFERENCES `users`(`id`),
  `default_warehouse_id` bigint COMMENT 'Giả định #6: 1 đại lý chỉ có 1 kho phục vụ mặc định',
  `phone` varchar(20),
  `email` varchar(150),
  `address` varchar(300),
  `credit_limit` decimal(18,2) NOT NULL DEFAULT 0 COMMENT 'Hạn mức tiền tối đa',
  `max_debt_days` integer NOT NULL DEFAULT 0 COMMENT 'Số ngày nợ tối đa',
  `credit_status` varchar(30) NOT NULL DEFAULT 'OK' CHECK (`credit_status` IN ('OK','OVER_LIMIT','OVERDUE')) COMMENT 'Cờ do job hằng ngày + sau mỗi lần thu tiền tính lại (S7-04)',
  `is_blocked` boolean NOT NULL DEFAULT false COMMENT 'Khoá giao dịch (S3-07)',
  `block_reason` text,
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE' CHECK (`status` IN ('ACTIVE','INACTIVE')) COMMENT 'ACTIVE | INACTIVE',
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'Khoá lạc quan (optimistic lock)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_customers_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_customers_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_customers_1 CHECK (credit_limit >= 0 AND max_debt_days >= 0),
  CONSTRAINT ck_customers_2 CHECK (NOT is_blocked OR block_reason IS NOT NULL),
  KEY ix_customers_rep (`sales_rep_id`),
  KEY ix_customers_region_group (`region_id`, `customer_group_id`),
  FULLTEXT KEY ftx_customers_name (`name`),
  KEY ix_customers_customer_group_id (`customer_group_id`),
  KEY ix_customers_region_id (`region_id`),
  KEY ix_customers_sales_rep_id (`sales_rep_id`),
  KEY ix_customers_default_warehouse_id (`default_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Đại lý (khách hàng sỉ). Đã có giao dịch thì không xoá, chỉ INACTIVE (S3-03).';

CREATE TABLE `customer_delivery_addresses` (
  `id` bigint AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_customer_delivery_addresses_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `label` varchar(100),
  `address` varchar(300) NOT NULL,
  `receiver_name` varchar(150),
  `receiver_phone` varchar(20),
  `route_note` text,
  `is_default` boolean NOT NULL DEFAULT false,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_customer_delivery_addresses_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_customer_delivery_addresses_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  `default_key` bigint GENERATED ALWAYS AS (CASE WHEN is_default AND is_active THEN customer_id END) STORED COMMENT 'Cột phụ: ép mỗi đại lý chỉ 1 điểm giao mặc định đang hoạt động',
  UNIQUE KEY ux_delivery_addr_default (`default_key`),
  KEY ix_customer_delivery_addresses_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhiều điểm giao hàng cho 1 đại lý; đúng 1 điểm mặc định (S3-04).';

CREATE TABLE `credit_limit_history` (
  `id` bigint AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_credit_limit_history_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `old_limit` decimal(18,2),
  `new_limit` decimal(18,2) NOT NULL DEFAULT 0,
  `old_debt_days` integer,
  `new_debt_days` integer NOT NULL,
  `reason` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_credit_limit_history_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_credit_limit_history_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mỗi lần đổi hạn mức / số ngày nợ phải có lý do (S3-05). CHỈ INSERT.';

CREATE TABLE `customer_block_history` (
  `id` bigint AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_customer_block_history_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `action` varchar(30) NOT NULL CHECK (`action` IN ('BLOCK','UNBLOCK')) COMMENT 'BLOCK | UNBLOCK',
  `reason` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_customer_block_history_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_customer_block_history_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Lịch sử khoá / mở khoá giao dịch đại lý (S3-07). CHỈ INSERT.';

CREATE TABLE `customer_assignment_history` (
  `id` bigint AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_customer_assignment_history_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `from_sales_rep_id` bigint,
  CONSTRAINT fk_customer_assignment_history_from_sales_rep_id FOREIGN KEY (`from_sales_rep_id`) REFERENCES `users`(`id`),
  `to_sales_rep_id` bigint NOT NULL,
  CONSTRAINT fk_customer_assignment_history_to_sales_rep_id FOREIGN KEY (`to_sales_rep_id`) REFERENCES `users`(`id`),
  `reason` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_customer_assignment_history_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_customer_assignment_history_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Lịch sử chuyển giao đại lý giữa các nhân viên (S3-06). CHỈ INSERT.';

-- ---------------------------------------------------------------------
-- EP-04: Đặt hàng & Duyệt đơn
-- ---------------------------------------------------------------------

CREATE TABLE `sales_orders` (
  `id` bigint AUTO_INCREMENT,
  `order_no` varchar(30) NOT NULL UNIQUE,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_sales_orders_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `delivery_address_id` bigint NOT NULL,
  CONSTRAINT fk_sales_orders_delivery_address_id FOREIGN KEY (`delivery_address_id`) REFERENCES `customer_delivery_addresses`(`id`),
  `warehouse_id` bigint NOT NULL COMMENT 'Kho phục vụ, chốt tại thời điểm tạo đơn',
  `sales_rep_id` bigint,
  CONSTRAINT fk_sales_orders_sales_rep_id FOREIGN KEY (`sales_rep_id`) REFERENCES `users`(`id`),
  `source` varchar(30) NOT NULL DEFAULT 'SALES_REP' CHECK (`source` IN ('SALES_REP','PORTAL')) COMMENT 'SALES_REP | PORTAL',
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','PENDING_APPROVAL','APPROVED','PICKING','SHIPPED','DELIVERED','CLOSED','REJECTED','CANCELLED')) COMMENT 'DRAFT | PENDING_APPROVAL | APPROVED | PICKING | SHIPPED | DELIVERED | CLOSED | REJECTED | CANCELLED',
  `requested_delivery_date` date,
  `subtotal_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `discount_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `needs_approval` boolean NOT NULL DEFAULT false COMMENT 'Vượt hạn mức hoặc bán dưới giá sàn',
  `submitted_at` datetime,
  `approved_at` datetime,
  `cancel_reason` text,
  `cancelled_at` datetime,
  `cancelled_by` bigint,
  CONSTRAINT fk_sales_orders_cancelled_by FOREIGN KEY (`cancelled_by`) REFERENCES `users`(`id`),
  `copied_from_order_id` bigint,
  CONSTRAINT fk_sales_orders_copied_from_order_id FOREIGN KEY (`copied_from_order_id`) REFERENCES `sales_orders`(`id`),
  `note` text,
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'Khoá lạc quan (optimistic lock)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_sales_orders_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_sales_orders_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_sales_orders_1 CHECK (total_amount >= 0),
  CONSTRAINT ck_sales_orders_2 CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL),
  KEY ix_orders_customer (`customer_id`, `created_at`),
  KEY ix_orders_status (`status`, `created_at`),
  KEY ix_orders_rep_created (`sales_rep_id`, `created_at`),
  KEY ix_sales_orders_customer_id (`customer_id`),
  KEY ix_sales_orders_delivery_address_id (`delivery_address_id`),
  KEY ix_sales_orders_warehouse_id (`warehouse_id`),
  KEY ix_sales_orders_sales_rep_id (`sales_rep_id`),
  KEY ix_sales_orders_copied_from_order_id (`copied_from_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Đơn bán hàng. Giỏ hàng của cổng đại lý = đơn ở trạng thái DRAFT (không cần bảng cart riêng).';

CREATE TABLE `sales_order_items` (
  `id` bigint AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  CONSTRAINT fk_sales_order_items_order_id FOREIGN KEY (`order_id`) REFERENCES `sales_orders`(`id`),
  `line_no` integer NOT NULL,
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_sales_order_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_sales_order_items_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `unit_factor` decimal(18,4) NOT NULL COMMENT 'Snapshot hệ số quy đổi',
  `qty` decimal(18,3) NOT NULL COMMENT 'Số lượng theo đơn vị người dùng chọn',
  `qty_base` decimal(18,3) NOT NULL COMMENT 'Số lượng quy về đơn vị cơ sở - dùng cho mọi tính toán tồn',
  `unit_price` decimal(18,2) NOT NULL DEFAULT 0,
  `floor_price` decimal(18,2) NOT NULL DEFAULT 0 COMMENT 'Snapshot giá sàn',
  `price_list_item_id` bigint,
  CONSTRAINT fk_sales_order_items_price_list_item_id FOREIGN KEY (`price_list_item_id`) REFERENCES `price_list_items`(`id`),
  `discount_policy_id` bigint,
  CONSTRAINT fk_sales_order_items_discount_policy_id FOREIGN KEY (`discount_policy_id`) REFERENCES `discount_policies`(`id`),
  `is_manual_price` boolean NOT NULL DEFAULT false,
  `is_below_floor` boolean NOT NULL DEFAULT false,
  `discount_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `line_total` decimal(18,2) NOT NULL DEFAULT 0,
  `qty_shipped_base` decimal(18,3) NOT NULL DEFAULT 0 COMMENT 'Cộng dồn khi xuất kho; còn thiếu = qty_base - qty_shipped_base (hàng còn nợ)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_sales_order_items_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_sales_order_items_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_sales_order_items_order_id_line_no (`order_id`, `line_no`),
  CONSTRAINT ck_sales_order_items_1 CHECK (qty > 0 AND qty_base > 0),
  CONSTRAINT ck_sales_order_items_2 CHECK (unit_price >= 0),
  CONSTRAINT ck_sales_order_items_3 CHECK (qty_shipped_base >= 0 AND qty_shipped_base <= qty_base),
  KEY ix_order_items_product (`product_id`),
  KEY ix_sales_order_items_product_id (`product_id`),
  KEY ix_sales_order_items_unit_id (`unit_id`),
  KEY ix_sales_order_items_price_list_item_id (`price_list_item_id`),
  KEY ix_sales_order_items_discount_policy_id (`discount_policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng hàng. Chụp lại (snapshot) đơn vị, hệ số, giá, giá sàn để đổi bảng giá/hệ số sau này không làm sai đơn cũ.';

CREATE TABLE `approval_requests` (
  `id` bigint AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  CONSTRAINT fk_approval_requests_order_id FOREIGN KEY (`order_id`) REFERENCES `sales_orders`(`id`),
  `approval_type` varchar(30) NOT NULL DEFAULT 'MANAGER_EXCEPTION' CHECK (`approval_type` IN ('MANAGER_EXCEPTION','REP_CONFIRMATION')) COMMENT 'MANAGER_EXCEPTION | REP_CONFIRMATION',
  `violations` json NOT NULL DEFAULT ('[]') COMMENT '[{type:''OVER_CREDIT_LIMIT'', exceeded_by:..},{type:''BELOW_FLOOR_PRICE'', line_no:..}]',
  `status` varchar(30) NOT NULL DEFAULT 'PENDING' CHECK (`status` IN ('PENDING','APPROVED','REJECTED','RETURNED')) COMMENT 'PENDING | APPROVED | REJECTED | RETURNED',
  `requested_by` bigint NOT NULL,
  CONSTRAINT fk_approval_requests_requested_by FOREIGN KEY (`requested_by`) REFERENCES `users`(`id`),
  `requested_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `decided_by` bigint,
  CONSTRAINT fk_approval_requests_decided_by FOREIGN KEY (`decided_by`) REFERENCES `users`(`id`),
  `decided_at` datetime,
  `decision_comment` text,
  PRIMARY KEY (`id`),
  CONSTRAINT ck_approval_requests_1 CHECK (status NOT IN ('REJECTED','RETURNED') OR decision_comment IS NOT NULL),
  KEY ix_approval_pending (`status`, `requested_at`),
  KEY ix_approval_requests_order_id (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Yêu cầu duyệt (vượt hạn mức / dưới giá sàn). Mỗi lần gửi duyệt là 1 dòng -> lịch sử duyệt (S4-05).';

CREATE TABLE `order_status_history` (
  `id` bigint AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  CONSTRAINT fk_order_status_history_order_id FOREIGN KEY (`order_id`) REFERENCES `sales_orders`(`id`),
  `from_status` varchar(30),
  `to_status` varchar(30) NOT NULL,
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_order_status_history_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_order_status_hist (`order_id`, `created_at`),
  KEY ix_order_status_history_order_id (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Vòng đời đơn: Nháp > Chờ duyệt > Đã duyệt > Đang soạn > Đã xuất > Đã giao > Đóng (+ Huỷ). CHỈ INSERT.';

-- ---------------------------------------------------------------------
-- EP-05: Kho & Tồn kho
-- ---------------------------------------------------------------------

CREATE TABLE `suppliers` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(200) NOT NULL,
  `tax_code` varchar(20),
  `contact_name` varchar(150),
  `phone` varchar(20),
  `email` varchar(150),
  `address` varchar(300),
  `payment_terms` varchar(200),
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE' CHECK (`status` IN ('ACTIVE','INACTIVE')) COMMENT 'ACTIVE | INACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_suppliers_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_suppliers_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhà cung cấp. Đã có phiếu nhập thì không xoá, chỉ INACTIVE (S2-09).';

CREATE TABLE `warehouses` (
  `id` bigint AUTO_INCREMENT,
  `code` varchar(30) NOT NULL UNIQUE,
  `name` varchar(150) NOT NULL,
  `address` varchar(300),
  `manager_user_id` bigint,
  CONSTRAINT fk_warehouses_manager_user_id FOREIGN KEY (`manager_user_id`) REFERENCES `users`(`id`),
  `warehouse_type` varchar(30) NOT NULL DEFAULT 'SELLABLE' CHECK (`warehouse_type` IN ('SELLABLE','DEFECT')) COMMENT 'SELLABLE | DEFECT',
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE' CHECK (`status` IN ('ACTIVE','INACTIVE')) COMMENT 'ACTIVE | INACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_warehouses_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_warehouses_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_warehouses_manager_user_id (`manager_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Kho. warehouse_type = DEFECT là ''kho hàng lỗi'' nhận hàng trả bị hỏng (S8-01).';

CREATE TABLE `warehouse_locations` (
  `id` bigint AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_warehouse_locations_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `code` varchar(30) NOT NULL,
  `name` varchar(100),
  `location_type` varchar(30) NOT NULL DEFAULT 'RACK' CHECK (`location_type` IN ('ZONE','RACK')) COMMENT 'ZONE | RACK',
  `pick_sequence` integer NOT NULL DEFAULT 0,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_warehouse_locations_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_warehouse_locations_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_warehouse_locations_warehouse_id_code (`warehouse_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Vị trí lưu trong kho (khu / kệ). pick_sequence = thứ tự đi soạn hàng (S6-03).';

CREATE TABLE `lots` (
  `id` bigint AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_lots_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_no` varchar(50) NOT NULL,
  `manufacture_date` date,
  `expiry_date` date,
  `supplier_id` bigint,
  CONSTRAINT fk_lots_supplier_id FOREIGN KEY (`supplier_id`) REFERENCES `suppliers`(`id`),
  `is_default` boolean NOT NULL DEFAULT false,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_lots_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_lots_product_id_lot_no (`product_id`, `lot_no`),
  CONSTRAINT ck_lots_1 CHECK (expiry_date IS NULL OR manufacture_date IS NULL OR expiry_date >= manufacture_date),
  `default_lot_key` bigint GENERATED ALWAYS AS (CASE WHEN is_default THEN product_id END) STORED COMMENT 'Cột phụ: ép mỗi SKU chỉ 1 lô mặc định',
  UNIQUE KEY ux_lots_one_default (`default_lot_key`),
  KEY ix_lots_expiry (`expiry_date`),
  KEY ix_lots_supplier_id (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Lô hàng + hạn sử dụng. SKU không quản lý lô dùng đúng 1 ''lô mặc định'' (is_default) để mọi phát sinh kho đều có lot_id.';

CREATE TABLE `stock_balances` (
  `warehouse_id` bigint,
  CONSTRAINT fk_stock_balances_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `product_id` bigint,
  CONSTRAINT fk_stock_balances_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `location_id` bigint COMMENT 'Vị trí lưu chính của SKU trong kho',
  CONSTRAINT fk_stock_balances_location_id FOREIGN KEY (`location_id`) REFERENCES `warehouse_locations`(`id`),
  `min_stock_base` decimal(18,3) NOT NULL DEFAULT 0 COMMENT 'Tồn tối thiểu (S5-09)',
  `qty_on_hand_base` decimal(18,3) NOT NULL DEFAULT 0 COMMENT 'Tồn thực tế',
  `qty_reserved_base` decimal(18,3) NOT NULL DEFAULT 0 COMMENT 'Đang giữ chỗ cho đơn đã duyệt',
  `qty_available_base` decimal(18,3) GENERATED ALWAYS AS (qty_on_hand_base - qty_reserved_base) STORED COMMENT 'Tồn khả dụng = thực tế - giữ chỗ',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'Khoá lạc quan (optimistic lock)',
  PRIMARY KEY (`warehouse_id`, `product_id`),
  CONSTRAINT ck_stock_balances_1 CHECK (qty_on_hand_base >= 0),
  CONSTRAINT ck_stock_balances_2 CHECK (qty_reserved_base >= 0),
  CONSTRAINT ck_stock_balances_3 CHECK (qty_reserved_base <= qty_on_hand_base),
  KEY ix_stock_balances_warehouse_id (`warehouse_id`),
  KEY ix_stock_balances_product_id (`product_id`),
  KEY ix_stock_balances_location_id (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SỔ TỒN theo (kho, SKU). Hàng cần khoá khi giữ chỗ/xuất kho. CHECK ở DB là chốt chặn cuối chống tồn âm.';

CREATE TABLE `stock_lot_balances` (
  `warehouse_id` bigint,
  CONSTRAINT fk_stock_lot_balances_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `lot_id` bigint,
  CONSTRAINT fk_stock_lot_balances_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `qty_on_hand_base` decimal(18,3) NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`warehouse_id`, `lot_id`),
  CONSTRAINT ck_stock_lot_balances_1 CHECK (qty_on_hand_base >= 0),
  KEY ix_stock_lot_balances_warehouse_id (`warehouse_id`),
  KEY ix_stock_lot_balances_lot_id (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tồn thực tế chi tiết theo (kho, lô) - phục vụ FEFO. Tổng các lô của 1 SKU = qty_on_hand_base ở stock_balances.';

CREATE TABLE `stock_movements` (
  `id` bigint AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_stock_movements_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_stock_movements_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_stock_movements_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `movement_type` varchar(30) NOT NULL CHECK (`movement_type` IN ('RECEIPT','ISSUE','TRANSFER_OUT','TRANSFER_IN','ADJUSTMENT','STOCKTAKE','RETURN_IN')) COMMENT 'RECEIPT | ISSUE | TRANSFER_OUT | TRANSFER_IN | ADJUSTMENT | STOCKTAKE | RETURN_IN',
  `qty_change_base` decimal(18,3) NOT NULL COMMENT 'Có dấu: + nhập, - xuất',
  `balance_after_base` decimal(18,3) NOT NULL COMMENT 'Tồn thực tế SKU-kho sau phát sinh',
  `ref_type` varchar(40) NOT NULL COMMENT 'Chứng từ gốc: GOODS_RECEIPT, DELIVERY_NOTE, ...',
  `ref_id` bigint NOT NULL,
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_stock_movements_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_stock_movements_1 CHECK (qty_change_base <> 0),
  KEY ix_movements_card (`product_id`, `warehouse_id`, `occurred_at`),
  KEY ix_movements_ref (`ref_type`, `ref_id`),
  KEY ix_stock_movements_warehouse_id (`warehouse_id`),
  KEY ix_stock_movements_product_id (`product_id`),
  KEY ix_stock_movements_lot_id (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='THẺ KHO - nguồn sự thật duy nhất của mọi biến động tồn. CHỈ INSERT; không sửa/xoá (S6-02).';

CREATE TABLE `stock_reservations` (
  `id` bigint AUTO_INCREMENT,
  `order_item_id` bigint NOT NULL,
  CONSTRAINT fk_stock_reservations_order_item_id FOREIGN KEY (`order_item_id`) REFERENCES `sales_order_items`(`id`),
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_stock_reservations_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_stock_reservations_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `qty_base` decimal(18,3) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE' CHECK (`status` IN ('ACTIVE','RELEASED','FULFILLED')) COMMENT 'ACTIVE | RELEASED | FULFILLED',
  `reserved_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime,
  `released_at` datetime,
  `release_reason` varchar(30),
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_stock_reservations_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_stock_reservations_1 CHECK (qty_base > 0),
  `active_key` bigint GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN order_item_id END) STORED COMMENT 'Cột phụ: ép mỗi dòng đơn chỉ 1 giữ chỗ đang ACTIVE',
  UNIQUE KEY ux_reservation_active (`active_key`),
  KEY ix_reservation_stock (`warehouse_id`, `product_id`, `status`),
  KEY ix_stock_reservations_order_item_id (`order_item_id`),
  KEY ix_stock_reservations_warehouse_id (`warehouse_id`),
  KEY ix_stock_reservations_product_id (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giữ chỗ tồn cho đơn đã duyệt (S5-06). Tổng ACTIVE theo (kho, SKU) phải bằng stock_balances.qty_reserved_base.';

CREATE TABLE `goods_receipts` (
  `id` bigint AUTO_INCREMENT,
  `receipt_no` varchar(30) NOT NULL UNIQUE,
  `supplier_id` bigint NOT NULL,
  CONSTRAINT fk_goods_receipts_supplier_id FOREIGN KEY (`supplier_id`) REFERENCES `suppliers`(`id`),
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_goods_receipts_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `document_ref` varchar(60) COMMENT 'Số chứng từ của nhà cung cấp',
  `receipt_date` date NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','CONFIRMED','CANCELLED')) COMMENT 'DRAFT | CONFIRMED | CANCELLED',
  `confirmed_by` bigint,
  CONSTRAINT fk_goods_receipts_confirmed_by FOREIGN KEY (`confirmed_by`) REFERENCES `users`(`id`),
  `confirmed_at` datetime,
  `adjusts_receipt_id` bigint,
  CONSTRAINT fk_goods_receipts_adjusts_receipt_id FOREIGN KEY (`adjusts_receipt_id`) REFERENCES `goods_receipts`(`id`),
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_goods_receipts_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_goods_receipts_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_goods_receipts_supplier_id (`supplier_id`),
  KEY ix_goods_receipts_warehouse_id (`warehouse_id`),
  KEY ix_goods_receipts_adjusts_receipt_id (`adjusts_receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu nhập kho. Xác nhận rồi thì không sửa; muốn sửa lập phiếu điều chỉnh (adjusts_receipt_id) (S5-04).';

CREATE TABLE `goods_receipt_items` (
  `id` bigint AUTO_INCREMENT,
  `receipt_id` bigint NOT NULL,
  CONSTRAINT fk_goods_receipt_items_receipt_id FOREIGN KEY (`receipt_id`) REFERENCES `goods_receipts`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_goods_receipt_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_goods_receipt_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_goods_receipt_items_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `unit_factor` decimal(18,4) NOT NULL,
  `qty` decimal(18,3) NOT NULL,
  `qty_base` decimal(18,3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT ck_goods_receipt_items_1 CHECK (qty_base <> 0),
  KEY ix_goods_receipt_items_receipt_id (`receipt_id`),
  KEY ix_goods_receipt_items_product_id (`product_id`),
  KEY ix_goods_receipt_items_lot_id (`lot_id`),
  KEY ix_goods_receipt_items_unit_id (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng phiếu nhập: nhập theo đơn vị bất kỳ, quy về đơn vị cơ sở khi ghi sổ.';

CREATE TABLE `stock_transfers` (
  `id` bigint AUTO_INCREMENT,
  `transfer_no` varchar(30) NOT NULL UNIQUE,
  `from_warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_stock_transfers_from_warehouse_id FOREIGN KEY (`from_warehouse_id`) REFERENCES `warehouses`(`id`),
  `to_warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_stock_transfers_to_warehouse_id FOREIGN KEY (`to_warehouse_id`) REFERENCES `warehouses`(`id`),
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','IN_TRANSIT','RECEIVED','CANCELLED')) COMMENT 'DRAFT | IN_TRANSIT | RECEIVED | CANCELLED',
  `shipped_at` datetime,
  `shipped_by` bigint,
  CONSTRAINT fk_stock_transfers_shipped_by FOREIGN KEY (`shipped_by`) REFERENCES `users`(`id`),
  `received_at` datetime,
  `received_by` bigint,
  CONSTRAINT fk_stock_transfers_received_by FOREIGN KEY (`received_by`) REFERENCES `users`(`id`),
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_stock_transfers_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_stock_transfers_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_stock_transfers_1 CHECK (from_warehouse_id <> to_warehouse_id),
  KEY ix_stock_transfers_from_warehouse_id (`from_warehouse_id`),
  KEY ix_stock_transfers_to_warehouse_id (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Chuyển kho nội bộ. IN_TRANSIT = hàng đang trên đường, CHƯA cộng vào kho đến (S5-07).';

CREATE TABLE `stock_transfer_items` (
  `id` bigint AUTO_INCREMENT,
  `transfer_id` bigint NOT NULL,
  CONSTRAINT fk_stock_transfer_items_transfer_id FOREIGN KEY (`transfer_id`) REFERENCES `stock_transfers`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_stock_transfer_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_stock_transfer_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `qty_sent_base` decimal(18,3) NOT NULL,
  `qty_received_base` decimal(18,3),
  `variance_reason` text,
  PRIMARY KEY (`id`),
  CONSTRAINT ck_stock_transfer_items_1 CHECK (qty_sent_base > 0),
  CONSTRAINT ck_stock_transfer_items_2 CHECK (qty_received_base IS NULL OR qty_received_base = qty_sent_base OR variance_reason IS NOT NULL),
  KEY ix_stock_transfer_items_transfer_id (`transfer_id`),
  KEY ix_stock_transfer_items_product_id (`product_id`),
  KEY ix_stock_transfer_items_lot_id (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng chuyển kho; chênh lệch khi nhận phải nhập lý do.';

CREATE TABLE `stocktakes` (
  `id` bigint AUTO_INCREMENT,
  `stocktake_no` varchar(30) NOT NULL UNIQUE,
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_stocktakes_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `scope_type` varchar(30) NOT NULL DEFAULT 'WAREHOUSE' CHECK (`scope_type` IN ('WAREHOUSE','CATEGORY')) COMMENT 'WAREHOUSE | CATEGORY',
  `category_id` bigint,
  CONSTRAINT fk_stocktakes_category_id FOREIGN KEY (`category_id`) REFERENCES `product_categories`(`id`),
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','COUNTING','COMPLETED','CANCELLED')) COMMENT 'DRAFT | COUNTING | COMPLETED | CANCELLED',
  `snapshot_at` datetime NOT NULL,
  `finalized_by` bigint,
  CONSTRAINT fk_stocktakes_finalized_by FOREIGN KEY (`finalized_by`) REFERENCES `users`(`id`),
  `finalized_at` datetime,
  `reason` text COMMENT 'Bắt buộc khi chốt',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_stocktakes_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_stocktakes_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_stocktakes_warehouse_id (`warehouse_id`),
  KEY ix_stocktakes_category_id (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu kiểm kê. snapshot_at = thời điểm chốt số tồn sổ. Chỉ WH_MANAGER được chốt (S5-08).';

CREATE TABLE `stocktake_items` (
  `id` bigint AUTO_INCREMENT,
  `stocktake_id` bigint NOT NULL,
  CONSTRAINT fk_stocktake_items_stocktake_id FOREIGN KEY (`stocktake_id`) REFERENCES `stocktakes`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_stocktake_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_stocktake_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `system_qty_base` decimal(18,3) NOT NULL,
  `counted_qty_base` decimal(18,3),
  `variance_base` decimal(18,3) GENERATED ALWAYS AS (counted_qty_base - system_qty_base) STORED COMMENT 'Chênh lệch',
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_stocktake_items_stocktake_id_product_id_lot_id (`stocktake_id`, `product_id`, `lot_id`),
  KEY ix_stocktake_items_product_id (`product_id`),
  KEY ix_stocktake_items_lot_id (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng kiểm kê: số tồn sổ tại snapshot_at vs số đếm thực tế.';

-- ---------------------------------------------------------------------
-- EP-06: Xuất kho & Giao hàng
-- ---------------------------------------------------------------------

CREATE TABLE `delivery_trips` (
  `id` bigint AUTO_INCREMENT,
  `trip_no` varchar(30) NOT NULL UNIQUE,
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_trips_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `region_id` bigint,
  CONSTRAINT fk_delivery_trips_region_id FOREIGN KEY (`region_id`) REFERENCES `regions`(`id`),
  `driver_name` varchar(150),
  `driver_phone` varchar(20),
  `vehicle_plate` varchar(20),
  `status` varchar(30) NOT NULL DEFAULT 'PLANNED' CHECK (`status` IN ('PLANNED','RUNNING','COMPLETED')) COMMENT 'PLANNED | RUNNING | COMPLETED',
  `planned_departure` datetime,
  `expected_return_at` datetime COMMENT 'Quá giờ -> cảnh báo (S6-07)',
  `departed_at` datetime,
  `completed_at` datetime,
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_delivery_trips_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_delivery_trips_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_delivery_trips_warehouse_id (`warehouse_id`),
  KEY ix_delivery_trips_region_id (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Chuyến giao hàng (phân tuyến). Tài xế không phải user hệ thống nên lưu thẳng tên/biển số (S6-06).';

CREATE TABLE `pick_lists` (
  `id` bigint AUTO_INCREMENT,
  `pick_no` varchar(30) NOT NULL UNIQUE,
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_pick_lists_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `status` varchar(30) NOT NULL DEFAULT 'OPEN' CHECK (`status` IN ('OPEN','PICKING','DONE','CANCELLED')) COMMENT 'OPEN | PICKING | DONE | CANCELLED',
  `assigned_to` bigint,
  CONSTRAINT fk_pick_lists_assigned_to FOREIGN KEY (`assigned_to`) REFERENCES `users`(`id`),
  `completed_at` datetime,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_pick_lists_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_pick_lists_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_pick_lists_warehouse_id (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu soạn hàng - gộp được nhiều đơn thành một lượt soạn (S6-03).';

CREATE TABLE `pick_list_items` (
  `id` bigint AUTO_INCREMENT,
  `pick_list_id` bigint NOT NULL,
  CONSTRAINT fk_pick_list_items_pick_list_id FOREIGN KEY (`pick_list_id`) REFERENCES `pick_lists`(`id`),
  `order_item_id` bigint NOT NULL,
  CONSTRAINT fk_pick_list_items_order_item_id FOREIGN KEY (`order_item_id`) REFERENCES `sales_order_items`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_pick_list_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_pick_list_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `location_id` bigint,
  CONSTRAINT fk_pick_list_items_location_id FOREIGN KEY (`location_id`) REFERENCES `warehouse_locations`(`id`),
  `qty_required_base` decimal(18,3) NOT NULL,
  `qty_picked_base` decimal(18,3),
  `lot_override_reason` text,
  `short_reason` text,
  `picked_at` datetime,
  PRIMARY KEY (`id`),
  CONSTRAINT ck_pick_list_items_1 CHECK (qty_required_base > 0),
  CONSTRAINT ck_pick_list_items_2 CHECK (qty_picked_base IS NULL OR qty_picked_base >= qty_required_base OR short_reason IS NOT NULL),
  KEY ix_pick_list_items_pick_list_id (`pick_list_id`),
  KEY ix_pick_list_items_order_item_id (`order_item_id`),
  KEY ix_pick_list_items_product_id (`product_id`),
  KEY ix_pick_list_items_lot_id (`lot_id`),
  KEY ix_pick_list_items_location_id (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng soạn: hệ thống gợi ý lô theo FEFO; đổi lô phải nhập lý do (S6-04); soạn thiếu phải nhập lý do.';

CREATE TABLE `delivery_notes` (
  `id` bigint AUTO_INCREMENT,
  `delivery_no` varchar(30) NOT NULL UNIQUE,
  `order_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_notes_order_id FOREIGN KEY (`order_id`) REFERENCES `sales_orders`(`id`),
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_notes_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `pick_list_id` bigint,
  CONSTRAINT fk_delivery_notes_pick_list_id FOREIGN KEY (`pick_list_id`) REFERENCES `pick_lists`(`id`),
  `trip_id` bigint,
  CONSTRAINT fk_delivery_notes_trip_id FOREIGN KEY (`trip_id`) REFERENCES `delivery_trips`(`id`),
  `status` varchar(30) NOT NULL DEFAULT 'ISSUED' CHECK (`status` IN ('ISSUED','IN_TRANSIT','DELIVERED','FAILED')) COMMENT 'ISSUED | IN_TRANSIT | DELIVERED | FAILED',
  `issued_at` datetime NOT NULL,
  `issued_by` bigint,
  CONSTRAINT fk_delivery_notes_issued_by FOREIGN KEY (`issued_by`) REFERENCES `users`(`id`),
  `recipient_name` varchar(150),
  `delivered_at` datetime,
  `failure_reason` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_delivery_notes_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_delivery_notes_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_delivery_order (`order_id`),
  KEY ix_delivery_notes_order_id (`order_id`),
  KEY ix_delivery_notes_warehouse_id (`warehouse_id`),
  KEY ix_delivery_notes_pick_list_id (`pick_list_id`),
  KEY ix_delivery_notes_trip_id (`trip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu xuất kho (1 phiếu / 1 đơn). Xác nhận rồi không sửa (S6-05). Giao xong mới đủ điều kiện xuất hoá đơn.';

CREATE TABLE `delivery_note_items` (
  `id` bigint AUTO_INCREMENT,
  `delivery_note_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_note_items_delivery_note_id FOREIGN KEY (`delivery_note_id`) REFERENCES `delivery_notes`(`id`),
  `order_item_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_note_items_order_item_id FOREIGN KEY (`order_item_id`) REFERENCES `sales_order_items`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_note_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_note_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `qty_issued_base` decimal(18,3) NOT NULL,
  `qty_delivered_base` decimal(18,3),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_delivery_note_items_1 CHECK (qty_issued_base > 0),
  CONSTRAINT ck_delivery_note_items_2 CHECK (qty_delivered_base IS NULL OR qty_delivered_base <= qty_issued_base),
  KEY ix_delivery_note_items_delivery_note_id (`delivery_note_id`),
  KEY ix_delivery_note_items_order_item_id (`order_item_id`),
  KEY ix_delivery_note_items_product_id (`product_id`),
  KEY ix_delivery_note_items_lot_id (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng phiếu xuất: số xuất vs số thực giao. Hoá đơn sinh từ số thực giao (S7-01).';

CREATE TABLE `delivery_proofs` (
  `id` bigint AUTO_INCREMENT,
  `delivery_note_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_proofs_delivery_note_id FOREIGN KEY (`delivery_note_id`) REFERENCES `delivery_notes`(`id`),
  `file_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_proofs_file_id FOREIGN KEY (`file_id`) REFERENCES `file_objects`(`id`),
  `proof_type` varchar(30) NOT NULL DEFAULT 'SIGNED_RECEIPT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_delivery_proofs_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_delivery_proofs_delivery_note_id (`delivery_note_id`),
  KEY ix_delivery_proofs_file_id (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Ảnh biên bản có chữ ký khi giao thành công (S6-08).';

-- ---------------------------------------------------------------------
-- EP-07: Hoá đơn, Công nợ & Thanh toán
-- ---------------------------------------------------------------------

CREATE TABLE `invoices` (
  `id` bigint AUTO_INCREMENT,
  `invoice_no` varchar(30) NOT NULL UNIQUE,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_invoices_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `issue_date` date NOT NULL,
  `due_date` date NOT NULL,
  `payment_term_days` integer NOT NULL COMMENT 'Snapshot số ngày nợ tại thời điểm phát hành',
  `status` varchar(30) NOT NULL DEFAULT 'ISSUED' CHECK (`status` IN ('ISSUED','PARTIALLY_PAID','PAID','CANCELLED')) COMMENT 'ISSUED | PARTIALLY_PAID | PAID | CANCELLED',
  `subtotal_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `discount_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `amount_paid` decimal(18,2) NOT NULL DEFAULT 0,
  `amount_credited` decimal(18,2) NOT NULL DEFAULT 0,
  `outstanding_amount` decimal(18,2) GENERATED ALWAYS AS (total_amount - amount_paid - amount_credited) STORED COMMENT 'Còn phải thu',
  `issued_by` bigint,
  CONSTRAINT fk_invoices_issued_by FOREIGN KEY (`issued_by`) REFERENCES `users`(`id`),
  `issued_at` datetime,
  `note` text,
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'Khoá lạc quan (optimistic lock)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_invoices_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_invoices_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_invoices_1 CHECK (total_amount >= 0),
  CONSTRAINT ck_invoices_2 CHECK (amount_paid + amount_credited <= total_amount),
  CONSTRAINT ck_invoices_3 CHECK (due_date >= issue_date),
  KEY ix_invoices_open_due (`status`, `due_date`),
  KEY ix_invoices_customer (`customer_id`, `issue_date`),
  KEY ix_invoices_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Hoá đơn bán hàng nội bộ. Đã phát hành không sửa/xoá; điều chỉnh bằng credit_notes (S7-01).';

CREATE TABLE `invoice_items` (
  `id` bigint AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  CONSTRAINT fk_invoice_items_invoice_id FOREIGN KEY (`invoice_id`) REFERENCES `invoices`(`id`),
  `delivery_note_item_id` bigint NOT NULL UNIQUE,
  CONSTRAINT fk_invoice_items_delivery_note_item_id FOREIGN KEY (`delivery_note_item_id`) REFERENCES `delivery_note_items`(`id`),
  `order_item_id` bigint NOT NULL,
  CONSTRAINT fk_invoice_items_order_item_id FOREIGN KEY (`order_item_id`) REFERENCES `sales_order_items`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_invoice_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_invoice_items_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `unit_factor` decimal(18,4) NOT NULL,
  `qty` decimal(18,3) NOT NULL,
  `qty_base` decimal(18,3) NOT NULL,
  `unit_price` decimal(18,2) NOT NULL DEFAULT 0,
  `discount_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `line_total` decimal(18,2) NOT NULL DEFAULT 0,
  `unit_cost_base_snapshot` decimal(18,4) NOT NULL COMMENT 'NHẠY CẢM: giá vốn/đơn vị cơ sở tại thời điểm bán - phục vụ báo cáo biên lợi nhuận, chỉ SALES_MANAGER thấy',
  PRIMARY KEY (`id`),
  CONSTRAINT ck_invoice_items_1 CHECK (qty_base > 0),
  KEY ix_invoice_items_invoice_id (`invoice_id`),
  KEY ix_invoice_items_order_item_id (`order_item_id`),
  KEY ix_invoice_items_product_id (`product_id`),
  KEY ix_invoice_items_unit_id (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng hoá đơn, mỗi dòng truy về 1 dòng phiếu xuất (UNIQUE -> không xuất hoá đơn 2 lần cho cùng 1 dòng giao).';

CREATE TABLE `payments` (
  `id` bigint AUTO_INCREMENT,
  `receipt_no` varchar(30) NOT NULL UNIQUE,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_payments_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `amount` decimal(18,2) NOT NULL DEFAULT 0 CHECK (`amount` > 0),
  `method` varchar(30) NOT NULL CHECK (`method` IN ('CASH','BANK_TRANSFER')) COMMENT 'CASH | BANK_TRANSFER',
  `payment_date` date NOT NULL,
  `collected_by` bigint NOT NULL,
  CONSTRAINT fk_payments_collected_by FOREIGN KEY (`collected_by`) REFERENCES `users`(`id`),
  `status` varchar(30) NOT NULL DEFAULT 'CONFIRMED' CHECK (`status` IN ('PENDING_HANDOVER','CONFIRMED','VOIDED')) COMMENT 'PENDING_HANDOVER | CONFIRMED | VOIDED',
  `receipt_file_id` bigint,
  CONSTRAINT fk_payments_receipt_file_id FOREIGN KEY (`receipt_file_id`) REFERENCES `file_objects`(`id`),
  `confirmed_by` bigint,
  CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (`confirmed_by`) REFERENCES `users`(`id`),
  `confirmed_at` datetime,
  `void_reason` text,
  `voided_by` bigint,
  CONSTRAINT fk_payments_voided_by FOREIGN KEY (`voided_by`) REFERENCES `users`(`id`),
  `voided_at` datetime,
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_payments_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_payments_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_payments_1 CHECK (amount > 0),
  CONSTRAINT ck_payments_2 CHECK (status <> 'VOIDED' OR void_reason IS NOT NULL),
  KEY ix_payments_customer (`customer_id`, `payment_date`),
  KEY ix_payments_collector (`collected_by`, `payment_date`),
  KEY ix_payments_customer_id (`customer_id`),
  KEY ix_payments_receipt_file_id (`receipt_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu thu. Thu tiền theo tuyến ở trạng thái PENDING_HANDOVER cho tới khi kế toán xác nhận (S7-05). Không xoá, chỉ VOIDED.';

CREATE TABLE `payment_allocations` (
  `id` bigint AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  CONSTRAINT fk_payment_allocations_payment_id FOREIGN KEY (`payment_id`) REFERENCES `payments`(`id`),
  `invoice_id` bigint NOT NULL,
  CONSTRAINT fk_payment_allocations_invoice_id FOREIGN KEY (`invoice_id`) REFERENCES `invoices`(`id`),
  `amount` decimal(18,2) NOT NULL DEFAULT 0 CHECK (`amount` > 0),
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_payment_allocations_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_payment_allocations_payment_id_invoice_id (`payment_id`, `invoice_id`),
  CONSTRAINT ck_payment_allocations_1 CHECK (amount > 0),
  KEY ix_payment_allocations_invoice_id (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Đối trừ 1 phiếu thu vào các hoá đơn cụ thể (hoặc tự động hoá đơn cũ nhất trước) (S7-02).';

CREATE TABLE `ar_ledger_entries` (
  `id` bigint AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_ar_ledger_entries_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `entry_type` varchar(30) NOT NULL CHECK (`entry_type` IN ('OPENING_BALANCE','INVOICE','PAYMENT','CREDIT_NOTE','ADJUSTMENT')) COMMENT 'OPENING_BALANCE | INVOICE | PAYMENT | CREDIT_NOTE | ADJUSTMENT',
  `debit_amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT 'Phát sinh tăng nợ',
  `credit_amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT 'Phát sinh giảm nợ',
  `ref_type` varchar(40),
  `ref_id` bigint,
  `entry_date` date NOT NULL,
  `note` text,
  `reverses_entry_id` bigint,
  CONSTRAINT fk_ar_ledger_entries_reverses_entry_id FOREIGN KEY (`reverses_entry_id`) REFERENCES `ar_ledger_entries`(`id`),
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_ar_ledger_entries_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_ar_ledger_entries_1 CHECK (debit_amount >= 0 AND credit_amount >= 0 AND ((debit_amount > 0) <> (credit_amount > 0))),
  KEY ix_ledger_customer (`customer_id`, `entry_date`, `id`),
  KEY ix_ar_ledger_entries_customer_id (`customer_id`),
  KEY ix_ar_ledger_entries_reverses_entry_id (`reverses_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SỔ CÔNG NỢ phải thu. CHỈ INSERT; sửa sai bằng bút toán đảo (reverses_entry_id). OPENING_BALANCE dùng nhập số dư đầu kỳ từ dữ liệu cũ.';

CREATE TABLE `customer_balances` (
  `customer_id` bigint,
  CONSTRAINT fk_customer_balances_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `outstanding_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `overdue_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'Khoá lạc quan (optimistic lock)',
  PRIMARY KEY (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Số dư nợ hiện tại (cache của SUM sổ công nợ). Cập nhật CÙNG transaction với sổ; khoá hàng này khi kiểm hạn mức để 2 đơn đồng thời không cùng vượt.';

CREATE TABLE `reconciliation_statements` (
  `id` bigint AUTO_INCREMENT,
  `statement_no` varchar(30) NOT NULL UNIQUE,
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_reconciliation_statements_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `period_from` date NOT NULL,
  `period_to` date NOT NULL,
  `opening_balance` decimal(18,2) NOT NULL DEFAULT 0,
  `total_debit` decimal(18,2) NOT NULL DEFAULT 0,
  `total_credit` decimal(18,2) NOT NULL DEFAULT 0,
  `closing_balance` decimal(18,2) NOT NULL DEFAULT 0,
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','SENT','CONFIRMED','DISPUTED')) COMMENT 'DRAFT | SENT | CONFIRMED | DISPUTED',
  `customer_response_note` text,
  `responded_by` bigint,
  CONSTRAINT fk_reconciliation_statements_responded_by FOREIGN KEY (`responded_by`) REFERENCES `users`(`id`),
  `responded_at` datetime,
  `file_id` bigint,
  CONSTRAINT fk_reconciliation_statements_file_id FOREIGN KEY (`file_id`) REFERENCES `file_objects`(`id`),
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_reconciliation_statements_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_reconciliation_statements_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_reconciliation_statements_1 CHECK (period_to >= period_from),
  KEY ix_reconciliation_statements_customer_id (`customer_id`),
  KEY ix_reconciliation_statements_file_id (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Biên bản đối chiếu công nợ định kỳ (S7-06, Should).';

CREATE TABLE `payment_reminder_logs` (
  `id` bigint AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  CONSTRAINT fk_payment_reminder_logs_invoice_id FOREIGN KEY (`invoice_id`) REFERENCES `invoices`(`id`),
  `reminder_type` varchar(30) NOT NULL CHECK (`reminder_type` IN ('BEFORE_DUE_3D','OVERDUE')) COMMENT 'BEFORE_DUE_3D | OVERDUE',
  `sent_on` date NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_payment_reminder_logs_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_payment_reminder_logs_invoice_id_reminder_type_sent_on (`invoice_id`, `reminder_type`, `sent_on`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Chống gửi trùng nhắc hạn: mỗi hoá đơn, mỗi loại nhắc, mỗi ngày chỉ 1 dòng (S7-08).';

-- ---------------------------------------------------------------------
-- EP-08: Trả hàng & Điều chỉnh
-- ---------------------------------------------------------------------

CREATE TABLE `sales_returns` (
  `id` bigint AUTO_INCREMENT,
  `return_no` varchar(30) NOT NULL UNIQUE,
  `invoice_id` bigint NOT NULL,
  CONSTRAINT fk_sales_returns_invoice_id FOREIGN KEY (`invoice_id`) REFERENCES `invoices`(`id`),
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_sales_returns_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `warehouse_id` bigint NOT NULL COMMENT 'Kho nhận hàng trả',
  CONSTRAINT fk_sales_returns_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `reason_code` varchar(30) NOT NULL CHECK (`reason_code` IN ('DAMAGED','WRONG_ITEM','EXPIRED','OTHER')) COMMENT 'DAMAGED | WRONG_ITEM | EXPIRED | OTHER',
  `reason_note` text,
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','SUBMITTED','RECEIVED','CANCELLED')) COMMENT 'DRAFT | SUBMITTED | RECEIVED | CANCELLED',
  `confirmed_by` bigint,
  CONSTRAINT fk_sales_returns_confirmed_by FOREIGN KEY (`confirmed_by`) REFERENCES `users`(`id`),
  `confirmed_at` datetime,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_sales_returns_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_sales_returns_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  KEY ix_sales_returns_invoice_id (`invoice_id`),
  KEY ix_sales_returns_customer_id (`customer_id`),
  KEY ix_sales_returns_warehouse_id (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu trả hàng từ đại lý. Quản lý kho xác nhận nhận hàng mới cộng tồn (S8-01).';

CREATE TABLE `sales_return_items` (
  `id` bigint AUTO_INCREMENT,
  `return_id` bigint NOT NULL,
  CONSTRAINT fk_sales_return_items_return_id FOREIGN KEY (`return_id`) REFERENCES `sales_returns`(`id`),
  `invoice_item_id` bigint NOT NULL,
  CONSTRAINT fk_sales_return_items_invoice_item_id FOREIGN KEY (`invoice_item_id`) REFERENCES `invoice_items`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_sales_return_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_sales_return_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `unit_id` bigint NOT NULL,
  CONSTRAINT fk_sales_return_items_unit_id FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`),
  `unit_factor` decimal(18,4) NOT NULL,
  `qty` decimal(18,3) NOT NULL,
  `qty_base` decimal(18,3) NOT NULL,
  `condition` varchar(30) NOT NULL DEFAULT 'GOOD' CHECK (`condition` IN ('GOOD','DAMAGED')) COMMENT 'GOOD | DAMAGED',
  `to_warehouse_id` bigint NOT NULL COMMENT 'Hàng tốt về kho bán, hàng hỏng về kho lỗi',
  CONSTRAINT fk_sales_return_items_to_warehouse_id FOREIGN KEY (`to_warehouse_id`) REFERENCES `warehouses`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_sales_return_items_1 CHECK (qty > 0 AND qty_base > 0),
  KEY ix_sales_return_items_return_id (`return_id`),
  KEY ix_sales_return_items_invoice_item_id (`invoice_item_id`),
  KEY ix_sales_return_items_product_id (`product_id`),
  KEY ix_sales_return_items_lot_id (`lot_id`),
  KEY ix_sales_return_items_unit_id (`unit_id`),
  KEY ix_sales_return_items_to_warehouse_id (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng trả: số lượng trả không vượt (đã giao - đã trả trước đó) - kiểm ở service + trigger nếu cần.';

CREATE TABLE `credit_notes` (
  `id` bigint AUTO_INCREMENT,
  `credit_note_no` varchar(30) NOT NULL UNIQUE,
  `invoice_id` bigint NOT NULL,
  CONSTRAINT fk_credit_notes_invoice_id FOREIGN KEY (`invoice_id`) REFERENCES `invoices`(`id`),
  `return_id` bigint UNIQUE,
  CONSTRAINT fk_credit_notes_return_id FOREIGN KEY (`return_id`) REFERENCES `sales_returns`(`id`),
  `customer_id` bigint NOT NULL,
  CONSTRAINT fk_credit_notes_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`),
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0 CHECK (`total_amount` > 0),
  `reason` text,
  `status` varchar(30) NOT NULL DEFAULT 'ISSUED' CHECK (`status` IN ('ISSUED','CANCELLED')) COMMENT 'ISSUED | CANCELLED',
  `issued_by` bigint,
  CONSTRAINT fk_credit_notes_issued_by FOREIGN KEY (`issued_by`) REFERENCES `users`(`id`),
  `issued_at` datetime,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_credit_notes_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_credit_notes_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_credit_notes_1 CHECK (total_amount > 0),
  KEY ix_credit_notes_invoice_id (`invoice_id`),
  KEY ix_credit_notes_customer_id (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Chứng từ điều chỉnh giảm công nợ. Giữ liên kết hoá đơn gốc; hoá đơn gốc KHÔNG bị sửa (S8-02).';

CREATE TABLE `credit_note_items` (
  `id` bigint AUTO_INCREMENT,
  `credit_note_id` bigint NOT NULL,
  CONSTRAINT fk_credit_note_items_credit_note_id FOREIGN KEY (`credit_note_id`) REFERENCES `credit_notes`(`id`),
  `invoice_item_id` bigint NOT NULL,
  CONSTRAINT fk_credit_note_items_invoice_item_id FOREIGN KEY (`invoice_item_id`) REFERENCES `invoice_items`(`id`),
  `return_item_id` bigint,
  CONSTRAINT fk_credit_note_items_return_item_id FOREIGN KEY (`return_item_id`) REFERENCES `sales_return_items`(`id`),
  `qty_base` decimal(18,3) NOT NULL,
  `unit_price` decimal(18,2) NOT NULL DEFAULT 0 COMMENT 'Lấy từ dòng hoá đơn gốc',
  `amount` decimal(18,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY ix_credit_note_items_credit_note_id (`credit_note_id`),
  KEY ix_credit_note_items_invoice_item_id (`invoice_item_id`),
  KEY ix_credit_note_items_return_item_id (`return_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Giảm trừ theo ĐÚNG giá đã bán trên hoá đơn gốc, không phải giá hiện hành.';

CREATE TABLE `stock_adjustments` (
  `id` bigint AUTO_INCREMENT,
  `adjustment_no` varchar(30) NOT NULL UNIQUE,
  `warehouse_id` bigint NOT NULL,
  CONSTRAINT fk_stock_adjustments_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`),
  `reason_type` varchar(30) NOT NULL CHECK (`reason_type` IN ('DAMAGED','LOST','EXPIRED_DISPOSAL')) COMMENT 'DAMAGED | LOST | EXPIRED_DISPOSAL',
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (`status` IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED')) COMMENT 'DRAFT | PENDING_APPROVAL | APPROVED | REJECTED',
  `total_loss_value` decimal(18,2) NOT NULL DEFAULT 0,
  `approved_by` bigint,
  CONSTRAINT fk_stock_adjustments_approved_by FOREIGN KEY (`approved_by`) REFERENCES `users`(`id`),
  `approved_at` datetime,
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_stock_adjustments_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_stock_adjustments_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_stock_adjustments_1 CHECK (approved_by IS NULL OR approved_by <> created_by),
  KEY ix_stock_adjustments_warehouse_id (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Phiếu điều chỉnh tồn (hàng hỏng/mất/huỷ hết hạn). Người lập ≠ người duyệt (S8-03).';

CREATE TABLE `stock_adjustment_items` (
  `id` bigint AUTO_INCREMENT,
  `adjustment_id` bigint NOT NULL,
  CONSTRAINT fk_stock_adjustment_items_adjustment_id FOREIGN KEY (`adjustment_id`) REFERENCES `stock_adjustments`(`id`),
  `product_id` bigint NOT NULL,
  CONSTRAINT fk_stock_adjustment_items_product_id FOREIGN KEY (`product_id`) REFERENCES `products`(`id`),
  `lot_id` bigint NOT NULL,
  CONSTRAINT fk_stock_adjustment_items_lot_id FOREIGN KEY (`lot_id`) REFERENCES `lots`(`id`),
  `qty_change_base` decimal(18,3) NOT NULL,
  `unit_cost_snapshot` decimal(18,4) NOT NULL COMMENT 'NHẠY CẢM: giá vốn',
  `loss_value` decimal(18,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT ck_stock_adjustment_items_1 CHECK (qty_change_base <> 0),
  KEY ix_stock_adjustment_items_adjustment_id (`adjustment_id`),
  KEY ix_stock_adjustment_items_product_id (`product_id`),
  KEY ix_stock_adjustment_items_lot_id (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng điều chỉnh tồn; mọi phiếu đã duyệt đều sinh dòng trên thẻ kho (stock_movements).';

-- ---------------------------------------------------------------------
-- EP-09: Chỉ tiêu, Dashboard & Báo cáo
-- ---------------------------------------------------------------------

CREATE TABLE `sales_targets` (
  `id` bigint AUTO_INCREMENT,
  `period_month` date NOT NULL COMMENT 'Ngày đầu tháng, vd 2026-09-01',
  `user_id` bigint,
  CONSTRAINT fk_sales_targets_user_id FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  `region_id` bigint,
  CONSTRAINT fk_sales_targets_region_id FOREIGN KEY (`region_id`) REFERENCES `regions`(`id`),
  `target_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `is_locked` boolean NOT NULL DEFAULT false,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  CONSTRAINT fk_sales_targets_created_by FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint,
  CONSTRAINT fk_sales_targets_updated_by FOREIGN KEY (`updated_by`) REFERENCES `users`(`id`),
  PRIMARY KEY (`id`),
  CONSTRAINT ck_sales_targets_1 CHECK (target_amount >= 0),
  CONSTRAINT ck_sales_targets_2 CHECK ((user_id IS NOT NULL) + (region_id IS NOT NULL) = 1),
  KEY ix_sales_targets_user_id (`user_id`),
  KEY ix_sales_targets_region_id (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Chỉ tiêu doanh số theo tháng cho nhân viên HOẶC khu vực. Qua kỳ thì khoá; sửa phải có lý do và ghi audit_logs (S8-04). Báo cáo/dashboard dùng VIEW hoặc MATERIALIZED VIEW, không cần bảng mới.';

-- ---------------------------------------------------------------------
-- KHOÁ NGOẠI TRỎ TỚI BẢNG ĐỊNH NGHĨA SAU (tham chiếu vòng / thứ tự tạo bảng)
-- ---------------------------------------------------------------------
ALTER TABLE `users` ADD CONSTRAINT fk_users_avatar_file_id FOREIGN KEY (`avatar_file_id`) REFERENCES `file_objects`(`id`);
ALTER TABLE `users` ADD CONSTRAINT fk_users_customer_id FOREIGN KEY (`customer_id`) REFERENCES `customers`(`id`);
ALTER TABLE `user_warehouses` ADD CONSTRAINT fk_user_warehouses_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`);
ALTER TABLE `user_regions` ADD CONSTRAINT fk_user_regions_region_id FOREIGN KEY (`region_id`) REFERENCES `regions`(`id`);
ALTER TABLE `customers` ADD CONSTRAINT fk_customers_default_warehouse_id FOREIGN KEY (`default_warehouse_id`) REFERENCES `warehouses`(`id`);
ALTER TABLE `sales_orders` ADD CONSTRAINT fk_sales_orders_warehouse_id FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`);

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- TRIGGER: chặn sửa/xoá ở các bảng append-only (thẻ kho, lịch sử, sổ công nợ, audit log, ...)
-- Thân trigger chỉ 1 câu lệnh (không có dấu ; bên trong) nên KHÔNG cần DELIMITER.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_audit_logs_no_update BEFORE UPDATE ON `audit_logs` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_audit_logs_no_delete BEFORE DELETE ON `audit_logs` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_price_history_no_update BEFORE UPDATE ON `price_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'price_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_price_history_no_delete BEFORE DELETE ON `price_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'price_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_credit_limit_history_no_update BEFORE UPDATE ON `credit_limit_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'credit_limit_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_credit_limit_history_no_delete BEFORE DELETE ON `credit_limit_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'credit_limit_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_customer_block_history_no_update BEFORE UPDATE ON `customer_block_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer_block_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_customer_block_history_no_delete BEFORE DELETE ON `customer_block_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer_block_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_customer_assignment_history_no_update BEFORE UPDATE ON `customer_assignment_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer_assignment_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_customer_assignment_history_no_delete BEFORE DELETE ON `customer_assignment_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer_assignment_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_order_status_history_no_update BEFORE UPDATE ON `order_status_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'order_status_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_order_status_history_no_delete BEFORE DELETE ON `order_status_history` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'order_status_history chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_stock_movements_no_update BEFORE UPDATE ON `stock_movements` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'stock_movements chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_stock_movements_no_delete BEFORE DELETE ON `stock_movements` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'stock_movements chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_ar_ledger_entries_no_update BEFORE UPDATE ON `ar_ledger_entries` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ar_ledger_entries chỉ cho INSERT (append-only), không được sửa/xoá';
CREATE TRIGGER trg_ar_ledger_entries_no_delete BEFORE DELETE ON `ar_ledger_entries` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ar_ledger_entries chỉ cho INSERT (append-only), không được sửa/xoá';

-- ---------------------------------------------------------------------
-- TRIGGER: bảng giá ACTIVE không được chồng khoảng ngày hiệu lực trong cùng 1 nhóm khách hàng
-- (thay cho EXCLUDE...WITH gist của Postgres). Thân trigger có IF...END IF nên CẦN đổi DELIMITER.
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_price_lists_no_overlap_ins BEFORE INSERT ON `price_lists` FOR EACH ROW
BEGIN
  IF NEW.status = 'ACTIVE' AND EXISTS (
    SELECT 1 FROM `price_lists` p
    WHERE p.customer_group_id = NEW.customer_group_id AND p.status = 'ACTIVE'
      AND NEW.valid_from <= COALESCE(p.valid_to, '9999-12-31')
      AND COALESCE(NEW.valid_to, '9999-12-31') >= p.valid_from
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bảng giá ACTIVE bị chồng ngày hiệu lực với bảng giá khác cùng nhóm khách hàng';
  END IF;
END$$

CREATE TRIGGER trg_price_lists_no_overlap_upd BEFORE UPDATE ON `price_lists` FOR EACH ROW
BEGIN
  IF NEW.status = 'ACTIVE' AND EXISTS (
    SELECT 1 FROM `price_lists` p
    WHERE p.customer_group_id = NEW.customer_group_id AND p.status = 'ACTIVE' AND p.id <> NEW.id
      AND NEW.valid_from <= COALESCE(p.valid_to, '9999-12-31')
      AND COALESCE(NEW.valid_to, '9999-12-31') >= p.valid_from
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bảng giá ACTIVE bị chồng ngày hiệu lực với bảng giá khác cùng nhóm khách hàng';
  END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- DỮ LIỆU KHỞI TẠO TỐI THIỂU
-- ---------------------------------------------------------------------
INSERT INTO `roles` (`code`, `name`, `description`) VALUES
  ('CUSTOMER',      'Đại lý',               'Cửa hàng/đại lý mua sỉ, dùng cổng đặt hàng'),
  ('SALES_REP',     'Nhân viên kinh doanh', 'Người đi thị trường, chăm sóc nhóm đại lý'),
  ('SALES_MANAGER', 'Quản lý kinh doanh',   'Duyệt đơn ngoại lệ, xem giá vốn và biên lợi nhuận'),
  ('WAREHOUSE',     'Nhân viên kho',        'Soạn hàng, nhập kho, kiểm kê'),
  ('WH_MANAGER',    'Quản lý kho',          'Duyệt điều chỉnh tồn, chuyển kho, chốt kiểm kê'),
  ('ACCOUNTANT',    'Kế toán công nợ',      'Phát hành hoá đơn, ghi nhận thanh toán'),
  ('ADMIN',         'Quản trị hệ thống',    'Quản lý tài khoản, vai trò, nhật ký hệ thống');

INSERT INTO `customer_groups` (`code`, `name`) VALUES
  ('AGENT_L1', 'Đại lý cấp 1'), ('AGENT_L2', 'Đại lý cấp 2'), ('RETAIL', 'Khách lẻ');

INSERT INTO `system_settings` (`key`, `value`, `description`) VALUES
  ('auth.max_failed_logins',        '5',  'Số lần sai mật khẩu liên tiếp trước khi khoá tạm'),
  ('auth.lock_minutes',             '15', 'Thời gian khoá tạm (phút)'),
  ('auth.reset_token_minutes',      '30', 'Hiệu lực liên kết đặt lại mật khẩu (phút)'),
  ('inventory.near_expiry_days',    '30', 'Ngưỡng cảnh báo lô sắp hết hạn (ngày)'),
  ('inventory.reservation_ttl_hours','48','GIẢ ĐỊNH - cần PO xác nhận: giữ chỗ tự nhả sau bao lâu nếu đơn chưa xuất'),
  ('credit.overdue_mode',           'REQUIRE_APPROVAL', 'Đại lý bị gắn cờ: BLOCK (chặn) hoặc REQUIRE_APPROVAL (chuyển duyệt) - S7-04'),
  ('reminder.days_before_due',      '3',  'Nhắc thanh toán trước hạn (ngày) - S7-08');


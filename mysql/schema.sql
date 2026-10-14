-- ============================================================
-- 青山商店（qingshan-store）数据库结构
--
-- 适用环境：开发 / 测试 / 生产
-- 本文件只包含建库建表，不含 DROP TABLE、种子数据和演示数据。
--
-- 执行顺序：
--   1) schema.sql      建库建表（本文件）
--   2) seed-base.sql   基础数据：分类 / 角色 / 管理员（所有环境）
--   3) seed-demo.sql   演示商品（仅开发 / 演示环境）
--
-- 说明：
--   · 本文件是表结构的唯一来源，后续调整表结构只改这里。
--   · 使用 CREATE TABLE IF NOT EXISTS，可重复执行；但不会升级已有表结构，
--     旧库缺列 / 索引时仍需手工 ALTER。
--   · 原 mysql/migrations 下的增量脚本已合并进本文件，该目录不再使用。
--   · 数据库名硬编码为 qingshan-store；如需改名，请同步修改本文件及
--     seed-base.sql、seed-demo.sql、drop-dev.sql 中的 CREATE DATABASE / USE。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `qingshan-store` DEFAULT CHARACTER SET utf8mb4;
USE `qingshan-store`;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 商品分类及套装分类
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int DEFAULT NULL COMMENT '类型   1 商品分类 2 套装分类',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '分类名称',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '分类图片',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` int DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='商品分类及套装分类';

-- ------------------------------------------------------------
-- 商品
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_code` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '商品编号，SKU编号为“商品编号-序号”',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '商品分类id',
  `price` decimal(10,2) DEFAULT NULL COMMENT '商品价格',
  `image` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '图片',
  `description` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '描述信息',
  `status` int DEFAULT '1' COMMENT '0下架 1上架',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `sales` int NOT NULL DEFAULT '0' COMMENT '销量',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_product_name` (`name`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品';

-- ------------------------------------------------------------
-- 商品属性/规格
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_attr` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `name` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '属性名称',
  `attr_values` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '属性值列表JSON',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品属性/规格关系表';

-- ------------------------------------------------------------
-- 商品SKU
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `sku_code` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'SKU编码',
  `attrs` varchar(500) COLLATE utf8mb4_bin NOT NULL COMMENT '规格属性JSON，例如：{"颜色":"黑色","尺码":"L"}',
  `price` decimal(10,2) NOT NULL COMMENT 'SKU价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT 'SKU库存',
  `image` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'SKU图片',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_sku_code` (`sku_code`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品SKU';

-- ------------------------------------------------------------
-- 商品图片
-- type：1样式图 2详情长图
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `url` varchar(500) COLLATE utf8mb4_bin NOT NULL COMMENT '图片地址',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '图片类型 1样式图 2详情长图',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序，值越小越靠前',
  PRIMARY KEY (`id`),
  KEY `idx_product_id_type` (`product_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品图片';

-- ------------------------------------------------------------
-- 角色与菜单权限
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '角色名称',
  `code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '角色编码',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `menu_paths` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '可访问的路由名称，逗号分隔，* 表示全部',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='角色与菜单权限';

-- ------------------------------------------------------------
-- 员工信息
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码（BCrypt）',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `role_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'employee' COMMENT '角色编码',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='员工信息';

-- ------------------------------------------------------------
-- 用户信息
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '登录密码（BCrypt）',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_openid` (`openid`),
  -- TODO(prod): 手机号登录依赖 phone 唯一，但目前没有唯一索引，需补 UNIQUE KEY `idx_phone` (`phone`)
  -- 注意 MySQL 唯一索引允许多个 NULL，可以安全添加
  UNIQUE KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户信息';

-- ------------------------------------------------------------
-- 地址簿
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='地址簿';

-- ------------------------------------------------------------
-- 购物车
-- product_style 存储已选属性 JSON，如 {"颜色":"黑色","尺码":"L"}
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `product_id` bigint DEFAULT NULL COMMENT '商品id',
  `bundle_id` bigint DEFAULT NULL COMMENT '套装id',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU id',
  `product_style` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品样式（已选属性JSON）',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='购物车';

-- ------------------------------------------------------------
-- 商品浏览历史
-- 同一用户同一商品只保留一条，重复浏览刷新 browse_time
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `browse_time` datetime NOT NULL COMMENT '最近浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_browse_time` (`user_id`, `browse_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品浏览历史';

-- ------------------------------------------------------------
-- 组合商品/套装
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bundle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '套装分类id',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '套装名称',
  `price` decimal(10,2) NOT NULL COMMENT '套装价格',
  `status` int DEFAULT '1' COMMENT '售卖状态 0下架 1上架',
  `description` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '图片',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_bundle_name` (`name`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='组合商品/套装';

CREATE TABLE IF NOT EXISTS `bundle_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bundle_id` bigint DEFAULT NULL COMMENT '套装id',
  `product_id` bigint DEFAULT NULL COMMENT '商品id',
  `name` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '商品名称（冗余字段）',
  `price` decimal(10,2) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '商品单价（冗余字段）',
  `copies` int DEFAULT NULL COMMENT '商品份数',
  PRIMARY KEY (`id`),
  KEY `idx_bundle_id` (`bundle_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='套装商品关系';

-- ------------------------------------------------------------
-- 订单
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '订单号',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待付款 2待发货 3已发货 4配送中 5已完成 6已取消 7退款',
  `user_id` bigint NOT NULL COMMENT '下单用户',
  `address_book_id` bigint NOT NULL COMMENT '收货地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT '支付方式 1微信 2支付宝 3银行卡',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10,2) NOT NULL COMMENT '实付金额',
  `remark` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '收货地址',
  `user_name` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户名称',
  `consignee` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `estimated_ship_time` datetime DEFAULT NULL COMMENT '预计发货时间',
  `shipping_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '发货状态 1立即发货 0预约发货',
  `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间（订单完成时间）',
  `freight_amount` decimal(10,2) DEFAULT '0.00' COMMENT '运费',
  `invoice_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '发票类型 0不开发票 1电子普通发票 2增值税专用发票',
  `invoice_title` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票抬头',
  `express_company` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '快递公司',
  `tracking_number` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '物流单号',
  `refund_reason` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '售后原因',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额（仅退款/退货退款）',
  `refund_time` datetime DEFAULT NULL COMMENT '退款/售后完成时间',
  `refund_type` tinyint DEFAULT NULL COMMENT '售后类型 1仅退款 2退货退款 3换货 4补寄',
  `refund_status` tinyint NOT NULL DEFAULT '0' COMMENT '售后状态 0无售后 1售后申请中 2待买家退货 3买家已退货待商家收货 4售后完成 5售后申请被拒绝 6售后关闭',
  `refund_description` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '售后补充说明',
  `refund_images` varchar(1000) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '售后凭证图片，多个以逗号分隔',
  `refund_apply_time` datetime DEFAULT NULL COMMENT '售后申请时间',
  `refund_reject_reason` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '拒绝售后原因',
  `return_express_company` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '退货快递公司',
  `return_tracking_number` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '退货物流单号',
  `return_time` datetime DEFAULT NULL COMMENT '买家退货时间',
  `original_amount` decimal(10,2) DEFAULT NULL COMMENT '改价前订单金额',
  `adjust_reason` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '改价原因',
  `adjust_time` datetime DEFAULT NULL COMMENT '改价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_number` (`number`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_refund_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `product_id` bigint DEFAULT NULL COMMENT '商品id',
  `bundle_id` bigint DEFAULT NULL COMMENT '套装id',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU id',
  `product_attr` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '已选属性',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='订单明细表';

-- ------------------------------------------------------------
-- 订单物流轨迹
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_logistics_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `express_company` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '快递公司',
  `tracking_number` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '物流单号',
  `trace_time` datetime NOT NULL COMMENT '轨迹时间',
  `description` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '物流描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '物流状态 0暂无轨迹 1已揽收 2运输中 3配送中 4已签收 5异常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_tracking_number` (`tracking_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='订单物流轨迹';

-- ------------------------------------------------------------
-- 支付/退款回调日志
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment_notify_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notify_id` varchar(64) COLLATE utf8mb4_bin NOT NULL COMMENT '第三方通知id，用于幂等',
  `order_number` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '订单号',
  `notify_type` tinyint NOT NULL COMMENT '通知类型 1支付 2退款',
  `request_body` text COLLATE utf8mb4_bin COMMENT '通知原始报文',
  `process_status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态 0待处理 1成功 2失败',
  `error_msg` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_notify_id` (`notify_id`),
  KEY `idx_order_number` (`order_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='支付/退款回调日志';

-- ------------------------------------------------------------
-- 入库/出库单据头
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `stock_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '单据编号，RK 入库 / CK 出库',
  `type` tinyint NOT NULL COMMENT '类型 1入库 2出库',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待审核 1已审核',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '总数量',
  `total_kinds` int NOT NULL DEFAULT '0' COMMENT '商品种数（按商品+SKU去重）',
  `remark` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_user` bigint DEFAULT NULL COMMENT '审核人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_order_no` (`order_no`),
  KEY `idx_type_status_create` (`type`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='入库/出库单据';

-- ------------------------------------------------------------
-- 单据明细（保存商品/SKU 快照，商品后续改名或删除不影响历史单据）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `stock_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '单据id',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU id，无规格商品为空',
  `product_code` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '商品编号快照',
  `sku_code` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'SKU编号快照',
  `product_name` varchar(64) COLLATE utf8mb4_bin NOT NULL COMMENT '商品名称快照',
  `sku_attrs` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '规格属性JSON快照',
  `quantity` int NOT NULL COMMENT '数量',
  `stock_before` int DEFAULT NULL COMMENT '审核前库存',
  `stock_after` int DEFAULT NULL COMMENT '审核后库存',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='入库/出库单据明细';

-- ------------------------------------------------------------
-- 库存流水台账（审核通过时追加，只增不改）
-- 仅记录手工入库/出库，不包含销售、退款、商品编辑产生的库存变化
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `stock_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '单据id',
  `order_no` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '单据编号',
  `type` tinyint NOT NULL COMMENT '类型 1入库 2出库',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU id',
  `product_code` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '商品编号快照',
  `sku_code` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'SKU编号快照',
  `product_name` varchar(64) COLLATE utf8mb4_bin NOT NULL COMMENT '商品名称快照',
  `sku_attrs` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '规格属性JSON快照',
  `quantity` int NOT NULL COMMENT '变动数量，按 type 区分方向',
  `stock_before` int NOT NULL COMMENT '变动前库存',
  `stock_after` int NOT NULL COMMENT '变动后库存',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '操作人',
  PRIMARY KEY (`id`),
  KEY `idx_type_create` (`type`,`create_time`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_sku_code` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='库存流水台账';

-- ------------------------------------------------------------
-- 种子数据执行顺序：seed-base.sql（所有环境）→ seed-demo.sql（仅开发）
-- ------------------------------------------------------------

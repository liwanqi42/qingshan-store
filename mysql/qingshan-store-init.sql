-- ============================================================
-- 青山商店（qingshan-store）数据库完整初始化脚本
--
-- TODO(prod): 这是开发/演示环境脚本，包含 DROP TABLE 和演示商品数据，禁止直接在生产执行。
--   上线前拆成两份：
--     1) schema-only：只建库建表，不带 DROP
--     2) seed：只插入必要的基础数据（分类/管理员），演示商品不要导入
--   并修改默认管理员 admin/123456 的密码
-- ============================================================

CREATE DATABASE IF NOT EXISTS `qingshan-store` DEFAULT CHARACTER SET utf8mb4;
USE `qingshan-store`;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `payment_notify_log`;
DROP TABLE IF EXISTS `order_logistics_trace`;
DROP TABLE IF EXISTS `order_detail`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `bundle_item`;
DROP TABLE IF EXISTS `bundle`;
DROP TABLE IF EXISTS `browse_history`;
DROP TABLE IF EXISTS `shopping_cart`;
DROP TABLE IF EXISTS `address_book`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `employee`;
DROP TABLE IF EXISTS `product_image`;
DROP TABLE IF EXISTS `product_sku`;
DROP TABLE IF EXISTS `product_attr`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;

-- ------------------------------------------------------------
-- 商品分类及套装分类
-- ------------------------------------------------------------
CREATE TABLE `category` (
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

INSERT INTO `category` (`id`,`type`,`name`,`image`,`sort`,`status`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES
    (11,1,'数码配件','/images/category/digital.svg',10,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (12,1,'家居日用','/images/category/home.svg',9,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (13,2,'组合套装','/images/category/bundle.svg',12,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (14,1,'服饰鞋包','/images/category/clothing.svg',11,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (15,1,'美妆个护','/images/category/beauty.svg',13,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (16,1,'食品饮料','/images/category/food.svg',4,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (17,1,'母婴玩具','/images/category/baby.svg',5,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (18,1,'运动户外','/images/category/sports.svg',6,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (19,1,'办公文具','/images/category/stationery.svg',7,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (20,1,'清洁用品','/images/category/cleaning.svg',8,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (21,1,'厨房用品','/images/category/kitchen.svg',11,1,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1);

-- ------------------------------------------------------------
-- 商品
-- ------------------------------------------------------------
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
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
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品';

INSERT INTO `product` (`id`,`name`,`category_id`,`price`,`image`,`description`,`status`,`stock`,`sales`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES
    (46,'无线蓝牙耳机',11,199.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/556a2d16-01d9-4452-9045-a2545e4baa0d.jpg','蓝牙5.3，入耳式，长续航',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (47,'快充充电宝',11,129.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/703ed3f2-1330-4ddd-94ea-10d708eeff83.jpg','10000mAh容量，支持快充',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (48,'手机支架',11,29.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/d992e39c-d2e9-4179-a3f6-20d2c5e73565.jpg','桌面可调节手机支架',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (49,'保温杯',12,79.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/ae72a4bb-6af5-4ed3-ad44-d33e0abbbd7f.jpg','304不锈钢，长效保温',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (50,'抽纸',12,19.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','原生木浆，柔软亲肤',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (51,'洗衣液',20,39.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','深层洁净，清新香味',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (52,'玻璃水杯',21,25.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','耐热玻璃水杯',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (53,'不粘锅',21,159.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','家用不粘炒锅',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (54,'纯棉T恤',14,59.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','纯棉舒适短袖T恤',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (55,'运动鞋',18,299.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','轻便透气运动鞋',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (56,'双肩背包',14,129.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','大容量通勤双肩包',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (57,'雨伞',12,39.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','防风折叠晴雨伞',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (58,'护眼台灯',19,69.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','多档调光护眼台灯',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (59,'笔记本',19,9.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','简约横线笔记本',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (60,'中性笔',19,12.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','黑色0.5mm中性笔',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (61,'面霜',15,89.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','保湿滋润面霜',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (62,'洗发水',15,49.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','控油清爽洗发水',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (63,'牙膏',15,19.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','清新薄荷牙膏',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (64,'坚果礼盒',16,99.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','混合坚果礼盒',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (65,'矿泉水',16,24.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','550ml整箱矿泉水',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (66,'蓝牙音箱',11,149.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','便携无线蓝牙音箱',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (67,'数据线',11,19.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','快充数据线',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (68,'收纳箱',12,59.00,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','可折叠家用收纳箱',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (69,'宠物玩具',21,29.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png','耐咬宠物互动玩具',1,100,0,'2024-01-01 10:00:00','2024-01-01 10:00:00',1,1),
    (73,'小熊软糖',16,18.90,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/4298533e-9eee-4802-aecd-049cc163eddd.jpg','大众口味的小熊软糖。',1,87,0,'2026-10-20 16:47:40','2026-10-20 16:47:40',1,1);

-- ------------------------------------------------------------
-- 商品属性/规格
-- ------------------------------------------------------------
CREATE TABLE `product_attr` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `name` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '属性名称',
  `attr_values` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '属性值列表JSON',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品属性/规格关系表';

INSERT INTO `product_attr` (`id`,`product_id`,`name`,`attr_values`) VALUES
    (40,46,'颜色','["黑色","白色","蓝色"]'),
    (41,46,'连接方式','["蓝牙5.3","有线"]'),
    (42,47,'容量','["10000mAh","20000mAh"]'),
    (43,47,'颜色','["白色","黑色"]'),
    (44,49,'容量','["500ml","750ml"]'),
    (45,49,'颜色','["白色","蓝色","粉色"]'),
    (46,50,'规格','["3层100抽","4层120抽"]'),
    (47,51,'规格','["1kg","2kg","3kg"]'),
    (48,54,'尺码','["S","M","L","XL","XXL"]'),
    (49,54,'颜色','["白色","黑色","灰色"]'),
    (50,55,'尺码','["39","40","41","42","43","44"]'),
    (51,55,'颜色','["黑色","白色","红色"]'),
    (52,56,'颜色','["黑色","蓝色","灰色"]'),
    (53,57,'颜色','["黑色","蓝色","红色"]'),
    (54,58,'颜色','["白色","暖光","自然光"]'),
    (55,61,'规格','["50g","100g"]'),
    (56,62,'规格','["500ml","750ml"]'),
    (57,64,'规格','["混合坚果500g","混合坚果1kg"]'),
    (58,65,'规格','["550ml*24瓶","550ml*12瓶"]'),
    (59,66,'颜色','["黑色","蓝色"]'),
    (60,67,'长度','["1m","2m"]'),
    (61,68,'规格','["45L","66L","88L"]'),
    (62,69,'款式','["逗猫棒","磨牙球","飞盘"]'),
    (106,73,'颜色','["白色","蓝色","红色"]'),
    (107,73,'大小','["中","大","超大"]');

-- ------------------------------------------------------------
-- 商品SKU
-- ------------------------------------------------------------
CREATE TABLE `product_sku` (
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
-- 商品SKU（每个规格组合独立价格与库存，库存合计与原商品库存一致）
-- ------------------------------------------------------------
INSERT INTO `product_sku` (`id`,`product_id`,`sku_code`,`attrs`,`price`,`stock`,`image`,`status`,`create_time`,`update_time`) VALUES
(1,46,'P46-001','{"颜色":"黑色","连接方式":"蓝牙5.3"}',199.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(2,46,'P46-002','{"颜色":"黑色","连接方式":"有线"}',199.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(3,46,'P46-003','{"颜色":"白色","连接方式":"蓝牙5.3"}',199.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(4,46,'P46-004','{"颜色":"白色","连接方式":"有线"}',199.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(5,46,'P46-005','{"颜色":"蓝色","连接方式":"蓝牙5.3"}',199.00,16,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(6,46,'P46-006','{"颜色":"蓝色","连接方式":"有线"}',199.00,16,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(7,47,'P47-001','{"容量":"10000mAh","颜色":"白色"}',129.00,25,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(8,47,'P47-002','{"容量":"10000mAh","颜色":"黑色"}',129.00,25,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(9,47,'P47-003','{"容量":"20000mAh","颜色":"白色"}',129.00,25,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(10,47,'P47-004','{"容量":"20000mAh","颜色":"黑色"}',129.00,25,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(11,49,'P49-001','{"容量":"500ml","颜色":"白色"}',79.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(12,49,'P49-002','{"容量":"500ml","颜色":"蓝色"}',79.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(13,49,'P49-003','{"容量":"500ml","颜色":"粉色"}',79.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(14,49,'P49-004','{"容量":"750ml","颜色":"白色"}',79.00,17,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(15,49,'P49-005','{"容量":"750ml","颜色":"蓝色"}',79.00,16,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(16,49,'P49-006','{"容量":"750ml","颜色":"粉色"}',79.00,16,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(17,50,'P50-001','{"规格":"3层100抽"}',19.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(18,50,'P50-002','{"规格":"4层120抽"}',19.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(19,51,'P51-001','{"规格":"1kg"}',39.90,34,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(20,51,'P51-002','{"规格":"2kg"}',39.90,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(21,51,'P51-003','{"规格":"3kg"}',39.90,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(22,54,'P54-001','{"尺码":"S","颜色":"白色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(23,54,'P54-002','{"尺码":"S","颜色":"黑色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(24,54,'P54-003','{"尺码":"S","颜色":"灰色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(25,54,'P54-004','{"尺码":"M","颜色":"白色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(26,54,'P54-005','{"尺码":"M","颜色":"黑色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(27,54,'P54-006','{"尺码":"M","颜色":"灰色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(28,54,'P54-007','{"尺码":"L","颜色":"白色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(29,54,'P54-008','{"尺码":"L","颜色":"黑色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(30,54,'P54-009','{"尺码":"L","颜色":"灰色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(31,54,'P54-010','{"尺码":"XL","颜色":"白色"}',59.00,7,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(32,54,'P54-011','{"尺码":"XL","颜色":"黑色"}',59.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(33,54,'P54-012','{"尺码":"XL","颜色":"灰色"}',59.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(34,54,'P54-013','{"尺码":"XXL","颜色":"白色"}',59.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(35,54,'P54-014','{"尺码":"XXL","颜色":"黑色"}',59.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(36,54,'P54-015','{"尺码":"XXL","颜色":"灰色"}',59.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(37,55,'P55-001','{"尺码":"39","颜色":"黑色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(38,55,'P55-002','{"尺码":"39","颜色":"白色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(39,55,'P55-003','{"尺码":"39","颜色":"红色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(40,55,'P55-004','{"尺码":"40","颜色":"黑色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(41,55,'P55-005','{"尺码":"40","颜色":"白色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(42,55,'P55-006','{"尺码":"40","颜色":"红色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(43,55,'P55-007','{"尺码":"41","颜色":"黑色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(44,55,'P55-008','{"尺码":"41","颜色":"白色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(45,55,'P55-009','{"尺码":"41","颜色":"红色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(46,55,'P55-010','{"尺码":"42","颜色":"黑色"}',299.00,6,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(47,55,'P55-011','{"尺码":"42","颜色":"白色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(48,55,'P55-012','{"尺码":"42","颜色":"红色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(49,55,'P55-013','{"尺码":"43","颜色":"黑色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(50,55,'P55-014','{"尺码":"43","颜色":"白色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(51,55,'P55-015','{"尺码":"43","颜色":"红色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(52,55,'P55-016','{"尺码":"44","颜色":"黑色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(53,55,'P55-017','{"尺码":"44","颜色":"白色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(54,55,'P55-018','{"尺码":"44","颜色":"红色"}',299.00,5,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(55,56,'P56-001','{"颜色":"黑色"}',129.00,34,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(56,56,'P56-002','{"颜色":"蓝色"}',129.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(57,56,'P56-003','{"颜色":"灰色"}',129.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(58,57,'P57-001','{"颜色":"黑色"}',39.00,34,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(59,57,'P57-002','{"颜色":"蓝色"}',39.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(60,57,'P57-003','{"颜色":"红色"}',39.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(61,58,'P58-001','{"颜色":"白色"}',69.00,34,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(62,58,'P58-002','{"颜色":"暖光"}',69.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(63,58,'P58-003','{"颜色":"自然光"}',69.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(64,61,'P61-001','{"规格":"50g"}',89.00,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(65,61,'P61-002','{"规格":"100g"}',89.00,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(66,62,'P62-001','{"规格":"500ml"}',49.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(67,62,'P62-002','{"规格":"750ml"}',49.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(68,64,'P64-001','{"规格":"混合坚果500g"}',99.00,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(69,64,'P64-002','{"规格":"混合坚果1kg"}',99.00,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(70,65,'P65-001','{"规格":"550ml*24瓶"}',24.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(71,65,'P65-002','{"规格":"550ml*12瓶"}',24.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(72,66,'P66-001','{"颜色":"黑色"}',149.00,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(73,66,'P66-002','{"颜色":"蓝色"}',149.00,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(74,67,'P67-001','{"长度":"1m"}',19.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(75,67,'P67-002','{"长度":"2m"}',19.90,50,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(76,68,'P68-001','{"规格":"45L"}',59.00,34,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(77,68,'P68-002','{"规格":"66L"}',59.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(78,68,'P68-003','{"规格":"88L"}',59.00,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(79,69,'P69-001','{"款式":"逗猫棒"}',29.90,34,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(80,69,'P69-002','{"款式":"磨牙球"}',29.90,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(81,69,'P69-003','{"款式":"飞盘"}',29.90,33,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(82,73,'P73-001','{"颜色":"白色","大小":"中"}',18.90,10,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(83,73,'P73-002','{"颜色":"白色","大小":"大"}',18.90,10,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(84,73,'P73-003','{"颜色":"白色","大小":"超大"}',18.90,10,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(85,73,'P73-004','{"颜色":"蓝色","大小":"中"}',18.90,10,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(86,73,'P73-005','{"颜色":"蓝色","大小":"大"}',18.90,10,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(87,73,'P73-006','{"颜色":"蓝色","大小":"超大"}',18.90,10,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(88,73,'P73-007','{"颜色":"红色","大小":"中"}',18.90,9,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(89,73,'P73-008','{"颜色":"红色","大小":"大"}',18.90,9,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00'),
(90,73,'P73-009','{"颜色":"红色","大小":"超大"}',18.90,9,NULL,1,'2026-10-20 21:00:00','2026-10-20 21:00:00');

-- ------------------------------------------------------------
-- 商品图片
-- type：1样式图 2详情长图
-- ------------------------------------------------------------
CREATE TABLE `product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `url` varchar(500) COLLATE utf8mb4_bin NOT NULL COMMENT '图片地址',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '图片类型 1样式图 2详情长图',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序，值越小越靠前',
  PRIMARY KEY (`id`),
  KEY `idx_product_id_type` (`product_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品图片';

INSERT INTO `product_image` (`id`,`product_id`,`url`,`type`,`sort`) VALUES
    (1,46,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/556a2d16-01d9-4452-9045-a2545e4baa0d.jpg',1,0),
    (2,47,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/703ed3f2-1330-4ddd-94ea-10d708eeff83.jpg',1,0),
    (3,48,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/d992e39c-d2e9-4179-a3f6-20d2c5e73565.jpg',1,0),
    (4,49,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/ae72a4bb-6af5-4ed3-ad44-d33e0abbbd7f.jpg',1,0),
    (5,50,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (6,51,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (7,52,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (8,53,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (9,54,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (10,55,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (11,56,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (12,57,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (13,58,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (14,59,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (15,60,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (16,61,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (17,62,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (18,63,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (19,64,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (20,65,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (21,66,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (22,67,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (23,68,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (24,69,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/0e9cacfb-528e-4361-ac55-835478f13705.png',1,0),
    (44,73,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/4298533e-9eee-4802-aecd-049cc163eddd.jpg',1,0),
    (45,73,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/60d2d5f8-be57-48bd-a796-925f5c180c24.jpg',1,1),
    (46,73,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/739b31a5-d922-4ee4-8cca-6869039ade3e.jpg',1,2),
    (47,73,'https://qingshan-store.oss-cn-beijing.aliyuncs.com/228856c9-8a89-4821-b002-9d1e4e4fa636.jpg',1,3);

-- ------------------------------------------------------------
-- 员工信息
-- ------------------------------------------------------------
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码（BCrypt）',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='员工信息';

-- 默认密码 123456（BCrypt），生产环境首次登录后必须立即修改
INSERT INTO `employee` (`id`,`name`,`username`,`password`,`phone`,`sex`,`id_number`,`status`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES
    (1,'管理员','admin','$2a$10$1AvxBUOmDSE6tpzpP5p3fusX/B9125r3cm1uz.Y.owUAt3L8Bo6va','13812312312','1','110101199001010047',1,'2022-02-15 15:51:20','2022-02-17 09:16:20',10,1);

-- ------------------------------------------------------------
-- 用户信息
-- ------------------------------------------------------------
CREATE TABLE `user` (
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
CREATE TABLE `address_book` (
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
CREATE TABLE `shopping_cart` (
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
CREATE TABLE `browse_history` (
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
CREATE TABLE `bundle` (
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

CREATE TABLE `bundle_item` (
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
CREATE TABLE `orders` (
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

CREATE TABLE `order_detail` (
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
CREATE TABLE `order_logistics_trace` (
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
CREATE TABLE `payment_notify_log` (
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

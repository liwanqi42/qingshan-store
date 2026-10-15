-- ============================================================
-- 青山商店（qingshan-store）基础数据
--
-- 适用环境：开发 / 测试 / 生产
-- 执行顺序：先执行 schema.sql，再执行本文件。
--   生产环境到此为止，不要再执行 seed-demo.sql（演示商品）。
--
-- 说明：
--   · 本文件可重复执行：分类和管理员使用 INSERT IGNORE，不会覆盖已有数据，
--     也不会把管理员改过的密码重置回默认值。
--   · sys_role 使用 ON DUPLICATE KEY UPDATE 只更新 menu_paths，
--     便于新增菜单后补齐权限，不影响角色名称和状态。
--   · 数据库名硬编码为 qingshan-store；如需改名，请同步修改本文件及
--     schema.sql、seed-demo.sql、drop-dev.sql 中的 CREATE DATABASE / USE。
--
-- ⚠ 默认管理员：admin / 123456
--   ⚠ 仅用于首次登录，上线前必须在管理端修改密码！
--   ⚠ 新增员工的初始密码仍由后端常量决定，生产也需一并更换。
-- ============================================================

USE `qingshan-store`;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 商品分类及套装分类
-- ------------------------------------------------------------
INSERT IGNORE INTO `category` (`id`,`type`,`name`,`image`,`sort`,`status`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES
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
-- 角色与菜单权限
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`,`name`,`code`,`status`,`menu_paths`,`create_time`,`update_time`) VALUES
    (1,'超级管理员','admin',1,'DashboardView,StatisticsView,OrderView,CategoryView,ProductView,BundleView,StockInView,StockOutView,StockLedgerView,SystemUserView,SystemMenuView,SystemRoleView,SystemDictView,ShopView','2022-02-15 15:51:20','2022-02-15 15:51:20'),
    (2,'普通员工','employee',1,'DashboardView,SystemUserView','2022-02-15 15:51:20','2022-02-15 15:51:20')
ON DUPLICATE KEY UPDATE `menu_paths` = VALUES(`menu_paths`);

-- ------------------------------------------------------------
-- 默认管理员
-- 密码：123456（BCrypt），生产环境首次登录后必须立即修改
-- ------------------------------------------------------------
INSERT IGNORE INTO `employee` (`id`,`name`,`username`,`password`,`phone`,`sex`,`id_number`,`role_code`,`status`,`must_change_password`,`token_version`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES
    (1,'管理员','admin','$2a$10$1AvxBUOmDSE6tpzpP5p3fusX/B9125r3cm1uz.Y.owUAt3L8Bo6va','13812312312','1','110101199001010047','admin',1,1,0,'2022-02-15 15:51:20','2022-02-17 09:16:20',10,1);

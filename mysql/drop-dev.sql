-- ============================================================
-- 青山商店（qingshan-store）开发环境重置脚本
--
-- ⚠ 仅用于开发 / 演示环境，禁止在生产执行！
-- ⚠ 执行后会删除全部业务表（数据库本身保留），需要重新执行：
--     1) schema.sql
--     2) seed-base.sql
--     3) seed-demo.sql（需要演示商品时）
--
-- 说明：数据库名硬编码为 qingshan-store；如需改名，请同步修改本文件及
--       schema.sql、seed-base.sql、seed-demo.sql 中的 CREATE DATABASE / USE。
-- ============================================================

USE `qingshan-store`;

DROP TABLE IF EXISTS `stock_ledger`;
DROP TABLE IF EXISTS `stock_order_item`;
DROP TABLE IF EXISTS `stock_order`;
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
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `product_image`;
DROP TABLE IF EXISTS `product_sku`;
DROP TABLE IF EXISTS `product_attr`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;

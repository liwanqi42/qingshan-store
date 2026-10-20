-- ============================================================
-- 性能基线冒烟数据（仅用于独立测试库，禁止在生产执行）
--
-- 用法：
--   mysql -u root -p qingshan-perf < scripts/perf-seed-orders.sql
--
-- 说明：
--   · 生成 10000 笔订单，每笔 2 条明细，订单号以 PERF 开头；
--   · 重复执行会先清理旧的 PERF 数据，再重新生成；
--   · 订单时间分布在最近 30 天，便于报表导出覆盖到这批数据。
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS seed_perf_orders;

DELIMITER $$
CREATE PROCEDURE seed_perf_orders(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE oid BIGINT;
    DECLARE order_time DATETIME;

    DELETE od FROM order_detail od
        JOIN orders o ON od.order_id = o.id
        WHERE o.number LIKE 'PERF%';
    DELETE FROM orders WHERE number LIKE 'PERF%';

    WHILE i <= total DO
        SET order_time = DATE_SUB(NOW(), INTERVAL (i % 30) DAY) + INTERVAL (i % 1440) MINUTE;

        INSERT INTO orders (
            number, status, user_id, address_book_id, order_time, checkout_time,
            pay_method, pay_status, amount, coupon_amount, remark,
            phone, address, user_name, consignee,
            shipping_status, freight_amount, invoice_type, refund_status
        ) VALUES (
            CONCAT('PERF', LPAD(i, 8, '0')),
            ELT(1 + (i % 3), 2, 5, 6),
            999999, 999999, order_time, order_time,
            1, 1, 59.90, 0.00, '性能冒烟数据',
            '13800000000', '北京市朝阳区性能测试路 1 号', '性能测试用户', '测试收货人',
            1, 0.00, 0, 0
        );
        SET oid = LAST_INSERT_ID();

        INSERT INTO order_detail (name, image, order_id, product_id, sku_id, product_attr, number, amount)
        VALUES
            ('性能测试商品A', '', oid, 1, NULL, NULL, 1, 29.95),
            ('性能测试商品B', '', oid, 2, NULL, NULL, 1, 29.95);

        SET i = i + 1;
        IF i % 1000 = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
END$$
DELIMITER ;

CALL seed_perf_orders(10000);
DROP PROCEDURE IF EXISTS seed_perf_orders;

SELECT COUNT(*) AS perf_orders FROM orders WHERE number LIKE 'PERF%';

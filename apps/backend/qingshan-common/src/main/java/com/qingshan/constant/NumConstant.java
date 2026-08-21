package com.qingshan.constant;
/**
 * 业务数值常量
 */
public class NumConstant {

    /** 单品最大购买数量 */
    public static final Integer PRODUCT_MAX_BUY = 999;

    /** 金额保留的小数位数 */
    public static final int AMOUNT_SCALE = 2;

    /** 下单防重锁时长（秒），锁覆盖整个事务提交过程 */
    public static final long ORDER_SUBMIT_LOCK_SECONDS = 5;

    /** 未支付订单默认保留时长（分钟），超时后由定时任务自动取消 */
    public static final long ORDER_TIMEOUT_MINUTES_DEFAULT = 15;

    /** 每个用户默认允许同时存在的未支付订单数量 */
    public static final int ORDER_MAX_UNPAID_ORDERS_DEFAULT = 999;

    /** 统计窗口内默认允许被自动取消的订单次数 */
    public static final int ORDER_MAX_TIMEOUT_ORDERS_DEFAULT = 999;

    /** 超时订单默认统计窗口（小时） */
    public static final long ORDER_TIMEOUT_WINDOW_HOURS_DEFAULT = 24;
}

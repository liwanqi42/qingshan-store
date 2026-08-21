package com.qingshan.properties;

import com.qingshan.constant.NumConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单业务配置，未配置时使用 NumConstant 中的默认值
 */
@Component
@ConfigurationProperties(prefix = "qingshan.order")
@Data
public class OrderProperties {

    /**
     * 未支付订单的保留时长（分钟），超时后由定时任务自动取消
     */
    private long timeoutMinutes = NumConstant.ORDER_TIMEOUT_MINUTES_DEFAULT;

    /**
     * 每个用户允许同时存在的未支付订单数量
     */
    private int maxUnpaidOrders = NumConstant.ORDER_MAX_UNPAID_ORDERS_DEFAULT;

    /**
     * 统计窗口内允许被自动取消的订单次数，超过后暂时禁止下单
     */
    private int maxTimeoutOrders = NumConstant.ORDER_MAX_TIMEOUT_ORDERS_DEFAULT;

    /**
     * 超时订单统计窗口（小时）
     */
    private long timeoutWindowHours = NumConstant.ORDER_TIMEOUT_WINDOW_HOURS_DEFAULT;

    /**
     * 是否启用模拟支付接口（仅开发环境使用，生产环境必须保持 false）
     *
     * TODO(wechat-pay): 接入真实微信支付后删除该字段，以及 application-dev.yml 中的 mock-pay-enabled 配置
     */
    private boolean mockPayEnabled = false;
}

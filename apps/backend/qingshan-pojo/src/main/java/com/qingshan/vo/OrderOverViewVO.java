package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单概览数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOverViewVO implements Serializable {
    //待付款数量
    private Integer pendingPaymentOrders;

    //待发货数量
    private Integer awaitingShipmentOrders;

    //已发货数量
    private Integer shippedOrders;

    //配送中数量
    private Integer inTransitOrders;

    //已完成数量
    private Integer completedOrders;

    //已取消数量
    private Integer cancelledOrders;

    //全部订单
    private Integer allOrders;
}

package com.qingshan.service;

import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderSubmitVO;

public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 模拟支付成功（仅开发环境使用）
     *
     * TODO(wechat-pay): 接入真实微信支付后删除该方法
     * @param orderNumber
     */
    void mockPay(String orderNumber);

    /**
     * 取消超时未支付的订单并回滚库存
     */
    void cancelTimeoutOrders();
}

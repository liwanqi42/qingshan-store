package com.qingshan.service;

import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.result.PageResult;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.vo.UserOrderVO;

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

    /**
     * 用户端订单分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);

    /**
     * 用户端订单详情
     * @param orderNumber 订单号
     * @return
     */
    UserOrderVO getDetail4User(String orderNumber);

    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id);

    /**
     * 再来一单
     * @param id
     * @return
     */
    void repetition(Long id);

    /**
     * 删除订单
     * @param id
     */
    void deleteOrderById(Long id);
}

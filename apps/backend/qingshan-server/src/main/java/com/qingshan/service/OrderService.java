package com.qingshan.service;

import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.dto.RefundApplyDTO;
import com.qingshan.result.PageResult;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderRemindVO;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.vo.RefundApplyVO;
import com.qingshan.vo.RefundDetailVO;
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
     * @param status 小程序端状态
     * @param keyword 订单号或商品名称关键词
     * @param days 时间范围（天），0表示不限
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status, String keyword, int days);

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
     * 申请售后
     * @param refundApplyDTO 订单号、退款原因
     * @return 退款编号
     */
    RefundApplyVO refundApply(RefundApplyDTO refundApplyDTO);

    /**
     * 退款详情
     * @param refundNo 退款编号（订单号）
     * @return 退款详情
     */
    RefundDetailVO refundDetail(String refundNo);

    /**
     * 撤销退款申请
     * @param refundNo 退款编号（订单号）
     */
    void undoRefund(String refundNo);

    /**
     * 确认收货
     * @param id 订单id
     */
    void confirmGoods(Long id);

    /**
     * 删除订单
     * @param id
     */
    void deleteOrderById(Long id);

    /**
     * 个人中心订单提醒数量
     * @return 待付款、待发货、待收货数量
     */
    OrderRemindVO getRemindCount4User();
}

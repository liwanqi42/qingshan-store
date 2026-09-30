package com.qingshan.service;

import com.qingshan.dto.*;
import com.qingshan.result.PageResult;
import com.qingshan.vo.*;

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
     * @param refundApplyDTO 订单号、售后类型、售后原因
     * @return 售后编号
     */
    RefundApplyVO refundApply(RefundApplyDTO refundApplyDTO);

    /**
     * 售后详情
     * @param refundNo 售后编号（订单号）
     * @return 售后详情
     */
    RefundDetailVO refundDetail(String refundNo);

    /**
     * 撤销售后申请
     * @param refundNo 售后编号（订单号）
     */
    void undoRefund(String refundNo);

    /**
     * 管理端同意售后：
     * 未发货订单的仅退款直接退款；已发货订单的退货退款进入待买家退货；
     * 换货/补寄只完成售后
     * @param ordersRefundDTO 订单id
     */
    void approveRefund(OrdersRefundDTO ordersRefundDTO);

    /**
     * 买家填写退货物流
     * @param ordersLogisticsDTO 订单id、退货快递公司、退货物流单号
     */
    void refundReturn(OrdersLogisticsDTO ordersLogisticsDTO);

    /**
     * 管理端确认收到退货并退款
     * @param ordersRefundDTO 订单id
     */
    void receiveRefund(OrdersRefundDTO ordersRefundDTO);

    /**
     * 管理端拒绝售后
     * @param ordersRefundRejectDTO 订单id、拒绝原因
     */
    void rejectRefund(OrdersRefundRejectDTO ordersRefundRejectDTO);

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

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端订单详情
     * @param id 订单id
     * @return
     */
    OrderVO getDetail4Admin(Long id);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 派送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * 催发货
     * @param id
     */
    void reminderToShip(Long id);
}

package com.qingshan.service.support;

import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.dto.OrdersLogisticsDTO;
import com.qingshan.entity.AddressBook;
import com.qingshan.entity.OrderDetail;
import com.qingshan.entity.Orders;
import com.qingshan.entity.ShoppingCart;
import com.qingshan.exception.AddressBookBusinessException;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.mapper.AddressBookMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.properties.OrderProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 订单业务校验
 */
@Component
public class OrderValidator {

    /**
     * 下单防重锁的 key 前缀，锁不主动释放，覆盖整个事务提交过程
     */
    private static final String SUBMIT_LOCK_KEY = "order:submit:lock:";

    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private OrderStatusHandler orderStatusHandler;

    @Autowired
    private ShopStatusService shopStatusService;

    /**
     * 打烊期间禁止新下单；已下单的支付、取消、退款不受影响
     */
    public void checkShopOpen() {
        if (!shopStatusService.isOpen()) {
            throw new OrderBusinessException(MessageConstant.SHOP_CLOSED);
        }
    }

    /**
     * 校验收货地址存在且属于当前用户
     */
    public AddressBook checkAddress(Long addressBookId, Long userId) {
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook == null || !Objects.equals(addressBook.getUserId(), userId)) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        return addressBook;
    }

    /**
     * 下单前风控：防重复提交、未支付订单上限、超时未支付冷却
     */
    public void checkSubmitFrequency(Long userId) {
        // 同一用户 5 秒内只能提交一次；锁不主动释放，覆盖事务提交过程
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(SUBMIT_LOCK_KEY + userId, "1",
                        NumConstant.ORDER_SUBMIT_LOCK_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new OrderBusinessException("请勿重复提交订单");
        }

        // 近 N 小时被系统自动取消的订单过多，判定为恶意占库存，暂时禁止下单
        int timeoutOrders = orderMapper.countTimeoutOrders(userId, MessageConstant.TIMEOUT_CANCEL_REASON,
                LocalDateTime.now().minusHours(orderProperties.getTimeoutWindowHours()));
        if (timeoutOrders >= orderProperties.getMaxTimeoutOrders()) {
            throw new OrderBusinessException("近期超时未支付订单过多，"
                    + orderProperties.getTimeoutWindowHours() + "小时后再试");
        }

        // 未支付订单数量上限，避免同一用户长期占用库存
        int unpaidOrders = orderMapper.countUnpaidOrders(userId);
        if (unpaidOrders >= orderProperties.getMaxUnpaidOrders()) {
            throw new OrderBusinessException("您有未支付的订单，请先支付或等待超时取消");
        }
    }

    /**
     * 校验购物车不为空
     */
    public void checkShoppingCartNotEmpty(List<ShoppingCart> cartList) {
        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
    }

    /**
     * 查询订单并校验属于当前用户
     */
    public Orders requireOwnedById(Long id, Long userId) {
        Orders orders = orderMapper.getById(id);
        checkOwned(orders, userId);
        return orders;
    }

    /**
     * 查询订单并校验属于当前用户
     */
    public Orders requireOwnedByNumber(String orderNumber, Long userId) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        checkOwned(orders, userId);
        return orders;
    }

    /**
     * 查询订单，不存在时抛出业务异常
     */
    public Orders requireById(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    /**
     * 查询订单，不存在时抛出业务异常
     */
    public Orders requireByNumber(String orderNumber) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    /**
     * 只有待发货且没有退款记录的订单可以接单、拒单
     */
    public void checkCanConfirm(Orders orders) {
        checkAwaitingShipment(orders);
    }

    /**
     * 订单只有状态为待发货且没有退款记录才可以拒单
     */
    public void checkCanReject(Orders orders) {
        checkAwaitingShipment(orders);
    }

    /**
     * 只有未完成的订单可以取消，已完成/已取消/已退款/退款中的订单不能重复取消
     */
    public void checkCanCancel(Orders orders) {
        if (!orderStatusHandler.isCancellable(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        checkNoRefundRecord(orders);
    }

    /**
     * 校验订单状态为已发货，且没有退款记录
     */
    public void checkCanDeliver(Orders orders) {
        if (!Orders.STATUS_SHIPPED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        checkNoRefundRecord(orders);
    }

    /**
     * 校验订单状态为配送中，且没有退款记录
     */
    public void checkCanComplete(Orders orders) {
        if (!Orders.STATUS_IN_TRANSIT.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        checkNoRefundRecord(orders);
    }

    /**
     * 只有待发货阶段（待接单、已接单待派送）且没有售后处理中的订单可以催发货
     */
    public void checkCanRemindShip(Orders orders) {
        if (!Orders.STATUS_AWAITING_SHIPMENT.equals(orders.getStatus())
                && !Orders.STATUS_SHIPPED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        checkNoRefundRecord(orders);
    }

    /**
     * 只允许删除已完成、已取消、已退款的订单
     */
    public void checkCanDelete(Orders orders) {
        if (!orderStatusHandler.isDeletable(orders)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 校验售后类型：仅退款、退货退款、换货、补寄
     */
    public void checkRefundType(Integer refundType) {
        if (!Orders.REFUND_TYPE_REFUND_ONLY.equals(refundType)
                && !Orders.REFUND_TYPE_RETURN_REFUND.equals(refundType)
                && !Orders.REFUND_TYPE_EXCHANGE.equals(refundType)
                && !Orders.REFUND_TYPE_RESEND.equals(refundType)) {
            throw new OrderBusinessException("请选择售后类型");
        }
    }

    /**
     * 按发货状态校验售后类型：
     * 未发货只能仅退款（同意后直接退款）；
     * 已发货只能退货退款/换货/补寄（退货退款需要先寄回商品再退款）
     */
    public void checkRefundTypeAllowed(Orders orders, Integer refundType) {
        if (!orderStatusHandler.isShipped(orders.getStatus())) {
            if (!Orders.REFUND_TYPE_REFUND_ONLY.equals(refundType)) {
                throw new OrderBusinessException(MessageConstant.REFUND_TYPE_ONLY_BEFORE_SHIP);
            }
            return;
        }
        if (Orders.REFUND_TYPE_REFUND_ONLY.equals(refundType)) {
            throw new OrderBusinessException(MessageConstant.REFUND_TYPE_RETURN_AFTER_SHIP);
        }
    }

    /**
     * 只有待买家退货的订单可以填写退货物流
     */
    public void checkCanSubmitReturn(Orders orders) {
        if (!Orders.REFUND_STATUS_WAIT_RETURN.equals(orders.getRefundStatus())) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_WAIT_RETURN);
        }
    }

    /**
     * 校验退货物流信息
     */
    public void checkReturnLogistics(OrdersLogisticsDTO ordersLogisticsDTO) {
        String expressCompany = ordersLogisticsDTO.getExpressCompany();
        String trackingNumber = ordersLogisticsDTO.getTrackingNumber();
        if (expressCompany == null || expressCompany.trim().isEmpty()
                || trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new OrderBusinessException(MessageConstant.REFUND_RETURN_LOGISTICS_REQUIRED);
        }
    }

    /**
     * 只有买家已退货、待商家收货的订单可以确认收货并退款
     */
    public void checkCanReceiveRefund(Orders orders) {
        if (!Orders.REFUND_STATUS_RETURNED.equals(orders.getRefundStatus())) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_RETURNED);
        }
    }

    /**
     * 校验售后原因
     */
    public void checkRefundReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new OrderBusinessException("请选择售后原因");
        }
    }

    /**
     * 校验订单明细不为空
     */
    public void checkDetailsNotEmpty(List<OrderDetail> orderDetailList) {
        if (orderDetailList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }

    /**
     * 只有退款申请中的订单可以审核退款
     */
    public void checkCanHandleRefund(Orders orders) {
        if (!Orders.REFUND_STATUS_APPLIED.equals(orders.getRefundStatus())) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_PENDING);
        }
    }

    /**
     * 校验拒绝退款原因
     */
    public void checkRejectReason(String rejectReason) {
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new OrderBusinessException(MessageConstant.REFUND_REJECT_REASON_REQUIRED);
        }
    }

    private void checkAwaitingShipment(Orders orders) {
        if (!Orders.STATUS_AWAITING_SHIPMENT.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        checkNoRefundRecord(orders);
    }

    /**
     * 有待处理的售后申请时给出明确提示，避免只报“订单状态错误”；
     * 已退款/售后已完成的订单也不能继续接单、派送、完成
     */
    private void checkNoRefundRecord(Orders orders) {
        if (orderStatusHandler.isRefunding(orders)) {
            throw new OrderBusinessException(MessageConstant.ORDER_REFUNDING);
        }
        if (orderStatusHandler.isRefundedOrRefunding(orders)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    private void checkOwned(Orders orders, Long userId) {
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (orders == null || !Objects.equals(orders.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }
}

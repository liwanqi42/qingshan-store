package com.qingshan.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.*;
import com.qingshan.entity.AddressBook;
import com.qingshan.entity.OrderDetail;
import com.qingshan.entity.Orders;
import com.qingshan.entity.User;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.mapper.OrderDetailMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.UserMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.CouponService;
import com.qingshan.service.OrderService;
import com.qingshan.service.support.OrderAmountCalculator;
import com.qingshan.service.support.OrderConverter;
import com.qingshan.service.support.OrderManager;
import com.qingshan.service.support.ProductCacheManager;
import com.qingshan.service.support.OrderStatusHandler;
import com.qingshan.service.support.OrderValidator;
import com.qingshan.utils.PageUtil;
import com.qingshan.utils.SnowflakeIdGenerator;
import com.qingshan.utils.WeChatPayUtil;
import com.qingshan.vo.*;
import com.qingshan.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductCacheManager productCacheManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private OrderValidator orderValidator;

    @Autowired
    private OrderManager orderManager;

    @Autowired
    private OrderStatusHandler orderStatusHandler;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderAmountCalculator orderAmountCalculator;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1.下单前校验：营业状态、风控、收货地址
        Long userId = BaseContext.getCurrentId();
        orderValidator.checkShopOpen();
        orderValidator.checkSubmitFrequency(userId);
        AddressBook addressBook = orderValidator.checkAddress(ordersSubmitDTO.getAddressBookId(), userId);

        //2.确定下单商品（购物车或直接购买），金额由服务端按商品价格计算
        OrderManager.OrderItems orderItems = orderManager.resolveOrderItems(ordersSubmitDTO.getItems(), userId);

        //3.优惠券校验与抵扣计算，金额只信服务端
        Long userCouponId = ordersSubmitDTO.getUserCouponId();
        BigDecimal goodsAmount = orderItems.getAmount();
        BigDecimal couponAmount = userCouponId == null
                ? BigDecimal.ZERO
                : couponService.resolveDiscount(userCouponId, userId, goodsAmount);
        BigDecimal payableAmount = orderAmountCalculator.payableAmount(goodsAmount, BigDecimal.ZERO, couponAmount);

        //4.扣减库存（下单即占用库存，超时未支付时再回滚）
        orderManager.deductStock(orderItems);

        //5.写入订单及订单明细
        Orders orders = orderConverter.toOrder(ordersSubmitDTO, addressBook, userId,
                snowflakeIdGenerator.nextIdStr(), LocalDateTime.now(), payableAmount,
                couponAmount, userCouponId);
        orderMapper.insert(orders);

        //6.条件锁定优惠券，防止同一张券并发用于多个订单；失败时整单回滚
        if (userCouponId != null) {
            couponService.lockForOrder(userCouponId, userId, orders.getId());
        }

        orderDetailMapper.insertBatch(orderConverter.toOrderDetails(orders.getId(), orderItems.getItems()));

        //7.清空本次下单涉及的购物车记录，直接购买的商品不在购物车中，无需清理
        orderManager.clearCartItems(orderItems, userId);

        return orderConverter.toOrderSubmitVO(orders);
    }

    /**
     * 订单支付：只允许支付自己的、待付款且未支付的订单，金额由服务端读取
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderValidator.requireOwnedByNumber(ordersPaymentDTO.getOrderNumber(), userId);

        if (Orders.PAY_STATUS_PAID.equals(orders.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_ALREADY_PAID);
        }
        if (!Orders.STATUS_PENDING_PAYMENT.equals(orders.getStatus())
                || !Orders.PAY_STATUS_UN_PAID.equals(orders.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        User user = userMapper.getById(userId);
        if (user == null || !StringUtils.hasText(user.getOpenid())) {
            // 微信支付必须绑定 openid，手机号注册用户需先完成微信授权登录
            throw new OrderBusinessException(MessageConstant.ORDER_NEED_WECHAT_LOGIN);
        }

        BigDecimal amount = orders.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("订单金额异常，无法支付");
        }

        //调用微信支付接口，生成预支付交易单，金额取订单实际应付金额
        JSONObject jsonObject = weChatPayUtil.pay(
                orders.getNumber(),
                amount,
                MessageConstant.ORDER_NAME_DESC,
                user.getOpenid()
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException(MessageConstant.ORDER_ALREADY_PAID);
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Transactional
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderValidator.requireByNumber(outTradeNo);

        // 只有待付款订单才能改为已支付，避免覆盖超时取消任务的结果
        int updated = orderMapper.paySuccess(ordersDB.getId(), LocalDateTime.now());
        if (updated == 0) {
            // 重复回调可以忽略；若订单已被自动取消但用户仍支付成功，需要走退款处理
            // TODO(wechat-pay): 订单已取消/超时而微信回调仍成功时，必须自动发起退款，并记录 payment_notify_log，
            //   不能只打 warn 后放过（否则用户付了钱但订单是关闭状态）
            log.warn("订单{}当前不是待付款状态，跳过支付状态更新", outTradeNo);
            return;
        }

        // 支付成功后核销优惠券
        couponService.markUsedByOrder(ordersDB.getId());

        // 支付成功后累加商品销量
        changeProductSales(ordersDB.getId(), 1);

        // 只有状态真正变更时才推送来单提醒，避免重复回调/已取消订单误报
        notifyNewOrder(ordersDB);
    }

    /**
     * 通过WebSocket向管理端推送来单提醒
     *
     * @param orders 已支付订单
     */
    private void notifyNewOrder(Orders orders) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号: " + orders.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 调整订单内商品的销量：支付成功时累加，退款/取消后扣减
     * @param orderId 订单id
     * @param delta 变化量，1 表示累加、-1 表示扣减
     */
    private void changeProductSales(Long orderId, int delta) {
        boolean changed = false;
        for (OrderDetail detail : orderDetailMapper.listByOrderId(orderId)) {
            if (detail.getProductId() == null || detail.getNumber() == null || detail.getNumber() <= 0) {
                continue;
            }
            productMapper.addSales(detail.getProductId(), delta * detail.getNumber());
            changed = true;
        }
        // 用户端商品列表有缓存，销量变化后需要立即失效
        if (changed) {
            productCacheManager.clearProductCache();
        }
    }

    /**
     * 模拟支付成功（仅开发环境使用）
     *
     * TODO(wechat-pay): 接入真实微信支付后删除该方法，支付结果由 PayNotifyController 回调 paySuccess() 更新
     * @param orderNumber
     */
    @Override
    @Transactional
    public void mockPay(String orderNumber) {
        Orders ordersDB = orderValidator.requireOwnedByNumber(orderNumber, BaseContext.getCurrentId());
        // 幂等：已支付的订单直接返回，重复点击不报错
        if (Orders.PAY_STATUS_PAID.equals(ordersDB.getPayStatus())) {
            return;
        }
        // 已取消、已退款等非待付款订单不能再支付
        if (!Orders.STATUS_PENDING_PAYMENT.equals(ordersDB.getStatus())
                || !Orders.PAY_STATUS_UN_PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        int updated = orderMapper.paySuccess(ordersDB.getId(), LocalDateTime.now());
        if (updated == 0) {
            // 已取消/已退款的订单不能再支付
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 支付成功后核销优惠券
        couponService.markUsedByOrder(ordersDB.getId());

        // 支付成功后累加商品销量
        changeProductSales(ordersDB.getId(), 1);

        // 开发环境模拟支付同样要推送来单提醒，方便联调
        notifyNewOrder(ordersDB);
    }

    /**
     * 取消超时未支付的订单并回滚库存
     */
    @Override
    @Transactional
    public void cancelTimeoutOrders() {
        int cancelled = orderManager.cancelTimeoutOrders();
        if (cancelled > 0) {
            log.info("自动取消超时未支付订单{}笔", cancelled);
        }
    }

    /**
     * 用户端订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status 小程序端状态
     * @return
     */
    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status, String keyword, int days) {
        // 设置分页，限制单页最大条数
        PageHelper.startPage(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        // 小程序端状态转成查询条件（一个前端状态可能对应多个后端状态）
        orderStatusHandler.applyUserStatusFilter(ordersPageQueryDTO, status);
        if (keyword != null && !keyword.trim().isEmpty()) {
            ordersPageQueryDTO.setKeyword(keyword.trim());
        }
        // 时间筛选：近 N 天
        if (days > 0) {
            ordersPageQueryDTO.setBeginTime(LocalDateTime.now().minusDays(days));
        }

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<UserOrderVO> list = orderManager.toUserOrderVOList(page);
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 用户端订单详情
     * @param orderNumber 订单号
     * @return
     */
    @Override
    public UserOrderVO getDetail4User(String orderNumber) {
        Orders orders = orderValidator.requireOwnedByNumber(orderNumber, BaseContext.getCurrentId());
        UserOrderVO orderVO = orderManager.toUserOrderVO(orders);
        // 详情页只展示门牌号等详细地址，去掉省市区前缀
        orderVO.getAddress().setAddress(orderManager.detailAddress(orders));
        return orderVO;
    }

    /**
     * 用户取消订单
     * @return
     */
    @Override
    @Transactional
    public void userCancelById(Long id) {
        Long userId = BaseContext.getCurrentId();
        orderValidator.requireOwnedById(id, userId);

        // 只有待付款订单可以取消；已支付订单需要走退款流程，暂不支持
        int updated = orderMapper.cancelByUser(id, userId, LocalDateTime.now(), "用户取消");
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 回滚下单时占用的库存
        orderManager.restoreStock(id);

        // 退回优惠券
        couponService.returnForOrder(id);
    }

    /**
     * 再来一单：把订单商品重新加入购物车
     *
     * @param id
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        orderValidator.requireOwnedById(id, userId);

        List<OrderDetail> orderDetailList = orderManager.listDetails(id);
        orderValidator.checkDetailsNotEmpty(orderDetailList);

        // 逐项复用加购逻辑：校验商品/SKU/库存，并合并同规格商品
        orderManager.reAddToShoppingCart(orderDetailList);
    }

    /**
     * 确认收货：只有已派送（配送中）的订单可以确认
     *
     * @param id
     */
    @Override
    public void confirmGoods(Long id) {
        Long userId = BaseContext.getCurrentId();
        orderValidator.requireOwnedById(id, userId);
        // 条件更新，避免重复确认或对未发货订单确认
        int updated = orderMapper.confirmGoods(id, userId);
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 申请售后：支持待发货、待收货、已完成的已支付订单。
     * 未发货只能仅退款，已发货只能退货退款/换货/补寄
     *
     * @param refundApplyDTO
     * @return 售后编号
     */
    @Override
    public RefundApplyVO refundApply(RefundApplyDTO refundApplyDTO) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderValidator.requireOwnedByNumber(refundApplyDTO.getOrderNo(), userId);
        // 兼容未传类型的历史客户端：未发货按仅退款，已发货按退货退款
        Integer refundType = refundApplyDTO.getRefundType() == null
                ? (orderStatusHandler.isShipped(orders.getStatus())
                    ? Orders.REFUND_TYPE_RETURN_REFUND : Orders.REFUND_TYPE_REFUND_ONLY)
                : refundApplyDTO.getRefundType();
        orderValidator.checkRefundType(refundType);
        orderValidator.checkRefundTypeAllowed(orders, refundType);
        orderValidator.checkRefundReason(refundApplyDTO.getReason());

        // 条件更新，避免重复申请或对未支付订单申请
        int updated = orderMapper.applyRefund(orders.getId(), userId, refundType,
                refundApplyDTO.getReason().trim(), LocalDateTime.now());
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        return RefundApplyVO.builder().refundNo(orders.getNumber()).build();
    }

    /**
     * 退款详情
     *
     * @param refundNo 退款编号（订单号）
     * @return 退款详情
     */
    @Override
    public RefundDetailVO refundDetail(String refundNo) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderValidator.requireOwnedByNumber(refundNo, userId);
        return orderManager.toRefundDetailVO(orders);
    }

    /**
     * 撤销退款申请，只有处理中的申请可以撤销
     *
     * @param refundNo 退款编号（订单号）
     */
    @Override
    public void undoRefund(String refundNo) {
        Long userId = BaseContext.getCurrentId();
        orderValidator.requireOwnedByNumber(refundNo, userId);
        int updated = orderMapper.undoRefund(refundNo, userId);
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 管理端同意售后：
     * 未发货的仅退款直接退款；已发货的退货退款进入待买家退货；
     * 换货/补寄只完成售后
     * 先抢占售后申请再处理，避免并发审核重复退款
     *
     * @param ordersRefundDTO 订单id
     */
    @Override
    @Transactional
    public void approveRefund(OrdersRefundDTO ordersRefundDTO) {
        Orders ordersDB = orderValidator.requireById(ordersRefundDTO.getId());
        orderValidator.checkCanHandleRefund(ordersDB);

        if (orderStatusHandler.isMoneyRefund(ordersDB.getRefundType())) {
            // 已发货：先等待买家寄回商品，商家确认收货后才退款
            if (orderStatusHandler.isShipped(ordersDB.getStatus())) {
                int updated = orderMapper.agreeReturnRefund(ordersDB.getId());
                if (updated == 0) {
                    throw new OrderBusinessException(MessageConstant.REFUND_NOT_PENDING);
                }
                return;
            }

            // 未发货：同意后直接退款。
            // 条件更新抢占退款申请，避免重复审核或用户已撤销申请后仍发起退款；
            // 退款失败时抛出异常，订单状态随事务回滚到退款申请中
            int updated = orderMapper.approveRefund(ordersDB.getId(), LocalDateTime.now());
            if (updated == 0) {
                throw new OrderBusinessException(MessageConstant.REFUND_NOT_PENDING);
            }

            orderManager.refundIfPaid(ordersDB);

            // 待发货/已接单待派送的订单尚未出库，退款后回滚库存
            orderManager.restoreStock(ordersDB.getId());

            // 退款后优惠券退回（未过期可再次使用）
            couponService.returnForOrder(ordersDB.getId());

            // 退款后扣减销量
            changeProductSales(ordersDB.getId(), -1);
            return;
        }

        // 换货/补寄：只完成售后，不发起退款，订单保持已支付状态
        int updated = orderMapper.completeAfterSales(ordersDB.getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_PENDING);
        }
    }

    /**
     * 买家填写退货物流：待买家退货 -> 买家已退货待商家收货
     *
     * @param ordersLogisticsDTO 订单id、退货快递公司、退货物流单号
     */
    @Override
    public void refundReturn(OrdersLogisticsDTO ordersLogisticsDTO) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderValidator.requireOwnedById(ordersLogisticsDTO.getId(), userId);
        orderValidator.checkCanSubmitReturn(orders);
        orderValidator.checkReturnLogistics(ordersLogisticsDTO);

        int updated = orderMapper.submitReturnLogistics(orders.getId(), userId,
                ordersLogisticsDTO.getExpressCompany().trim(),
                ordersLogisticsDTO.getTrackingNumber().trim(), LocalDateTime.now());
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_WAIT_RETURN);
        }
    }

    /**
     * 管理端确认收到退货并退款：买家已退货待商家收货 -> 售后完成/已退款。
     * 先条件更新抢占，再发起退款；退款失败时事务回滚，订单仍停留在待商家收货
     *
     * @param ordersRefundDTO 订单id
     */
    @Override
    @Transactional
    public void receiveRefund(OrdersRefundDTO ordersRefundDTO) {
        Orders ordersDB = orderValidator.requireById(ordersRefundDTO.getId());
        orderValidator.checkCanReceiveRefund(ordersDB);

        int updated = orderMapper.receiveRefund(ordersDB.getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_RETURNED);
        }

        orderManager.refundIfPaid(ordersDB);
        // 商品已退回商家，恢复库存
        orderManager.restoreStock(ordersDB.getId());

        // 退款后优惠券退回（未过期可再次使用）
        couponService.returnForOrder(ordersDB.getId());

        // 退款后扣减销量
        changeProductSales(ordersDB.getId(), -1);
    }

    /**
     * 管理端拒绝退款：订单状态不变，恢复商户后续接单、发货操作
     *
     * @param ordersRefundRejectDTO 订单id、拒绝原因
     */
    @Override
    public void rejectRefund(OrdersRefundRejectDTO ordersRefundRejectDTO) {
        Orders ordersDB = orderValidator.requireById(ordersRefundRejectDTO.getId());
        orderValidator.checkCanHandleRefund(ordersDB);
        orderValidator.checkRejectReason(ordersRefundRejectDTO.getRejectReason());

        // 条件更新，避免重复审核或用户已撤销申请
        int updated = orderMapper.rejectRefund(ordersDB.getId(),
                ordersRefundRejectDTO.getRejectReason().trim());
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.REFUND_NOT_PENDING);
        }
    }

    /**
     * 删除订单：只允许删除已完成、已取消、已退款的订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteOrderById(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = orderValidator.requireOwnedById(id, userId);
        orderValidator.checkCanDelete(ordersDB);

        orderDetailMapper.deleteByOrderId(id);
        orderMapper.deleteByIdAndUser(id, userId);
    }

    /**
     * 个人中心订单提醒数量
     * @return 待付款、待发货、待收货数量
     */
    @Override
    public OrderRemindVO getRemindCount4User() {
        OrderRemindVO remind = orderMapper.getRemindCount(BaseContext.getCurrentId());
        // 用户没有任何订单时，各统计值按 0 处理
        if (remind == null) {
            return OrderRemindVO.builder().waitPay(0).notExpress(0).waitReceive(0).build();
        }
        return remind;
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setPage(PageUtil.pageNum(ordersPageQueryDTO.getPage()));
        ordersPageQueryDTO.setPageSize(PageUtil.pageSize(ordersPageQueryDTO.getPageSize()));
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = orderManager.toOrderVOList(page);
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 管理端订单详情
     * @param id 订单id
     * @return
     */
    @Override
    public OrderVO getDetail4Admin(Long id) {
        Orders orders = orderValidator.requireById(id);
        return orderManager.toOrderVO(orders);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        return orderManager.statistics();
    }

    /**
     * 接单（待发货 -> 已接单待派送）
     * 接单不代表已发货，发货时间在派送时记录
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderValidator.requireById(ordersConfirmDTO.getId());
        orderValidator.checkCanConfirm(ordersDB);

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.STATUS_SHIPPED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒单（待发货 -> 已取消，已支付订单同时退款）
     * @param ordersRejectionDTO
     */
    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderValidator.requireById(ordersRejectionDTO.getId());
        // 订单只有状态为2（待发货）且没有退款记录才可以拒单
        orderValidator.checkCanReject(ordersDB);

        //用户已支付，需要先退款；退款失败则不允许拒单
        boolean paid = orderManager.refundIfPaid(ordersDB);

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.STATUS_CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        if (paid) {
            orders.setPayStatus(Orders.PAY_STATUS_REFUND);
        }

        orderMapper.update(orders);

        // 回滚下单时占用的库存
        orderManager.restoreStock(ordersDB.getId());

        // 退回优惠券
        couponService.returnForOrder(ordersDB.getId());

        // 已支付订单退款后扣减销量
        if (paid) {
            changeProductSales(ordersDB.getId(), -1);
        }
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    @Override
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单
        Orders ordersDB = orderValidator.requireById(ordersCancelDTO.getId());
        // 只有未完成的订单可以取消，已完成/已取消/已退款/退款中的订单不能重复取消
        orderValidator.checkCanCancel(ordersDB);

        //用户已支付，需要先退款；退款失败则不允许取消订单
        boolean paid = orderManager.refundIfPaid(ordersDB);

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.STATUS_CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        if (paid) {
            orders.setPayStatus(Orders.PAY_STATUS_REFUND);
        }
        orderMapper.update(orders);

        // 回滚下单时占用的库存
        orderManager.restoreStock(ordersDB.getId());

        // 退回优惠券
        couponService.returnForOrder(ordersDB.getId());

        // 已支付订单退款后扣减销量
        if (paid) {
            changeProductSales(ordersDB.getId(), -1);
        }
    }

    /**
     * 派送订单（已接单待派送 -> 配送中），此时商品才真正出库发货
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderValidator.requireById(id);
        // 校验订单状态为3（已接单待派送），且没有退款记录
        orderValidator.checkCanDeliver(ordersDB);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.STATUS_IN_TRANSIT);
        orders.setShipTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 完成订单（配送中 -> 已完成）
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderValidator.requireById(id);
        // 校验订单状态为4（配送中），且没有退款记录
        orderValidator.checkCanComplete(ordersDB);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.STATUS_COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 催发货：只允许催促自己的、仍处于待发货阶段的订单
     * @param id
     */
    @Override
    public void reminderToShip(Long id) {
        Orders ordersDB = orderValidator.requireOwnedById(id, BaseContext.getCurrentId());
        orderValidator.checkCanRemindShip(ordersDB);

        // type=1来单提醒，type=2登录后待接单提醒，type=3用户催发货
        Map<String, Object> map = new HashMap<>();
        map.put("type", 3);
        map.put("orderId", ordersDB.getId());
        map.put("content", "用户催发货，订单号: " + ordersDB.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}

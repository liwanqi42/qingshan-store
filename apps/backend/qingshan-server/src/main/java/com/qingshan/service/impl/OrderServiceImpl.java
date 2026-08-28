package com.qingshan.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.OrdersPageQueryDTO;
import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.dto.OrdersSubmitItemDTO;
import com.qingshan.dto.RefundApplyDTO;
import com.qingshan.dto.ShoppingCartDTO;
import com.qingshan.entity.*;
import com.qingshan.exception.AddressBookBusinessException;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.mapper.*;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.properties.OrderProperties;
import com.qingshan.result.PageResult;
import com.qingshan.service.OrderService;
import com.qingshan.service.ShoppingCartService;
import com.qingshan.service.support.OrderItemResolver;
import com.qingshan.service.support.OrderStockService;
import com.qingshan.utils.SnowflakeIdGenerator;
import com.qingshan.utils.WeChatPayUtil;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderRemindVO;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.vo.RefundApplyVO;
import com.qingshan.vo.RefundDetailVO;
import com.qingshan.vo.UserOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


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
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private OrderItemResolver orderItemResolver;

    @Autowired
    private OrderStockService orderStockService;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 退款详情页的时间格式
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1.下单前校验：风控、收货地址
        Long userId = BaseContext.getCurrentId();
        checkSubmitFrequency(userId);
        AddressBook addressBook = checkAddress(ordersSubmitDTO.getAddressBookId(), userId);

        //2.确定下单商品（购物车或直接购买），金额由服务端按商品价格计算
        OrderItems orderItems = resolveOrderItems(ordersSubmitDTO.getItems(), userId);

        //3.扣减库存（下单即占用库存，超时未支付时再回滚）
        for (ResolvedOrderItem item : orderItems.getItems()) {
            orderStockService.deduct(item);
        }

        //4.写入订单及订单明细
        Orders orders = createOrder(ordersSubmitDTO, addressBook, userId, orderItems.getAmount());
        orderMapper.insert(orders);
        orderDetailMapper.insertBatch(toOrderDetails(orders.getId(), orderItems.getItems()));

        //5.清空本次下单涉及的购物车记录，直接购买的商品不在购物车中，无需清理
        if (!orderItems.getCartIds().isEmpty()) {
            List<Long> cartIds = new ArrayList<>(new LinkedHashSet<>(orderItems.getCartIds()));
            shoppingCartMapper.deleteByIds(cartIds, userId);
        }

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 校验收货地址存在且属于当前用户
     */
    private AddressBook checkAddress(Long addressBookId, Long userId) {
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook == null || !Objects.equals(addressBook.getUserId(), userId)) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        return addressBook;
    }

    /**
     * 确定下单商品：优先使用前端提交的明细（直接购买的商品不在购物车中），否则读取购物车
     */
    private OrderItems resolveOrderItems(List<OrdersSubmitItemDTO> submitItems, Long userId) {
        List<ResolvedOrderItem> items = new ArrayList<>();
        List<Long> cartIds = new ArrayList<>();

        if (submitItems != null && !submitItems.isEmpty()) {
            for (OrdersSubmitItemDTO submitItem : submitItems) {
                items.add(orderItemResolver.resolve(submitItem.getProductId(), submitItem.getBundleId(),
                        submitItem.getSkuId(), submitItem.getProductStyle(), submitItem.getNumber()));
                if (submitItem.getCartId() != null) {
                    cartIds.add(submitItem.getCartId());
                }
            }
            return new OrderItems(items, cartIds);
        }

        List<ShoppingCart> cartList = shoppingCartMapper.list(ShoppingCart.builder().userId(userId).build());
        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        for (ShoppingCart cart : cartList) {
            items.add(ResolvedOrderItem.builder()
                    .productId(cart.getProductId())
                    .bundleId(cart.getBundleId())
                    .skuId(cart.getSkuId())
                    .name(cart.getName())
                    .image(cart.getImage())
                    .productStyle(cart.getProductStyle())
                    .number(cart.getNumber())
                    .amount(cart.getAmount())
                    .build());
            cartIds.add(cart.getId());
        }
        return new OrderItems(items, cartIds);
    }

    /**
     * 组装订单主体
     */
    private Orders createOrder(OrdersSubmitDTO ordersSubmitDTO, AddressBook addressBook,
                               Long userId, BigDecimal amount) {
        return Orders.builder()
                .number(snowflakeIdGenerator.nextIdStr())
                .status(Orders.STATUS_PENDING_PAYMENT)
                .userId(userId)
                .addressBookId(ordersSubmitDTO.getAddressBookId())
                .orderTime(LocalDateTime.now())
                .payMethod(ordersSubmitDTO.getPayMethod())
                .payStatus(Orders.PAY_STATUS_UN_PAID)
                .amount(amount)
                .remark(ordersSubmitDTO.getRemark())
                .phone(addressBook.getPhone())
                .consignee(addressBook.getConsignee())
                .address(formatAddress(addressBook))
                .estimatedShipTime(ordersSubmitDTO.getEstimatedShipTime())
                .shippingStatus(ordersSubmitDTO.getShippingStatus())
                .freightAmount(BigDecimal.ZERO)
                .invoiceType(ordersSubmitDTO.getInvoiceType())
                .invoiceTitle(ordersSubmitDTO.getInvoiceTitle())
                .build();
    }

    /**
     * 下单商品 -> 订单明细
     */
    private List<OrderDetail> toOrderDetails(Long orderId, List<ResolvedOrderItem> items) {
        List<OrderDetail> orderDetails = new ArrayList<>(items.size());
        for (ResolvedOrderItem item : items) {
            orderDetails.add(OrderDetail.builder()
                    .orderId(orderId)
                    .name(item.getName())
                    .image(item.getImage())
                    .productId(item.getProductId())
                    .bundleId(item.getBundleId())
                    .skuId(item.getSkuId())
                    .productAttr(item.getProductStyle())
                    .number(item.getNumber())
                    .amount(item.getAmount())
                    .build());
        }
        return orderDetails;
    }

    /**
     * 本次下单的商品及待清理的购物车记录
     */
    private static class OrderItems {
        private final List<ResolvedOrderItem> items;
        private final List<Long> cartIds;

        private OrderItems(List<ResolvedOrderItem> items, List<Long> cartIds) {
            this.items = items;
            this.cartIds = cartIds;
        }

        private List<ResolvedOrderItem> getItems() {
            return items;
        }

        private List<Long> getCartIds() {
            return cartIds;
        }

        private BigDecimal getAmount() {
            BigDecimal amount = BigDecimal.ZERO;
            for (ResolvedOrderItem item : items) {
                if (item.getAmount() != null) {
                    amount = amount.add(item.getAmount());
                }
            }
            return amount.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // TODO(security): 支付前校验订单存在且属于当前用户，防止用他人订单号发起支付
        // TODO(wechat-pay): 金额不能写死，改为校验订单存在且属于当前用户后取订单实际金额 orders.getAmount()
        //当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                MessageConstant.ORDER_NAME_DESC, //商品描述
                user.getOpenid() //微信用户的openid
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
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有待付款订单才能改为已支付，避免覆盖超时取消任务的结果
        int updated = orderMapper.paySuccess(ordersDB.getId(), LocalDateTime.now());
        if (updated == 0) {
            // 重复回调可以忽略；若订单已被自动取消但用户仍支付成功，需要走退款处理
            log.warn("订单{}当前不是待付款状态，跳过支付状态更新", outTradeNo);
        }
    }

    /**
     * 模拟支付成功（仅开发环境使用）
     *
     * TODO(wechat-pay): 接入真实微信支付后删除该方法，支付结果由 PayNotifyController 回调 paySuccess() 更新
     * @param orderNumber
     */
    @Override
    public void mockPay(String orderNumber) {
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null || !Objects.equals(ordersDB.getUserId(), BaseContext.getCurrentId())) {
            // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 幂等：已支付的订单直接返回，重复点击不报错
        if (Orders.PAY_STATUS_PAID.equals(ordersDB.getPayStatus())) {
            return;
        }
        int updated = orderMapper.paySuccess(ordersDB.getId(), LocalDateTime.now());
        if (updated == 0) {
            // 已取消/已退款的订单不能再支付
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 取消超时未支付的订单并回滚库存
     */
    @Override
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderProperties.getTimeoutMinutes());
        List<Long> orderIds = orderMapper.listTimeoutOrderIds(deadline);
        int cancelled = 0;
        for (Long orderId : orderIds) {
            // 先条件更新，抢不到说明订单已被支付或已被其它实例处理
            int updated = orderMapper.cancelTimeoutOrder(orderId, LocalDateTime.now(), MessageConstant.TIMEOUT_CANCEL_REASON);
            if (updated == 0) {
                continue;
            }
            List<OrderDetail> orderDetails = orderDetailMapper.listByOrderId(orderId);
            for (OrderDetail orderDetail : orderDetails) {
                orderStockService.restore(orderDetail);
            }
            cancelled++;
        }
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
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        // 小程序端状态转成后端 orders.status（一个前端状态可能对应多个后端状态）
        ordersPageQueryDTO.setStatuses(toBackendStatuses(status));
        if (keyword != null && !keyword.trim().isEmpty()) {
            ordersPageQueryDTO.setKeyword(keyword.trim());
        }
        // 时间筛选：近 N 天
        if (days > 0) {
            ordersPageQueryDTO.setBeginTime(LocalDateTime.now().minusDays(days));
        }

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<UserOrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
                list.add(toUserOrderVO(orders, orderDetails));
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 用户端订单详情
     * @param orderNumber 订单号
     * @return
     */
    @Override
    public UserOrderVO getDetail4User(String orderNumber) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (orders == null || !Objects.equals(orders.getUserId(), BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        UserOrderVO orderVO = toUserOrderVO(orders, orderDetails);
        // 详情页只展示门牌号等详细地址，去掉省市区前缀
        orderVO.getAddress().setAddress(detailAddress(orders));
        return orderVO;
    }

    /**
     * 从下单时保存的完整地址中截取详细地址（省市区之后的部分）
     */
    private String detailAddress(Orders orders) {
        String address = orders.getAddress();
        if (address == null || address.isEmpty()) {
            return address;
        }
        AddressBook addressBook = addressBookMapper.getById(orders.getAddressBookId());
        if (addressBook == null) {
            return address;
        }
        String prefix = (addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName());
        if (!prefix.isEmpty() && address.startsWith(prefix)) {
            return address.substring(prefix.length());
        }
        return address;
    }

    /**
     * 用户取消订单
     * @return
     */
    @Override
    @Transactional
    public void userCancelById(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = orderMapper.getById(id);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (ordersDB == null || !Objects.equals(ordersDB.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有待付款订单可以取消；已支付订单需要走退款流程，暂不支持
        int updated = orderMapper.cancelByUser(id, userId, LocalDateTime.now(), "用户取消");
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 回滚下单时占用的库存
        for (OrderDetail orderDetail : orderDetailMapper.listByOrderId(id)) {
            orderStockService.restore(orderDetail);
        }
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
        Orders orders = orderMapper.getById(id);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (orders == null || !Objects.equals(orders.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (orderDetailList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 逐项复用加购逻辑：校验商品/SKU/库存，并合并同规格商品
        for (OrderDetail detail : orderDetailList) {
            ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
            shoppingCartDTO.setProductId(detail.getProductId());
            shoppingCartDTO.setBundleId(detail.getBundleId());
            shoppingCartDTO.setSkuId(detail.getSkuId());
            shoppingCartDTO.setProductStyle(detail.getProductAttr());
            shoppingCartDTO.setNumber(detail.getNumber());
            shoppingCartService.addShoppingCart(shoppingCartDTO);
        }
    }

    /**
     * 确认收货：只有已发货、配送中的订单可以确认
     *
     * @param id
     */
    @Override
    public void confirmGoods(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = orderMapper.getById(id);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (ordersDB == null || !Objects.equals(ordersDB.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 条件更新，避免重复确认或对未发货订单确认
        int updated = orderMapper.confirmGoods(id, userId);
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 申请售后：支持待发货、待收货、已完成的已支付订单
     *
     * @param refundApplyDTO
     * @return 退款编号
     */
    @Override
    public RefundApplyVO refundApply(RefundApplyDTO refundApplyDTO) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByNumber(refundApplyDTO.getOrderNo());
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (orders == null || !Objects.equals(orders.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (refundApplyDTO.getReason() == null || refundApplyDTO.getReason().trim().isEmpty()) {
            throw new OrderBusinessException("请选择退款原因");
        }

        // 条件更新，避免重复申请或对未支付订单申请
        int updated = orderMapper.applyRefund(orders.getId(), userId,
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
        Orders orders = orderMapper.getByNumber(refundNo);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (orders == null || !Objects.equals(orders.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<RefundDetailVO.GoodsItem> goodsList = new ArrayList<>();
        for (OrderDetail detail : orderDetailMapper.getByOrderId(orders.getId())) {
            goodsList.add(RefundDetailVO.GoodsItem.builder()
                    .goodsId(detail.getProductId() != null ? detail.getProductId() : detail.getBundleId())
                    .picture(detail.getImage())
                    .title(detail.getName())
                    .specs(formatStyle(detail.getProductAttr()))
                    .build());
        }

        return RefundDetailVO.builder()
                .refundNo(orders.getNumber())
                .status(toRefundDetailStatus(orders.getRefundStatus()))
                .reason(orders.getRefundReason())
                .refundAmount(orders.getRefundAmount())
                .applyTime(formatDateTime(orders.getRefundApplyTime()))
                .refundTime(formatDateTime(orders.getRefundTime()))
                .goodsList(goodsList)
                .build();
    }

    /**
     * 撤销退款申请，只有处理中的申请可以撤销
     *
     * @param refundNo 退款编号（订单号）
     */
    @Override
    public void undoRefund(String refundNo) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByNumber(refundNo);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (orders == null || !Objects.equals(orders.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        int updated = orderMapper.undoRefund(refundNo, userId);
        if (updated == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 退款状态 -> 退款详情页状态 0处理中 1退款成功 2已结束
     */
    private Integer toRefundDetailStatus(Integer refundStatus) {
        if (refundStatus == null) {
            return 2;
        }
        if (refundStatus.equals(Orders.REFUND_STATUS_APPLIED)
                || refundStatus.equals(Orders.REFUND_STATUS_WAIT_RETURN)
                || refundStatus.equals(Orders.REFUND_STATUS_RETURNED)) {
            return 0;
        }
        if (refundStatus.equals(Orders.REFUND_STATUS_COMPLETED)) {
            return 1;
        }
        return 2;
    }

    /**
     * 存在售后记录时返回退款编号（订单号），否则返回空串
     */
    private String refundNo(Orders orders) {
        Integer refundStatus = orders.getRefundStatus();
        if (refundStatus != null && (refundStatus.equals(Orders.REFUND_STATUS_APPLIED)
                || refundStatus.equals(Orders.REFUND_STATUS_WAIT_RETURN)
                || refundStatus.equals(Orders.REFUND_STATUS_RETURNED)
                || refundStatus.equals(Orders.REFUND_STATUS_COMPLETED))) {
            return orders.getNumber();
        }
        return "";
    }

    /**
     * LocalDateTime -> 退款详情页展示格式
     */
    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME_FORMATTER);
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
        Orders ordersDB = orderMapper.getById(id);
        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        if (ordersDB == null || !Objects.equals(ordersDB.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer status = ordersDB.getStatus();
        // 退款完成的订单即使原状态未改为 7，也允许删除
        boolean refundCompleted = Orders.REFUND_STATUS_COMPLETED.equals(ordersDB.getRefundStatus());
        if (!Orders.STATUS_COMPLETED.equals(status)
                && !Orders.STATUS_CANCELLED.equals(status)
                && !Orders.STATUS_REFUND.equals(status)
                && !refundCompleted) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

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
     * 小程序端订单状态 -> 后端 orders.status
     * -999全部(不筛选) 0待付款 1待发货 2待收货 3已完成 4退款申请 5已退款
     */
    private List<Integer> toBackendStatuses(Integer status) {
        if (status == null || status == -999) {
            return null;
        }
        switch (status) {
            case 0:
                return toStatusList(Orders.STATUS_PENDING_PAYMENT);
            case 1:
                return toStatusList(Orders.STATUS_AWAITING_SHIPMENT);
            case 2:
                return toStatusList(Orders.STATUS_SHIPPED, Orders.STATUS_IN_TRANSIT);
            case 3:
                return toStatusList(Orders.STATUS_COMPLETED);
            case 4:
            case 5:
                return toStatusList(Orders.STATUS_REFUND);
            default:
                // 兼容直接传后端状态
                return toStatusList(status);
        }
    }

    /**
     * 必须用公开的 ArrayList，OGNL 无法反射调用 Collections$SingletonList 等包级私有类的方法
     */
    private List<Integer> toStatusList(Integer... statuses) {
        List<Integer> list = new ArrayList<>(statuses.length);
        Collections.addAll(list, statuses);
        return list;
    }

    /**
     * 后端订单状态 -> 小程序端订单状态
     */
    private Integer toAppStatus(Orders orders) {
        // 售后流程优先于订单原状态：申请中显示“退款中”，完成后显示“交易关闭”
        Integer refundStatus = orders.getRefundStatus();
        if (refundStatus != null) {
            if (refundStatus.equals(Orders.REFUND_STATUS_APPLIED)
                    || refundStatus.equals(Orders.REFUND_STATUS_WAIT_RETURN)
                    || refundStatus.equals(Orders.REFUND_STATUS_RETURNED)) {
                return 4;
            }
            if (refundStatus.equals(Orders.REFUND_STATUS_COMPLETED)) {
                return 5;
            }
        }
        Integer status = orders.getStatus();
        if (Orders.STATUS_REFUND.equals(status)) {
            return Orders.PAY_STATUS_REFUND.equals(orders.getPayStatus()) ? 5 : 4;
        }
        if (Orders.STATUS_CANCELLED.equals(status)) {
            return -1;
        }
        if (Orders.STATUS_COMPLETED.equals(status)) {
            return 3;
        }
        // 已发货、配送中都属于“待收货”
        if (Orders.STATUS_SHIPPED.equals(status) || Orders.STATUS_IN_TRANSIT.equals(status)) {
            return 2;
        }
        if (Orders.STATUS_AWAITING_SHIPMENT.equals(status)) {
            return 1;
        }
        return 0;
    }

    /**
     * 订单 + 明细 -> 小程序端订单视图
     */
    private UserOrderVO toUserOrderVO(Orders orders, List<OrderDetail> orderDetails) {
        List<UserOrderVO.GoodsItem> goodsList = new ArrayList<>();
        int goodsNum = 0;
        BigDecimal goodsAmount = BigDecimal.ZERO;
        for (OrderDetail detail : orderDetails) {
            int number = detail.getNumber() == null ? 0 : detail.getNumber();
            BigDecimal lineAmount = detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount();
            // 订单明细存的是该行总价，页面展示的是单价
            BigDecimal unitPrice = number > 0
                    ? lineAmount.divide(BigDecimal.valueOf(number), NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP)
                    : lineAmount;
            goodsList.add(UserOrderVO.GoodsItem.builder()
                    .goodsId(detail.getProductId() != null ? detail.getProductId() : detail.getBundleId())
                    .picture(detail.getImage())
                    .title(detail.getName())
                    .specs(formatStyle(detail.getProductAttr()))
                    .price(unitPrice)
                    .num(detail.getNumber())
                    .productId(detail.getProductId())
                    .bundleId(detail.getBundleId())
                    .skuId(detail.getSkuId())
                    .productStyle(detail.getProductAttr())
                    .build());
            goodsNum += number;
            goodsAmount = goodsAmount.add(lineAmount);
        }

        BigDecimal dispatchAmount = orders.getFreightAmount() == null ? BigDecimal.ZERO : orders.getFreightAmount();
        BigDecimal discountAmount = goodsAmount.add(dispatchAmount).subtract(orders.getAmount());
        if (discountAmount.signum() < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        return UserOrderVO.builder()
                .id(orders.getId())
                .orderNo(orders.getNumber())
                .status(toAppStatus(orders))
                .payAmount(orders.getAmount())
                .payMethod(orders.getPayMethod())
                .goodsNum(goodsNum)
                .goodsList(goodsList)
                .goodsAmount(goodsAmount)
                .discountAmount(discountAmount)
                .dispatchAmount(dispatchAmount)
                .placeTime(orders.getOrderTime())
                .payTime(orders.getCheckoutTime())
                .deliverTime(orders.getShipTime())
                .address(UserOrderVO.Address.builder()
                        .contacts(orders.getConsignee())
                        .mobile(orders.getPhone())
                        .provinceStr("")
                        .cityStr("")
                        .areaStr("")
                        .address(orders.getAddress())
                        .fullAddress(orders.getAddress())
                        .build())
                .refundApply(UserOrderVO.RefundApply.builder().refundNo(refundNo(orders)).build())
                .build();
    }

    /**
     * 规格 JSON 字符串 -> 展示文本，如 {"颜色":"白色","大小":"中"} -> "白色 中"
     */
    private String formatStyle(String productAttr) {
        if (productAttr == null || productAttr.isEmpty()) {
            return "";
        }
        try {
            JSONObject style = JSON.parseObject(productAttr);
            StringBuilder text = new StringBuilder();
            for (Object value : style.values()) {
                if (value == null) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append(value);
            }
            return text.toString();
        } catch (Exception e) {
            return productAttr;
        }
    }

    /**
     * 下单前风控：防重复提交、未支付订单上限、超时未支付冷却
     */
    private void checkSubmitFrequency(Long userId) {
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
     * 拼接完整收货地址
     */
    private String formatAddress(AddressBook addressBook) {
        StringBuilder address = new StringBuilder();
        String[] parts = {addressBook.getProvinceName(), addressBook.getCityName(),
                addressBook.getDistrictName(), addressBook.getDetail()};
        for (String part : parts) {
            if (part != null) {
                address.append(part);
            }
        }
        return address.toString();
    }
}

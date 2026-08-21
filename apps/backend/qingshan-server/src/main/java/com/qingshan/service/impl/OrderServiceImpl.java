package com.qingshan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.dto.OrdersSubmitItemDTO;
import com.qingshan.entity.*;
import com.qingshan.exception.AddressBookBusinessException;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.mapper.*;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.properties.OrderProperties;
import com.qingshan.service.OrderService;
import com.qingshan.service.support.OrderItemResolver;
import com.qingshan.service.support.OrderStockService;
import com.qingshan.utils.SnowflakeIdGenerator;
import com.qingshan.utils.WeChatPayUtil;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private OderDetailMapper oderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

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
        oderDetailMapper.insertBatch(toOrderDetails(orders.getId(), orderItems.getItems()));

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
            List<OrderDetail> orderDetails = oderDetailMapper.listByOrderId(orderId);
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

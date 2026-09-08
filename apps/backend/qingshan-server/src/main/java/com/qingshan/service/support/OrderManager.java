package com.qingshan.service.support;

import com.github.pagehelper.Page;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.dto.OrdersSubmitItemDTO;
import com.qingshan.entity.AddressBook;
import com.qingshan.entity.OrderDetail;
import com.qingshan.entity.Orders;
import com.qingshan.entity.ShoppingCart;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.mapper.AddressBookMapper;
import com.qingshan.mapper.OrderDetailMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.mapper.ShoppingCartMapper;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.properties.OrderProperties;
import com.qingshan.service.OrderStockService;
import com.qingshan.service.ShoppingCartService;
import com.qingshan.utils.WeChatPayUtil;
import com.qingshan.vo.OrderStatisticsVO;
import com.qingshan.vo.OrderVO;
import com.qingshan.vo.RefundDetailVO;
import com.qingshan.vo.UserOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单数据组装：循环查询、库存与购物车批量操作、退款调用
 */
@Component
@Slf4j
public class OrderManager {

    /**
     * 模拟支付的退款金额
     *
     * TODO(wechat-pay): 退款金额不能写死 0.01，必须用订单实际金额 ordersDB.getAmount(),
     *   且 refund(退款金额/total) 两个参数都要与微信侧原始支付金额一致
     */
    private static final BigDecimal MOCK_REFUND_AMOUNT = new BigDecimal(0.01);

    @Autowired
    private OrderProperties orderProperties;

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
    private OrderConverter orderConverter;

    @Autowired
    private OrderValidator orderValidator;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 确定下单商品：优先使用前端提交的明细（直接购买的商品不在购物车中），否则读取购物车
     */
    public OrderItems resolveOrderItems(List<OrdersSubmitItemDTO> submitItems, Long userId) {
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
        orderValidator.checkShoppingCartNotEmpty(cartList);
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
     * 扣减下单商品库存
     */
    public void deductStock(OrderItems orderItems) {
        for (ResolvedOrderItem item : orderItems.getItems()) {
            orderStockService.deduct(item);
        }
    }

    /**
     * 清空本次下单涉及的购物车记录
     */
    public void clearCartItems(OrderItems orderItems, Long userId) {
        if (orderItems.getCartIds().isEmpty()) {
            return;
        }
        List<Long> cartIds = new ArrayList<>(new LinkedHashSet<>(orderItems.getCartIds()));
        shoppingCartMapper.deleteByIds(cartIds, userId);
    }

    /**
     * 取消超时未支付的订单并回滚库存
     *
     * @return 取消的订单数量
     */
    public int cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderProperties.getTimeoutMinutes());
        List<Long> orderIds = orderMapper.listTimeoutOrderIds(deadline);
        int cancelled = 0;
        for (Long orderId : orderIds) {
            // 先条件更新，抢不到说明订单已被支付或已被其它实例处理
            int updated = orderMapper.cancelTimeoutOrder(orderId, LocalDateTime.now(),
                    MessageConstant.TIMEOUT_CANCEL_REASON);
            if (updated == 0) {
                continue;
            }
            restoreStock(orderId);
            cancelled++;
        }
        return cancelled;
    }

    /**
     * 回滚订单占用的库存
     */
    public void restoreStock(Long orderId) {
        for (OrderDetail orderDetail : orderDetailMapper.listByOrderId(orderId)) {
            orderStockService.restore(orderDetail);
        }
    }

    /**
     * 查询订单明细
     */
    public List<OrderDetail> listDetails(Long orderId) {
        return orderDetailMapper.getByOrderId(orderId);
    }

    /**
     * 再来一单：逐项复用加购逻辑：校验商品/SKU/库存，并合并同规格商品
     */
    public void reAddToShoppingCart(List<OrderDetail> orderDetailList) {
        for (OrderDetail detail : orderDetailList) {
            shoppingCartService.addShoppingCart(orderConverter.toShoppingCartDTO(detail));
        }
    }

    /**
     * 分页订单 -> 小程序端订单视图列表
     */
    public List<UserOrderVO> toUserOrderVOList(Page<Orders> page) {
        List<UserOrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                list.add(toUserOrderVO(orders));
            }
        }
        return list;
    }

    /**
     * 订单 -> 小程序端订单视图
     */
    public UserOrderVO toUserOrderVO(Orders orders) {
        return orderConverter.toUserOrderVO(orders, listDetails(orders.getId()));
    }

    /**
     * 订单 -> 退款详情
     */
    public RefundDetailVO toRefundDetailVO(Orders orders) {
        List<RefundDetailVO.GoodsItem> goodsList = new ArrayList<>();
        for (OrderDetail detail : listDetails(orders.getId())) {
            goodsList.add(orderConverter.toRefundGoodsItem(detail));
        }
        return orderConverter.toRefundDetailVO(orders, goodsList);
    }

    /**
     * 管理端分页订单 -> 订单视图列表
     */
    public List<OrderVO> toOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();

        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                orderVOList.add(orderConverter.toOrderVO(orders, getOrderProductsStr(orderDetailList)));
            }
        }
        return orderVOList;
    }

    /**
     * 管理端订单详情
     */
    public OrderVO toOrderVO(Orders orders) {
        List<OrderDetail> orderDetailList = listDetails(orders.getId());
        OrderVO orderVO = orderConverter.toOrderVO(orders, getOrderProductsStr(orderDetailList));
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 查询订单中的详细地址（省市区之后的部分）
     */
    public String detailAddress(Orders orders) {
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
     * 各个状态的订单数量统计
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待发货、已发货、配送中的订单数量
        Integer pendingShipment = orderMapper.countStatus(Orders.STATUS_AWAITING_SHIPMENT);
        Integer shipped = orderMapper.countStatus(Orders.STATUS_SHIPPED);
        Integer inTransit = orderMapper.countStatus(Orders.STATUS_IN_TRANSIT);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setPendingShipment(pendingShipment);
        orderStatisticsVO.setShipped(shipped);
        orderStatisticsVO.setInTransit(inTransit);
        return orderStatisticsVO;
    }

    /**
     * 用户已支付时先退款；退款失败则不允许继续
     *
     * @return 是否已支付
     */
    public boolean refundIfPaid(Orders ordersDB) {
        boolean paid = Orders.PAY_STATUS_PAID.equals(ordersDB.getPayStatus());
        if (paid) {
            // 开发环境使用模拟支付，没有真实微信订单，退款同样走模拟，避免调用微信接口失败
            if (orderProperties.isMockPayEnabled()) {
                log.info("模拟退款：订单{}", ordersDB.getNumber());
                return true;
            }
            try {
                String refund = weChatPayUtil.refund(
                        ordersDB.getNumber(),
                        ordersDB.getNumber(),
                        MOCK_REFUND_AMOUNT,
                        MOCK_REFUND_AMOUNT);
                log.info("申请退款：{}", refund);
            } catch (Exception e) {
                log.error("订单{}退款失败", ordersDB.getNumber(), e);
                throw new OrderBusinessException("订单退款失败");
            }
        }
        return paid;
    }

    private String getOrderProductsStr(List<OrderDetail> orderDetailList) {
        List<String> orderProductList = orderDetailList.stream().map(x -> {
            String orderProduct = x.getName() + "*" + x.getNumber() + ";";
            return orderProduct;
        }).collect(Collectors.toList());

        return String.join("", orderProductList);
    }

    /**
     * 本次下单的商品及待清理的购物车记录
     */
    public static class OrderItems {
        private final List<ResolvedOrderItem> items;
        private final List<Long> cartIds;

        private OrderItems(List<ResolvedOrderItem> items, List<Long> cartIds) {
            this.items = items;
            this.cartIds = cartIds;
        }

        public List<ResolvedOrderItem> getItems() {
            return items;
        }

        public List<Long> getCartIds() {
            return cartIds;
        }

        public BigDecimal getAmount() {
            BigDecimal amount = BigDecimal.ZERO;
            for (ResolvedOrderItem item : items) {
                if (item.getAmount() != null) {
                    amount = amount.add(item.getAmount());
                }
            }
            return amount.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
    }
}

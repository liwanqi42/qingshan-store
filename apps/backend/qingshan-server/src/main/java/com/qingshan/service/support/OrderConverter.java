package com.qingshan.service.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.qingshan.constant.NumConstant;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.dto.ShoppingCartDTO;
import com.qingshan.entity.AddressBook;
import com.qingshan.entity.OrderDetail;
import com.qingshan.entity.Orders;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.properties.OrderProperties;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.vo.OrderVO;
import com.qingshan.vo.RefundDetailVO;
import com.qingshan.vo.UserOrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单相关对象转换
 */
@Component
public class OrderConverter {

    /**
     * 退款详情页的时间格式
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OrderStatusHandler orderStatusHandler;

    @Autowired
    private OrderProperties orderProperties;

    /**
     * 组装订单主体
     */
    public Orders toOrder(OrdersSubmitDTO ordersSubmitDTO, AddressBook addressBook, Long userId,
                          String number, LocalDateTime orderTime, BigDecimal amount,
                          BigDecimal couponAmount, Long userCouponId) {
        return Orders.builder()
                .number(number)
                .status(Orders.STATUS_PENDING_PAYMENT)
                .userId(userId)
                .userCouponId(userCouponId)
                .addressBookId(ordersSubmitDTO.getAddressBookId())
                .orderTime(orderTime)
                .payMethod(ordersSubmitDTO.getPayMethod())
                .payStatus(Orders.PAY_STATUS_UN_PAID)
                .amount(amount)
                .couponAmount(couponAmount)
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
    public List<OrderDetail> toOrderDetails(Long orderId, List<ResolvedOrderItem> items) {
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
     * 订单 -> 下单结果
     */
    public OrderSubmitVO toOrderSubmitVO(Orders orders) {
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .couponAmount(orders.getCouponAmount())
                .build();
    }

    /**
     * 订单 -> 管理端订单视图
     */
    public OrderVO toOrderVO(Orders orders, String orderItems) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderItems(orderItems);
        return orderVO;
    }

    /**
     * 订单 + 明细 -> 小程序端订单视图
     */
    public UserOrderVO toUserOrderVO(Orders orders, List<OrderDetail> orderDetails) {
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

        Integer appStatus = orderStatusHandler.toAppStatus(orders);

        return UserOrderVO.builder()
                .id(orders.getId())
                .orderNo(orders.getNumber())
                .status(appStatus)
                .payAmount(orders.getAmount())
                .payMethod(orders.getPayMethod())
                .goodsNum(goodsNum)
                .goodsList(goodsList)
                .goodsAmount(goodsAmount)
                .discountAmount(discountAmount)
                .dispatchAmount(dispatchAmount)
                .placeTime(orders.getOrderTime())
                // 待付款状态才返回倒计时，前端据此展示自动取消剩余时间
                .remainingSeconds(appStatus != null && appStatus == 0
                        ? remainingSeconds(orders) : null)
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
                .refundApply(UserOrderVO.RefundApply.builder()
                        .refundNo(orderStatusHandler.refundNo(orders))
                        .build())
                .build();
    }

    /**
     * 待付款订单距离自动取消还剩多少秒，已超时返回 0
     */
    private Long remainingSeconds(Orders orders) {
        if (orders.getOrderTime() == null) {
            return null;
        }
        long elapsed = Duration.between(orders.getOrderTime(), LocalDateTime.now()).getSeconds();
        long remaining = orderProperties.getTimeoutMinutes() * 60 - elapsed;
        return Math.max(0L, remaining);
    }

    /**
     * 订单明细 -> 退款详情商品
     */
    public RefundDetailVO.GoodsItem toRefundGoodsItem(OrderDetail detail) {
        return RefundDetailVO.GoodsItem.builder()
                .goodsId(detail.getProductId() != null ? detail.getProductId() : detail.getBundleId())
                .picture(detail.getImage())
                .title(detail.getName())
                .specs(formatStyle(detail.getProductAttr()))
                .build();
    }

    /**
     * 订单 + 售后商品 -> 售后详情
     */
    public RefundDetailVO toRefundDetailVO(Orders orders, List<RefundDetailVO.GoodsItem> goodsList) {
        return RefundDetailVO.builder()
                .refundNo(orders.getNumber())
                .orderId(orders.getId())
                .status(orderStatusHandler.toRefundDetailStatus(orders.getRefundStatus()))
                .refundStatus(orders.getRefundStatus())
                .refundType(orders.getRefundType())
                .reason(orders.getRefundReason())
                .refundAmount(orders.getRefundAmount())
                .applyTime(formatDateTime(orders.getRefundApplyTime()))
                .refundTime(formatDateTime(orders.getRefundTime()))
                .returnExpressCompany(orders.getReturnExpressCompany())
                .returnTrackingNumber(orders.getReturnTrackingNumber())
                .returnTime(formatDateTime(orders.getReturnTime()))
                .rejectReason(orders.getRefundRejectReason())
                .goodsList(goodsList)
                .build();
    }

    /**
     * 订单明细 -> 购物车加购参数
     */
    public ShoppingCartDTO toShoppingCartDTO(OrderDetail detail) {
        ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
        shoppingCartDTO.setProductId(detail.getProductId());
        shoppingCartDTO.setBundleId(detail.getBundleId());
        shoppingCartDTO.setSkuId(detail.getSkuId());
        shoppingCartDTO.setProductStyle(detail.getProductAttr());
        shoppingCartDTO.setNumber(detail.getNumber());
        return shoppingCartDTO;
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
     * LocalDateTime -> 退款详情页展示格式
     */
    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME_FORMATTER);
    }
}

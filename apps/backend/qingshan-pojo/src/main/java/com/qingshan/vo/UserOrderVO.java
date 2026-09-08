package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端订单视图，字段与小程序订单列表/详情页一一对应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单号
    private String orderNo;

    //小程序端状态：-1已取消 0待付款 1待发货 2待收货 3已完成 4退款申请 5已退款
    private Integer status;

    //实付金额
    private BigDecimal payAmount;

    //支付方式 1微信 2支付宝 3银行卡
    private Integer payMethod;

    //商品总件数
    private Integer goodsNum;

    //商品列表
    private List<GoodsItem> goodsList;

    //商品总价
    private BigDecimal goodsAmount;

    //优惠金额
    private BigDecimal discountAmount;

    //运费
    private BigDecimal dispatchAmount;

    //下单时间
    private LocalDateTime placeTime;

    //待付款订单剩余支付秒数，其它状态为 null；用于小程序端自动取消倒计时
    private Long remainingSeconds;

    //支付时间
    private LocalDateTime payTime;

    //发货时间
    private LocalDateTime deliverTime;

    //成交时间
    private LocalDateTime finishTime;

    //收货地址
    private Address address;

    //退款信息，无退款时 refundNo 为空串
    private RefundApply refundApply;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoodsItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long goodsId;
        private String picture;
        private String title;
        private String specs;
        private BigDecimal price;
        private Integer num;
        private Long productId;
        private Long bundleId;
        private Long skuId;
        private String productStyle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contacts;
        private String mobile;
        private String provinceStr;
        private String cityStr;
        private String areaStr;
        //详细地址（门牌号）
        private String address;
        //省市区 + 详细地址的完整地址
        private String fullAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundApply implements Serializable {

        private static final long serialVersionUID = 1L;

        private String refundNo;
    }
}

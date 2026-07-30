package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 订单状态 1待付款 2待发货 3已发货 4配送中 5已完成 6已取消 7退款
     */
    public static final Integer STATUS_PENDING_PAYMENT = 1;
    public static final Integer STATUS_AWAITING_SHIPMENT = 2;
    public static final Integer STATUS_SHIPPED = 3;
    public static final Integer STATUS_IN_TRANSIT = 4;
    public static final Integer STATUS_COMPLETED = 5;
    public static final Integer STATUS_CANCELLED = 6;
    public static final Integer STATUS_REFUND = 7;

    /**
     * 支付状态 0未支付 1已支付 2退款
     */
    public static final Integer PAY_STATUS_UN_PAID = 0;
    public static final Integer PAY_STATUS_PAID = 1;
    public static final Integer PAY_STATUS_REFUND = 2;

    /**
     * 退款类型 1仅退款 2退货退款
     */
    public static final Integer REFUND_TYPE_REFUND_ONLY = 1;
    public static final Integer REFUND_TYPE_RETURN_REFUND = 2;

    /**
     * 退款状态 0无退款 1退款申请中 2待买家退货 3买家已退货待商家收货 4退款完成 5退款申请被拒绝 6退款关闭
     */
    public static final Integer REFUND_STATUS_NONE = 0;
    public static final Integer REFUND_STATUS_APPLIED = 1;
    public static final Integer REFUND_STATUS_WAIT_RETURN = 2;
    public static final Integer REFUND_STATUS_RETURNED = 3;
    public static final Integer REFUND_STATUS_COMPLETED = 4;
    public static final Integer REFUND_STATUS_REJECTED = 5;
    public static final Integer REFUND_STATUS_CLOSED = 6;

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单号
    private String number;

    //订单状态 1待付款 2待发货 3已发货 4配送中 5已完成 6已取消 7退款
    private Integer status;

    //下单用户id
    private Long userId;

    //地址id
    private Long addressBookId;

    //下单时间
    private LocalDateTime orderTime;

    //结账时间
    private LocalDateTime checkoutTime;

    //支付方式 1微信 2支付宝 3银行卡
    private Integer payMethod;

    //支付状态 0未支付 1已支付 2退款
    private Integer payStatus;

    //实收金额
    private BigDecimal amount;

    //备注
    private String remark;

    //用户名
    private String userName;

    //手机号
    private String phone;

    //地址
    private String address;

    //收货人
    private String consignee;

    //订单取消原因
    private String cancelReason;

    //订单拒绝原因
    private String rejectionReason;

    //订单取消时间
    private LocalDateTime cancelTime;

    //预计发货时间
    private LocalDateTime estimatedShipTime;

    //发货状态 1立即发货 0预约发货
    private Integer shippingStatus;

    //发货时间
    private LocalDateTime shipTime;

    //快递公司
    private String expressCompany;

    //物流单号
    private String trackingNumber;

    //运费
    private BigDecimal freightAmount;

    //发票类型：0 不开发票，1 电子普通发票，2 增值税专用发票
    private Integer invoiceType;

    //发票抬头
    private String invoiceTitle;

    //退款原因
    private String refundReason;

    //退款金额
    private BigDecimal refundAmount;

    //退款时间
    private LocalDateTime refundTime;

    //退款类型 1仅退款 2退货退款
    private Integer refundType;

    //退款状态 0无退款 1退款申请中 2待买家退货 3买家已退货待商家收货 4退款完成 5退款申请被拒绝 6退款关闭
    private Integer refundStatus;

    //退款补充说明
    private String refundDescription;

    //退款凭证图片，多个以逗号分隔
    private String refundImages;

    //退款申请时间
    private LocalDateTime refundApplyTime;

    //拒绝退款原因
    private String refundRejectReason;

    //退货快递公司
    private String returnExpressCompany;

    //退货物流单号
    private String returnTrackingNumber;

    //买家退货时间
    private LocalDateTime returnTime;

    //改价前订单金额
    private BigDecimal originalAmount;

    //改价原因
    private String adjustReason;

    //改价时间
    private LocalDateTime adjustTime;

}

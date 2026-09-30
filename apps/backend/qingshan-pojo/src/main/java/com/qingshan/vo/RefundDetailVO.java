package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 售后详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 售后编号，当前直接使用订单号 */
    private String refundNo;

    /** 订单id，填写退货物流时使用 */
    private Long orderId;

    /** 售后状态 0处理中 1已完成 2已结束 */
    private Integer status;

    /** 原始售后状态 1申请中 2待买家退货 3买家已退货待商家收货 4售后完成 5已拒绝 6已关闭 */
    private Integer refundStatus;

    /** 售后类型 1仅退款 2退货退款 3换货 4补寄 */
    private Integer refundType;

    /** 售后原因 */
    private String reason;

    /** 退款金额，换货/补寄为空 */
    private BigDecimal refundAmount;

    /** 申请时间 yyyy-MM-dd HH:mm:ss */
    private String applyTime;

    /** 售后完成时间 yyyy-MM-dd HH:mm:ss */
    private String refundTime;

    /** 退货快递公司 */
    private String returnExpressCompany;

    /** 退货物流单号 */
    private String returnTrackingNumber;

    /** 买家退货时间 yyyy-MM-dd HH:mm:ss */
    private String returnTime;

    /** 拒绝售后原因 */
    private String rejectReason;

    private List<GoodsItem> goodsList;

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
    }
}

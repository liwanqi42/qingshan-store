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

    /** 售后状态 0处理中 1已完成 2已结束 */
    private Integer status;

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

package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 退款详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 退款编号，当前直接使用订单号 */
    private String refundNo;

    /** 退款状态 0处理中 1退款成功 2已结束 */
    private Integer status;

    /** 退款原因 */
    private String reason;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 申请时间 yyyy-MM-dd HH:mm:ss */
    private String applyTime;

    /** 退款完成时间 yyyy-MM-dd HH:mm:ss */
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

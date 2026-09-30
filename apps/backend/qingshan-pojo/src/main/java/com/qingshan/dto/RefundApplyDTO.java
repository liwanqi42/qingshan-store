package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户申请售后
 */
@Data
public class RefundApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 售后类型 1:仅退款（未发货） 2:退货退款（已发货） 3:换货 4:补寄（已发货） */
    private Integer refundType;

    /** 售后原因 */
    private String reason;
}

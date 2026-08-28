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

    /** 退款原因 */
    private String reason;
}

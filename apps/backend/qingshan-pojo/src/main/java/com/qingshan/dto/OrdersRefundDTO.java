package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 直接退款 DTO
 */
@Data
public class OrdersRefundDTO implements Serializable {

    private Long id;

    //退款原因（直接退款时必填）
    private String refundReason;

    //退款金额
    private BigDecimal refundAmount;

    //备注
    private String remark;
}

package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商家拒绝退款申请 DTO
 */
@Data
public class OrdersRefundRejectDTO implements Serializable {

    private Long id;

    //拒绝退款原因
    private String rejectReason;
}

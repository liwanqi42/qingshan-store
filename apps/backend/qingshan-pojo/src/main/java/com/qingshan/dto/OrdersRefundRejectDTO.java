package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商家拒绝售后申请 DTO
 */
@Data
public class OrdersRefundRejectDTO implements Serializable {

    private Long id;

    //拒绝售后原因
    private String rejectReason;
}

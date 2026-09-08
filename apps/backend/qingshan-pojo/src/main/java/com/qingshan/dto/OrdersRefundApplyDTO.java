package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户申请售后 DTO
 */
@Data
public class OrdersRefundApplyDTO implements Serializable {

    private Long id;

    //售后类型 1仅退款 2退货退款 3换货 4补寄
    private Integer refundType;

    //售后原因
    private String refundReason;

    //补充说明
    private String description;

    //退款凭证图片，多个以逗号分隔
    private String images;
}

package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户申请退款/退货退款 DTO
 */
@Data
public class OrdersRefundApplyDTO implements Serializable {

    private Long id;

    //退款类型 1仅退款 2退货退款
    private Integer refundType;

    //退款原因
    private String refundReason;

    //补充说明
    private String description;

    //退款凭证图片，多个以逗号分隔
    private String images;
}

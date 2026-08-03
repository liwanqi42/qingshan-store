package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单物流 DTO，发货和退货共用
 */
@Data
public class OrdersLogisticsDTO implements Serializable {

    private Long id;

    //快递公司（发货或退货）
    private String expressCompany;

    //物流单号（发货或退货）
    private String trackingNumber;
}

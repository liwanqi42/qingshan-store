package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单结算预览 DTO
 */
@Data
public class OrdersPreviewDTO implements Serializable {

    //收货地址id
    private Long addressBookId;

    //购物车id，多个以逗号分隔；为空表示结算全部购物车
    private String cartIds;
}

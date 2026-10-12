package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 入库/出库单据明细入参
 */
@Data
public class StockOrderItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //商品id
    private Long productId;

    //SKU id，无规格商品为空
    private Long skuId;

    //数量
    private Integer quantity;
}

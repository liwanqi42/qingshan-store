package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入库/出库单据明细，保存商品/SKU快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //单据id
    private Long orderId;

    //商品id
    private Long productId;

    //SKU id，无规格商品为空
    private Long skuId;

    //商品编号快照
    private String productCode;

    //SKU编号快照
    private String skuCode;

    //商品名称快照
    private String productName;

    //规格属性JSON快照
    private String skuAttrs;

    //数量
    private Integer quantity;

    //审核前库存
    private Integer stockBefore;

    //审核后库存
    private Integer stockAfter;

    private LocalDateTime createTime;
}

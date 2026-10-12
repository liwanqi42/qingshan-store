package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品规格（SKU）快速录入选项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;

    private String skuCode;

    //规格属性JSON
    private String attrs;

    private BigDecimal price;

    private Integer stock;

    //0禁用 1启用
    private Integer status;
}

package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单结算预览明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewItemVO implements Serializable {

    private Long cartId;

    private Long productId;

    private Long bundleId;

    private Long skuId;

    //商品/套装名称
    private String name;

    private String image;

    //已选规格
    private String productStyle;

    private BigDecimal price;

    private Integer number;

    private BigDecimal amount;

    //可用库存
    private Integer stock;
}

package com.qingshan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 解析后的下单商品
 * 价格、规格、库存等字段均以服务端数据为准
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedOrderItem {

    //商品id
    private Long productId;

    //套装id
    private Long bundleId;

    //SKU id
    private Long skuId;

    //名称
    private String name;

    //图片
    private String image;

    //已选属性JSON
    private String productStyle;

    //数量
    private Integer number;

    //当前可售库存，套装为空
    private Integer stock;

    //单价
    private BigDecimal price;

    //金额 = 单价 × 数量
    private BigDecimal amount;

}

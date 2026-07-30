package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 套装商品关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //套装id
    private Long bundleId;

    //商品id
    private Long productId;

    //商品名称 （冗余字段）
    private String name;

    //商品原价
    private BigDecimal price;

    //商品份数
    private Integer copies;
}

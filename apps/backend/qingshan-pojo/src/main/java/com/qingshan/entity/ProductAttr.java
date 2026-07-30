package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品属性/规格
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttr implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    //商品id
    private Long productId;

    //属性名称
    private String name;

    //属性值列表JSON
    private String attrValues;

}

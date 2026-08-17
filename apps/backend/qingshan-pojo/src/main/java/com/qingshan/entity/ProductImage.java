package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品图片（商品详情页轮播图）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    //样式图（详情页轮播）
    public static final int TYPE_STYLE = 1;

    //详情长图（商品详情页图文区）
    public static final int TYPE_DETAIL = 2;

    private Long id;

    //商品id
    private Long productId;

    //图片地址
    private String url;

    //图片类型 1样式图 2详情长图
    private Integer type;

    //排序，值越小越靠前
    private Integer sort;
}

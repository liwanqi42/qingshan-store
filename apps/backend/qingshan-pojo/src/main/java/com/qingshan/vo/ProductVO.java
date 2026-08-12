package com.qingshan.vo;

import com.qingshan.entity.ProductAttr;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO implements Serializable {

    private Long id;
    //商品名称
    private String name;
    //分类id
    private Long categoryId;
    //商品价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 下架 1 上架
    private Integer status;
    //库存
    private Integer stock;
    //销量
    private Integer sales;
    //更新时间
    private LocalDateTime updateTime;
    //分类名称
    private String categoryName;
    //商品属性/规格
    private List<ProductAttr> attrs = new ArrayList<>();


}

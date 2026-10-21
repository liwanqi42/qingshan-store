package com.qingshan.vo;

import com.qingshan.entity.ProductAttr;
import com.qingshan.entity.ProductSku;
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

    private static final long serialVersionUID = 1L;

    private Long id;
    //商品编号，SKU编号为“商品编号-序号”
    private String productCode;
    //商品名称
    private String name;
    //分类id
    private Long categoryId;
    //商品价格
    private BigDecimal price;
    //封面图片
    private String image;
    //样式图列表（详情页轮播）
    @Builder.Default
    private List<String> images = new ArrayList<>();
    //详情长图列表
    @Builder.Default
    private List<String> detailImages = new ArrayList<>();
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
    @Builder.Default
    private List<ProductAttr> attrs = new ArrayList<>();
    //商品SKU（每个规格组合的独立价格与库存）
    @Builder.Default
    private List<ProductSku> skuList = new ArrayList<>();


}

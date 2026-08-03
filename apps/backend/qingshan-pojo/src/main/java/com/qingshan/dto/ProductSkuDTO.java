package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品SKU DTO
 */
@Data
public class ProductSkuDTO implements Serializable {

    //SKU id，新增时为空
    private Long id;

    //商品id
    private Long productId;

    //SKU编码
    private String skuCode;

    //规格属性JSON，例如：{"颜色":"黑色","尺码":"L"}
    private String attrs;

    //SKU价格
    private BigDecimal price;

    //SKU库存
    private Integer stock;

    //SKU图片
    private String image;

    //状态 0禁用 1启用
    private Integer status;
}

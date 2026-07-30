package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

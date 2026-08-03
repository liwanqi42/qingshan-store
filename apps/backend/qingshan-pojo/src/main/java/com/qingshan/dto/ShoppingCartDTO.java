package com.qingshan.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ShoppingCartDTO implements Serializable {

    //商品id
    private Long productId;
    //套装id
    private Long bundleId;
    //SKU id
    private Long skuId;
    //商品样式
    private String productStyle;

}

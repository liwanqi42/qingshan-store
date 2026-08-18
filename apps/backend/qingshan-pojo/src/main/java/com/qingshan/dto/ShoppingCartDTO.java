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
    //商品样式（已选属性JSON，无SKU时由前端提交，有SKU时以SKU属性为准）
    private String productStyle;
    //数量
    private Integer number;

}

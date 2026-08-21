package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 下单商品明细
 * 直接购买的商品不在购物车中，需要随下单请求一起提交
 */
@Data
public class OrdersSubmitItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //购物车记录id，来自购物车结算时传入，直接购买时为空
    private Long cartId;
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

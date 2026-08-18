package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改购物车商品数量
 */
@Data
public class ShoppingCartEditDTO implements Serializable {

    //购物车记录id
    private Long id;

    //修改后的数量，小于等于0表示删除
    private Integer number;
}

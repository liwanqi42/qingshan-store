package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除购物车商品
 */
@Data
public class ShoppingCartDeleteDTO implements Serializable {

    //要删除的购物车记录id列表
    private List<Long> ids;
}

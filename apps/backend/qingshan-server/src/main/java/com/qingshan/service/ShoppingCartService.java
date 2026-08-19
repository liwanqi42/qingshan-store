package com.qingshan.service;

import com.qingshan.dto.ShoppingCartDTO;
import com.qingshan.dto.ShoppingCartEditDTO;
import com.qingshan.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询当前用户的购物车
     * @return 购物车列表
     */
    List<ShoppingCart> list();

    /**
     * 修改购物车商品数量，数量小于等于0时删除
     * @param shoppingCartEditDTO 购物车记录id及修改后的数量
     */
    void editShoppingCart(ShoppingCartEditDTO shoppingCartEditDTO);

    /**
     * 批量删除当前用户购物车中的商品
     * @param ids 购物车记录id列表
     */
    void deleteShoppingCart(List<Long> ids);

    /**
     * 统计当前用户购物车中的商品条目数
     * @return 条目数
     */
    Integer count();
}

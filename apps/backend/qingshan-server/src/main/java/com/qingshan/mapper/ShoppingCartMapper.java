package com.qingshan.mapper;

import com.qingshan.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态条件查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据主键查询购物车记录
     * @param id 购物车记录id
     * @return 购物车记录
     */
    @Select("select * from shopping_cart where id = #{id}")
    ShoppingCart getById(Long id);

    /**
     * 统计用户购物车中的商品条目数
     * @param userId 用户id
     * @return 条目数
     */
    @Select("select count(*) from shopping_cart where user_id = #{userId}")
    Integer countByUserId(Long userId);

    /**
     * 新增购物车记录
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 删除购物车记录
     * @param id 购物车记录id
     */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    /**
     * 批量删除当前用户的购物车记录
     * @param ids 购物车记录id列表
     * @param userId 用户id，避免删除他人购物车记录
     */
    void deleteByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 修改购物车商品数量及金额
     * @param shoppingCart
     */
    void updateNumber(ShoppingCart shoppingCart);

    /**
     * 批量插入购物车数据
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}

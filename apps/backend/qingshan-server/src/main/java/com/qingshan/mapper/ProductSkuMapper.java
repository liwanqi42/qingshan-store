package com.qingshan.mapper;

import com.qingshan.entity.ProductSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductSkuMapper {

    /**
     * 根据id查询SKU
     * @param id SKU id
     * @return SKU信息
     */
    @Select("select * from product_sku where id = #{id}")
    ProductSku getById(Long id);

    /**
     * 查询商品下所有SKU
     * @param productId 商品id
     * @return SKU列表
     */
    @Select("select * from product_sku where product_id = #{productId} order by id")
    List<ProductSku> listByProductId(Long productId);

    /**
     * 删除商品下所有SKU
     * @param productId 商品id
     */
    @Delete("delete from product_sku where product_id = #{productId}")
    void deleteByProductId(Long productId);

    /**
     * 新增SKU
     * @param sku SKU信息
     */
    @Insert("insert into product_sku (product_id, sku_code, attrs, price, stock, image, status, create_time, update_time) "
            + "values (#{productId}, #{skuCode}, #{attrs}, #{price}, #{stock}, #{image}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProductSku sku);
}

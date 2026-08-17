package com.qingshan.mapper;

import com.qingshan.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductSkuMapper {

    /**
     * 根据id查询SKU
     * @param id SKU id
     * @return SKU信息
     */
    @Select("select * from product_sku where id = #{id}")
    ProductSku getById(Long id);
}

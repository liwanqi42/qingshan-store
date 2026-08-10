package com.qingshan.mapper;

import com.qingshan.entity.ProductAttr;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductAttrsMapper {
    /**
     * 批量插入样式数据
     * @param attrs
     */
    void insertBatch(List<ProductAttr> attrs);

    /**
     * 根据商品id删除对应样式
     * @param productId
     */
    @Delete("delete from product_attr where product_id = #{productId}")
    void deleteByProductId(Long productId);

    void deleteByProductAttrIds(List<Long> ids);

    @Select("select * from product_attr where product_id = #{productId}")
    List<ProductAttr> getById(Long productId);
}

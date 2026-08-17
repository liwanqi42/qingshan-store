package com.qingshan.mapper;

import com.qingshan.entity.ProductImage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    /**
     * 批量插入商品图片
     * @param images 商品图片列表
     */
    void insertBatch(@Param("images") List<ProductImage> images);

    /**
     * 根据商品id和图片类型删除商品图片
     * @param productId 商品id
     * @param type 图片类型 1样式图 2详情长图
     */
    @Delete("delete from product_image where product_id = #{productId} and type = #{type}")
    void deleteByProductIdAndType(@Param("productId") Long productId, @Param("type") Integer type);

    /**
     * 根据商品id集合删除商品图片
     * @param ids 商品id列表
     */
    void deleteByProductIds(@Param("ids") List<Long> ids);

    /**
     * 根据商品id和图片类型查询商品图片，按排序值升序
     * @param productId 商品id
     * @param type 图片类型 1样式图 2详情长图
     * @return 商品图片列表
     */
    @Select("select * from product_image where product_id = #{productId} and type = #{type} order by sort asc, id asc")
    List<ProductImage> getByProductIdAndType(@Param("productId") Long productId, @Param("type") Integer type);
}

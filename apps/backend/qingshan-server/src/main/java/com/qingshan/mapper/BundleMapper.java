package com.qingshan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BundleMapper {

    /**
     * 根据套装分类id查询套装数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from bundle where category_id = #{categoryId}")
    Integer countByCategoryId(@Param("categoryId") Long categoryId);

}

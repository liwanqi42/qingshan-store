package com.qingshan.mapper;

import com.qingshan.entity.BundleItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BundleItemMapper {

    void insertBatch(List<BundleItem> bundleItems);

    /**
     * 根据套餐id删除套餐和菜品的关联关系
     * @param setmealId
     */
    @Delete("delete from bundle_item where bundle_id = #{bundleId}")
    void deleteByBundleId(Long setmealId);

    /**
     * 根据套餐id查询套餐和菜品的关联关系
     * @param bundleId
     * @return
     */
    @Select("select * from bundle_item where bundle_id = #{bundleId}")
    List<BundleItem> getByBundleId(Long bundleId);
}

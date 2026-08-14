package com.qingshan.mapper;

import com.qingshan.entity.BundleItem;
import com.qingshan.vo.ProductItemVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BundleItemMapper {

    void insertBatch(List<BundleItem> bundleItems);

    /**
     * 根据套装id删除套装和商品的关联关系
     * @param bundleId
     */
    @Delete("delete from bundle_item where bundle_id = #{bundleId}")
    void deleteByBundleId(Long bundleId);

    /**
     * 根据套装id查询套装和商品的关联关系
     * @param bundleId
     * @return
     */
    @Select("select * from bundle_item where bundle_id = #{bundleId}")
    List<BundleItem> getByBundleId(Long bundleId);

    /**
     * 根据套装id查询包含的商品
     * @param bundleId 套装id
     * @return 套装内商品列表
     */
    @Select("select bi.name, bi.copies, p.image, p.description " +
            "from bundle_item bi left join product p on bi.product_id = p.id " +
            "where bi.bundle_id = #{bundleId}")
    List<ProductItemVO> getProductItemByBundleId(Long bundleId);
}

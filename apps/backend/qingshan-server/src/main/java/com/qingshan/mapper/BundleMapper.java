package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.annotation.AutoFill;
import com.qingshan.dto.BundlePageQueryDTO;
import com.qingshan.entity.Bundle;
import com.qingshan.entity.Product;
import com.qingshan.enumeration.OperationType;
import com.qingshan.vo.BundleVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BundleMapper {

    /**
     * 新增套装
     * @param bundle
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Bundle bundle);

    /**
     * 根据套装分类id查询套装数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from bundle where category_id = #{categoryId}")
    Integer countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 分页查询
     * @param bundlePageQueryDTO
     * @return
     */
    Page<BundleVO> pageQuery(BundlePageQueryDTO bundlePageQueryDTO);

    /**
     * 根据id查询捆绑包
     * @param id
     * @return
     */
    @Select("select * from bundle where id = #{id}")
    Bundle getByid(Long id);

    /**
     * 根据id删除捆绑包
     * @param id
     */
    @Delete("delete from bundle where id = #{id}")
    void deleteById(Long id);

    /**
     * 更新捆绑包
     * @param bundle
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Bundle bundle);

    @Select("select a.* from product a left join bundle_item b on a.id = b.product_id where b.bundle_id = #{bundleId}")
    List<Product> getByBundleId(Long bundleId);

    /**
     * 动态条件查询套装
     * @param bundle
     * @return
     */
    List<Bundle> list(Bundle bundle);
}

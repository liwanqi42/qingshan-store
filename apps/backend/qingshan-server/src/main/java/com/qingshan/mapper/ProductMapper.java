package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.annotation.AutoFill;
import com.qingshan.dto.ProductPageQueryDTO;
import com.qingshan.entity.Product;
import com.qingshan.enumeration.OperationType;
import com.qingshan.vo.ProductVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {

    /**
     * 根据商品分类id查询商品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from product where category_id = #{categoryId}")
    Integer countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 根据条件统计商品数量
     * @param status
     * @param categoryId
     * @return
     */
    Integer countByCondition(@Param("status") Integer status, @Param("categoryId") Long categoryId);

    /**
     * 新增商品及样式
     * @param product
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Product product);

    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    Page<ProductVO> pageQuery(ProductPageQueryDTO productPageQueryDTO);

    /**
     * 根据主键查询商品
     * @param id
     * @return
     */
    @Select("select * from product where id = #{id}")
    Product getById(Long id);

    /**
     * 根据id删除商品数据
     * @param id
     */
    @Delete("delete from product where id = #{id}")
    void deleteById(Long id);


    /**
     * 根据id集合删除商品数据
     * @param ids
     */
    void deleteByProductIds(List<Long> ids);

    /**
     * 修改商品信息
     * @param product
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Product product);

    List<Product> list(Product product);

    /**
     * 扣减库存，库存不足时不更新（返回0）
     * @param id 商品id
     * @param number 扣减数量
     * @return 更新的行数
     */
    @Update("update product set stock = stock - #{number} where id = #{id} and stock >= #{number}")
    int deductStock(@Param("id") Long id, @Param("number") Integer number);

    /**
     * 回滚库存
     * @param id 商品id
     * @param number 回滚数量
     * @return 更新的行数
     */
    @Update("update product set stock = stock + #{number} where id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("number") Integer number);
}

package com.qingshan.mapper;

import com.qingshan.entity.ProductSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 修改SKU，保留原有id与编号
     * @param sku SKU信息
     */
    @Update("update product_sku set sku_code = #{skuCode}, attrs = #{attrs}, price = #{price}, "
            + "stock = #{stock}, image = #{image}, status = #{status}, update_time = #{updateTime} "
            + "where id = #{id} and product_id = #{productId}")
    void update(ProductSku sku);

    /**
     * 仅修改SKU编号，用于批量重排编号时先落到临时编号，避免唯一键冲突
     * @param id SKU id
     * @param skuCode 临时编号
     */
    @Update("update product_sku set sku_code = #{skuCode} where id = #{id}")
    void updateSkuCode(@Param("id") Long id, @Param("skuCode") String skuCode);

    /**
     * 删除商品下不在保留列表中的SKU
     * @param productId 商品id
     * @param ids 需要保留的SKU id列表，为空时删除全部
     */
    @Delete("<script>"
            + "delete from product_sku where product_id = #{productId}"
            + "<if test='ids != null and ids.size() > 0'> and id not in "
            + "<foreach collection='ids' open='(' close=')' separator=',' item='id'>#{id}</foreach>"
            + "</if>"
            + "</script>")
    void deleteByProductIdExcludingIds(@Param("productId") Long productId, @Param("ids") List<Long> ids);

    /**
     * 扣减 SKU 库存，库存不足时不更新（返回0）
     * @param id SKU id
     * @param number 扣减数量
     * @return 更新的行数
     */
    @Update("update product_sku set stock = stock - #{number} where id = #{id} and stock >= #{number}")
    int deductStock(@Param("id") Long id, @Param("number") Integer number);

    /**
     * 回滚 SKU 库存
     * @param id SKU id
     * @param number 回滚数量
     * @return 更新的行数
     */
    @Update("update product_sku set stock = stock + #{number} where id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("number") Integer number);
}

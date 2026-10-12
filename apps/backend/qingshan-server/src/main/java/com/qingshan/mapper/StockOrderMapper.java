package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.annotation.AutoFill;
import com.qingshan.dto.StockOrderPageQueryDTO;
import com.qingshan.entity.StockOrder;
import com.qingshan.enumeration.OperationType;
import com.qingshan.vo.StockItemVO;
import com.qingshan.vo.StockOrderExportVO;
import com.qingshan.vo.StockOrderVO;
import com.qingshan.vo.StockSkuVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockOrderMapper {

    /**
     * 新增出/入库单据
     * @param order 单据
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(StockOrder order);

    /**
     * 修改待审核单据（状态由 SQL 条件保证）
     * @param order 单据
     * @return 更新行数
     */
    @AutoFill(value = OperationType.UPDATE)
    int updateDraft(StockOrder order);

    /**
     * 根据id查询单据
     * @param id 单据id
     * @return 单据
     */
    @Select("select * from stock_order where id = #{id}")
    StockOrder getById(Long id);

    /**
     * 审核单据，只有待审核状态能更新成功
     * @param id 单据id
     * @param userId 审核人
     * @return 更新行数
     */
    int audit(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 删除待审核单据，已审核单据不会删除
     * @param id 单据id
     * @return 删除行数
     */
    int deleteDraft(Long id);

    /**
     * 单据分页查询
     * @param queryDTO 查询条件
     * @return 单据列表
     */
    Page<StockOrderVO> pageQuery(StockOrderPageQueryDTO queryDTO);

    /**
     * 单据明细导出查询（一行一条明细）
     * @param queryDTO 查询条件
     * @return 导出数据
     */
    List<StockOrderExportVO> listForExport(StockOrderPageQueryDTO queryDTO);

    /**
     * 按 SKU 编号精确查询快速录入信息
     * @param skuCode SKU编号
     * @return 商品/SKU信息
     */
    StockItemVO getSkuItem(@Param("skuCode") String skuCode);

    /**
     * 按商品编号精确查询快速录入信息
     * @param productCode 商品编号
     * @return 商品信息
     */
    StockItemVO getProductItem(@Param("productCode") String productCode);

    /**
     * 查询商品下的全部SKU选项
     * @param productId 商品id
     * @return SKU列表
     */
    List<StockSkuVO> listSkuOptions(@Param("productId") Long productId);
}

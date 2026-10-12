package com.qingshan.mapper;

import com.qingshan.entity.StockOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockOrderItemMapper {

    /**
     * 批量新增单据明细
     * @param items 明细列表
     */
    void insertBatch(@Param("items") List<StockOrderItem> items);

    /**
     * 查询单据明细
     * @param orderId 单据id
     * @return 明细列表
     */
    List<StockOrderItem> listByOrderId(Long orderId);

    /**
     * 删除单据明细
     * @param orderId 单据id
     */
    void deleteByOrderId(Long orderId);

    /**
     * 审核后回填明细的库存前后值
     * @param item 明细
     */
    void updateStockSnapshot(StockOrderItem item);
}

package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.dto.StockLedgerPageQueryDTO;
import com.qingshan.entity.StockLedger;
import com.qingshan.vo.StockLedgerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockLedgerMapper {

    /**
     * 批量新增库存流水
     * @param items 流水列表
     */
    void insertBatch(@Param("items") List<StockLedger> items);

    /**
     * 台账分页查询
     * @param queryDTO 查询条件
     * @return 台账列表
     */
    Page<StockLedgerVO> pageQuery(StockLedgerPageQueryDTO queryDTO);

    /**
     * 台账导出查询
     * @param queryDTO 查询条件
     * @return 导出数据
     */
    List<StockLedgerVO> listForExport(StockLedgerPageQueryDTO queryDTO);
}

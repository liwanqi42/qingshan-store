package com.qingshan.service;

import com.qingshan.dto.StockLedgerPageQueryDTO;
import com.qingshan.dto.StockOrderDTO;
import com.qingshan.dto.StockOrderPageQueryDTO;
import com.qingshan.result.PageResult;
import com.qingshan.vo.StockItemVO;
import com.qingshan.vo.StockOrderVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 入库/出库单据与库存台账
 */
public interface StockService {

    /**
     * 按商品编号或SKU编号查询快速录入信息
     * @param code 商品编号或SKU编号
     * @return 商品/SKU信息
     */
    StockItemVO lookupItem(String code);

    /**
     * 新建待审核单据
     * @param stockOrderDTO 单据信息
     */
    void save(StockOrderDTO stockOrderDTO);

    /**
     * 修改待审核单据
     * @param stockOrderDTO 单据信息
     */
    void update(StockOrderDTO stockOrderDTO);

    /**
     * 删除待审核单据
     * @param id 单据id
     */
    void delete(Long id);

    /**
     * 审核单据，入库增加库存、出库扣减库存（库存不足整单回滚）
     * @param id 单据id
     */
    void audit(Long id);

    /**
     * 单据分页查询
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult pageQuery(StockOrderPageQueryDTO queryDTO);

    /**
     * 查询单据详情
     * @param id 单据id
     * @return 单据详情
     */
    StockOrderVO getDetail(Long id);

    /**
     * 导出单据明细 Excel
     * @param queryDTO 查询条件
     * @param response 响应
     */
    void exportOrders(StockOrderPageQueryDTO queryDTO, HttpServletResponse response);

    /**
     * 库存台账分页查询
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult ledgerPage(StockLedgerPageQueryDTO queryDTO);

    /**
     * 导出库存台账 Excel
     * @param queryDTO 查询条件
     * @param response 响应
     */
    void exportLedger(StockLedgerPageQueryDTO queryDTO, HttpServletResponse response);
}

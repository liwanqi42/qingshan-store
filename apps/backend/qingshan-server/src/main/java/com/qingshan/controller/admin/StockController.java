package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.dto.StockLedgerPageQueryDTO;
import com.qingshan.dto.StockOrderDTO;
import com.qingshan.dto.StockOrderPageQueryDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.RoleService;
import com.qingshan.service.StockService;
import com.qingshan.service.support.ProductCacheManager;
import com.qingshan.vo.StockItemVO;
import com.qingshan.vo.StockOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 入库/出库单据与库存台账
 */
@RestController
@RequestMapping("/admin/stock")
@Api(tags = "库存管理相关接口")
@Slf4j
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductCacheManager productCacheManager;

    @Autowired
    private RoleService roleService;

    /**
     * 按商品编号或SKU编号查询快速录入信息
     * @param code 商品编号或SKU编号
     * @return 商品/SKU信息
     */
    @GetMapping("/item")
    @ApiOperation("按商品编号或SKU编号查询")
    public Result<StockItemVO> lookupItem(@RequestParam String code) {
        log.info("库存快速录入查询: {}", code);
        return Result.success(stockService.lookupItem(code));
    }

    /**
     * 新建待审核单据
     * @param stockOrderDTO 单据信息
     * @return 操作结果
     */
    @PostMapping("/orders")
    @ApiOperation("新建入库/出库单")
    public Result save(@RequestBody StockOrderDTO stockOrderDTO) {
        log.info("新建库存单据: type={}, items={}", stockOrderDTO.getType(),
                stockOrderDTO.getItems() == null ? 0 : stockOrderDTO.getItems().size());
        stockService.save(stockOrderDTO);
        return Result.success();
    }

    /**
     * 修改待审核单据
     * @param stockOrderDTO 单据信息
     * @return 操作结果
     */
    @PutMapping("/orders")
    @ApiOperation("修改入库/出库单")
    public Result update(@RequestBody StockOrderDTO stockOrderDTO) {
        log.info("修改库存单据: id={}", stockOrderDTO.getId());
        stockService.update(stockOrderDTO);
        return Result.success();
    }

    /**
     * 删除待审核单据
     * @param id 单据id
     * @return 操作结果
     */
    @DeleteMapping("/orders/{id}")
    @ApiOperation("删除入库/出库单")
    public Result delete(@PathVariable Long id) {
        log.info("删除库存单据: {}", id);
        stockService.delete(id);
        return Result.success();
    }

    /**
     * 审核单据，审核通过后才会变更库存
     * @param id 单据id
     * @return 操作结果
     */
    @PostMapping("/orders/{id}/audit")
    @RequireRole
    @ApiOperation("审核入库/出库单")
    public Result audit(@PathVariable Long id) {
        log.info("审核库存单据: {}", id);
        //审核会实际变更库存，仅允许超级管理员操作
        roleService.checkCurrentAdmin();
        stockService.audit(id);

        //库存变化后清理用户端商品缓存
        productCacheManager.clearProductCache();
        return Result.success();
    }

    /**
     * 单据分页查询
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping("/orders/page")
    @ApiOperation("入库/出库单分页查询")
    public Result<PageResult> page(StockOrderPageQueryDTO queryDTO) {
        log.info("库存单据分页查询: {}", queryDTO);
        return Result.success(stockService.pageQuery(queryDTO));
    }

    /**
     * 单据详情
     * @param id 单据id
     * @return 单据详情
     */
    @GetMapping("/orders/{id}")
    @ApiOperation("入库/出库单详情")
    public Result<StockOrderVO> detail(@PathVariable Long id) {
        return Result.success(stockService.getDetail(id));
    }

    /**
     * 导出单据明细 Excel
     * @param queryDTO 查询条件
     * @param response 响应
     */
    @GetMapping("/orders/export")
    @ApiOperation("导出入库/出库单 Excel")
    public void exportOrders(StockOrderPageQueryDTO queryDTO, HttpServletResponse response) {
        stockService.exportOrders(queryDTO, response);
    }

    /**
     * 库存台账分页查询
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping("/ledger/page")
    @ApiOperation("库存台账分页查询")
    public Result<PageResult> ledgerPage(StockLedgerPageQueryDTO queryDTO) {
        log.info("库存台账分页查询: {}", queryDTO);
        return Result.success(stockService.ledgerPage(queryDTO));
    }

    /**
     * 导出库存台账 Excel
     * @param queryDTO 查询条件
     * @param response 响应
     */
    @GetMapping("/ledger/export")
    @ApiOperation("导出库存台账 Excel")
    public void exportLedger(StockLedgerPageQueryDTO queryDTO, HttpServletResponse response) {
        stockService.exportLedger(queryDTO, response);
    }
}

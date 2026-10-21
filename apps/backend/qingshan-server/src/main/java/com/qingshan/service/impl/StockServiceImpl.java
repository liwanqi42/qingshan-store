package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.StockLedgerPageQueryDTO;
import com.qingshan.dto.StockOrderDTO;
import com.qingshan.dto.StockOrderItemDTO;
import com.qingshan.dto.StockOrderPageQueryDTO;
import com.qingshan.entity.*;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.*;
import com.qingshan.result.PageResult;
import com.qingshan.service.StockService;
import com.qingshan.utils.PageUtil;
import com.qingshan.utils.SnowflakeIdGenerator;
import com.qingshan.vo.StockItemVO;
import com.qingshan.vo.StockLedgerVO;
import com.qingshan.vo.StockOrderExportVO;
import com.qingshan.vo.StockOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 入库/出库单据与库存台账
 */
@Service
@Slf4j
public class StockServiceImpl implements StockService {

    /**
     * 单张单据最多明细行数，避免一次录入过多导致事务过长
     */
    private static final int MAX_ITEM_LINES = 200;

    /**
     * 单行最大数量
     */
    private static final int MAX_QUANTITY = 999999;

    private static final int MAX_REMARK_LENGTH = 200;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] ORDER_EXPORT_HEADERS = {
            "单据编号", "类型", "状态", "商品编号", "SKU编号", "商品名称", "规格",
            "数量", "制单人", "制单时间", "审核人", "审核时间", "备注"
    };

    private static final String[] LEDGER_EXPORT_HEADERS = {
            "单据编号", "类型", "商品编号", "SKU编号", "商品名称", "规格",
            "变动数量", "变动前库存", "变动后库存", "操作人", "操作时间"
    };

    @Autowired
    private StockOrderMapper stockOrderMapper;

    @Autowired
    private StockOrderItemMapper stockOrderItemMapper;

    @Autowired
    private StockLedgerMapper stockLedgerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 按编号查询快速录入信息：SKU编号优先，其次商品编号
     * @param code 商品编号或SKU编号
     * @return 商品/SKU信息
     */
    @Override
    public StockItemVO lookupItem(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BaseException(MessageConstant.STOCK_ITEM_CODE_REQUIRED);
        }

        String keyword = code.trim();

        StockItemVO skuItem = stockOrderMapper.getSkuItem(keyword);
        if (skuItem != null) {
            if (skuItem.getSkuStatus() != null && skuItem.getSkuStatus() != 1) {
                throw new BaseException(MessageConstant.STOCK_SKU_DISABLED);
            }
            skuItem.setSkuList(stockOrderMapper.listSkuOptions(skuItem.getProductId()));
            return skuItem;
        }

        StockItemVO productItem = stockOrderMapper.getProductItem(keyword);
        if (productItem == null) {
            throw new BaseException(MessageConstant.STOCK_ITEM_NOT_FOUND);
        }
        productItem.setSkuList(stockOrderMapper.listSkuOptions(productItem.getProductId()));
        return productItem;
    }

    /**
     * 新建待审核单据
     * @param stockOrderDTO 单据信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(StockOrderDTO stockOrderDTO) {
        validateType(stockOrderDTO.getType());
        List<StockOrderItem> items = resolveItems(stockOrderDTO.getItems());

        StockOrder order = StockOrder.builder()
                .orderNo(generateOrderNo(stockOrderDTO.getType()))
                .type(stockOrderDTO.getType())
                .status(StockOrder.STATUS_PENDING)
                .totalQuantity(sumQuantity(items))
                .totalKinds(items.size())
                .remark(normalizeRemark(stockOrderDTO.getRemark()))
                .build();
        stockOrderMapper.insert(order);

        fillItems(order.getId(), items);
        stockOrderItemMapper.insertBatch(items);
    }

    /**
     * 修改待审核单据
     * @param stockOrderDTO 单据信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockOrderDTO stockOrderDTO) {
        if (stockOrderDTO.getId() == null) {
            throw new BaseException(MessageConstant.STOCK_ORDER_NOT_FOUND);
        }
        validateType(stockOrderDTO.getType());

        StockOrder existing = stockOrderMapper.getById(stockOrderDTO.getId());
        if (existing == null) {
            throw new BaseException(MessageConstant.STOCK_ORDER_NOT_FOUND);
        }
        if (!Integer.valueOf(StockOrder.STATUS_PENDING).equals(existing.getStatus())) {
            throw new BaseException(MessageConstant.STOCK_ORDER_UPDATED);
        }
        if (!existing.getType().equals(stockOrderDTO.getType())) {
            throw new BaseException(MessageConstant.STOCK_ORDER_TYPE_IMMUTABLE);
        }

        List<StockOrderItem> items = resolveItems(stockOrderDTO.getItems());
        StockOrder update = StockOrder.builder()
                .id(stockOrderDTO.getId())
                .totalQuantity(sumQuantity(items))
                .totalKinds(items.size())
                .remark(normalizeRemark(stockOrderDTO.getRemark()))
                .build();

        //状态在 SQL 条件中再次校验，避免编辑期间单据被审核
        if (stockOrderMapper.updateDraft(update) == 0) {
            throw new BaseException(MessageConstant.STOCK_ORDER_STATUS_ERROR);
        }

        stockOrderItemMapper.deleteByOrderId(stockOrderDTO.getId());
        fillItems(stockOrderDTO.getId(), items);
        stockOrderItemMapper.insertBatch(items);
    }

    /**
     * 删除待审核单据
     * @param id 单据id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (stockOrderMapper.deleteDraft(id) == 0) {
            throw new BaseException(MessageConstant.STOCK_ORDER_DELETED);
        }
        stockOrderItemMapper.deleteByOrderId(id);
    }

    /**
     * 审核单据：入库增加库存、出库原子扣减库存，任一行失败整单回滚
     * @param id 单据id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id) {
        StockOrder order = stockOrderMapper.getById(id);
        if (order == null) {
            throw new BaseException(MessageConstant.STOCK_ORDER_NOT_FOUND);
        }
        if (!Integer.valueOf(StockOrder.STATUS_PENDING).equals(order.getStatus())) {
            throw new BaseException(MessageConstant.STOCK_ORDER_STATUS_ERROR);
        }

        List<StockOrderItem> items = stockOrderItemMapper.listByOrderId(id);
        if (items == null || items.isEmpty()) {
            throw new BaseException(MessageConstant.STOCK_ORDER_EMPTY);
        }

        //固定按商品、SKU顺序处理，与订单扣减的 SKU -> 商品顺序保持一致，降低死锁概率
        items.sort(Comparator.comparing(StockOrderItem::getProductId)
                .thenComparing(StockOrderItem::getSkuId,
                        Comparator.nullsFirst(Long::compareTo)));

        Long userId = BaseContext.getCurrentId();
        if (stockOrderMapper.audit(id, userId) == 0) {
            throw new BaseException(MessageConstant.STOCK_ORDER_STATUS_ERROR);
        }

        boolean stockOut = StockOrder.TYPE_OUT == order.getType();
        LocalDateTime now = LocalDateTime.now();
        List<StockLedger> ledgers = new ArrayList<>(items.size());

        for (StockOrderItem item : items) {
            StockChange change = applyStockChange(item, stockOut);
            item.setStockBefore(change.getBefore());
            item.setStockAfter(change.getAfter());
            stockOrderItemMapper.updateStockSnapshot(item);

            if (stockOut) {
                productMapper.addSales(item.getProductId(), item.getQuantity());
            }

            ledgers.add(StockLedger.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .type(order.getType())
                    .productId(item.getProductId())
                    .skuId(item.getSkuId())
                    .productCode(item.getProductCode())
                    .skuCode(item.getSkuCode())
                    .productName(item.getProductName())
                    .skuAttrs(item.getSkuAttrs())
                    .quantity(item.getQuantity())
                    .stockBefore(change.getBefore())
                    .stockAfter(change.getAfter())
                    .createTime(now)
                    .createUser(userId)
                    .build());
        }

        stockLedgerMapper.insertBatch(ledgers);
    }

    /**
     * 单据分页查询
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery(StockOrderPageQueryDTO queryDTO) {
        validateType(queryDTO.getType());
        normalizeQuery(queryDTO);
        queryDTO.setPage(PageUtil.pageNum(queryDTO.getPage()));
        queryDTO.setPageSize(PageUtil.pageSize(queryDTO.getPageSize()));

        try (Page<StockOrderVO> page = PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize())) {
            List<StockOrderVO> records = stockOrderMapper.pageQuery(queryDTO);
            return new PageResult(page.getTotal(), records);
        }
    }

    /**
     * 查询单据详情
     * @param id 单据id
     * @return 单据详情
     */
    @Override
    public StockOrderVO getDetail(Long id) {
        StockOrder order = stockOrderMapper.getById(id);
        if (order == null) {
            throw new BaseException(MessageConstant.STOCK_ORDER_NOT_FOUND);
        }

        StockOrderVO vo = new StockOrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setItems(stockOrderItemMapper.listByOrderId(id));
        vo.setCreateUserName(getEmployeeName(order.getCreateUser()));
        vo.setAuditUserName(getEmployeeName(order.getAuditUser()));
        return vo;
    }

    /**
     * 导出单据明细 Excel
     * @param queryDTO 查询条件
     * @param response 响应
     */
    @Override
    public void exportOrders(StockOrderPageQueryDTO queryDTO, HttpServletResponse response) {
        validateType(queryDTO.getType());
        normalizeQuery(queryDTO);

        List<StockOrderExportVO> rows = stockOrderMapper.listForExport(queryDTO);
        List<Object[]> data = new ArrayList<>(rows.size());
        for (StockOrderExportVO row : rows) {
            data.add(new Object[]{
                    row.getOrderNo(),
                    typeLabel(row.getType()),
                    statusLabel(row.getStatus()),
                    row.getProductCode(),
                    row.getSkuCode(),
                    row.getProductName(),
                    formatAttrs(row.getSkuAttrs()),
                    row.getQuantity(),
                    row.getCreateUserName(),
                    formatTime(row.getCreateTime()),
                    row.getAuditUserName(),
                    formatTime(row.getAuditTime()),
                    row.getRemark()
            });
        }

        String typeName = queryDTO.getType() == StockOrder.TYPE_IN ? "入库单" : "出库单";
        writeExcel(response, typeName + "明细.xlsx", typeName + "明细",
                ORDER_EXPORT_HEADERS, data);
    }

    /**
     * 库存台账分页查询
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult ledgerPage(StockLedgerPageQueryDTO queryDTO) {
        validateOptionalType(queryDTO.getType());
        normalizeQuery(queryDTO);
        queryDTO.setPage(PageUtil.pageNum(queryDTO.getPage()));
        queryDTO.setPageSize(PageUtil.pageSize(queryDTO.getPageSize()));

        try (Page<StockLedgerVO> page = PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize())) {
            List<StockLedgerVO> records = stockLedgerMapper.pageQuery(queryDTO);
            return new PageResult(page.getTotal(), records);
        }
    }

    /**
     * 导出库存台账 Excel
     * @param queryDTO 查询条件
     * @param response 响应
     */
    @Override
    public void exportLedger(StockLedgerPageQueryDTO queryDTO, HttpServletResponse response) {
        validateOptionalType(queryDTO.getType());
        normalizeQuery(queryDTO);

        List<StockLedgerVO> rows = stockLedgerMapper.listForExport(queryDTO);
        List<Object[]> data = new ArrayList<>(rows.size());
        for (StockLedgerVO row : rows) {
            int sign = row.getType() != null && row.getType() == StockOrder.TYPE_OUT ? -1 : 1;
            data.add(new Object[]{
                    row.getOrderNo(),
                    typeLabel(row.getType()),
                    row.getProductCode(),
                    row.getSkuCode(),
                    row.getProductName(),
                    formatAttrs(row.getSkuAttrs()),
                    sign * (row.getQuantity() == null ? 0 : row.getQuantity()),
                    row.getStockBefore(),
                    row.getStockAfter(),
                    row.getCreateUserName(),
                    formatTime(row.getCreateTime())
            });
        }

        writeExcel(response, "库存流水台账.xlsx", "库存流水",
                LEDGER_EXPORT_HEADERS, data);
    }

    /**
     * 校验并执行单行库存变动，返回变动前后库存
     * @param item 明细
     * @param stockOut 是否出库
     * @return 变动前后库存
     */
    private StockChange applyStockChange(StockOrderItem item, boolean stockOut) {
        Product product = productMapper.getById(item.getProductId());
        if (product == null) {
            throw new BaseException(MessageConstant.STOCK_ITEM_DELETED
                    + "：" + item.getProductName());
        }

        int quantity = item.getQuantity();
        int before;
        int after;

        if (item.getSkuId() != null) {
            ProductSku sku = productSkuMapper.getById(item.getSkuId());
            if (sku == null) {
                throw new BaseException(MessageConstant.STOCK_SKU_NOT_FOUND
                        + "：" + item.getSkuCode());
            }
            if (!item.getProductId().equals(sku.getProductId())) {
                throw new BaseException(MessageConstant.STOCK_SKU_MISMATCH);
            }

            if (stockOut) {
                //先扣SKU再扣商品汇总库存，与下单扣减顺序一致
                if (productSkuMapper.deductStock(item.getSkuId(), quantity) == 0) {
                    throw stockNotEnough(item);
                }
                after = productSkuMapper.getStock(item.getSkuId());
                before = after + quantity;

                if (productMapper.deductStock(item.getProductId(), quantity) == 0) {
                    throw stockNotEnough(item);
                }
            } else {
                productSkuMapper.restoreStock(item.getSkuId(), quantity);
                after = productSkuMapper.getStock(item.getSkuId());
                before = after - quantity;
                productMapper.restoreStock(item.getProductId(), quantity);
            }
        } else {
            //草稿保存后商品才维护规格时，禁止继续按商品汇总库存出入库
            if (productSkuMapper.countByProductId(item.getProductId()) > 0) {
                throw new BaseException(MessageConstant.STOCK_ITEM_HAS_SKU
                        + "：" + item.getProductName());
            }

            if (stockOut) {
                if (productMapper.deductStock(item.getProductId(), quantity) == 0) {
                    throw stockNotEnough(item);
                }
            } else {
                productMapper.restoreStock(item.getProductId(), quantity);
            }
            after = productMapper.getById(item.getProductId()).getStock();
            before = stockOut ? after + quantity : after - quantity;
        }

        return new StockChange(before, after);
    }

    /**
     * 校验明细并把重复的商品+SKU行合并为一条
     * @param itemDTOList 明细入参
     * @return 合并后的明细
     */
    private List<StockOrderItem> resolveItems(List<StockOrderItemDTO> itemDTOList) {
        if (itemDTOList == null || itemDTOList.isEmpty()) {
            throw new BaseException(MessageConstant.STOCK_ORDER_EMPTY);
        }
        if (itemDTOList.size() > MAX_ITEM_LINES) {
            throw new BaseException(MessageConstant.STOCK_ORDER_TOO_MANY_ITEMS);
        }

        Map<String, StockOrderItem> merged = new LinkedHashMap<>();
        for (StockOrderItemDTO itemDTO : itemDTOList) {
            if (itemDTO.getProductId() == null) {
                throw new BaseException(MessageConstant.PRODUCT_NOT_FOUND);
            }
            int quantity = itemDTO.getQuantity() == null ? 0 : itemDTO.getQuantity();
            if (quantity <= 0 || quantity > MAX_QUANTITY) {
                throw new BaseException(MessageConstant.STOCK_QUANTITY_INVALID);
            }

            Product product = productMapper.getById(itemDTO.getProductId());
            if (product == null) {
                throw new BaseException(MessageConstant.PRODUCT_NOT_FOUND);
            }

            String key;
            StockOrderItem.StockOrderItemBuilder builder = StockOrderItem.builder()
                    .productId(product.getId())
                    .productCode(product.getProductCode())
                    .productName(product.getName());

            if (itemDTO.getSkuId() != null) {
                ProductSku sku = productSkuMapper.getById(itemDTO.getSkuId());
                if (sku == null) {
                    throw new BaseException(MessageConstant.STOCK_SKU_NOT_FOUND);
                }
                if (!product.getId().equals(sku.getProductId())) {
                    throw new BaseException(MessageConstant.STOCK_SKU_MISMATCH);
                }
                if (!Integer.valueOf(1).equals(sku.getStatus())) {
                    throw new BaseException(MessageConstant.STOCK_SKU_DISABLED
                            + "：" + sku.getSkuCode());
                }
                builder.skuId(sku.getId())
                        .skuCode(sku.getSkuCode())
                        .skuAttrs(sku.getAttrs());
                key = product.getId() + ":" + sku.getId();
            } else {
                if (productSkuMapper.countByProductId(product.getId()) > 0) {
                    throw new BaseException(MessageConstant.STOCK_ITEM_SKU_REQUIRED
                            + "：" + product.getName());
                }
                key = product.getId() + ":";
            }

            StockOrderItem existing = merged.get(key);
            if (existing == null) {
                merged.put(key, builder.quantity(quantity).build());
            } else {
                int total = existing.getQuantity() + quantity;
                if (total > MAX_QUANTITY) {
                    throw new BaseException(MessageConstant.STOCK_QUANTITY_INVALID);
                }
                existing.setQuantity(total);
            }
        }

        return new ArrayList<>(merged.values());
    }

    private void fillItems(Long orderId, List<StockOrderItem> items) {
        LocalDateTime now = LocalDateTime.now();
        for (StockOrderItem item : items) {
            item.setOrderId(orderId);
            item.setCreateTime(now);
        }
    }

    private int sumQuantity(List<StockOrderItem> items) {
        int total = 0;
        for (StockOrderItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    private String generateOrderNo(Integer type) {
        String prefix = type == StockOrder.TYPE_IN ? "RK" : "CK";
        return prefix + snowflakeIdGenerator.nextIdStr();
    }

    private String normalizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        String trimmed = remark.trim();
        if (trimmed.length() > MAX_REMARK_LENGTH) {
            throw new BaseException("备注不能超过" + MAX_REMARK_LENGTH + "个字符");
        }
        return trimmed;
    }

    private void validateType(Integer type) {
        if (type == null
                || (type != StockOrder.TYPE_IN && type != StockOrder.TYPE_OUT)) {
            throw new BaseException(MessageConstant.STOCK_ORDER_TYPE_INVALID);
        }
    }

    private void validateOptionalType(Integer type) {
        if (type != null) {
            validateType(type);
        }
    }

    private void normalizeQuery(StockOrderPageQueryDTO queryDTO) {
        queryDTO.setOrderNo(trimToNull(queryDTO.getOrderNo()));
        queryDTO.setCode(trimToNull(queryDTO.getCode()));
    }

    private void normalizeQuery(StockLedgerPageQueryDTO queryDTO) {
        queryDTO.setOrderNo(trimToNull(queryDTO.getOrderNo()));
        queryDTO.setCode(trimToNull(queryDTO.getCode()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String getEmployeeName(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        Employee employee = employeeMapper.getById(employeeId);
        return employee == null ? null : employee.getName();
    }

    private BaseException stockNotEnough(StockOrderItem item) {
        String label = StringUtils.hasText(item.getSkuCode())
                ? item.getProductName() + " / " + item.getSkuCode()
                : item.getProductName();
        return new BaseException("「" + label + "」" + MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
    }

    /**
     * attrs 为 JSON 字符串，这里只做简单展示；前端会格式化展示
     */
    private String formatAttrs(String attrs) {
        return attrs;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMATTER);
    }

    private String typeLabel(Integer type) {
        if (type == null) {
            return "";
        }
        return type == StockOrder.TYPE_IN ? "入库" : "出库";
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == StockOrder.STATUS_AUDITED ? "已审核" : "待审核";
    }

    /**
     * 写出 Excel 到响应流
     * @param response 响应
     * @param fileName 文件名
     * @param sheetName 工作表名
     * @param headers 表头
     * @param rows 数据行
     */
    private void writeExcel(HttpServletResponse response, String fileName, String sheetName,
                            String[] headers, List<Object[]> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row headRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Object[] values = rows.get(rowIndex);
                for (int col = 0; col < values.length; col++) {
                    Cell cell = row.createCell(col);
                    Object value = values[col];
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }

            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedName);

            try (ServletOutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
        } catch (IOException e) {
            log.error("导出库存单据失败", e);
            throw new RuntimeException("导出库存单据失败", e);
        }
    }

    /**
     * 单行库存变动结果
     */
    private static class StockChange {
        private final int before;
        private final int after;

        StockChange(int before, int after) {
            this.before = before;
            this.after = after;
        }

        int getBefore() {
            return before;
        }

        int getAfter() {
            return after;
        }
    }
}

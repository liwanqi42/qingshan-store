package com.qingshan.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 库存流水台账分页查询入参
 */
@Data
public class StockLedgerPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int page;

    private int pageSize;

    //类型 1入库 2出库
    private Integer type;

    //单据编号
    private String orderNo;

    //商品编号或SKU编号
    private String code;

    //开始日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDate;

    //结束日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}

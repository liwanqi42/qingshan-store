package com.qingshan.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 单据导出 Excel 行数据（一行一条明细）
 */
@Data
public class StockOrderExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Integer type;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private String createUserName;

    private LocalDateTime auditTime;

    private String auditUserName;

    private String productCode;

    private String skuCode;

    private String productName;

    private String skuAttrs;

    private Integer quantity;
}

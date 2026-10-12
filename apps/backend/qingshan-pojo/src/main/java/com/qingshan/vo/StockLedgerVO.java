package com.qingshan.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水台账
 */
@Data
public class StockLedgerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private String orderNo;

    //类型 1入库 2出库
    private Integer type;

    private Long productId;

    private Long skuId;

    private String productCode;

    private String skuCode;

    private String productName;

    private String skuAttrs;

    private Integer quantity;

    private Integer stockBefore;

    private Integer stockAfter;

    private LocalDateTime createTime;

    private Long createUser;

    private String createUserName;
}

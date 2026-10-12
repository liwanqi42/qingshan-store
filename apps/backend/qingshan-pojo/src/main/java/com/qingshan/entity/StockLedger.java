package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水台账，仅记录手工入库/出库审核通过产生的库存变化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLedger implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //单据id
    private Long orderId;

    //单据编号
    private String orderNo;

    //类型 1入库 2出库
    private Integer type;

    //商品id
    private Long productId;

    //SKU id
    private Long skuId;

    //商品编号快照
    private String productCode;

    //SKU编号快照
    private String skuCode;

    //商品名称快照
    private String productName;

    //规格属性JSON快照
    private String skuAttrs;

    //变动数量，按 type 区分方向
    private Integer quantity;

    //变动前库存
    private Integer stockBefore;

    //变动后库存
    private Integer stockAfter;

    private LocalDateTime createTime;

    //操作人
    private Long createUser;
}

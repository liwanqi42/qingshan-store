package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 入库/出库单据保存入参
 */
@Data
public class StockOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //单据id，修改草稿时传入
    private Long id;

    //类型 1入库 2出库
    private Integer type;

    //备注
    private String remark;

    //商品明细
    private List<StockOrderItemDTO> items;
}

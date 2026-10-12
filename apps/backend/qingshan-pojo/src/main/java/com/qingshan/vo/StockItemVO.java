package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品编号/SKU编号查询结果，用于入库出库快速录入
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;

    private String productCode;

    private String productName;

    private String image;

    //商品价格
    private BigDecimal price;

    //商品总库存
    private Integer stock;

    //命中SKU时返回
    private Long skuId;

    private String skuCode;

    //规格属性JSON
    private String attrs;

    private BigDecimal skuPrice;

    private Integer skuStock;

    //0禁用 1启用
    private Integer skuStatus;

    //商品下全部SKU，输入商品编号且存在规格时供前端选择
    @Builder.Default
    private List<StockSkuVO> skuList = new ArrayList<>();
}

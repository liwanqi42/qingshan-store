package com.qingshan.service;

import com.qingshan.entity.ProductSku;

/**
 * 商品规格解析
 */
public interface ProductStyleService {

    /**
     * 解析并校验商品已选规格
     * 有 SKU 时以 SKU 属性为准；无 SKU 时按商品属性定义校验并生成规范 JSON
     * @param productId 商品id
     * @param sku SKU，可为空
     * @param productStyle 前端提交的已选属性JSON
     * @return 规范化的已选属性JSON
     */
    String resolve(Long productId, ProductSku sku, String productStyle);
}

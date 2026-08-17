package com.qingshan.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingshan.entity.ProductAttr;
import com.qingshan.entity.ProductSku;
import com.qingshan.exception.ProductStyleBusinessException;
import com.qingshan.mapper.ProductAttrsMapper;
import com.qingshan.service.ProductStyleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductStyleServiceImpl implements ProductStyleService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private ProductAttrsMapper productAttrsMapper;

    /**
     * 解析并校验商品已选规格
     * 有 SKU 时直接使用 SKU 的属性JSON；无 SKU 时按商品属性定义校验并生成规范JSON
     * @param productId 商品id
     * @param sku SKU，可为空
     * @param productStyle 前端提交的已选属性JSON
     * @return 规范化的已选属性JSON
     */
    @Override
    public String resolve(Long productId, ProductSku sku, String productStyle) {
        if (sku != null) {
            if (!StringUtils.hasText(sku.getAttrs())) {
                throw new ProductStyleBusinessException("商品规格异常");
            }
            return sku.getAttrs();
        }

        Map<String, String> selected = parseSelectedStyle(productStyle);
        List<ProductAttr> attrs = productAttrsMapper.getById(productId);

        if (attrs == null || attrs.isEmpty()) {
            return selected.isEmpty() ? null : toJson(selected);
        }

        Map<String, String> canonical = new LinkedHashMap<>();
        for (ProductAttr attr : attrs) {
            String value = selected.get(attr.getName());
            if (!StringUtils.hasText(value)) {
                throw new ProductStyleBusinessException("请选择商品规格：" + attr.getName());
            }
            if (!parseAttrValues(attr.getAttrValues()).contains(value)) {
                throw new ProductStyleBusinessException("商品规格不存在：" + attr.getName() + " " + value);
            }
            canonical.put(attr.getName(), value);
        }
        for (String attrName : selected.keySet()) {
            if (!canonical.containsKey(attrName)) {
                throw new ProductStyleBusinessException("商品规格不存在：" + attrName);
            }
        }
        return toJson(canonical);
    }

    /**
     * 解析前端提交的已选属性JSON
     * @param productStyle 已选属性JSON，例如 {"颜色":"黑色","尺码":"L"}
     * @return 属性名与属性值的映射
     */
    private Map<String, String> parseSelectedStyle(String productStyle) {
        Map<String, String> selected = new LinkedHashMap<>();
        if (!StringUtils.hasText(productStyle)) {
            return selected;
        }
        try {
            Map<String, Object> raw = OBJECT_MAPPER.readValue(productStyle, new TypeReference<Map<String, Object>>() {
            });
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getValue() != null) {
                    selected.put(entry.getKey(), entry.getValue().toString());
                }
            }
            return selected;
        } catch (JsonProcessingException e) {
            throw new ProductStyleBusinessException("商品规格格式错误");
        }
    }

    /**
     * 解析商品属性的可选值列表
     * @param attrValues 属性值JSON
     * @return 属性值列表
     */
    private List<String> parseAttrValues(String attrValues) {
        if (!StringUtils.hasText(attrValues)) {
            return Collections.emptyList();
        }
        try {
            List<String> values = OBJECT_MAPPER.readValue(attrValues, new TypeReference<List<String>>() {
            });
            return values == null ? Collections.emptyList() : values;
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private String toJson(Map<String, String> style) {
        try {
            return OBJECT_MAPPER.writeValueAsString(style);
        } catch (JsonProcessingException e) {
            throw new ProductStyleBusinessException("商品规格格式错误");
        }
    }
}

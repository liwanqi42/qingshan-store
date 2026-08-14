package com.qingshan.entity;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 商品属性/规格
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttr implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    //商品id
    private Long productId;

    //属性名称
    private String name;

    //属性值列表JSON
    @Setter(AccessLevel.NONE)
    private String attrValues;

    /**
     * 兼容前端直接传数组的提交方式，数据库中仍保存JSON字符串
     * @param attrValuesValue 属性值JSON字符串或属性值数组
     */
    @JsonSetter("attrValues")
    public void setAttrValuesValue(Object attrValuesValue) {
        if (attrValuesValue instanceof List) {
            try {
                this.attrValues = OBJECT_MAPPER.writeValueAsString(attrValuesValue);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("attrValues 格式错误", e);
            }
        } else {
            this.attrValues = attrValuesValue == null ? null : attrValuesValue.toString();
        }
    }

}

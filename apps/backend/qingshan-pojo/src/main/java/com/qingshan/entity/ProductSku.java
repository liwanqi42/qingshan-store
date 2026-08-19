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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;

    //商品id
    private Long productId;

    //SKU编码
    private String skuCode;

    //规格属性JSON，例如：{"颜色":"黑色","尺码":"L"}
    @Setter(AccessLevel.NONE)
    private String attrs;

    //SKU价格
    private BigDecimal price;

    //SKU库存
    private Integer stock;

    //SKU图片
    private String image;

    //状态 0禁用 1启用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 兼容前端直接传对象或JSON字符串两种方式，数据库统一保存JSON字符串
     * @param attrsValue 规格属性对象或JSON字符串
     */
    @JsonSetter("attrs")
    public void setAttrsValue(Object attrsValue) {
        this.attrs = toJsonString(attrsValue);
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return ((String) value).trim();
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("attrs 格式错误", e);
        }
    }

}

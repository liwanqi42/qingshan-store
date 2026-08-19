package com.qingshan.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.qingshan.entity.ProductAttr;
import com.qingshan.entity.ProductSku;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDTO implements Serializable {

    private Long id;
    //商品名称
    private String name;
    //商品分类id
    private Long categoryId;
    //商品价格
    private BigDecimal price;
    //封面图片
    @Setter(AccessLevel.NONE)
    private String image;
    //样式图列表（详情页轮播）
    @Setter(AccessLevel.NONE)
    private List<String> images = new ArrayList<>();
    //详情长图列表
    @Setter(AccessLevel.NONE)
    private List<String> detailImages = new ArrayList<>();
    //以下标记用于修改商品时区分"未传字段"和"传了空数组"
    @JsonIgnore
    private boolean imageProvided;
    @JsonIgnore
    private boolean imagesProvided;
    @JsonIgnore
    private boolean detailImagesProvided;
    //描述信息
    private String description;
    //0 下架 1 上架
    private Integer status;
    //库存
    private Integer stock;
    //商品属性/规格
    private List<ProductAttr> attrs = new ArrayList<>();
    //SKU列表：修改时不传表示保留原有SKU，传空数组表示清空
    private List<ProductSku> skuList;

    /**
     * 兼容前端把图片数组直接放在 image 字段中的提交方式
     * @param imageValue 图片地址或图片地址数组
     */
    @JsonSetter("image")
    public void setImageValue(Object imageValue) {
        this.imageProvided = true;
        if (imageValue instanceof List) {
            List<String> imageList = toStringList(imageValue);
            this.images = imageList;
            this.imagesProvided = true;
            this.image = imageList.isEmpty() ? null : imageList.get(0);
        } else {
            this.image = imageValue == null ? null : imageValue.toString();
        }
    }

    /**
     * 兼容 images 传数组或单个字符串两种方式
     * @param imagesValue 图片地址或图片地址数组
     */
    @JsonSetter("images")
    public void setImagesValue(Object imagesValue) {
        this.imagesProvided = true;
        this.images = toStringList(imagesValue);
    }

    /**
     * 详情长图，兼容数组或单个字符串
     * @param detailImagesValue 详情长图地址或地址数组
     */
    @JsonSetter("detailImages")
    public void setDetailImagesValue(Object detailImagesValue) {
        this.detailImagesProvided = true;
        this.detailImages = toStringList(detailImagesValue);
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        } else if (value != null) {
            result.add(value.toString());
        }
        return result;
    }

}

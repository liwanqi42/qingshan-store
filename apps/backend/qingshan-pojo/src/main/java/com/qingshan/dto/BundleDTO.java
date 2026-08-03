package com.qingshan.dto;

import com.qingshan.entity.BundleItem;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BundleDTO implements Serializable {

    private Long id;

    //分类id
    private Long categoryId;

    //套装名称
    private String name;

    //套装价格
    private BigDecimal price;

    //状态 0:下架 1:上架
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    //套装商品关系
    private List<BundleItem> bundleItems = new ArrayList<>();

}

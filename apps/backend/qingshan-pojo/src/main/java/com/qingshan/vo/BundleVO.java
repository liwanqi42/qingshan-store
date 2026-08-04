package com.qingshan.vo;

import com.qingshan.entity.BundleItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleVO implements Serializable {

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

    //更新时间
    private LocalDateTime updateTime;

    //分类名称
    private String categoryName;

    //套装和商品的关联关系
    private List<BundleItem> bundleItems = new ArrayList<>();
}

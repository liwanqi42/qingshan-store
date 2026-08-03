package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BundlePageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    //套装名称
    private String name;

    //分类id
    private Long categoryId;

    //状态 0下架 1上架
    private Integer status;

}

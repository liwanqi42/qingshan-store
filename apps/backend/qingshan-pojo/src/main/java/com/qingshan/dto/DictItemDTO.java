package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典项新增/修改
 */
@Data
public class DictItemDTO implements Serializable {

    private Long id;

    //字典类型id
    private Long typeId;

    //字典标签
    private String label;

    //字典值，新增后不可修改
    private String value;

    //标签样式
    private String tagType;

    //排序
    private Integer sort;

    //状态 0:禁用，1:启用
    private Integer status;
}

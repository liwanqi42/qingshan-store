package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型新增/修改
 */
@Data
public class DictTypeDTO implements Serializable {

    private Long id;

    //字典类型编码，新增后不可修改
    private String type;

    //字典名称
    private String name;

    //状态 0:禁用，1:启用
    private Integer status;

    //排序
    private Integer sort;
}

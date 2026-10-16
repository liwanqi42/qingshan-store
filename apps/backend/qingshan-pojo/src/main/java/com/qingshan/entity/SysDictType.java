package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysDictType implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //字典类型编码
    private String type;

    //字典名称
    private String name;

    //状态 0:禁用，1:启用
    private Integer status;

    //排序，越小越靠前
    private Integer sort;

    //是否内置 0:否，1:是
    private Integer isSystem;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}

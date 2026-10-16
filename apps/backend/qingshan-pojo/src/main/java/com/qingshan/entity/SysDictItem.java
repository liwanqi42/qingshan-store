package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysDictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //字典类型id
    private Long typeId;

    //字典标签
    private String label;

    //字典值
    private String value;

    //标签样式 success/info/warning/danger
    private String tagType;

    //排序，越小越靠前
    private Integer sort;

    //状态 0:禁用，1:启用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}

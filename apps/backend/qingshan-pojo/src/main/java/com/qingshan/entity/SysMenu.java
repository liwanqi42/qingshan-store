package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单展示配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //路由名称
    private String name;

    //路由路径
    private String path;

    //菜单标题
    private String title;

    //菜单图标
    private String icon;

    //排序，越小越靠前
    private Integer sort;

    //是否隐藏 0:否，1:是
    private Integer hidden;

    //是否归入更多管理 0:否，1:是
    private Integer more;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}

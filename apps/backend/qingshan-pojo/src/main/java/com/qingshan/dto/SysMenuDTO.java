package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单配置新增/修改
 */
@Data
public class SysMenuDTO implements Serializable {

    private Long id;

    //路由名称，新增后不可修改
    private String name;

    //路由路径
    private String path;

    //菜单标题
    private String title;

    //菜单图标
    private String icon;

    //排序
    private Integer sort;

    //是否隐藏
    private Boolean hidden;

    //是否归入更多管理
    private Boolean more;
}

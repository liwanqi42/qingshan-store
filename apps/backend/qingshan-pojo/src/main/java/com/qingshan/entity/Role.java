package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色与菜单权限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 超级管理员角色编码 */
    public static final String CODE_ADMIN = "admin";

    /** 普通员工角色编码 */
    public static final String CODE_EMPLOYEE = "employee";

    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 状态 0:禁用，1:启用 */
    private Integer status;

    /** 可访问的路由名称，逗号分隔，* 表示全部 */
    private String menuPaths;

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;

    //创建人
    private Long createUser;

    //修改人
    private Long updateUser;
}

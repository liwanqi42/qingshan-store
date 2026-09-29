package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色新增/编辑参数
 */
@Data
public class RoleDTO implements Serializable {

    private Long id;

    private String name;

    private String code;

    private Integer status;

    /** 可访问的路由名称 */
    private List<String> menuPaths;
}

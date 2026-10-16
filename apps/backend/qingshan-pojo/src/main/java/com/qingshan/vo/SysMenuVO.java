package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 菜单配置视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysMenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String path;

    private String title;

    private String icon;

    private Integer sort;

    private Integer hidden;

    private Integer more;
}

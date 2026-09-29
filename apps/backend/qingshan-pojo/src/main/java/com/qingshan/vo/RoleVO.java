package com.qingshan.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "角色信息")
public class RoleVO implements Serializable {

    @ApiModelProperty("主键值")
    private Long id;

    @ApiModelProperty("角色名称")
    private String name;

    @ApiModelProperty("角色编码")
    private String code;

    @ApiModelProperty("状态 0:禁用，1:启用")
    private Integer status;

    @ApiModelProperty("可访问的路由名称")
    private List<String> menuPaths;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}

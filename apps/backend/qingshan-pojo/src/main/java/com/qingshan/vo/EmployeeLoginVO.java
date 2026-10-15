package com.qingshan.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "员工登录返回的数据格式")
public class EmployeeLoginVO implements Serializable {

    @ApiModelProperty("主键值")
    private Long id;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("jwt令牌")
    private String token;

    @ApiModelProperty("角色编码")
    private String roleCode;

    @ApiModelProperty("角色列表")
    private List<String> roles;

    @ApiModelProperty("按钮权限标识列表")
    private List<String> permissions;

    @ApiModelProperty("可访问的路由名称列表，* 表示全部")
    private List<String> menuPaths;

    @ApiModelProperty("是否必须先修改初始密码")
    private Boolean mustChangePassword;

}

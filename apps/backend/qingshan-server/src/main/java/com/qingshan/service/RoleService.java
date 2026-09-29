package com.qingshan.service;

import com.qingshan.dto.RoleDTO;
import com.qingshan.entity.Role;
import com.qingshan.vo.RoleVO;

import java.util.List;

public interface RoleService {

    /**
     * 查询角色列表
     * @param status 状态 0:禁用，1:启用，为空查询全部
     * @return 角色列表
     */
    List<RoleVO> list(Integer status);

    /**
     * 查询启用状态的角色，不存在或已停用时抛出业务异常
     * @param code 角色编码
     * @return 角色
     */
    Role requireEnabledByCode(String code);

    /**
     * 角色可访问的路由名称，* 表示全部
     * @param role 角色
     * @return 路由名称列表
     */
    List<String> resolveMenuPaths(Role role);

    /**
     * 校验当前登录员工是否为管理员
     */
    void checkCurrentAdmin();

    void save(RoleDTO roleDTO);

    void update(RoleDTO roleDTO);

    void deleteById(Long id);

    void enableOrDisable(Integer status, Long id);

    /**
     * 恢复两个内置角色的默认菜单权限
     */
    void reset();
}

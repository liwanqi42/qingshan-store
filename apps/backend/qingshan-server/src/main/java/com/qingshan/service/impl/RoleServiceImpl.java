package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.RoleDTO;
import com.qingshan.entity.Employee;
import com.qingshan.entity.Role;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.EmployeeMapper;
import com.qingshan.mapper.RoleMapper;
import com.qingshan.service.RoleService;
import com.qingshan.vo.RoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 * 提供角色新增、修改、删除、启用禁用、菜单权限、内置角色重置等能力
 */
@Service
public class RoleServiceImpl implements RoleService {

    /**
     * 角色编码正则：以字母开头，后续可包含字母、数字、下划线
     */
    private static final String CODE_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]*$";

    /**
     * 超级管理员默认菜单路径，逗号分隔
     */
    private static final String DEFAULT_ADMIN_MENUS =
            "DashboardView,StatisticsView,OrderView,CategoryView,ProductView,BundleView,"
                    + "StockInView,StockOutView,StockLedgerView,"
                    + "SystemUserView,SystemMenuView,SystemRoleView,SystemDictView,ShopView";

    /**
     * 普通员工默认菜单路径，逗号分隔
     */
    private static final String DEFAULT_EMPLOYEE_MENUS = "DashboardView,SystemUserView";

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 根据状态查询角色列表
     * @param status 状态：启用/禁用，null 查询全部
     * @return 角色VO列表
     */
    @Override
    public List<RoleVO> list(Integer status) {
        return roleMapper.list(status).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色编码获取【已启用】角色，不存在或禁用抛异常
     * @param code 角色编码
     * @return 角色实体
     */
    @Override
    public Role requireEnabledByCode(String code) {
        Role role = StringUtils.hasText(code) ? roleMapper.getByCode(code) : null;
        if (role == null || !StatusConstant.ENABLE.equals(role.getStatus())) {
            throw new BaseException(MessageConstant.ROLE_UNAVAILABLE);
        }
        return role;
    }

    /**
     * 解析角色对应的菜单路径集合
     * 超级管理员角色且未配置菜单时返回 ["*"] 代表全权限
     * @param role 角色
     * @return 菜单标识列表
     */
    @Override
    public List<String> resolveMenuPaths(Role role) {
        List<String> menuPaths = splitMenuPaths(role.getMenuPaths());
        if (menuPaths.isEmpty() && Role.CODE_ADMIN.equals(role.getCode())) {
            return Collections.singletonList("*");
        }
        return menuPaths;
    }

    /**
     * 校验当前登录用户是否是可用的超级管理员
     * 不是管理员 / 角色禁用 / 用户不存在 抛出无权限异常
     */
    @Override
    public void checkCurrentAdmin() {
        Long empId = BaseContext.getCurrentId();
        Employee employee = empId == null ? null : employeeMapper.getById(empId);
        Role role = employee == null ? null : roleMapper.getByCode(employee.getRoleCode());
        if (role == null
                || !Role.CODE_ADMIN.equals(role.getCode())
                || !StatusConstant.ENABLE.equals(role.getStatus())) {
            throw new BaseException(MessageConstant.NO_PERMISSION);
        }
    }

    /**
     * 新增角色
     * @param roleDTO 角色入参DTO
     */
    @Override
    public void save(RoleDTO roleDTO) {
        // 校验角色名称非空
        checkRoleName(roleDTO.getName());
        // 校验角色编码格式合法
        checkRoleCode(roleDTO.getCode());

        String code = roleDTO.getCode().trim();
        // 编码不能重复
        if (roleMapper.getByCode(code) != null) {
            throw new BaseException(MessageConstant.ROLE_CODE_EXISTS);
        }

        Role role = Role.builder()
                .name(roleDTO.getName().trim())
                .code(code)
                .status(roleDTO.getStatus() == null ? StatusConstant.ENABLE : roleDTO.getStatus())
                .menuPaths(joinMenuPaths(roleDTO.getMenuPaths()))
                .build();
        roleMapper.insert(role);
    }

    /**
     * 修改角色
     * 规则：角色编码不允许修改；超级管理员角色不能被禁用
     * @param roleDTO 角色入参DTO
     */
    @Override
    @Transactional
    public void update(RoleDTO roleDTO) {
        Role existing = requireById(roleDTO.getId());

        // 如果传了名称，校验名称
        if (roleDTO.getName() != null) {
            checkRoleName(roleDTO.getName());
        }
        // 编码不允许变更
        if (roleDTO.getCode() != null && !existing.getCode().equals(roleDTO.getCode())) {
            throw new BaseException(MessageConstant.ROLE_CODE_IMMUTABLE);
        }
        // 超级管理员禁止禁用
        if (Role.CODE_ADMIN.equals(existing.getCode())
                && StatusConstant.DISABLE.equals(roleDTO.getStatus())) {
            throw new BaseException(MessageConstant.ADMIN_ROLE_PROTECTED);
        }

        Role role = Role.builder()
                .id(existing.getId())
                .name(roleDTO.getName() == null ? null : roleDTO.getName().trim())
                .status(roleDTO.getStatus())
                .menuPaths(joinMenuPaths(roleDTO.getMenuPaths()))
                .build();
        roleMapper.update(role);

        // 角色被停用后，该角色下员工已签发的 token 立即失效
        if (StatusConstant.DISABLE.equals(roleDTO.getStatus())
                && !StatusConstant.DISABLE.equals(existing.getStatus())) {
            employeeMapper.increaseTokenVersionByRoleCode(existing.getCode());
        }
    }

    /**
     * 删除角色
     * 超级管理员禁止删除；已有员工绑定该角色禁止删除
     * @param id 角色id
     */
    @Override
    public void deleteById(Long id) {
        Role existing = requireById(id);
        if (Role.CODE_ADMIN.equals(existing.getCode())) {
            throw new BaseException(MessageConstant.ADMIN_ROLE_PROTECTED);
        }
        if (employeeMapper.countByRoleCode(existing.getCode()) > 0) {
            throw new BaseException(MessageConstant.ROLE_IN_USE);
        }
        roleMapper.deleteById(id);
    }

    /**
     * 启用/禁用角色
     * 超级管理员不能禁用
     * @param status 目标状态
     * @param id 角色id
     */
    @Override
    @Transactional
    public void enableOrDisable(Integer status, Long id) {
        Role existing = requireById(id);
        if (Role.CODE_ADMIN.equals(existing.getCode())
                && StatusConstant.DISABLE.equals(status)) {
            throw new BaseException(MessageConstant.ADMIN_ROLE_PROTECTED);
        }
        roleMapper.update(Role.builder().id(id).status(status).build());

        // 角色被停用后，该角色下员工已签发的 token 立即失效
        if (StatusConstant.DISABLE.equals(status)) {
            employeeMapper.increaseTokenVersionByRoleCode(existing.getCode());
        }
    }

    /**
     * 重置内置角色（超级管理员、普通员工）
     * 恢复名称、启用状态、默认菜单权限
     */
    @Override
    public void reset() {
        restoreBuiltInRole(Role.CODE_ADMIN, "超级管理员", DEFAULT_ADMIN_MENUS);
        restoreBuiltInRole(Role.CODE_EMPLOYEE, "普通员工", DEFAULT_EMPLOYEE_MENUS);
    }

    /**
     * 恢复内置角色配置，角色不存在则跳过
     * @param code 角色编码
     * @param name 角色名称
     * @param menuPaths 默认菜单串
     */
    private void restoreBuiltInRole(String code, String name, String menuPaths) {
        Role role = roleMapper.getByCode(code);
        if (role == null) {
            return;
        }
        roleMapper.update(Role.builder()
                .id(role.getId())
                .name(name)
                .status(StatusConstant.ENABLE)
                .menuPaths(menuPaths)
                .build());
    }

    /**
     * 根据id查询角色，不存在抛异常
     * @param id 角色id
     * @return 角色实体
     */
    private Role requireById(Long id) {
        Role role = id == null ? null : roleMapper.getById(id);
        if (role == null) {
            throw new BaseException(MessageConstant.ROLE_NOT_FOUND);
        }
        return role;
    }

    /**
     * 校验角色名称非空
     * @param name 角色名称
     */
    private void checkRoleName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BaseException(MessageConstant.ROLE_NAME_REQUIRED);
        }
    }

    /**
     * 校验角色编码：非空 + 格式匹配正则
     * @param code 角色编码
     */
    private void checkRoleCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BaseException(MessageConstant.ROLE_CODE_REQUIRED);
        }
        if (!code.trim().matches(CODE_PATTERN)) {
            throw new BaseException(MessageConstant.ROLE_CODE_INVALID);
        }
    }

    /**
     * Role 实体转 RoleVO
     * @param role 角色实体
     * @return VO
     */
    private RoleVO toVO(Role role) {
        return RoleVO.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .status(role.getStatus())
                .menuPaths(splitMenuPaths(role.getMenuPaths()))
                .createTime(role.getCreateTime())
                .build();
    }

    /**
     * 逗号分隔字符串 转 菜单列表，自动过滤空项
     * @param menuPaths 逗号拼接菜单串
     * @return 菜单标识集合
     */
    private List<String> splitMenuPaths(String menuPaths) {
        if (!StringUtils.hasText(menuPaths)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String item : menuPaths.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 菜单列表 转 逗号分隔字符串，去重（保持顺序）、过滤空值
     * @param menuPaths 前端传入菜单集合
     * @return 逗号拼接字符串
     */
    private String joinMenuPaths(List<String> menuPaths) {
        if (menuPaths == null) {
            return null;
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String item : menuPaths) {
            if (item != null && !item.trim().isEmpty()) {
                distinct.add(item.trim());
            }
        }
        return String.join(",", distinct);
    }
}

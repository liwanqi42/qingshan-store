package com.qingshan.controller.admin;

import com.qingshan.dto.RoleDTO;
import com.qingshan.result.Result;
import com.qingshan.service.RoleService;
import com.qingshan.vo.RoleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与菜单权限管理
 */
@RestController
@RequestMapping("/admin/role")
@Api(tags = "角色相关接口")
@Slf4j
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 查询角色列表
     * @param status 状态 0:禁用，1:启用，为空查询全部
     * @return 角色列表
     */
    @GetMapping("/list")
    @ApiOperation("查询角色列表")
    public Result<List<RoleVO>> list(Integer status) {
        return Result.success(roleService.list(status));
    }

    /**
     * 新增角色
     * @param roleDTO 角色信息
     * @return
     */
    @PostMapping
    @ApiOperation("新增角色")
    public Result save(@RequestBody RoleDTO roleDTO) {
        roleService.checkCurrentAdmin();
        log.info("新增角色：{}", roleDTO);
        roleService.save(roleDTO);
        return Result.success();
    }

    /**
     * 修改角色
     * @param roleDTO 角色信息
     * @return
     */
    @PutMapping
    @ApiOperation("修改角色")
    public Result update(@RequestBody RoleDTO roleDTO) {
        roleService.checkCurrentAdmin();
        roleService.update(roleDTO);
        return Result.success();
    }

    /**
     * 删除角色
     * @param id 角色id
     * @return
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除角色")
    public Result delete(@PathVariable Long id) {
        roleService.checkCurrentAdmin();
        roleService.deleteById(id);
        return Result.success();
    }

    /**
     * 启用、禁用角色
     * @param status 状态 0:禁用，1:启用
     * @param id 角色id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用角色")
    public Result enableOrDisable(@PathVariable("status") Integer status, Long id) {
        roleService.checkCurrentAdmin();
        roleService.enableOrDisable(status, id);
        return Result.success();
    }

    /**
     * 恢复内置角色的默认菜单权限
     * @return
     */
    @PostMapping("/reset")
    @ApiOperation("恢复默认角色配置")
    public Result reset() {
        roleService.checkCurrentAdmin();
        roleService.reset();
        return Result.success();
    }
}

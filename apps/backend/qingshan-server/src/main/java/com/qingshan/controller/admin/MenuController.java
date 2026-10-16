package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.dto.SysMenuDTO;
import com.qingshan.result.Result;
import com.qingshan.service.MenuService;
import com.qingshan.vo.SysMenuVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * 菜单展示配置管理
 */
@RestController
@RequestMapping("/admin/menu")
@Api(tags = "菜单管理接口")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/list")
    @ApiOperation("查询菜单配置")
    public Result<List<SysMenuVO>> list() {
        return Result.success(menuService.list());
    }

    @PostMapping
    @RequireRole
    @ApiOperation("新增菜单配置")
    public Result save(@RequestBody SysMenuDTO menuDTO) {
        menuService.save(menuDTO);
        return Result.success();
    }

    @PutMapping
    @RequireRole
    @ApiOperation("修改菜单配置")
    public Result update(@RequestBody SysMenuDTO menuDTO) {
        menuService.update(menuDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole
    @ApiOperation("删除菜单配置")
    public Result delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }

    @PostMapping("/reset")
    @RequireRole
    @ApiOperation("恢复默认菜单配置")
    public Result reset() {
        menuService.reset();
        return Result.success();
    }
}

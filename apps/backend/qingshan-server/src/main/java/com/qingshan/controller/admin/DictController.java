package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.dto.DictItemDTO;
import com.qingshan.dto.DictTypeDTO;
import com.qingshan.result.Result;
import com.qingshan.service.DictService;
import com.qingshan.vo.DictTypeVO;
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
 * 字典管理
 */
@RestController
@RequestMapping("/admin/dict")
@Api(tags = "字典管理接口")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /**
     * 查询全部字典
     */
    @GetMapping("/list")
    @ApiOperation("查询全部字典")
    public Result<List<DictTypeVO>> list() {
        return Result.success(dictService.listForAdmin());
    }

    @PostMapping("/type")
    @RequireRole
    @ApiOperation("新增字典类型")
    public Result saveType(@RequestBody DictTypeDTO dictTypeDTO) {
        dictService.saveType(dictTypeDTO);
        return Result.success();
    }

    @PutMapping("/type")
    @RequireRole
    @ApiOperation("修改字典类型")
    public Result updateType(@RequestBody DictTypeDTO dictTypeDTO) {
        dictService.updateType(dictTypeDTO);
        return Result.success();
    }

    @DeleteMapping("/type/{id}")
    @RequireRole
    @ApiOperation("删除字典类型")
    public Result deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.success();
    }

    @PostMapping("/item")
    @RequireRole
    @ApiOperation("新增字典项")
    public Result saveItem(@RequestBody DictItemDTO dictItemDTO) {
        dictService.saveItem(dictItemDTO);
        return Result.success();
    }

    @PutMapping("/item")
    @RequireRole
    @ApiOperation("修改字典项")
    public Result updateItem(@RequestBody DictItemDTO dictItemDTO) {
        dictService.updateItem(dictItemDTO);
        return Result.success();
    }

    @DeleteMapping("/item/{id}")
    @RequireRole
    @ApiOperation("删除字典项")
    public Result deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.success();
    }

    @PostMapping("/reset")
    @RequireRole
    @ApiOperation("恢复默认字典")
    public Result reset() {
        dictService.reset();
        return Result.success();
    }
}

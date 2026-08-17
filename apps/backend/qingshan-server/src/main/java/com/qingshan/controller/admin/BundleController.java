package com.qingshan.controller.admin;

import com.qingshan.dto.BundleDTO;
import com.qingshan.dto.BundlePageQueryDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.BundleService;
import com.qingshan.vo.BundleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/bundle")
@Api(tags = "捆绑包相关接口")
@Slf4j
public class BundleController {

    @Autowired
    private BundleService bundleService;

    /**
     * 新增捆绑包
     * @param bundleDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增捆绑包")
    @CacheEvict(cacheNames = "bundleCache", key = "#bundleDTO.categoryId")
    public Result<String> save(@RequestBody BundleDTO bundleDTO){
        log.info("新增捆绑包: {}", bundleDTO);
        bundleService.save(bundleDTO);
        return Result.success();
    }

    /**
     * 分页查询
     * @param bundlePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(BundlePageQueryDTO bundlePageQueryDTO){
        PageResult pageResult = bundleService.pageQuery(bundlePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除捆绑包
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除捆绑包")
    @CacheEvict(cacheNames = "bundleCache", allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        bundleService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐，用于修改页面回显数据
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<BundleVO> getById(@PathVariable Long id) {
        BundleVO bundleVO = bundleService.getByIdWithItems(id);
        return Result.success(bundleVO);
    }

    /**
     * 修改套餐
     * @param bundleDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "bundleCache", allEntries = true)
    public Result update(@RequestBody BundleDTO bundleDTO) {
        bundleService.update(bundleDTO);
        return Result.success();
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("套餐起售停售")
    @CacheEvict(cacheNames = "bundleCache", allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        bundleService.startOrStop(status, id);
        return Result.success();
    }


}

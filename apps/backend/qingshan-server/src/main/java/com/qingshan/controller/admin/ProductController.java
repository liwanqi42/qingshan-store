package com.qingshan.controller.admin;


import com.qingshan.dto.ProductDTO;
import com.qingshan.dto.ProductPageQueryDTO;
import com.qingshan.entity.Product;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.ProductService;
import com.qingshan.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理
 */
@RestController
@RequestMapping("/admin/product")
@Slf4j
@Api(tags = "商品相关接口")
public class ProductController {

    @Autowired
    public ProductService productService;

    /**
     * 新增商品及样式
     * @param productDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增商品")
    public Result save(@RequestBody ProductDTO productDTO){
        log.info("新增商品: {}", productDTO);
        productService.saveWithAttr(productDTO);
        return Result.success();
    }

    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("商品分页查询")
    public  Result<PageResult> page(ProductPageQueryDTO productPageQueryDTO){
        log.info("商品分页查询:{}",productPageQueryDTO);
        PageResult pageResult = productService.pageQuery(productPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 商品的批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("商品的批量删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("商品的批量删除:{}", ids);
        productService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询商品样式
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询商品样式")
    public Result<ProductVO> getById(@PathVariable Long id){
        log.info("根据id查询商品样式: {}",id);
        ProductVO productVO = productService.getByidWithAttr(id);
        return Result.success(productVO);
    }

    /**
     * 修改商品信息
     * @param productDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改商品信息")
    public Result update(@RequestBody ProductDTO productDTO){
        log.info("修改商品信息:{}", productDTO);
        productService.updateWithAtrr(productDTO);
        return Result.success();
    }

    /**
     * 商品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("商品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        productService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Product>> list(Long categoryId){
        List<Product> list = productService.list(categoryId);
        return Result.success(list);
    }
}

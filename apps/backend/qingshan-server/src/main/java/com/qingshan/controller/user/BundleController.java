package com.qingshan.controller.user;

import com.qingshan.constant.StatusConstant;
import com.qingshan.entity.Bundle;
import com.qingshan.result.Result;
import com.qingshan.service.BundleService;
import com.qingshan.vo.ProductItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userBundleController")
@RequestMapping("/user/bundle")
@Api(tags = "C端-套装浏览接口")
public class BundleController {
    @Autowired
    private BundleService bundleService;

    /**
     * 根据分类id查询在售套装
     *
     * @param categoryId 套装分类id
     * @return 套装列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套装")
    @Cacheable(cacheNames = "bundleCache", key = "#categoryId")
    public Result<List<Bundle>> list(Long categoryId) {
        Bundle bundle = Bundle.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)//查询上架中的套装
                .build();

        List<Bundle> list = bundleService.list(bundle);
        return Result.success(list);
    }

    /**
     * 根据套装id查询包含的商品列表
     *
     * @param id 套装id
     * @return 套装内商品列表
     */
    @GetMapping("/product/{id}")
    @ApiOperation("根据套装id查询包含的商品列表")
    public Result<List<ProductItemVO>> productList(@PathVariable("id") Long id) {
        List<ProductItemVO> list = bundleService.getProductItemByBundleId(id);
        return Result.success(list);
    }
}

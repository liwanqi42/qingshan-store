package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.dto.CouponDTO;
import com.qingshan.dto.CouponPageQueryDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.CouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/coupon")
@Api(tags = "管理端优惠券接口")
@Slf4j
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 分页查询优惠券，含发放与领取统计
     */
    @GetMapping("/page")
    @ApiOperation("分页查询优惠券")
    public Result<PageResult> page(CouponPageQueryDTO couponPageQueryDTO) {
        return Result.success(couponService.pageQuery(couponPageQueryDTO));
    }

    /**
     * 新增优惠券
     */
    @PostMapping
    @RequireRole
    @ApiOperation("新增优惠券")
    public Result save(@RequestBody CouponDTO couponDTO) {
        log.info("新增优惠券：{}", couponDTO.getName());
        couponService.save(couponDTO);
        return Result.success();
    }

    /**
     * 修改优惠券
     */
    @PutMapping
    @RequireRole
    @ApiOperation("修改优惠券")
    public Result update(@RequestBody CouponDTO couponDTO) {
        couponService.update(couponDTO);
        return Result.success();
    }

    /**
     * 删除未被领取的优惠券
     */
    @DeleteMapping
    @RequireRole
    @ApiOperation("删除优惠券")
    public Result delete(@RequestParam Long id) {
        couponService.delete(id);
        return Result.success();
    }

    /**
     * 上下架
     */
    @PostMapping("/status/{status}")
    @RequireRole
    @ApiOperation("优惠券上下架")
    public Result updateStatus(@PathVariable Integer status, @RequestParam Long id) {
        couponService.updateStatus(id, status);
        return Result.success();
    }
}

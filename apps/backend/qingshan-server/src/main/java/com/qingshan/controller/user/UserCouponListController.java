package com.qingshan.controller.user;

import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.CouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/user/coupon")
@Api(tags = "用户端我的优惠券接口")
public class UserCouponListController {

    private final CouponService couponService;

    public UserCouponListController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 我的优惠券：status 0未使用 1已使用 2已过期
     */
    @GetMapping("/list")
    @ApiOperation("我的优惠券")
    public Result<PageResult> list(@RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(couponService.listMine(status, page, size));
    }
}

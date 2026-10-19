package com.qingshan.controller.user;

import com.qingshan.dto.UserCouponTakeDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.CouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("UserCouponController")
@RequestMapping("/user/coupon")
@Api(tags = "用户端优惠券接口")
@Slf4j
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 可领券列表
     */
    @GetMapping("/list")
    @ApiOperation("可领券列表")
    public Result<PageResult> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(couponService.listAvailable(page, size));
    }

    /**
     * 领券
     */
    @PostMapping("/take")
    @ApiOperation("领取优惠券")
    public Result take(@RequestBody UserCouponTakeDTO userCouponTakeDTO) {
        couponService.take(userCouponTakeDTO.getCouponId());
        return Result.success();
    }
}

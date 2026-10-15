package com.qingshan.controller.user;

import com.qingshan.result.Result;
import com.qingshan.service.support.ShopStatusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "商店相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private ShopStatusService shopStatusService;

    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus(){
        int shopStatus = shopStatusService.getStatus();
        log.info("获取店铺营业状态:{}", shopStatus == ShopStatusService.STATUS_OPEN ? "营业中" : "打烊中");
        return Result.success(shopStatus);
    }
}

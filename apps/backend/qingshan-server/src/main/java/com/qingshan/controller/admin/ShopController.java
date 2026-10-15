package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.exception.BaseException;
import com.qingshan.result.Result;
import com.qingshan.service.support.ShopStatusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "商店相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private ShopStatusService shopStatusService;

    /**
     * 设置店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @RequireRole
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){
        if (status == null
                || (status != ShopStatusService.STATUS_OPEN && status != ShopStatusService.STATUS_CLOSED)) {
            throw new BaseException("营业状态不正确");
        }
        log.info("设置店铺营业状态:{}", status == ShopStatusService.STATUS_OPEN ? "营业中" : "打烊中");

        // TODO(prod): 营业状态只存 Redis，Redis 重启/清空后会回到「打烊」；
        //   需开启 Redis 持久化，或把营业状态落到数据库并在缓存缺失时回源
        shopStatusService.setStatus(status);

        return Result.success();
    }

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

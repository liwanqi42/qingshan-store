package com.qingshan.controller.admin;

import com.qingshan.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "商店相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺营业状态:{}",status == 1? "营业中":"打烊中");

        // TODO(prod): 营业状态只存 Redis，Redis 重启/清空后会回到「打烊」；
        //   需开启 Redis 持久化，或把营业状态落到数据库并在缓存缺失时回源
        redisTemplate.opsForValue().set(KEY,status);

        return Result.success();
    }

    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        //未设置营业状态时默认按打烊处理，避免空指针
        if (shopStatus == null) {
            shopStatus = 0;
        }
        log.info("获取店铺营业状态:{}",shopStatus == 1 ? "营业中":"打烊中");
        return Result.success(shopStatus);
    }
}

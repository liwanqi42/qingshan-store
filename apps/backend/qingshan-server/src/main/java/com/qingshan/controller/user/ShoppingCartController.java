package com.qingshan.controller.user;

import com.qingshan.dto.ShoppingCartDTO;
import com.qingshan.dto.ShoppingCartEditDTO;
import com.qingshan.entity.ShoppingCart;
import com.qingshan.result.Result;
import com.qingshan.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingcart")
@Slf4j
@Api(tags = "C端购物车相关接口")
public class ShoppingCartController {


    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车,商品信息为：{}", shoppingCartDTO);

        shoppingCartService.addShoppingCart(shoppingCartDTO);

        return Result.success();
    }

    /**
     * 查看购物车
     * @return 当前用户的购物车列表
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list(){
        return Result.success(shoppingCartService.list());
    }

    /**
     * 修改购物车商品数量，数量小于等于0时删除
     * @param shoppingCartEditDTO
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("修改购物车商品数量")
    public Result edit(@RequestBody ShoppingCartEditDTO shoppingCartEditDTO){
        log.info("修改购物车商品数量：{}", shoppingCartEditDTO);
        shoppingCartService.editShoppingCart(shoppingCartEditDTO);
        return Result.success();
    }

    /**
     * 购物车商品条目数
     * @return
     */
    @GetMapping("/count")
    @ApiOperation("购物车商品条目数")
    public Result<Integer> count(){
        return Result.success(shoppingCartService.count());
    }
}

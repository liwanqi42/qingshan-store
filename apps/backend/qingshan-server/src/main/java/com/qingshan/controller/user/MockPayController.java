package com.qingshan.controller.user;

import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.result.Result;
import com.qingshan.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟支付接口，仅开发环境开启（qingshan.order.mock-pay-enabled=true），生产环境不注册该接口
 *
 * TODO(wechat-pay): 接入真实微信支付后删除整个类，前端改调 OrderController 的 PUT /user/order/payment
 */
@RestController
@RequestMapping("user/order")
@Api(tags = "用户端模拟支付接口")
@Slf4j
@ConditionalOnProperty(prefix = "qingshan.order", name = "mock-pay-enabled", havingValue = "true")
public class MockPayController {

    @Autowired
    private OrderService orderService;

    /**
     * 演示环境模拟支付成功，直接把订单置为已支付
     *
     * @param ordersPaymentDTO 订单号
     * @return
     */
    @PutMapping("/mock-pay")
    @ApiOperation("模拟支付成功（仅开发环境）")
    public Result mockPay(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        log.info("模拟支付订单：{}", ordersPaymentDTO.getOrderNumber());
        orderService.mockPay(ordersPaymentDTO.getOrderNumber());
        return Result.success();
    }
}

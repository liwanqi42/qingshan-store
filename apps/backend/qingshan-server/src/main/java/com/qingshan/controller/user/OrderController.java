package com.qingshan.controller.user;

import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.OrderService;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.vo.UserOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("UserOrderController")
@RequestMapping("user/order")
@Api(tags = "用户端订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        // TODO(security): 上线前不要打印完整下单参数
        log.info("用户下单,参数：{}",ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status   订单状态 1待付款 2待发货 3已发货 4配送中 5已完成 6已取消 7退款
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) Integer status) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 订单详情
     * @param orderNo 订单号
     * @return
     */
    @GetMapping("/detail")
    @ApiOperation("订单详情")
    public Result<UserOrderVO> detail(@RequestParam String orderNo) {
        return Result.success(orderService.getDetail4User(orderNo));
    }

    /**
     * 用户取消订单
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestParam("id") Long id) {
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再来一单
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 删除订单
     * @param id 订单id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除订单")
    public Result delete(@RequestParam("id") Long id) {
        orderService.deleteOrderById(id);
        return Result.success();
    }
}

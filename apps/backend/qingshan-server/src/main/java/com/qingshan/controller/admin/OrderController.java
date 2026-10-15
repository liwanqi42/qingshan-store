package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.dto.*;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.OrderService;
import com.qingshan.vo.OrderStatisticsVO;
import com.qingshan.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/order/")
@Api(tags = "订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     *
     * @param id 订单id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        return Result.success(orderService.getDetail4Admin(id));
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 接单
     *
     * @return
     */
    @PutMapping("/confirm")
    @RequireRole
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     *
     * @return
     */
    @PutMapping("/rejection")
    @RequireRole
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     *
     * @return
     */
    @PutMapping("/cancel")
    @RequireRole
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 同意售后
     *
     * @return
     */
    @PutMapping("/refund/approve")
    @RequireRole
    @ApiOperation("同意售后")
    public Result approveRefund(@RequestBody OrdersRefundDTO ordersRefundDTO) {
        orderService.approveRefund(ordersRefundDTO);
        return Result.success();
    }

    /**
     * 确认收到退货并退款
     *
     * @return
     */
    @PutMapping("/refund/receive")
    @RequireRole
    @ApiOperation("确认收到退货并退款")
    public Result receiveRefund(@RequestBody OrdersRefundDTO ordersRefundDTO) {
        orderService.receiveRefund(ordersRefundDTO);
        return Result.success();
    }

    /**
     * 拒绝售后
     *
     * @return
     */
    @PutMapping("/refund/reject")
    @RequireRole
    @ApiOperation("拒绝售后")
    public Result rejectRefund(@RequestBody OrdersRefundRejectDTO ordersRefundRejectDTO) {
        orderService.rejectRefund(ordersRefundRejectDTO);
        return Result.success();
    }

    /**
     * 派送订单
     *
     * @return
     */
    @PutMapping("/delivery/{id}")
    @RequireRole
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     *
     * @return
     */
    @PutMapping("/complete/{id}")
    @RequireRole
    @ApiOperation("完成订单")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }
}

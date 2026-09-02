package com.qingshan.controller.user;

import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.dto.RefundApplyDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.OrderService;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderRemindVO;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.vo.RefundApplyVO;
import com.qingshan.vo.RefundDetailVO;
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
     * @param status   订单状态 -999全部 0待付款 1待发货 2待收货 3已完成 4退款/售后 5已退款
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "0") int days) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status, keyword, days);
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
     * 申请售后
     * @return 退款编号
     */
    @PutMapping("/refund_apply")
    @ApiOperation("申请售后")
    public Result<RefundApplyVO> refundApply(@RequestBody RefundApplyDTO refundApplyDTO) {
        return Result.success(orderService.refundApply(refundApplyDTO));
    }

    /**
     * 退款详情
     * @param refundNo 退款编号（订单号）
     * @return
     */
    @GetMapping("/refund_detail")
    @ApiOperation("退款详情")
    public Result<RefundDetailVO> refundDetail(@RequestParam("refundNo") String refundNo) {
        return Result.success(orderService.refundDetail(refundNo));
    }

    /**
     * 撤销退款申请
     * @param refundNo 退款编号（订单号）
     * @return
     */
    @PutMapping("/refund_undo")
    @ApiOperation("撤销退款申请")
    public Result undoRefund(@RequestParam("refundNo") String refundNo) {
        orderService.undoRefund(refundNo);
        return Result.success();
    }

    /**
     * 确认收货
     * @param id 订单id
     * @return
     */
    @PutMapping("/confirm_goods")
    @ApiOperation("确认收货")
    public Result confirmGoods(@RequestParam("id") Long id) {
        orderService.confirmGoods(id);
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

    /**
     * 个人中心订单提醒数量
     * @return 待付款、待发货、待收货数量
     */
    @GetMapping("/remind")
    @ApiOperation("订单提醒数量")
    public Result<OrderRemindVO> remind() {
        return Result.success(orderService.getRemindCount4User());
    }
}

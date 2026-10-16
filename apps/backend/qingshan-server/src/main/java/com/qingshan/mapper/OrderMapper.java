package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.dto.GoodsSalesDTO;
import com.qingshan.dto.OrdersPageQueryDTO;
import com.qingshan.entity.Orders;
import com.qingshan.vo.OrderRemindVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    Integer countByCondition(@Param("begin") LocalDateTime begin,
                             @Param("end") LocalDateTime end,
                             @Param("status") Integer status);

    /**
     * 根据条件统计订单金额
     * @param begin
     * @param end
     * @param status
     * @return
     */
    Double sumByCondition(@Param("begin") LocalDateTime begin,
                          @Param("end") LocalDateTime end,
                          @Param("status") Integer status);

    /**
     * 统计指定时间区间内的销量排名前10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 根据订单id查询订单
     * @param id
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 查询超时未支付的订单id
     * @param deadline 下单时间早于该时间的待付款订单视为超时
     * @return 订单id列表
     */
    @Select("select id from orders where status = 1 and pay_status = 0 and order_time <= #{deadline} " +
            "order by order_time limit 100")
    List<Long> listTimeoutOrderIds(@Param("deadline") LocalDateTime deadline);

    /**
     * 取消超时未支付订单，只有仍处于待付款状态的订单会被更新
     * @param id 订单id
     * @param cancelTime 取消时间
     * @param cancelReason 取消原因
     * @return 更新的行数
     */
    @Update("update orders set status = 6, cancel_reason = #{cancelReason}, cancel_time = #{cancelTime} " +
            "where id = #{id} and status = 1 and pay_status = 0")
    int cancelTimeoutOrder(@Param("id") Long id,
                           @Param("cancelTime") LocalDateTime cancelTime,
                           @Param("cancelReason") String cancelReason);

    /**
     * 用户取消订单，只有属于该用户的待付款订单会被更新
     * @param id 订单id
     * @param userId 用户id
     * @param cancelTime 取消时间
     * @param cancelReason 取消原因
     * @return 更新的行数
     */
    @Update("update orders set status = 6, cancel_reason = #{cancelReason}, cancel_time = #{cancelTime} " +
            "where id = #{id} and user_id = #{userId} and status = 1 and pay_status = 0")
    int cancelByUser(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("cancelTime") LocalDateTime cancelTime,
                     @Param("cancelReason") String cancelReason);

    /**
     * 支付成功后更新订单状态，只有待付款订单会被更新
     * @param id 订单id
     * @param checkoutTime 结账时间
     * @return 更新的行数
     */
    @Update("update orders set status = 2, pay_status = 1, checkout_time = #{checkoutTime} " +
            "where id = #{id} and status = 1 and pay_status = 0")
    int paySuccess(@Param("id") Long id, @Param("checkoutTime") LocalDateTime checkoutTime);

    /**
     * 统计用户未支付订单数量
     * @param userId 用户id
     * @return 未支付订单数量
     */
    @Select("select count(*) from orders where user_id = #{userId} and status = 1 and pay_status = 0")
    int countUnpaidOrders(@Param("userId") Long userId);

    /**
     * 统计用户在指定时间之后被系统自动取消的订单数量
     * @param userId 用户id
     * @param cancelReason 取消原因
     * @param since 统计起始时间
     * @return 订单数量
     */
    @Select("select count(*) from orders where user_id = #{userId} and status = 6 " +
            "and cancel_reason = #{cancelReason} and cancel_time >= #{since}")
    int countTimeoutOrders(@Param("userId") Long userId,
                           @Param("cancelReason") String cancelReason,
                           @Param("since") LocalDateTime since);

    /**
     * 申请售后，只有属于该用户、已支付且未在售后流程中的订单会被更新
     * 仅退款/退货退款记录退款金额，换货/补寄不退款，退款金额置空
     * @param id 订单id
     * @param userId 用户id
     * @param refundType 售后类型
     * @param reason 售后原因
     * @param applyTime 申请时间
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 1, refund_type = #{refundType}, refund_reason = #{reason}, " +
            "refund_amount = case when #{refundType} in (1, 2) then amount else null end, " +
            "refund_apply_time = #{applyTime}, refund_time = null, " +
            "refund_reject_reason = null " +
            "where id = #{id} and user_id = #{userId} and pay_status = 1 " +
            "and status in (2, 3, 4, 5) and (refund_status is null or refund_status in (0, 5, 6))")
    int applyRefund(@Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("refundType") Integer refundType,
                    @Param("reason") String reason,
                    @Param("applyTime") LocalDateTime applyTime);

    /**
     * 用户撤销售后申请：只有申请中、待买家退货的申请可以撤销；
     * 买家已寄回商品（状态3）后不能撤销，需等待商家收货退款
     * @param number 订单号（即售后编号）
     * @param userId 用户id
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 6 " +
            "where number = #{number} and user_id = #{userId} and refund_status in (1, 2)")
    int undoRefund(@Param("number") String number, @Param("userId") Long userId);

    /**
     * 管理端同意仅退款/退货退款：只有退款申请中的订单会被更新
     * @param id 订单id
     * @param refundTime 退款完成时间
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 4, refund_time = #{refundTime}, " +
            "status = 7, pay_status = 2 " +
            "where id = #{id} and pay_status = 1 and refund_status = 1 " +
            "and (refund_type is null or refund_type in (1, 2))")
    int approveRefund(@Param("id") Long id, @Param("refundTime") LocalDateTime refundTime);

    /**
     * 管理端同意已发货订单的退款：进入待买家退货，不立即退款
     * @param id 订单id
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 2 " +
            "where id = #{id} and pay_status = 1 and refund_status = 1 " +
            "and (refund_type is null or refund_type in (1, 2)) and status in (4, 5)")
    int agreeReturnRefund(@Param("id") Long id);

    /**
     * 买家填写退货物流：待买家退货 -> 买家已退货待商家收货
     * @param id 订单id
     * @param userId 用户id
     * @param expressCompany 退货快递公司
     * @param trackingNumber 退货物流单号
     * @param returnTime 退货时间
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 3, return_express_company = #{expressCompany}, " +
            "return_tracking_number = #{trackingNumber}, return_time = #{returnTime} " +
            "where id = #{id} and user_id = #{userId} and pay_status = 1 and refund_status = 2")
    int submitReturnLogistics(@Param("id") Long id,
                              @Param("userId") Long userId,
                              @Param("expressCompany") String expressCompany,
                              @Param("trackingNumber") String trackingNumber,
                              @Param("returnTime") LocalDateTime returnTime);

    /**
     * 管理端确认收到退货并退款：买家已退货待商家收货 -> 售后完成/已退款
     * @param id 订单id
     * @param refundTime 退款完成时间
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 4, refund_time = #{refundTime}, " +
            "status = 7, pay_status = 2 " +
            "where id = #{id} and pay_status = 1 and refund_status = 3 " +
            "and (refund_type is null or refund_type in (1, 2))")
    int receiveRefund(@Param("id") Long id, @Param("refundTime") LocalDateTime refundTime);

    /**
     * 管理端同意换货/补寄：只完成售后，不退款，订单状态和支付状态保持不变
     * @param id 订单id
     * @param finishTime 售后完成时间
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 4, refund_time = #{finishTime} " +
            "where id = #{id} and refund_status = 1 and refund_type in (3, 4)")
    int completeAfterSales(@Param("id") Long id, @Param("finishTime") LocalDateTime finishTime);

    /**
     * 管理端拒绝售后申请：只有售后申请中的订单会被更新，订单状态保持不变
     * @param id 订单id
     * @param rejectReason 拒绝原因
     * @return 更新的行数
     */
    @Update("update orders set refund_status = 5, refund_reject_reason = #{rejectReason} " +
            "where id = #{id} and refund_status = 1")
    int rejectRefund(@Param("id") Long id, @Param("rejectReason") String rejectReason);

    /**
     * 确认收货，只有属于该用户且已派送（配送中）的订单会被更新
     * @param id 订单id
     * @param userId 用户id
     * @return 更新的行数
     */
    @Update("update orders set status = 5, delivery_time = now() " +
            "where id = #{id} and user_id = #{userId} and status = 4 " +
            "and (refund_status is null or refund_status in (0, 5, 6))")
    int confirmGoods(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 删除属于指定用户的订单
     * @param id 订单id
     * @param userId 用户id
     * @return 删除的行数
     */
    @Delete("delete from orders where id = #{id} and user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 用户端订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计用户各状态订单数量（个人中心提醒）
     * @param userId 用户id
     * @return 待付款、待发货、待收货数量
     */
    @Select("select " +
            "coalesce(sum(case when status = 1 and pay_status = 0 then 1 else 0 end), 0) as waitPay, " +
            "coalesce(sum(case when status in (2, 3) then 1 else 0 end), 0) as notExpress, " +
            "coalesce(sum(case when status = 4 then 1 else 0 end), 0) as waitReceive " +
            "from orders where user_id = #{userId} " +
            "and (refund_status is null or refund_status not in (1, 2, 3, 4))")
    OrderRemindVO getRemindCount(@Param("userId") Long userId);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 手工录入物流轨迹时补齐订单上的快递公司和单号，已有值不覆盖
     * @param orderId 订单id
     * @param expressCompany 快递公司
     * @param trackingNumber 物流单号
     */
    @Update("update orders set " +
            "express_company = case when express_company is null or express_company = '' " +
            "then #{expressCompany} else express_company end, " +
            "tracking_number = case when tracking_number is null or tracking_number = '' " +
            "then #{trackingNumber} else tracking_number end " +
            "where id = #{orderId}")
    int fillLogisticsInfo(@Param("orderId") Long orderId,
                          @Param("expressCompany") String expressCompany,
                          @Param("trackingNumber") String trackingNumber);
}

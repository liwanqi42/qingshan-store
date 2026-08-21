package com.qingshan.mapper;

import com.qingshan.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
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
}

package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.entity.UserCoupon;
import com.qingshan.vo.UserCouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserCouponMapper {

    void insert(UserCoupon userCoupon);

    @Select("select * from user_coupon where id = #{id}")
    UserCoupon getById(Long id);

    /**
     * 统计用户已领取的某张优惠券数量（限领校验）
     */
    @Select("select count(*) from user_coupon where user_id = #{userId} and coupon_id = #{couponId}")
    int countByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 下单锁券：仅未使用且券仍可用的记录会被更新，保证一券一单
     */
    int lockForOrder(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("orderId") Long orderId,
                     @Param("now") LocalDateTime now);

    /**
     * 支付成功核销优惠券
     */
    int markUsedByOrder(@Param("orderId") Long orderId, @Param("useTime") LocalDateTime useTime);

    /**
     * 订单取消/退款退券：未过期回到未使用，已过期标记为已过期
     */
    int returnByOrder(@Param("orderId") Long orderId, @Param("now") LocalDateTime now);

    /**
     * 用户我的优惠券分页，状态按有效期动态计算
     */
    Page<UserCouponVO> pageByUser(@Param("userId") Long userId,
                                  @Param("status") Integer status,
                                  @Param("now") LocalDateTime now);
}

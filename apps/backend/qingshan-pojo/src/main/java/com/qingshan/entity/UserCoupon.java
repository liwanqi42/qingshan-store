package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户领取的优惠券
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon implements Serializable {

    // region 状态
    /** 0-未使用 */
    public static final Integer STATUS_UNUSED = 0;
    /** 1-已使用 */
    public static final Integer STATUS_USED = 1;
    /** 2-已过期 */
    public static final Integer STATUS_EXPIRED = 2;
    /** 3-锁定中（已下单待支付） */
    public static final Integer STATUS_LOCKED = 3;
    // endregion

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户id
    private Long userId;

    //优惠券id
    private Long couponId;

    //领取时间
    private LocalDateTime receiveTime;

    //使用时间
    private LocalDateTime useTime;

    //使用的订单id
    private Long orderId;

    //状态 0未使用 1已使用 2已过期 3锁定中
    private Integer status;
}

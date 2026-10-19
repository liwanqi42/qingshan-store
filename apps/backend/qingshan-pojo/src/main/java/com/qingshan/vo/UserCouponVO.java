package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端优惠券视图，字段与小程序券中心/结算页/我的券对应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //用户优惠券id
    private Long userCouponId;

    //优惠券id
    private Long couponId;

    //优惠券名称
    private String couponName;

    //类型 1满减券 2折扣券 3代金券 4满折券
    private Integer type;

    //使用门槛
    private BigDecimal fullMoney;

    //减免金额/代金金额
    private BigDecimal minus;

    //折扣率
    private BigDecimal rate;

    //有效期开始
    private LocalDateTime startTime;

    //有效期结束
    private LocalDateTime endTime;

    //领取时间
    private LocalDateTime receiveTime;

    //使用时间
    private LocalDateTime useTime;

    //使用的订单id
    private Long orderId;

    //状态 0未使用 1已使用 2已过期 3锁定中
    private Integer status;
}

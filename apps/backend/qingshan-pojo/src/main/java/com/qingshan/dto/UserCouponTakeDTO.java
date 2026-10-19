package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户领券参数
 */
@Data
public class UserCouponTakeDTO implements Serializable {

    //优惠券id
    private Long couponId;
}

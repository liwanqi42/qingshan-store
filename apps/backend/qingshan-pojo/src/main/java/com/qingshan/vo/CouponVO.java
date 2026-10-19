package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端优惠券视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //优惠券名称
    private String name;

    //类型 1满减券 2折扣券 3代金券 4满折券
    private Integer type;

    //使用门槛
    private BigDecimal fullMoney;

    //减免金额/代金金额
    private BigDecimal minus;

    //折扣率，如0.85
    private BigDecimal rate;

    //有效期开始
    private LocalDateTime startTime;

    //有效期结束
    private LocalDateTime endTime;

    //发放总量，0表示不限量
    private Integer totalCount;

    //已领取数量
    private Integer receivedCount;

    //剩余数量，不限量时为 null
    private Integer remainCount;

    //已使用数量
    private Integer usedCount;

    //每人限领数量
    private Integer limitPerUser;

    //状态 0下架 1上架
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

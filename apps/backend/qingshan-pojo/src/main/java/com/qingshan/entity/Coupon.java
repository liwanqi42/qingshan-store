package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon implements Serializable {

    // region 券类型
    /** 1-满减券 */
    public static final Integer TYPE_FULL_REDUCTION = 1;
    /** 2-折扣券 */
    public static final Integer TYPE_DISCOUNT = 2;
    /** 3-代金券 */
    public static final Integer TYPE_CASH = 3;
    /** 4-满金额折扣券 */
    public static final Integer TYPE_FULL_DISCOUNT = 4;
    // endregion

    // region 状态
    /** 0-下架 */
    public static final Integer STATUS_OFF = 0;
    /** 1-上架 */
    public static final Integer STATUS_ON = 1;
    // endregion

    private static final long serialVersionUID = 1L;

    private Long id;

    //优惠券名称
    private String name;

    //类型 1满减券 2折扣券 3代金券 4满折券
    private Integer type;

    //使用门槛，0表示无门槛
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

    //每人限领数量
    private Integer limitPerUser;

    //状态 0下架 1上架
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}

package com.qingshan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端优惠券新增/修改参数
 */
@Data
public class CouponDTO implements Serializable {

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    //有效期结束
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    //发放总量，0表示不限量
    private Integer totalCount;

    //每人限领数量
    private Integer limitPerUser;

    //状态 0下架 1上架
    private Integer status;
}

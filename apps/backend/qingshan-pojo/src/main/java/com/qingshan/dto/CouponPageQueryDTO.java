package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端优惠券分页查询
 */
@Data
public class CouponPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    //优惠券名称
    private String name;

    //类型
    private Integer type;

    //状态 0下架 1上架
    private Integer status;
}

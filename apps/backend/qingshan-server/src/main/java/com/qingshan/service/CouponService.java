package com.qingshan.service;

import com.qingshan.dto.CouponDTO;
import com.qingshan.dto.CouponPageQueryDTO;
import com.qingshan.result.PageResult;

import java.math.BigDecimal;

/**
 * 优惠券服务：管理端维护，用户端领取与使用
 */
public interface CouponService {

    /**
     * 管理端分页查询
     */
    PageResult pageQuery(CouponPageQueryDTO couponPageQueryDTO);

    /**
     * 新增优惠券
     */
    void save(CouponDTO couponDTO);

    /**
     * 修改优惠券
     */
    void update(CouponDTO couponDTO);

    /**
     * 删除未被领取的优惠券
     */
    void delete(Long id);

    /**
     * 上下架
     */
    void updateStatus(Long id, Integer status);

    /**
     * 用户端可领券列表
     */
    PageResult listAvailable(int page, int size);

    /**
     * 用户领券：限领 + 库存条件更新
     */
    void take(Long couponId);

    /**
     * 我的优惠券
     */
    PageResult listMine(Integer status, int page, int size);

    /**
     * 下单前校验用户券并计算抵扣金额
     */
    BigDecimal resolveDiscount(Long userCouponId, Long userId, BigDecimal goodsAmount);

    /**
     * 下单锁券
     */
    void lockForOrder(Long userCouponId, Long userId, Long orderId);

    /**
     * 支付成功核销
     */
    void markUsedByOrder(Long orderId);

    /**
     * 订单取消/退款退券
     */
    void returnForOrder(Long orderId);
}

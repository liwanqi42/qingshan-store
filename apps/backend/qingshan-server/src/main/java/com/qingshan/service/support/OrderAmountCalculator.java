package com.qingshan.service.support;

import com.qingshan.constant.NumConstant;
import com.qingshan.entity.Coupon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单金额计算：优惠券抵扣与实付金额统一收口，避免下单、预览等多处重复。
 *
 * 抵扣只作用于商品金额，不含运费；最多抵扣到商品金额减 0.01，
 * 保证优惠后实付金额仍大于 0，支付渠道不会因 0 元订单失败。
 */
@Component
public class OrderAmountCalculator {

    private static final BigDecimal MIN_PAYABLE = new BigDecimal("0.01");

    /**
     * 优惠券是否满足使用条件（类型与门槛）
     */
    public boolean isUsable(Coupon coupon, BigDecimal goodsAmount) {
        if (coupon == null || coupon.getType() == null || goodsAmount == null) {
            return false;
        }
        if (Coupon.TYPE_FULL_REDUCTION.equals(coupon.getType())
                || Coupon.TYPE_FULL_DISCOUNT.equals(coupon.getType())) {
            BigDecimal threshold = coupon.getFullMoney() == null ? BigDecimal.ZERO : coupon.getFullMoney();
            return goodsAmount.compareTo(threshold) >= 0;
        }
        return true;
    }

    /**
     * 计算优惠券抵扣金额
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal goodsAmount) {
        if (!isUsable(coupon, goodsAmount) || goodsAmount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal discount = BigDecimal.ZERO;
        switch (coupon.getType()) {
            case 1:
                discount = coupon.getMinus() == null ? BigDecimal.ZERO : coupon.getMinus();
                break;
            case 2:
                discount = goodsAmount.multiply(BigDecimal.ONE.subtract(rate(coupon)));
                break;
            case 3:
                discount = coupon.getMinus() == null ? BigDecimal.ZERO : coupon.getMinus();
                break;
            case 4:
                discount = goodsAmount.multiply(BigDecimal.ONE.subtract(rate(coupon)));
                break;
            default:
                discount = BigDecimal.ZERO;
        }

        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }
        // 抵扣上限：商品金额 - 0.01，避免出现 0 元订单
        BigDecimal maxDiscount = goodsAmount.subtract(MIN_PAYABLE);
        if (maxDiscount.signum() < 0) {
            return BigDecimal.ZERO.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return discount.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 实付金额 = 商品金额 + 运费 - 优惠金额
     */
    public BigDecimal payableAmount(BigDecimal goodsAmount, BigDecimal freightAmount, BigDecimal discountAmount) {
        BigDecimal goods = goodsAmount == null ? BigDecimal.ZERO : goodsAmount;
        BigDecimal freight = freightAmount == null ? BigDecimal.ZERO : freightAmount;
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal payable = goods.add(freight).subtract(discount);
        if (payable.signum() < 0) {
            payable = BigDecimal.ZERO;
        }
        return payable.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(Coupon coupon) {
        return coupon.getRate() == null ? BigDecimal.ONE : coupon.getRate();
    }
}

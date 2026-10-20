package com.qingshan.service.support;

import com.qingshan.entity.Coupon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderAmountCalculatorTest {

    private final OrderAmountCalculator calculator = new OrderAmountCalculator();

    @Test
    void fullReductionCouponRequiresThreshold() {
        Coupon coupon = coupon(Coupon.TYPE_FULL_REDUCTION, "100", "20", null);

        assertFalse(calculator.isUsable(coupon, new BigDecimal("99.99")));
        assertTrue(calculator.isUsable(coupon, new BigDecimal("100")));
        assertEquals(new BigDecimal("20.00"), calculator.calculateDiscount(coupon, new BigDecimal("100")));
    }

    @Test
    void discountCouponUsesRate() {
        Coupon coupon = coupon(Coupon.TYPE_DISCOUNT, "0", "0", "0.85");

        assertTrue(calculator.isUsable(coupon, new BigDecimal("100")));
        assertEquals(new BigDecimal("15.00"), calculator.calculateDiscount(coupon, new BigDecimal("100")));
    }

    @Test
    void fullDiscountCouponRequiresThresholdAndUsesRate() {
        Coupon coupon = coupon(Coupon.TYPE_FULL_DISCOUNT, "100", "0", "0.8");

        assertFalse(calculator.isUsable(coupon, new BigDecimal("99")));
        assertEquals(new BigDecimal("40.00"), calculator.calculateDiscount(coupon, new BigDecimal("200")));
    }

    @Test
    void discountNeverMakesOrderAmountZero() {
        Coupon coupon = coupon(Coupon.TYPE_CASH, "0", "50", null);

        // 商品金额 10 元，代金券 50 元，最多抵扣到 9.99 元
        assertEquals(new BigDecimal("9.99"), calculator.calculateDiscount(coupon, new BigDecimal("10")));
    }

    @Test
    void payableAmountIncludesFreightAndNeverNegative() {
        assertEquals(new BigDecimal("95.00"), calculator.payableAmount(
                new BigDecimal("100"), new BigDecimal("5"), new BigDecimal("10")));
        assertEquals(new BigDecimal("0.00"), calculator.payableAmount(
                new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("99")));
        assertEquals(new BigDecimal("12.34"), calculator.payableAmount(
                new BigDecimal("12.34"), null, null));
    }

    @Test
    void discountIsZeroWhenGoodsAmountIsZeroOrCouponMissing() {
        Coupon coupon = coupon(Coupon.TYPE_FULL_REDUCTION, "0", "10", null);

        assertEquals(new BigDecimal("0.00"), calculator.calculateDiscount(coupon, BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.00"), calculator.calculateDiscount(null, new BigDecimal("100")));
    }

    private Coupon coupon(Integer type, String fullMoney, String minus, String rate) {
        return Coupon.builder()
                .type(type)
                .fullMoney(new BigDecimal(fullMoney))
                .minus(new BigDecimal(minus))
                .rate(rate == null ? null : new BigDecimal(rate))
                .build();
    }
}

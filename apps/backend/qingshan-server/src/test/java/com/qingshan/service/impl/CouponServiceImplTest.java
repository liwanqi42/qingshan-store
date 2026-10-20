package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.entity.Coupon;
import com.qingshan.entity.UserCoupon;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.CouponMapper;
import com.qingshan.mapper.UserCouponMapper;
import com.qingshan.service.support.OrderAmountCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    private static final long USER_ID = 7L;
    private static final long COUPON_ID = 10L;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(couponMapper, userCouponMapper, new OrderAmountCalculator());
        BaseContext.setCurrentId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void takeRejectsSoldOutCoupon() {
        when(couponMapper.getByIdForUpdate(COUPON_ID)).thenReturn(availableCoupon(1, 1));
        when(userCouponMapper.countByUserAndCoupon(USER_ID, COUPON_ID)).thenReturn(0);
        when(couponMapper.increaseReceived(eq(COUPON_ID), any(LocalDateTime.class))).thenReturn(0);

        BaseException ex = assertThrows(BaseException.class, () -> couponService.take(COUPON_ID));

        assertEquals(MessageConstant.COUPON_SOLD_OUT, ex.getMessage());
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    void takeRejectsWhenUserLimitReached() {
        when(couponMapper.getByIdForUpdate(COUPON_ID)).thenReturn(availableCoupon(2, 100));
        when(userCouponMapper.countByUserAndCoupon(USER_ID, COUPON_ID)).thenReturn(2);

        BaseException ex = assertThrows(BaseException.class, () -> couponService.take(COUPON_ID));

        assertEquals(MessageConstant.COUPON_LIMIT_REACHED, ex.getMessage());
        verify(couponMapper, never()).increaseReceived(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void takeInsertsUnusedCoupon() {
        when(couponMapper.getByIdForUpdate(COUPON_ID)).thenReturn(availableCoupon(1, 100));
        when(userCouponMapper.countByUserAndCoupon(USER_ID, COUPON_ID)).thenReturn(0);
        when(couponMapper.increaseReceived(eq(COUPON_ID), any(LocalDateTime.class))).thenReturn(1);

        couponService.take(COUPON_ID);

        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(captor.capture());
        UserCoupon inserted = captor.getValue();
        assertEquals(USER_ID, inserted.getUserId());
        assertEquals(COUPON_ID, inserted.getCouponId());
        assertEquals(UserCoupon.STATUS_UNUSED, inserted.getStatus());
    }

    @Test
    void takeRejectsUnavailableCoupon() {
        Coupon coupon = availableCoupon(1, 100);
        coupon.setStatus(Coupon.STATUS_OFF);
        when(couponMapper.getByIdForUpdate(COUPON_ID)).thenReturn(coupon);

        BaseException ex = assertThrows(BaseException.class, () -> couponService.take(COUPON_ID));

        assertEquals(MessageConstant.COUPON_NOT_AVAILABLE, ex.getMessage());
    }

    @Test
    void concurrentTakeNeverOverIssuesStock() throws Exception {
        int stock = 5;
        int threads = 20;
        AtomicInteger remaining = new AtomicInteger(stock);
        when(couponMapper.getByIdForUpdate(COUPON_ID)).thenReturn(availableCoupon(1, stock));
        when(userCouponMapper.countByUserAndCoupon(anyLong(), eq(COUPON_ID))).thenReturn(0);
        when(couponMapper.increaseReceived(eq(COUPON_ID), any(LocalDateTime.class)))
                .thenAnswer(invocation -> remaining.getAndDecrement() > 0 ? 1 : 0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            long userId = 1000L + i;
            pool.submit(() -> {
                BaseContext.setCurrentId(userId);
                try {
                    start.await();
                    couponService.take(COUPON_ID);
                    success.incrementAndGet();
                } catch (BaseException e) {
                    if (MessageConstant.COUPON_SOLD_OUT.equals(e.getMessage())) {
                        soldOut.incrementAndGet();
                    } else {
                        unexpected.add(e);
                    }
                } catch (Throwable t) {
                    unexpected.add(t);
                } finally {
                    BaseContext.removeCurrentId();
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "并发领券超时");
        pool.shutdown();

        assertTrue(unexpected.isEmpty(), "出现非预期异常：" + unexpected);
        assertEquals(stock, success.get(), "成功领券数量应等于库存");
        assertEquals(threads - stock, soldOut.get(), "其余请求应提示已领完");
        verify(userCouponMapper, org.mockito.Mockito.times(stock)).insert(any(UserCoupon.class));
    }

    @Test
    void resolveDiscountRejectsCouponOwnedByAnotherUser() {
        when(userCouponMapper.getById(1L)).thenReturn(UserCoupon.builder()
                .id(1L).userId(USER_ID + 1).couponId(COUPON_ID)
                .status(UserCoupon.STATUS_UNUSED).build());

        BaseException ex = assertThrows(BaseException.class,
                () -> couponService.resolveDiscount(1L, USER_ID, new BigDecimal("100")));

        assertEquals(MessageConstant.COUPON_NOT_OWNED, ex.getMessage());
    }

    @Test
    void resolveDiscountRejectsUsedCoupon() {
        when(userCouponMapper.getById(1L)).thenReturn(UserCoupon.builder()
                .id(1L).userId(USER_ID).couponId(COUPON_ID)
                .status(UserCoupon.STATUS_USED).build());

        BaseException ex = assertThrows(BaseException.class,
                () -> couponService.resolveDiscount(1L, USER_ID, new BigDecimal("100")));

        assertEquals(MessageConstant.COUPON_ALREADY_USED, ex.getMessage());
    }

    @Test
    void resolveDiscountRejectsBelowThreshold() {
        when(userCouponMapper.getById(1L)).thenReturn(UserCoupon.builder()
                .id(1L).userId(USER_ID).couponId(COUPON_ID)
                .status(UserCoupon.STATUS_UNUSED).build());
        when(couponMapper.getById(COUPON_ID)).thenReturn(availableCoupon(1, 100));

        BaseException ex = assertThrows(BaseException.class,
                () -> couponService.resolveDiscount(1L, USER_ID, new BigDecimal("50")));

        assertEquals(MessageConstant.COUPON_THRESHOLD_NOT_MET, ex.getMessage());
    }

    @Test
    void resolveDiscountReturnsCalculatedAmount() {
        when(userCouponMapper.getById(1L)).thenReturn(UserCoupon.builder()
                .id(1L).userId(USER_ID).couponId(COUPON_ID)
                .status(UserCoupon.STATUS_UNUSED).build());
        when(couponMapper.getById(COUPON_ID)).thenReturn(availableCoupon(1, 100));

        BigDecimal discount = couponService.resolveDiscount(1L, USER_ID, new BigDecimal("120"));

        assertEquals(new BigDecimal("20.00"), discount);
    }

    @Test
    void lockForOrderFailsWhenCouponAlreadyUsed() {
        when(userCouponMapper.lockForOrder(eq(1L), eq(USER_ID), eq(99L), any(LocalDateTime.class)))
                .thenReturn(0);

        BaseException ex = assertThrows(BaseException.class,
                () -> couponService.lockForOrder(1L, USER_ID, 99L));

        assertEquals(MessageConstant.COUPON_LOCK_FAILED, ex.getMessage());
    }

    /**
     * 满 100 减 20 的可用券
     */
    private Coupon availableCoupon(int limitPerUser, int totalCount) {
        LocalDateTime now = LocalDateTime.now();
        return Coupon.builder()
                .id(COUPON_ID)
                .name("测试券")
                .type(Coupon.TYPE_FULL_REDUCTION)
                .fullMoney(new BigDecimal("100"))
                .minus(new BigDecimal("20"))
                .rate(BigDecimal.ONE)
                .startTime(now.minusDays(1))
                .endTime(now.plusDays(1))
                .totalCount(totalCount)
                .receivedCount(0)
                .limitPerUser(limitPerUser)
                .status(Coupon.STATUS_ON)
                .build();
    }
}

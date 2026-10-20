package com.qingshan.service.support;

import com.qingshan.entity.OrderDetail;
import com.qingshan.entity.Orders;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.mapper.AddressBookMapper;
import com.qingshan.mapper.OrderDetailMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.mapper.ShoppingCartMapper;
import com.qingshan.properties.OrderProperties;
import com.qingshan.service.CouponService;
import com.qingshan.service.OrderStockService;
import com.qingshan.service.ShoppingCartService;
import com.qingshan.utils.WeChatPayUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderManagerTest {

    @Mock
    private OrderProperties orderProperties;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private AddressBookMapper addressBookMapper;
    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private ShoppingCartService shoppingCartService;
    @Mock
    private OrderItemResolver orderItemResolver;
    @Mock
    private OrderStockService orderStockService;
    @Mock
    private OrderConverter orderConverter;
    @Mock
    private OrderValidator orderValidator;
    @Mock
    private CouponService couponService;
    @Mock
    private WeChatPayUtil weChatPayUtil;

    @InjectMocks
    private OrderManager orderManager;

    @Test
    void refundIfPaidUsesActualOrderAmount() throws Exception {
        when(orderProperties.isMockPayEnabled()).thenReturn(false);
        Orders orders = paidOrder(new BigDecimal("88.50"));

        assertTrue(orderManager.refundIfPaid(orders));

        verify(weChatPayUtil).refund("QS-1", "QS-1", new BigDecimal("88.50"), new BigDecimal("88.50"));
    }

    @Test
    void refundIfPaidSkipsWechatInMockPayMode() throws Exception {
        when(orderProperties.isMockPayEnabled()).thenReturn(true);
        Orders orders = paidOrder(new BigDecimal("88.50"));

        assertTrue(orderManager.refundIfPaid(orders));

        verify(weChatPayUtil, never()).refund(anyString(), anyString(), any(), any());
    }

    @Test
    void refundIfUnpaidDoesNothing() throws Exception {
        Orders orders = paidOrder(new BigDecimal("88.50"));
        orders.setPayStatus(Orders.PAY_STATUS_UN_PAID);

        assertFalse(orderManager.refundIfPaid(orders));

        verify(weChatPayUtil, never()).refund(anyString(), anyString(), any(), any());
    }

    @Test
    void refundRejectsAbnormalAmount() {
        when(orderProperties.isMockPayEnabled()).thenReturn(false);
        Orders orders = paidOrder(BigDecimal.ZERO);

        assertThrows(OrderBusinessException.class, () -> orderManager.refundIfPaid(orders));
    }

    @Test
    void cancelTimeoutOrdersOnlyRollsBackSuccessfullyCancelledOrders() {
        when(orderMapper.listTimeoutOrderIds(any(LocalDateTime.class))).thenReturn(Arrays.asList(1L, 2L));
        when(orderMapper.cancelTimeoutOrder(eq(1L), any(LocalDateTime.class), anyString())).thenReturn(0);
        when(orderMapper.cancelTimeoutOrder(eq(2L), any(LocalDateTime.class), anyString())).thenReturn(1);
        when(orderDetailMapper.listByOrderId(2L)).thenReturn(Collections.emptyList());

        int cancelled = orderManager.cancelTimeoutOrders();

        assertEquals(1, cancelled);
        verify(orderStockService, never()).restore(any(OrderDetail.class));
        verify(couponService, never()).returnForOrder(1L);
        verify(couponService).returnForOrder(2L);
    }

    @Test
    void restoreStockRollsBackEveryDetail() {
        OrderDetail first = new OrderDetail();
        first.setProductId(1L);
        first.setNumber(1);
        OrderDetail second = new OrderDetail();
        second.setProductId(2L);
        second.setSkuId(3L);
        second.setNumber(2);
        when(orderDetailMapper.listByOrderId(10L)).thenReturn(Arrays.asList(first, second));

        orderManager.restoreStock(10L);

        verify(orderStockService).restore(first);
        verify(orderStockService).restore(second);
    }

    private Orders paidOrder(BigDecimal amount) {
        return Orders.builder()
                .id(1L)
                .number("QS-1")
                .payStatus(Orders.PAY_STATUS_PAID)
                .amount(amount)
                .build();
    }
}

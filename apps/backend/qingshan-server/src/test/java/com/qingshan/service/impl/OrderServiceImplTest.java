package com.qingshan.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.OrdersPaymentDTO;
import com.qingshan.dto.OrdersSubmitDTO;
import com.qingshan.entity.AddressBook;
import com.qingshan.entity.Orders;
import com.qingshan.entity.User;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.mapper.OrderDetailMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.UserMapper;
import com.qingshan.service.CouponService;
import com.qingshan.service.support.*;
import com.qingshan.utils.SnowflakeIdGenerator;
import com.qingshan.utils.WeChatPayUtil;
import com.qingshan.vo.OrderPaymentVO;
import com.qingshan.vo.OrderSubmitVO;
import com.qingshan.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final long USER_ID = 7L;
    private static final String ORDER_NUMBER = "QS202610240001";

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductCacheManager productCacheManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private WeChatPayUtil weChatPayUtil;
    @Mock
    private OrderConverter orderConverter;
    @Mock
    private OrderValidator orderValidator;
    @Mock
    private OrderManager orderManager;
    @Mock
    private OrderStatusHandler orderStatusHandler;
    @Mock
    private CouponService couponService;
    @Spy
    private OrderAmountCalculator orderAmountCalculator = new OrderAmountCalculator();
    @Mock
    private WebSocketServer webSocketServer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void paymentUsesServerSideOrderAmount() throws Exception {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        when(orderValidator.requireOwnedByNumber(ORDER_NUMBER, USER_ID)).thenReturn(orders);
        when(userMapper.getById(USER_ID)).thenReturn(User.builder().id(USER_ID).openid("openid-1").build());

        JSONObject payResponse = new JSONObject();
        payResponse.put("package", "prepay_id=wx123");
        payResponse.put("nonceStr", "nonce");
        payResponse.put("timeStamp", "1729750000");
        payResponse.put("signType", "RSA");
        payResponse.put("paySign", "sign");
        when(weChatPayUtil.pay(eq(ORDER_NUMBER), eq(new BigDecimal("88.50")), anyString(), eq("openid-1")))
                .thenReturn(payResponse);

        OrdersPaymentDTO dto = new OrdersPaymentDTO();
        dto.setOrderNumber(ORDER_NUMBER);
        OrderPaymentVO vo = orderService.payment(dto);

        verify(weChatPayUtil).pay(eq(ORDER_NUMBER), eq(new BigDecimal("88.50")), anyString(), eq("openid-1"));
        assertEquals("prepay_id=wx123", vo.getPackageStr());
        assertEquals("nonce", vo.getNonceStr());
    }

    @Test
    void paymentRejectsAlreadyPaidOrder() throws Exception {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setPayStatus(Orders.PAY_STATUS_PAID);
        when(orderValidator.requireOwnedByNumber(ORDER_NUMBER, USER_ID)).thenReturn(orders);

        OrdersPaymentDTO dto = new OrdersPaymentDTO();
        dto.setOrderNumber(ORDER_NUMBER);
        OrderBusinessException ex = assertThrows(OrderBusinessException.class, () -> orderService.payment(dto));

        assertEquals(MessageConstant.ORDER_ALREADY_PAID, ex.getMessage());
        verify(weChatPayUtil, never()).pay(anyString(), any(), anyString(), anyString());
    }

    @Test
    void paymentRejectsNonPendingOrder() {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setStatus(Orders.STATUS_CANCELLED);
        when(orderValidator.requireOwnedByNumber(ORDER_NUMBER, USER_ID)).thenReturn(orders);

        OrdersPaymentDTO dto = new OrdersPaymentDTO();
        dto.setOrderNumber(ORDER_NUMBER);
        OrderBusinessException ex = assertThrows(OrderBusinessException.class, () -> orderService.payment(dto));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, ex.getMessage());
    }

    @Test
    void paySuccessIsIdempotentForRepeatedCallback() {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setId(100L);
        when(orderValidator.requireByNumber(ORDER_NUMBER)).thenReturn(orders);
        // 状态已被其它回调/超时任务改过，条件更新返回 0
        when(orderMapper.paySuccess(eq(100L), any(LocalDateTime.class))).thenReturn(0);

        orderService.paySuccess(ORDER_NUMBER);

        verify(couponService, never()).markUsedByOrder(anyLong());
        verify(webSocketServer, never()).sendToAllClient(anyString());
    }

    @Test
    void paySuccessConsumesCouponAndNotifiesOnlyOnce() {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setId(100L);
        when(orderValidator.requireByNumber(ORDER_NUMBER)).thenReturn(orders);
        when(orderMapper.paySuccess(eq(100L), any(LocalDateTime.class))).thenReturn(1);
        when(orderDetailMapper.listByOrderId(100L)).thenReturn(Collections.emptyList());

        orderService.paySuccess(ORDER_NUMBER);

        verify(couponService).markUsedByOrder(100L);
        verify(webSocketServer).sendToAllClient(anyString());
    }

    @Test
    void mockPayIsNoopForAlreadyPaidOrder() {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setPayStatus(Orders.PAY_STATUS_PAID);
        when(orderValidator.requireOwnedByNumber(ORDER_NUMBER, USER_ID)).thenReturn(orders);

        orderService.mockPay(ORDER_NUMBER);

        verify(orderMapper, never()).paySuccess(anyLong(), any(LocalDateTime.class));
        verify(couponService, never()).markUsedByOrder(anyLong());
    }

    @Test
    void userCancelRollsBackStockAndCoupon() {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setId(100L);
        when(orderValidator.requireOwnedById(100L, USER_ID)).thenReturn(orders);
        when(orderMapper.cancelByUser(eq(100L), eq(USER_ID), any(LocalDateTime.class), anyString()))
                .thenReturn(1);

        orderService.userCancelById(100L);

        verify(orderManager).restoreStock(100L);
        verify(couponService).returnForOrder(100L);
    }

    @Test
    void userCancelDoesNotRollbackWhenConditionalUpdateFails() {
        Orders orders = pendingOrder(new BigDecimal("88.50"));
        orders.setId(100L);
        when(orderValidator.requireOwnedById(100L, USER_ID)).thenReturn(orders);
        when(orderMapper.cancelByUser(eq(100L), eq(USER_ID), any(LocalDateTime.class), anyString()))
                .thenReturn(0);

        OrderBusinessException ex = assertThrows(OrderBusinessException.class,
                () -> orderService.userCancelById(100L));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, ex.getMessage());
        verify(orderManager, never()).restoreStock(anyLong());
        verify(couponService, never()).returnForOrder(anyLong());
    }

    @Test
    void submitOrderDeductsStockBeforePersistingOrder() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(1L);
        AddressBook addressBook = AddressBook.builder().id(1L).userId(USER_ID).build();
        when(orderValidator.checkAddress(1L, USER_ID)).thenReturn(addressBook);

        OrderManager.OrderItems items = mock(OrderManager.OrderItems.class);
        when(orderManager.resolveOrderItems(org.mockito.ArgumentMatchers.isNull(), eq(USER_ID))).thenReturn(items);
        when(items.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(items.getItems()).thenReturn(Collections.emptyList());
        when(orderConverter.toOrderDetails(eq(100L), any())).thenReturn(Collections.emptyList());
        when(snowflakeIdGenerator.nextIdStr()).thenReturn(ORDER_NUMBER);
        Orders persisted = pendingOrder(new BigDecimal("100.00"));
        persisted.setId(100L);
        when(orderConverter.toOrder(eq(dto), eq(addressBook), eq(USER_ID), eq(ORDER_NUMBER),
                any(LocalDateTime.class), eq(new BigDecimal("100.00")), eq(BigDecimal.ZERO),
                org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(persisted);
        when(orderConverter.toOrderSubmitVO(persisted)).thenReturn(OrderSubmitVO.builder().id(100L).build());

        OrderSubmitVO vo = orderService.submitOrder(dto);

        InOrder inOrder = inOrder(orderManager, orderMapper);
        inOrder.verify(orderManager).deductStock(items);
        inOrder.verify(orderMapper).insert(persisted);
        assertEquals(100L, vo.getId());
        verify(couponService, never()).lockForOrder(anyLong(), anyLong(), anyLong());
    }

    @Test
    void submitOrderLocksCouponAfterOrderIsPersisted() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(1L);
        dto.setUserCouponId(55L);
        AddressBook addressBook = AddressBook.builder().id(1L).userId(USER_ID).build();
        when(orderValidator.checkAddress(1L, USER_ID)).thenReturn(addressBook);

        OrderManager.OrderItems items = mock(OrderManager.OrderItems.class);
        when(orderManager.resolveOrderItems(org.mockito.ArgumentMatchers.isNull(), eq(USER_ID))).thenReturn(items);
        when(items.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(items.getItems()).thenReturn(Collections.emptyList());
        when(orderConverter.toOrderDetails(eq(100L), any())).thenReturn(Collections.emptyList());
        when(couponService.resolveDiscount(55L, USER_ID, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("20.00"));
        when(snowflakeIdGenerator.nextIdStr()).thenReturn(ORDER_NUMBER);
        Orders persisted = pendingOrder(new BigDecimal("80.00"));
        persisted.setId(100L);
        when(orderConverter.toOrder(any(), any(), anyLong(), anyString(), any(LocalDateTime.class),
                any(BigDecimal.class), any(BigDecimal.class), anyLong())).thenReturn(persisted);
        when(orderConverter.toOrderSubmitVO(persisted)).thenReturn(OrderSubmitVO.builder().id(100L).build());

        orderService.submitOrder(dto);

        verify(couponService).lockForOrder(55L, USER_ID, 100L);
        verify(orderDetailMapper).insertBatch(any());
    }

    @Test
    void submitOrderStopsBeforeDetailsWhenCouponLockFails() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(1L);
        dto.setUserCouponId(55L);
        AddressBook addressBook = AddressBook.builder().id(1L).userId(USER_ID).build();
        when(orderValidator.checkAddress(1L, USER_ID)).thenReturn(addressBook);

        OrderManager.OrderItems items = mock(OrderManager.OrderItems.class);
        when(orderManager.resolveOrderItems(org.mockito.ArgumentMatchers.isNull(), eq(USER_ID))).thenReturn(items);
        when(items.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(couponService.resolveDiscount(55L, USER_ID, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("20.00"));
        when(snowflakeIdGenerator.nextIdStr()).thenReturn(ORDER_NUMBER);
        Orders persisted = pendingOrder(new BigDecimal("80.00"));
        persisted.setId(100L);
        when(orderConverter.toOrder(any(), any(), anyLong(), anyString(), any(LocalDateTime.class),
                any(BigDecimal.class), any(BigDecimal.class), anyLong())).thenReturn(persisted);
        org.mockito.Mockito.doThrow(new OrderBusinessException(MessageConstant.COUPON_LOCK_FAILED))
                .when(couponService).lockForOrder(55L, USER_ID, 100L);

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(dto));

        // 锁券失败后不再写明细、不清购物车；事务回滚由 @Transactional 保证
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(orderManager, never()).clearCartItems(any(), anyLong());
    }

    private Orders pendingOrder(BigDecimal amount) {
        return Orders.builder()
                .id(100L)
                .number(ORDER_NUMBER)
                .userId(USER_ID)
                .status(Orders.STATUS_PENDING_PAYMENT)
                .payStatus(Orders.PAY_STATUS_UN_PAID)
                .amount(amount)
                .build();
    }
}

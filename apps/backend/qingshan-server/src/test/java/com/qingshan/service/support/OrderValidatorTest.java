package com.qingshan.service.support;

import com.qingshan.constant.MessageConstant;
import com.qingshan.entity.AddressBook;
import com.qingshan.entity.Orders;
import com.qingshan.exception.AddressBookBusinessException;
import com.qingshan.exception.OrderBusinessException;
import com.qingshan.mapper.AddressBookMapper;
import com.qingshan.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private AddressBookMapper addressBookMapper;

    @Mock
    private ShopStatusService shopStatusService;

    @Spy
    private OrderStatusHandler orderStatusHandler = new OrderStatusHandler();

    @InjectMocks
    private OrderValidator orderValidator;

    @Test
    void checkShopOpenRejectsWhenClosed() {
        when(shopStatusService.isOpen()).thenReturn(false);

        OrderBusinessException ex = assertThrows(OrderBusinessException.class,
                () -> orderValidator.checkShopOpen());

        assertEquals(MessageConstant.SHOP_CLOSED, ex.getMessage());
    }

    @Test
    void checkShopOpenPassesWhenOpen() {
        when(shopStatusService.isOpen()).thenReturn(true);

        assertDoesNotThrow(() -> orderValidator.checkShopOpen());
    }

    @Test
    void requireOwnedByNumberHidesOtherUsersOrder() {
        Orders orders = Orders.builder().id(1L).userId(2L).build();
        when(orderMapper.getByNumber("NO-1")).thenReturn(orders);

        OrderBusinessException ex = assertThrows(OrderBusinessException.class,
                () -> orderValidator.requireOwnedByNumber("NO-1", 1L));

        // 不区分“不存在”和“不属于当前用户”，避免泄露他人订单信息
        assertEquals(MessageConstant.ORDER_NOT_FOUND, ex.getMessage());
    }

    @Test
    void requireOwnedByNumberRejectsMissingOrder() {
        when(orderMapper.getByNumber("NO-404")).thenReturn(null);

        OrderBusinessException ex = assertThrows(OrderBusinessException.class,
                () -> orderValidator.requireOwnedByNumber("NO-404", 1L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, ex.getMessage());
    }

    @Test
    void requireOwnedByNumberReturnsOwnedOrder() {
        Orders orders = Orders.builder().id(1L).userId(1L).build();
        when(orderMapper.getByNumber("NO-1")).thenReturn(orders);

        assertSame(orders, orderValidator.requireOwnedByNumber("NO-1", 1L));
    }

    @Test
    void checkAddressRejectsOtherUsersAddress() {
        AddressBook addressBook = new AddressBook();
        addressBook.setId(5L);
        addressBook.setUserId(2L);
        when(addressBookMapper.getById(5L)).thenReturn(addressBook);

        AddressBookBusinessException ex = assertThrows(AddressBookBusinessException.class,
                () -> orderValidator.checkAddress(5L, 1L));

        assertEquals(MessageConstant.ADDRESS_BOOK_IS_NULL, ex.getMessage());
    }

    @Test
    void checkCanCancelRejectsCompletedOrCancelledOrder() {
        OrderBusinessException ex = assertThrows(OrderBusinessException.class,
                () -> orderValidator.checkCanCancel(Orders.builder()
                        .status(Orders.STATUS_COMPLETED)
                        .refundStatus(Orders.REFUND_STATUS_NONE)
                        .build()));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, ex.getMessage());
    }

    @Test
    void checkCanCancelPassesForPendingPaymentOrder() {
        assertDoesNotThrow(() -> orderValidator.checkCanCancel(Orders.builder()
                .status(Orders.STATUS_PENDING_PAYMENT)
                .refundStatus(Orders.REFUND_STATUS_NONE)
                .build()));
    }

    @Test
    void checkCanCancelRejectsOrderWithRefundInProgress() {
        OrderBusinessException ex = assertThrows(OrderBusinessException.class,
                () -> orderValidator.checkCanCancel(Orders.builder()
                        .status(Orders.STATUS_PENDING_PAYMENT)
                        .refundStatus(Orders.REFUND_STATUS_APPLIED)
                        .build()));

        assertEquals(MessageConstant.ORDER_REFUNDING, ex.getMessage());
    }

    @Test
    void refundTypeIsLimitedByShippingStatus() {
        Orders beforeShip = Orders.builder().status(Orders.STATUS_AWAITING_SHIPMENT).build();
        Orders afterShip = Orders.builder().status(Orders.STATUS_IN_TRANSIT).build();

        OrderBusinessException beforeShipEx = assertThrows(OrderBusinessException.class,
                () -> orderValidator.checkRefundTypeAllowed(beforeShip, Orders.REFUND_TYPE_RETURN_REFUND));
        assertEquals(MessageConstant.REFUND_TYPE_ONLY_BEFORE_SHIP, beforeShipEx.getMessage());

        OrderBusinessException afterShipEx = assertThrows(OrderBusinessException.class,
                () -> orderValidator.checkRefundTypeAllowed(afterShip, Orders.REFUND_TYPE_REFUND_ONLY));
        assertEquals(MessageConstant.REFUND_TYPE_RETURN_AFTER_SHIP, afterShipEx.getMessage());

        assertDoesNotThrow(() -> orderValidator.checkRefundTypeAllowed(beforeShip, Orders.REFUND_TYPE_REFUND_ONLY));
        assertDoesNotThrow(() -> orderValidator.checkRefundTypeAllowed(afterShip, Orders.REFUND_TYPE_RETURN_REFUND));
    }
}

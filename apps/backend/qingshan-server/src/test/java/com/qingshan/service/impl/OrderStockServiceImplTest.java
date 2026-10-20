package com.qingshan.service.impl;

import com.qingshan.entity.OrderDetail;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.ProductSkuMapper;
import com.qingshan.model.ResolvedOrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStockServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    @InjectMocks
    private OrderStockServiceImpl orderStockService;

    @Test
    void deductSkuAlsoDeductsProductStock() {
        when(productSkuMapper.deductStock(11L, 2)).thenReturn(1);
        when(productMapper.deductStock(1L, 2)).thenReturn(1);

        orderStockService.deduct(item(1L, 11L, 2));

        verify(productSkuMapper).deductStock(11L, 2);
        verify(productMapper).deductStock(1L, 2);
    }

    @Test
    void deductWithoutSkuOnlyDeductsProductStock() {
        when(productMapper.deductStock(1L, 3)).thenReturn(1);

        orderStockService.deduct(item(1L, null, 3));

        verify(productMapper).deductStock(1L, 3);
        verify(productSkuMapper, never()).deductStock(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void deductThrowsWhenSkuStockIsInsufficient() {
        when(productSkuMapper.deductStock(11L, 2)).thenReturn(0);

        ShoppingCartBusinessException ex = assertThrows(ShoppingCartBusinessException.class,
                () -> orderStockService.deduct(item(1L, 11L, 2)));

        assertTrue(ex.getMessage().contains("测试商品"));
        // SKU 扣减失败后不能再扣商品总库存
        verify(productMapper, never()).deductStock(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void deductThrowsWhenProductStockIsInsufficient() {
        when(productSkuMapper.deductStock(11L, 2)).thenReturn(1);
        when(productMapper.deductStock(1L, 2)).thenReturn(0);

        assertThrows(ShoppingCartBusinessException.class,
                () -> orderStockService.deduct(item(1L, 11L, 2)));
    }

    @Test
    void deductIgnoresNonPositiveNumber() {
        orderStockService.deduct(item(1L, 11L, 0));

        verify(productSkuMapper, never()).deductStock(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(productMapper, never()).deductStock(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void restoreSkuAlsoRestoresProductStock() {
        OrderDetail detail = new OrderDetail();
        detail.setSkuId(11L);
        detail.setProductId(1L);
        detail.setNumber(2);

        orderStockService.restore(detail);

        verify(productSkuMapper).restoreStock(11L, 2);
        verify(productMapper).restoreStock(1L, 2);
    }

    @Test
    void restoreWithoutSkuOnlyRestoresProductStock() {
        OrderDetail detail = new OrderDetail();
        detail.setProductId(1L);
        detail.setNumber(2);

        orderStockService.restore(detail);

        verify(productMapper).restoreStock(1L, 2);
        verify(productSkuMapper, never()).restoreStock(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    private ResolvedOrderItem item(Long productId, Long skuId, Integer number) {
        return ResolvedOrderItem.builder()
                .productId(productId)
                .skuId(skuId)
                .name("测试商品")
                .number(number)
                .build();
    }
}

package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.entity.OrderDetail;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.ProductSkuMapper;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.service.OrderStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderStockServiceImpl implements OrderStockService {
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    /**
     * 扣减下单商品库存
     * @param item 下单商品
     */
    public void deduct(ResolvedOrderItem item) {
        Integer number = item.getNumber();
        if (number == null || number <= 0) {
            return;
        }
        if (item.getSkuId() != null) {
            //SKU 库存与商品总库存（启用SKU库存之和）需要同时扣减
            if (productSkuMapper.deductStock(item.getSkuId(), number) == 0) {
                throw stockNotEnough(item.getName());
            }
            if (item.getProductId() != null && productMapper.deductStock(item.getProductId(), number) == 0) {
                throw stockNotEnough(item.getName());
            }
        } else if (item.getProductId() != null) {
            if (productMapper.deductStock(item.getProductId(), number) == 0) {
                throw stockNotEnough(item.getName());
            }
        }
        //套装没有独立库存，不做扣减
    }

    /**
     * 回滚订单明细对应的库存
     * @param orderDetail 订单明细
     */
    public void restore(OrderDetail orderDetail) {
        Integer number = orderDetail.getNumber();
        if (number == null || number <= 0) {
            return;
        }
        if (orderDetail.getSkuId() != null) {
            productSkuMapper.restoreStock(orderDetail.getSkuId(), number);
            if (orderDetail.getProductId() != null) {
                productMapper.restoreStock(orderDetail.getProductId(), number);
            }
        } else if (orderDetail.getProductId() != null) {
            productMapper.restoreStock(orderDetail.getProductId(), number);
        }
    }

    private ShoppingCartBusinessException stockNotEnough(String name) {
        return new ShoppingCartBusinessException(
                "「" + name + "」" + MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
    }
}

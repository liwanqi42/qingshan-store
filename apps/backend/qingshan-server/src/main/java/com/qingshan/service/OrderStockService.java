package com.qingshan.service;

import com.qingshan.entity.OrderDetail;
import com.qingshan.model.ResolvedOrderItem;

public interface OrderStockService {

    void deduct(ResolvedOrderItem item);

    void restore(OrderDetail orderDetail);
}

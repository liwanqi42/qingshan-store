package com.qingshan.dto;

import com.qingshan.entity.OrderDetail;
import com.qingshan.entity.Orders;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrdersDTO extends Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<OrderDetail> orderDetails;

}

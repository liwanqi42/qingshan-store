package com.qingshan.mapper;

import com.qingshan.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OderDetailMapper {
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 查询订单明细
     * @param orderId 订单id
     * @return 订单明细列表
     */
    List<OrderDetail> listByOrderId(Long orderId);
}

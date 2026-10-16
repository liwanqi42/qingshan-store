package com.qingshan.mapper;

import com.qingshan.entity.OrderLogisticsTrace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderLogisticsTraceMapper {

    /**
     * 按轨迹时间倒序查询订单物流轨迹
     */
    @Select("select * from order_logistics_trace where order_id = #{orderId} " +
            "order by trace_time desc, id desc")
    List<OrderLogisticsTrace> listByOrderId(Long orderId);

    @Select("select * from order_logistics_trace where id = #{id}")
    OrderLogisticsTrace getById(Long id);

    @Insert("insert into order_logistics_trace(order_id, express_company, tracking_number, " +
            "trace_time, description, status, create_time) values " +
            "(#{orderId}, #{expressCompany}, #{trackingNumber}, #{traceTime}, #{description}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderLogisticsTrace trace);

    @Delete("delete from order_logistics_trace where id = #{id}")
    void deleteById(Long id);
}

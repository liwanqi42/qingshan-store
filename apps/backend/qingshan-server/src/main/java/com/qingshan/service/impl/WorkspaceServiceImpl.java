package com.qingshan.service.impl;

import com.qingshan.constant.StatusConstant;
import com.qingshan.entity.Orders;
import com.qingshan.mapper.BundleMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.UserMapper;
import com.qingshan.service.WorkspaceService;
import com.qingshan.vo.BusinessDataVO;
import com.qingshan.vo.OrderOverViewVO;
import com.qingshan.vo.SaleOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BundleMapper bundleMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);

        //查询总订单数
        Integer totalOrderCount = orderMapper.countByMap(map);

        map.put("status", Orders.STATUS_COMPLETED);
        //营业额
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByMap(map);

        //订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != null && totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        //平均客单价
        Double unitPrice = 0.0;
        if (validOrderCount != null && validOrderCount != 0) {
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数
        map.remove("status");
        Integer newUsers = userMapper.countByMap(map);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 查询订单管理数据
     *
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));

        //待付款
        map.put("status", Orders.STATUS_PENDING_PAYMENT);
        Integer pendingPaymentOrders = orderMapper.countByMap(map);

        //待发货
        map.put("status", Orders.STATUS_AWAITING_SHIPMENT);
        Integer awaitingShipmentOrders = orderMapper.countByMap(map);

        //已接单待派送
        map.put("status", Orders.STATUS_SHIPPED);
        Integer shippedOrders = orderMapper.countByMap(map);

        //配送中
        map.put("status", Orders.STATUS_IN_TRANSIT);
        Integer inTransitOrders = orderMapper.countByMap(map);

        //已完成
        map.put("status", Orders.STATUS_COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);

        //已取消
        map.put("status", Orders.STATUS_CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);

        //全部订单
        map.remove("status");
        Integer allOrders = orderMapper.countByMap(map);

        return OrderOverViewVO.builder()
                .pendingPaymentOrders(pendingPaymentOrders)
                .awaitingShipmentOrders(awaitingShipmentOrders)
                .shippedOrders(shippedOrders)
                .inTransitOrders(inTransitOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询商品总览
     *
     * @return
     */
    @Override
    public SaleOverViewVO getProductOverView() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", StatusConstant.ENABLE);
        Integer onSale = productMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer offSale = productMapper.countByMap(map);

        return SaleOverViewVO.builder()
                .onSale(onSale)
                .offSale(offSale)
                .build();
    }

    /**
     * 查询套装总览
     *
     * @return
     */
    @Override
    public SaleOverViewVO getBundleOverView() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", StatusConstant.ENABLE);
        Integer onSale = bundleMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer offSale = bundleMapper.countByMap(map);

        return SaleOverViewVO.builder()
                .onSale(onSale)
                .offSale(offSale)
                .build();
    }
}

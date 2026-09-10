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
        //查询总订单数
        Integer totalOrderCount = orderMapper.countByCondition(begin, end, null);

        //营业额
        Double turnover = orderMapper.sumByCondition(begin, end, Orders.STATUS_COMPLETED);
        turnover = turnover == null ? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByCondition(begin, end, Orders.STATUS_COMPLETED);

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
        Integer newUsers = userMapper.countByCondition(begin, end);

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
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

        //待付款
        Integer pendingPaymentOrders = orderMapper.countByCondition(begin, null, Orders.STATUS_PENDING_PAYMENT);

        //待发货
        Integer awaitingShipmentOrders = orderMapper.countByCondition(begin, null, Orders.STATUS_AWAITING_SHIPMENT);

        //已接单待派送
        Integer shippedOrders = orderMapper.countByCondition(begin, null, Orders.STATUS_SHIPPED);

        //配送中
        Integer inTransitOrders = orderMapper.countByCondition(begin, null, Orders.STATUS_IN_TRANSIT);

        //已完成
        Integer completedOrders = orderMapper.countByCondition(begin, null, Orders.STATUS_COMPLETED);

        //已取消
        Integer cancelledOrders = orderMapper.countByCondition(begin, null, Orders.STATUS_CANCELLED);

        //全部订单
        Integer allOrders = orderMapper.countByCondition(begin, null, null);

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
        Integer onSale = productMapper.countByCondition(StatusConstant.ENABLE, null);
        Integer offSale = productMapper.countByCondition(StatusConstant.DISABLE, null);

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
        Integer onSale = bundleMapper.countByCondition(StatusConstant.ENABLE, null);
        Integer offSale = bundleMapper.countByCondition(StatusConstant.DISABLE, null);

        return SaleOverViewVO.builder()
                .onSale(onSale)
                .offSale(offSale)
                .build();
    }
}

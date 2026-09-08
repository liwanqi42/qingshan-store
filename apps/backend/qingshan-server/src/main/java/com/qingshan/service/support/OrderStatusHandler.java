package com.qingshan.service.support;

import com.qingshan.dto.OrdersPageQueryDTO;
import com.qingshan.entity.Orders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 订单状态处理：前端状态映射、查询条件组装、状态判断
 */
@Component
public class OrderStatusHandler {

    /**
     * 小程序端“全部”状态
     */
    private static final int USER_STATUS_ALL = -999;

    /**
     * 退款处理中的状态
     */
    private static final Set<Integer> REFUNDING_STATUSES = new HashSet<>(Arrays.asList(
            Orders.REFUND_STATUS_APPLIED,
            Orders.REFUND_STATUS_WAIT_RETURN,
            Orders.REFUND_STATUS_RETURNED));

    /**
     * 存在退款记录的状态（退款中 + 已退款）
     */
    private static final Set<Integer> REFUND_RECORD_STATUSES = new HashSet<>(Arrays.asList(
            Orders.REFUND_STATUS_APPLIED,
            Orders.REFUND_STATUS_WAIT_RETURN,
            Orders.REFUND_STATUS_RETURNED,
            Orders.REFUND_STATUS_COMPLETED));

    /**
     * 可取消的订单状态
     */
    private static final Set<Integer> CANCELLABLE_STATUSES = new HashSet<>(Arrays.asList(
            Orders.STATUS_PENDING_PAYMENT,
            Orders.STATUS_AWAITING_SHIPMENT,
            Orders.STATUS_SHIPPED,
            Orders.STATUS_IN_TRANSIT));

    /**
     * 小程序端状态 -> 查询条件
     */
    private static final Map<Integer, Consumer<OrdersPageQueryDTO>> USER_STATUS_FILTERS =
            buildUserStatusFilters();

    /**
     * 订单 -> 小程序端状态，按顺序匹配，第一个非空结果即为状态
     */
    private static final List<Function<Orders, Integer>> APP_STATUS_RULES = Arrays.asList(
            orders -> hasRefundStatus(orders, Orders.REFUND_STATUS_APPLIED,
                    Orders.REFUND_STATUS_WAIT_RETURN, Orders.REFUND_STATUS_RETURNED) ? 4 : null,
            orders -> hasRefundStatus(orders, Orders.REFUND_STATUS_COMPLETED) ? 5 : null,
            orders -> Orders.STATUS_REFUND.equals(orders.getStatus())
                    ? (Orders.PAY_STATUS_REFUND.equals(orders.getPayStatus()) ? 5 : 4) : null,
            orders -> Orders.STATUS_CANCELLED.equals(orders.getStatus()) ? -1 : null,
            orders -> Orders.STATUS_COMPLETED.equals(orders.getStatus()) ? 3 : null,
            orders -> Orders.STATUS_SHIPPED.equals(orders.getStatus())
                    || Orders.STATUS_IN_TRANSIT.equals(orders.getStatus()) ? 2 : null,
            orders -> Orders.STATUS_AWAITING_SHIPMENT.equals(orders.getStatus()) ? 1 : null);

    private static Map<Integer, Consumer<OrdersPageQueryDTO>> buildUserStatusFilters() {
        Map<Integer, Consumer<OrdersPageQueryDTO>> filters = new HashMap<>();
        filters.put(0, dto -> dto.setStatuses(toStatusList(Orders.STATUS_PENDING_PAYMENT)));
        filters.put(1, dto -> {
            dto.setStatuses(toStatusList(Orders.STATUS_AWAITING_SHIPMENT));
            dto.setExcludeRefunding(true);
        });
        filters.put(2, dto -> {
            dto.setStatuses(toStatusList(Orders.STATUS_SHIPPED, Orders.STATUS_IN_TRANSIT));
            dto.setExcludeRefunding(true);
        });
        filters.put(3, dto -> {
            dto.setStatuses(toStatusList(Orders.STATUS_COMPLETED));
            dto.setExcludeRefunding(true);
        });
        filters.put(4, dto -> dto.setRefundStatuses(toStatusList(
                Orders.REFUND_STATUS_APPLIED,
                Orders.REFUND_STATUS_WAIT_RETURN,
                Orders.REFUND_STATUS_RETURNED,
                Orders.REFUND_STATUS_COMPLETED)));
        filters.put(5, dto -> dto.setRefundStatuses(toStatusList(Orders.REFUND_STATUS_COMPLETED)));
        return Collections.unmodifiableMap(filters);
    }

    /**
     * 小程序端订单状态 -> 查询条件
     * -999全部(不筛选) 0待付款 1待发货 2待收货 3已完成 4退款/售后 5已退款
     */
    public void applyUserStatusFilter(OrdersPageQueryDTO ordersPageQueryDTO, Integer status) {
        if (status == null || status == USER_STATUS_ALL) {
            return;
        }
        Consumer<OrdersPageQueryDTO> filter = USER_STATUS_FILTERS.get(status);
        if (filter != null) {
            filter.accept(ordersPageQueryDTO);
            return;
        }
        // 兼容直接传后端状态
        ordersPageQueryDTO.setStatuses(toStatusList(status));
    }

    /**
     * 后端订单状态 -> 小程序端订单状态
     */
    public Integer toAppStatus(Orders orders) {
        for (Function<Orders, Integer> rule : APP_STATUS_RULES) {
            Integer status = rule.apply(orders);
            if (status != null) {
                return status;
            }
        }
        return 0;
    }

    /**
     * 退款状态 -> 退款详情页状态 0处理中 1退款成功 2已结束
     */
    public Integer toRefundDetailStatus(Integer refundStatus) {
        if (refundStatus == null) {
            return 2;
        }
        if (REFUNDING_STATUSES.contains(refundStatus)) {
            return 0;
        }
        if (Orders.REFUND_STATUS_COMPLETED.equals(refundStatus)) {
            return 1;
        }
        return 2;
    }

    /**
     * 是否存在退款记录（退款中或已完成）
     */
    public boolean hasRefundRecord(Orders orders) {
        Integer refundStatus = orders.getRefundStatus();
        return refundStatus != null && REFUND_RECORD_STATUSES.contains(refundStatus);
    }

    /**
     * 是否处于退款处理中（尚未退款完成）
     */
    public boolean isRefunding(Orders orders) {
        Integer refundStatus = orders.getRefundStatus();
        return refundStatus != null && REFUNDING_STATUSES.contains(refundStatus);
    }

    /**
     * 是否处于退款中或已退款，这类订单不能再接单、派送、完成
     */
    public boolean isRefundedOrRefunding(Orders orders) {
        return hasRefundRecord(orders);
    }

    /**
     * 可取消的订单状态：待付款、待发货、已发货、配送中
     */
    public boolean isCancellable(Integer status) {
        return CANCELLABLE_STATUSES.contains(status);
    }

    /**
     * 可删除的订单：已完成、已取消、已退款，或退款已完成
     */
    public boolean isDeletable(Orders orders) {
        Integer status = orders.getStatus();
        boolean refundCompleted = Orders.REFUND_STATUS_COMPLETED.equals(orders.getRefundStatus());
        return Orders.STATUS_COMPLETED.equals(status)
                || Orders.STATUS_CANCELLED.equals(status)
                || Orders.STATUS_REFUND.equals(status)
                || refundCompleted;
    }

    /**
     * 存在售后记录时返回退款编号（订单号），否则返回空串
     */
    public String refundNo(Orders orders) {
        return hasRefundRecord(orders) ? orders.getNumber() : "";
    }

    private static boolean hasRefundStatus(Orders orders, Integer... statuses) {
        Integer refundStatus = orders.getRefundStatus();
        if (refundStatus == null) {
            return false;
        }
        for (Integer status : statuses) {
            if (status.equals(refundStatus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 必须用公开的 ArrayList，OGNL 无法反射调用 Collections$SingletonList 等包级私有类的方法
     */
    private static List<Integer> toStatusList(Integer... statuses) {
        List<Integer> list = new ArrayList<>(statuses.length);
        Collections.addAll(list, statuses);
        return list;
    }
}

package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.LogisticsTraceDTO;
import com.qingshan.entity.OrderLogisticsTrace;
import com.qingshan.entity.Orders;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.OrderLogisticsTraceMapper;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.service.LogisticsService;
import com.qingshan.service.support.OrderValidator;
import com.qingshan.vo.LogisticsTraceVO;
import com.qingshan.vo.OrderLogisticsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 手工维护的订单物流轨迹
 */
@Service
public class LogisticsServiceImpl implements LogisticsService {

    private static final int MAX_TEXT_LENGTH = 500;

    private final OrderValidator orderValidator;
    private final OrderMapper orderMapper;
    private final OrderLogisticsTraceMapper traceMapper;

    public LogisticsServiceImpl(OrderValidator orderValidator,
                                OrderMapper orderMapper,
                                OrderLogisticsTraceMapper traceMapper) {
        this.orderValidator = orderValidator;
        this.orderMapper = orderMapper;
        this.traceMapper = traceMapper;
    }

    @Override
    public OrderLogisticsVO getForAdmin(Long orderId) {
        Orders order = orderValidator.requireById(orderId);
        return buildVO(order);
    }

    @Override
    public OrderLogisticsVO getForUser(String orderNumber) {
        Orders order = orderValidator.requireOwnedByNumber(orderNumber, BaseContext.getCurrentId());
        return buildVO(order);
    }

    @Override
    @Transactional
    public Long addTrace(LogisticsTraceDTO traceDTO) {
        if (traceDTO == null) {
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }
        Orders order = orderValidator.requireById(traceDTO.getOrderId());
        if (traceDTO.getTraceTime() == null) {
            throw new BaseException(MessageConstant.LOGISTICS_TIME_REQUIRED);
        }
        String description = trimToNull(traceDTO.getDescription());
        if (description == null) {
            throw new BaseException(MessageConstant.LOGISTICS_DESCRIPTION_REQUIRED);
        }
        String expressCompany = trimToNull(traceDTO.getExpressCompany());
        String trackingNumber = trimToNull(traceDTO.getTrackingNumber());
        if (description.length() > MAX_TEXT_LENGTH
                || (expressCompany != null && expressCompany.length() > 64)
                || (trackingNumber != null && trackingNumber.length() > 64)) {
            throw new BaseException(MessageConstant.LOGISTICS_TEXT_TOO_LONG);
        }
        Integer status = traceDTO.getStatus() == null ? 0 : traceDTO.getStatus();
        if (status < 0 || status > 5) {
            throw new BaseException(MessageConstant.LOGISTICS_STATUS_INVALID);
        }

        OrderLogisticsTrace trace = OrderLogisticsTrace.builder()
                .orderId(order.getId())
                .expressCompany(expressCompany)
                .trackingNumber(trackingNumber)
                .traceTime(traceDTO.getTraceTime())
                .description(description)
                .status(status)
                .createTime(LocalDateTime.now())
                .build();
        traceMapper.insert(trace);

        // 订单头上还没有快递信息时补齐，已有值不覆盖
        if (expressCompany != null || trackingNumber != null) {
            orderMapper.fillLogisticsInfo(order.getId(), expressCompany, trackingNumber);
        }
        return trace.getId();
    }

    @Override
    public void deleteTrace(Long id) {
        OrderLogisticsTrace trace = id == null ? null : traceMapper.getById(id);
        if (trace == null) {
            throw new BaseException(MessageConstant.LOGISTICS_TRACE_NOT_FOUND);
        }
        traceMapper.deleteById(id);
    }

    private OrderLogisticsVO buildVO(Orders order) {
        List<OrderLogisticsTrace> traces = traceMapper.listByOrderId(order.getId());
        String expressCompany = order.getExpressCompany();
        String trackingNumber = order.getTrackingNumber();
        // 订单头缺失时用最新的轨迹补全展示
        for (OrderLogisticsTrace trace : traces) {
            if (!StringUtils.hasText(expressCompany) && StringUtils.hasText(trace.getExpressCompany())) {
                expressCompany = trace.getExpressCompany();
            }
            if (!StringUtils.hasText(trackingNumber) && StringUtils.hasText(trace.getTrackingNumber())) {
                trackingNumber = trace.getTrackingNumber();
            }
            if (StringUtils.hasText(expressCompany) && StringUtils.hasText(trackingNumber)) {
                break;
            }
        }

        List<LogisticsTraceVO> traceVOs = new ArrayList<>(traces.size());
        for (OrderLogisticsTrace trace : traces) {
            traceVOs.add(LogisticsTraceVO.builder()
                    .id(trace.getId())
                    .expressCompany(trace.getExpressCompany())
                    .trackingNumber(trace.getTrackingNumber())
                    .traceTime(trace.getTraceTime())
                    .description(trace.getDescription())
                    .status(trace.getStatus())
                    .createTime(trace.getCreateTime())
                    .build());
        }

        return OrderLogisticsVO.builder()
                .orderId(order.getId())
                .orderNumber(order.getNumber())
                .expressCompany(expressCompany)
                .trackingNumber(trackingNumber)
                .shipTime(order.getShipTime())
                .traces(traceVOs)
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

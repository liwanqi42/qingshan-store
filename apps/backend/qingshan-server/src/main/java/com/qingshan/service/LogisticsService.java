package com.qingshan.service;

import com.qingshan.dto.LogisticsTraceDTO;
import com.qingshan.vo.OrderLogisticsVO;

public interface LogisticsService {

    /**
     * 管理端查询订单物流轨迹
     */
    OrderLogisticsVO getForAdmin(Long orderId);

    /**
     * 用户端查询自己的订单物流轨迹
     */
    OrderLogisticsVO getForUser(String orderNumber);

    /**
     * 新增手工物流轨迹
     * @return 轨迹id
     */
    Long addTrace(LogisticsTraceDTO traceDTO);

    /**
     * 删除物流轨迹
     */
    void deleteTrace(Long id);
}

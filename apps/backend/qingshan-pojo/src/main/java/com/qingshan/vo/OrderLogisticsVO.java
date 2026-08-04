package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单物流信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogisticsVO implements Serializable {

    private Long orderId;

    //订单号
    private String orderNumber;

    //快递公司
    private String expressCompany;

    //物流单号
    private String trackingNumber;

    //发货时间
    private LocalDateTime shipTime;

    //物流轨迹
    private List<LogisticsTraceVO> traces;
}

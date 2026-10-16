package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流轨迹
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTraceVO implements Serializable {

    private Long id;

    //快递公司
    private String expressCompany;

    //物流单号
    private String trackingNumber;

    //轨迹时间
    private LocalDateTime traceTime;

    //物流描述
    private String description;

    //物流状态 0暂无轨迹 1已揽收 2运输中 3配送中 4已签收 5异常
    private Integer status;

    //录入时间
    private LocalDateTime createTime;
}

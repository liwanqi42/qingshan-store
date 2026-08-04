package com.qingshan.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderStatisticsVO implements Serializable {
    //待发货数量
    private Integer pendingShipment;

    //已发货数量
    private Integer shipped;

    //配送中数量
    private Integer inTransit;
}

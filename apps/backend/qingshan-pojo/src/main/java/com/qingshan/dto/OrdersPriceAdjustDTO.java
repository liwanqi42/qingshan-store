package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单改价 DTO
 */
@Data
public class OrdersPriceAdjustDTO implements Serializable {

    private Long id;

    //调整后订单金额
    private BigDecimal amount;

    //改价原因
    private String adjustReason;
}

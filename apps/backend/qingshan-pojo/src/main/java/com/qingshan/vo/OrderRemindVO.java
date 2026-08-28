package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 个人中心订单提醒数量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRemindVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待付款数量 */
    private Integer waitPay;

    /** 待发货数量 */
    private Integer notExpress;

    /** 待收货数量 */
    private Integer waitReceive;
}

package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 售后申请结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundApplyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 退款编号，当前直接使用订单号 */
    private String refundNo;
}

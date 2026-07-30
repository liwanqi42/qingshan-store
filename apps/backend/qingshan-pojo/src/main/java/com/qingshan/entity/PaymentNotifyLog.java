package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付/退款回调日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //第三方通知id，用于幂等
    private String notifyId;

    //订单号
    private String orderNumber;

    //通知类型 1支付 2退款
    private Integer notifyType;

    //通知原始报文
    private String requestBody;

    //处理状态 0待处理 1成功 2失败
    private Integer processStatus;

    //失败原因
    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

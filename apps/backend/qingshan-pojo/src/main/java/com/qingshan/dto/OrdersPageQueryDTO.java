package com.qingshan.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdersPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    private String number;

    private  String phone;

    private Integer status;

    private List<Integer> statuses;

    //订单号或商品名称关键词
    private String keyword;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long userId;

    //退款状态 0无退款 1退款申请中 2待买家退货 3买家已退货待商家收货 4退款完成 5退款申请被拒绝 6退款关闭
    private Integer refundStatus;

    //退款状态筛选（包含）
    private List<Integer> refundStatuses;

    //是否排除退款中/已退款的订单，用于待发货、待收货、已完成等常规标签
    private boolean excludeRefunding;

}

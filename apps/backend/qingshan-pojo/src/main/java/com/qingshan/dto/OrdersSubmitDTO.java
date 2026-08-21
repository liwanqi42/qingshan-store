package com.qingshan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdersSubmitDTO implements Serializable {
    //地址簿id
    private Long addressBookId;
    //付款方式 1微信 2支付宝 3银行卡
    private int payMethod;
    //备注
    private String remark;
    //预计发货时间
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedShipTime;
    //发货状态 1立即发货 0预约发货
    private Integer shippingStatus;
    //运费
    private BigDecimal freightAmount;
    //发票类型：0 不开发票，1 电子普通发票，2 增值税专用发票
    private Integer invoiceType;
    //发票抬头
    private String invoiceTitle;
    //总金额
    private BigDecimal amount;
    //下单商品明细，直接购买时传入；为空时按购物车下单
    private List<OrdersSubmitItemDTO> items;
}

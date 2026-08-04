package com.qingshan.vo;

import com.qingshan.entity.AddressBook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单结算预览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewVO implements Serializable {

    //收货地址
    private AddressBook address;

    //商品明细
    private List<OrderPreviewItemVO> items;

    //商品总额
    private BigDecimal totalAmount;

    //运费
    private BigDecimal freightAmount;

    //优惠金额
    private BigDecimal discountAmount;

    //应付金额
    private BigDecimal payableAmount;

    //库存是否充足
    private Boolean stockEnough;
}

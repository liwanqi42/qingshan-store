package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品/套装总览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleOverViewVO implements Serializable {

    // 已上架数量
    private Integer onSale;

    // 已下架数量
    private Integer offSale;
}

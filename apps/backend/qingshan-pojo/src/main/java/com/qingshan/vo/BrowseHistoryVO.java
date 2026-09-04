package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浏览历史记录（含商品信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //浏览记录id
    private Long id;

    //商品id
    private Long goodsId;

    //商品名称
    private String title;

    //商品图片
    private String picture;

    //商品价格
    private BigDecimal price;

    //最近浏览时间
    private LocalDateTime browseTime;
}

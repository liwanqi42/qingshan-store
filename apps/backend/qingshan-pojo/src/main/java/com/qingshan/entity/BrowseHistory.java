package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品浏览历史
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户id
    private Long userId;

    //商品id
    private Long productId;

    //最近浏览时间
    private LocalDateTime browseTime;
}

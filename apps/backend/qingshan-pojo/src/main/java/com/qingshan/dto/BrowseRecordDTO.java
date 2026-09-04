package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 记录商品浏览
 */
@Data
public class BrowseRecordDTO implements Serializable {

    //商品id
    private Long productId;
}

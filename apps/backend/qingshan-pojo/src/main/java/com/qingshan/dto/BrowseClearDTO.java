package com.qingshan.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除浏览历史
 */
@Data
public class BrowseClearDTO implements Serializable {

    //要删除的浏览记录id列表
    private List<Long> ids;
}

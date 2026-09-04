package com.qingshan.service;

import com.qingshan.result.PageResult;

import java.util.List;

public interface BrowseHistoryService {

    /**
     * 记录当前用户浏览的商品
     * @param productId 商品id
     */
    void record(Long productId);

    /**
     * 分页查询当前用户的浏览历史
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    PageResult pageQuery(int page, int size);

    /**
     * 批量删除当前用户的浏览历史
     * @param ids 浏览记录id列表
     */
    void clear(List<Long> ids);
}

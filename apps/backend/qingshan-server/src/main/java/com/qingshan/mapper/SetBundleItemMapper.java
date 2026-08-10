package com.qingshan.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetBundleItemMapper {

    /**
     * 根据商品id查询对应的捆绑包id
     * @param productIds
     * @return
     */

    List<Long> getBundleIdsByProductIds(List<Long> productIds);
}

package com.qingshan.mapper;

import com.qingshan.entity.BrowseHistory;
import com.qingshan.vo.BrowseHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BrowseHistoryMapper {

    /**
     * 记录浏览历史，用户已浏览过该商品时刷新浏览时间
     * @param browseHistory 浏览记录：用户id、商品id、浏览时间
     */
    void record(BrowseHistory browseHistory);

    /**
     * 分页查询当前用户的浏览历史（含商品信息）
     * @param userId 用户id
     * @return 浏览历史列表，按浏览时间倒序
     */
    List<BrowseHistoryVO> pageQuery(@Param("userId") Long userId);

    /**
     * 批量删除当前用户的浏览历史
     * @param ids 浏览记录id列表
     * @param userId 用户id，避免删除他人记录
     */
    void deleteByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}

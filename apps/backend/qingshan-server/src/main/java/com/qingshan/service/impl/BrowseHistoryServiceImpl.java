package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.entity.BrowseHistory;
import com.qingshan.entity.Product;
import com.qingshan.exception.UserNotLoginException;
import com.qingshan.mapper.BrowseHistoryMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.BrowseHistoryService;
import com.qingshan.utils.PageUtil;
import com.qingshan.vo.BrowseHistoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 记录浏览历史：商品不存在时静默忽略，避免影响详情页展示
     * @param productId 商品id
     */
    @Override
    public void record(Long productId) {
        if (productId == null) {
            return;
        }
        Product product = productMapper.getById(productId);
        if (product == null) {
            return;
        }
        browseHistoryMapper.record(BrowseHistory.builder()
                .userId(getCurrentUserId())
                .productId(productId)
                .browseTime(LocalDateTime.now())
                .build());
    }

    /**
     * 分页查询浏览历史
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery(int page, int size) {
        Long userId = getCurrentUserId();
        try (Page<BrowseHistoryVO> pageResult = PageHelper.startPage(PageUtil.pageNum(page), PageUtil.pageSize(size))) {
            List<BrowseHistoryVO> records = browseHistoryMapper.pageQuery(userId);
            return new PageResult(pageResult.getTotal(), records);
        }
    }

    /**
     * 批量删除浏览历史，仅能删除当前用户的记录
     * @param ids 浏览记录id列表
     */
    @Override
    public void clear(List<Long> ids) {
        Long userId = getCurrentUserId();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        browseHistoryMapper.deleteByIds(ids, userId);
    }

    /**
     * 获取当前登录用户id
     * @return 用户id
     */
    private Long getCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }
}

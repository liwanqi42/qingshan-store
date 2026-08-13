package com.qingshan.service;

import com.qingshan.dto.BundleDTO;
import com.qingshan.dto.BundlePageQueryDTO;
import com.qingshan.result.PageResult;
import com.qingshan.vo.BundleVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface BundleService {
    /**
     * 新增捆绑包
     * @param bundleDTO
     */
    void save(BundleDTO bundleDTO);

    /**
     * 分页查询
     * @param bundlePageQueryDTO
     * @return
     */
    PageResult pageQuery(BundlePageQueryDTO bundlePageQueryDTO);

    /**
     * 批量删除捆绑包
     * @param ids
     */
    void deleteBatch(List<Long> ids);


    /**
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
     */
    BundleVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     * @param bundleDTO
     */
    void update(BundleDTO bundleDTO);

    /**
     *
     * 捆绑包起售停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}

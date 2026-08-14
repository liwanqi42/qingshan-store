package com.qingshan.service;

import com.qingshan.dto.BundleDTO;
import com.qingshan.dto.BundlePageQueryDTO;
import com.qingshan.entity.Bundle;
import com.qingshan.result.PageResult;
import com.qingshan.vo.BundleVO;
import com.qingshan.vo.ProductItemVO;

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
     * 根据id查询套装和关联的商品数据
     * @param id
     * @return
     */
    BundleVO getByIdWithItems(Long id);

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

    /**
     * 条件查询套装
     * @param bundle 查询条件
     * @return 套装列表
     */
    List<Bundle> list(Bundle bundle);

    /**
     * 根据套装id查询包含的商品
     * @param id 套装id
     * @return 套装内商品列表
     */
    List<ProductItemVO> getProductItemByBundleId(Long id);
}

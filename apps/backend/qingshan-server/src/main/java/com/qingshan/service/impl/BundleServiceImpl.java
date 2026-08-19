package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.BundleDTO;
import com.qingshan.dto.BundlePageQueryDTO;
import com.qingshan.entity.Bundle;
import com.qingshan.entity.BundleItem;
import com.qingshan.entity.Product;
import com.qingshan.exception.BundleEnableFailedException;
import com.qingshan.exception.DeletionNotAllowedException;
import com.qingshan.mapper.BundleItemMapper;
import com.qingshan.mapper.BundleMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.BundleService;
import com.qingshan.vo.BundleVO;
import com.qingshan.vo.ProductItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BundleServiceImpl implements BundleService {

    @Autowired
    private BundleMapper bundleMapper;

    @Autowired
    private BundleItemMapper bundleItemMapper;

    @Override
    @Transactional
    public void save(BundleDTO bundleDTO) {
        Bundle bundle = new Bundle();

        BeanUtils.copyProperties(bundleDTO, bundle);

        bundleMapper.insert(bundle);

        Long bundleId = bundle.getId();

        List<BundleItem> bundleItems = bundleDTO.getBundleItems();
        if (bundleItems != null && !bundleItems.isEmpty()) {
            bundleItems.forEach(bundleItem -> bundleItem.setBundleId(bundleId));

            bundleItemMapper.insertBatch(bundleItems);
        }
    }

    @Override
    public PageResult pageQuery(BundlePageQueryDTO bundlePageQueryDTO) {
        try (Page<BundleVO> page = PageHelper.startPage(bundlePageQueryDTO.getPage(), bundlePageQueryDTO.getPageSize())) {
            List<BundleVO> records = bundleMapper.pageQuery(bundlePageQueryDTO);
            return new PageResult(page.getTotal(), records);
        }
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id ->{
            Bundle bundle = bundleMapper.getById(id);
            if (StatusConstant.ENABLE.equals(bundle.getStatus())){
                throw new DeletionNotAllowedException(MessageConstant.BUNDLE_ON_SALE);
            }
        });

        ids.forEach(bundleId -> {
            bundleMapper.deleteById(bundleId);
            bundleItemMapper.deleteByBundleId(bundleId);
        });
    }

    @Override
    public BundleVO getByIdWithItems(Long id) {
        Bundle bundle = bundleMapper.getById(id);
        List<BundleItem> bundleItems = bundleItemMapper.getByBundleId(id);

        BundleVO bundleVO = new BundleVO();
        BeanUtils.copyProperties(bundle, bundleVO);
        bundleVO.setBundleItems(bundleItems);

        return bundleVO;
    }

    @Override
    @Transactional
    public void update(BundleDTO bundleDTO) {
        Bundle bundle = new Bundle();
        BeanUtils.copyProperties(bundleDTO, bundle);

        bundleMapper.update(bundle);

        Long bundleId = bundleDTO.getId();

        bundleItemMapper.deleteByBundleId(bundleId);

        List<BundleItem> bundleItems = bundleDTO.getBundleItems();
        if (bundleItems != null && !bundleItems.isEmpty()) {
            bundleItems.forEach(bundleItem -> bundleItem.setBundleId(bundleId));

            bundleItemMapper.insertBatch(bundleItems);
        }

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        if(status.equals(StatusConstant.ENABLE)){
            List<Product> productList = bundleMapper.getByBundleId(id);
            if (productList != null && ! productList.isEmpty()){
                productList.forEach(product -> {
                    if (StatusConstant.DISABLE.equals(product.getStatus())){
                        throw new BundleEnableFailedException(MessageConstant.BUNDLE_ENABLE_FAILED);
                    }
                });
            }
        }

        Bundle bundle = Bundle.builder()
                .id(id)
                .status(status)
                .build();
        bundleMapper.update(bundle);
    }

    @Override
    public List<Bundle> list(Bundle bundle) {
        return bundleMapper.list(bundle);
    }

    @Override
    public List<ProductItemVO> getProductItemByBundleId(Long id) {
        return bundleItemMapper.getProductItemByBundleId(id);
    }
}

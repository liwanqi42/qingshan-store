package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.ProductDTO;
import com.qingshan.dto.ProductPageQueryDTO;
import com.qingshan.entity.Bundle;
import com.qingshan.entity.Product;
import com.qingshan.entity.ProductAttr;
import com.qingshan.exception.DeletionNotAllowedException;
import com.qingshan.mapper.ProductAttrsMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.SetBundleItemMapper;
import com.qingshan.mapper.setBundleMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.ProductService;
import com.qingshan.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductAttrsMapper productAttrsMapper;

    @Autowired
    private SetBundleItemMapper setBundleItemMapper;

    @Autowired
    private setBundleMapper setBundleMapper;
    /**
     * 新增商品及样式
     * @param productDTO
     */
    @Override
    @Transactional
    public void saveWithAttr(ProductDTO productDTO) {

        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        //向商品表插入数据
        productMapper.insert(product);

        Long productId = product.getId();

        //向样式表插入n条数据
        List<ProductAttr> attrs = productDTO.getAttrs();
        if (attrs != null && attrs.size()>0){
            attrs.forEach(productAttr -> {
                productAttr.setProductId(productId);
            });
            productAttrsMapper.insertBatch(attrs);
        }
    }

    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        PageHelper.startPage(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize());
        Page<ProductVO> page = productMapper.pageQuery(productPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 商品的批量删除
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Product product = productMapper.getById(id);
            if (product.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.PRODUCT_ON_SALE);
            }
        }

        //是否被捆绑包关联
        List<Long> setBundleIds = setBundleItemMapper.getBundleIdsByProductIds(ids);
        if (setBundleIds != null && setBundleIds.size() >0){
            throw new DeletionNotAllowedException((MessageConstant.PRODUCT_BE_RELATED_BY_BUNDLE));
        }

//        for (Long id : ids) {
//            productMapper.deleteById(id);
//            productAttrsMapper.deleteByProductId(id);
//        }

        productMapper.deleteByProductIds(ids);

        productAttrsMapper.deleteByProductAttrIds(ids);


    }

    /**
     * 根据id查询商品样式
     * @param id
     * @return
     */
    @Override
    public ProductVO getByidWithAttr(Long id) {
        Product product = productMapper.getById(id);
        List<ProductAttr> productAttrs = productAttrsMapper.getById(id);

        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        productVO.setAttrs(productAttrs);

        //TODO 下面前端上线后必须删除
        //兼容管理端前端回显：前端读取的是 flavors 字段，结构为 {name, value}
        List<Map<String, String>> flavors = new ArrayList<>();
        if (productAttrs != null) {
            for (ProductAttr attr : productAttrs) {
                Map<String, String> flavor = new LinkedHashMap<>();
                flavor.put("name", attr.getName());
                flavor.put("value", attr.getAttrValues());
                flavors.add(flavor);
            }
        }
        productVO.setFlavors(flavors);
        //删除到这

        return productVO;
    }

    /**
     * 修改商品信息
     * @param productDTO
     */
    @Override
    public void updateWithAtrr(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO,product);

        productMapper.update(product);

        productAttrsMapper.deleteByProductId(productDTO.getId());

        List<ProductAttr> attrs = productDTO.getAttrs();
        if (attrs != null && attrs.size()>0){
            attrs.forEach(productAttr -> {
                productAttr.setProductId(productDTO.getId());
            });
            productAttrsMapper.insertBatch(attrs);
        }

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Product product = Product.builder()
                .id(id)
                .status(status)
                .build();
        productMapper.update(product);

        if (status == StatusConstant.DISABLE) {
            List<Long> productIds = new ArrayList<>();
            productIds.add(id);
            List<Long> bundleIdsByProductIds = setBundleItemMapper.getBundleIdsByProductIds(productIds);
            if (bundleIdsByProductIds != null && bundleIdsByProductIds.size() >0){
                for (Long bundleIdsByProductId : bundleIdsByProductIds) {
                    Bundle bundle = Bundle.builder()
                            .id(bundleIdsByProductId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setBundleMapper.update(bundle);
                }
            }
        }
    }
}

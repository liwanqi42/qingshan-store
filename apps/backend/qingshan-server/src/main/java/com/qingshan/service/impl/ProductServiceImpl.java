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
import java.util.List;

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
     * @param productDTO 商品信息及样式
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
        if (attrs != null && !attrs.isEmpty()){
            attrs.forEach(productAttr -> productAttr.setProductId(productId));
            productAttrsMapper.insertBatch(attrs);
        }
    }

    /**
     * 商品分页查询
     * @param productPageQueryDTO 分页查询参数
     * @return 分页查询结果
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        try (Page<ProductVO> page = PageHelper.startPage(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize())) {
            List<ProductVO> records = productMapper.pageQuery(productPageQueryDTO);
            return new PageResult(page.getTotal(), records);
        }
    }

    /**
     * 商品的批量删除
     * @param ids 商品id列表
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Product product = productMapper.getById(id);
            if (StatusConstant.ENABLE.equals(product.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.PRODUCT_ON_SALE);
            }
        }

        //是否被捆绑包关联
        List<Long> setBundleIds = setBundleItemMapper.getBundleIdsByProductIds(ids);
        if (setBundleIds != null && !setBundleIds.isEmpty()){
            throw new DeletionNotAllowedException((MessageConstant.PRODUCT_BE_RELATED_BY_BUNDLE));
        }


        productMapper.deleteByProductIds(ids);

        productAttrsMapper.deleteByProductAttrIds(ids);


    }

    /**
     * 根据id查询商品样式
     * @param id 商品id
     * @return 商品信息及样式
     */
    @Override
    public ProductVO getByidWithAttr(Long id) {
        Product product = productMapper.getById(id);
        List<ProductAttr> productAttrs = productAttrsMapper.getById(id);

        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        productVO.setAttrs(productAttrs);

        return productVO;
    }

    /**
     * 修改商品信息
     * @param productDTO 商品信息及样式
     */
    @Override
    public void updateWithAtrr(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO,product);

        productMapper.update(product);

        productAttrsMapper.deleteByProductId(productDTO.getId());

        List<ProductAttr> attrs = productDTO.getAttrs();
        if (attrs != null && !attrs.isEmpty()){
            attrs.forEach(productAttr -> productAttr.setProductId(productDTO.getId()));
            productAttrsMapper.insertBatch(attrs);
        }

    }

    /**
     * 商品起售停售
     * @param status 商品状态，0停售 1起售
     * @param id 商品id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Product product = Product.builder()
                .id(id)
                .status(status)
                .build();
        productMapper.update(product);

        if (StatusConstant.DISABLE.equals(status)) {
            List<Long> productIds = new ArrayList<>();
            productIds.add(id);
            List<Long> bundleIdsByProductIds = setBundleItemMapper.getBundleIdsByProductIds(productIds);
            if (bundleIdsByProductIds != null && !bundleIdsByProductIds.isEmpty()){
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

    /**
     * 根据分类id查询菜品
     * @param categoryId 分类id
     * @return 商品列表
     */
    @Override
    public List<Product> list(Long categoryId) {
        Product product = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return productMapper.list(product);
    }
}

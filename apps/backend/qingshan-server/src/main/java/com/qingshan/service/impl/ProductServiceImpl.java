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
import com.qingshan.entity.ProductImage;
import com.qingshan.entity.ProductSku;
import com.qingshan.exception.DeletionNotAllowedException;
import com.qingshan.exception.ProductStyleBusinessException;
import com.qingshan.mapper.ProductAttrsMapper;
import com.qingshan.mapper.ProductImageMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.ProductSkuMapper;
import com.qingshan.mapper.SetBundleItemMapper;
import com.qingshan.mapper.setBundleMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.ProductService;
import com.qingshan.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductAttrsMapper productAttrsMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

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

        //未单独指定封面时，使用样式图中的第一张作为封面
        List<String> images = normalizeStyleImages(productDTO);
        List<String> detailImages = normalizeImageList(productDTO.getDetailImages());
        if (isBlank(product.getImage()) && !images.isEmpty()) {
            product.setImage(images.get(0));
        }

        //有SKU时，商品总库存取各SKU库存之和
        List<ProductSku> skuList = normalizeSkus(productDTO.getSkuList(), productDTO.getPrice());
        if (skuList != null && !skuList.isEmpty()) {
            product.setStock(sumStock(skuList));
        }

        //向商品表插入数据
        productMapper.insert(product);

        Long productId = product.getId();

        //分别保存样式图和详情长图
        saveImages(productId, images, ProductImage.TYPE_STYLE);
        saveImages(productId, detailImages, ProductImage.TYPE_DETAIL);

        //向样式表插入n条数据
        List<ProductAttr> attrs = productDTO.getAttrs();
        if (attrs != null && !attrs.isEmpty()){
            attrs.forEach(productAttr -> productAttr.setProductId(productId));
            productAttrsMapper.insertBatch(attrs);
        }

        //保存每个规格组合的SKU（独立价格与库存）
        saveSkus(productId, skuList);
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
    @Transactional
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

        productImageMapper.deleteByProductIds(ids);

        for (Long id : ids) {
            productSkuMapper.deleteByProductId(id);
        }

    }

    /**
     * 根据id查询商品样式
     * @param id 商品id
     * @return 商品信息及样式
     */
    @Override
    public ProductVO getByIdWithAttr(Long id) {
        Product product = productMapper.getById(id);
        List<ProductAttr> productAttrs = productAttrsMapper.getById(id);

        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        productVO.setImage(trimToNull(product.getImage()));
        productVO.setAttrs(productAttrs);
        //每个规格组合的独立库存，前端据此展示和限制各样式可购买数量
        productVO.setSkuList(productSkuMapper.listByProductId(id));

        //样式图列表，兼容历史数据（仅有封面图时也作为一张轮播图）
        List<String> images = getImageUrls(id, ProductImage.TYPE_STYLE);
        if (images.isEmpty() && !isBlank(product.getImage())) {
            images.add(product.getImage().trim());
        }
        productVO.setImages(images);
        //详情长图列表
        productVO.setDetailImages(getImageUrls(id, ProductImage.TYPE_DETAIL));

        return productVO;
    }

    /**
     * 修改商品信息
     * @param productDTO 商品信息及样式
     */
    @Override
    @Transactional
    public void updateWithAtrr(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO,product);

        List<String> images = normalizeStyleImages(productDTO);
        List<String> detailImages = normalizeImageList(productDTO.getDetailImages());
        if (isBlank(product.getImage()) && !images.isEmpty()) {
            product.setImage(images.get(0));
        }

        //SKU整体替换；未传skuList时保留原有SKU
        List<ProductSku> skuList = normalizeSkus(productDTO.getSkuList(), productDTO.getPrice());
        if (skuList != null && !skuList.isEmpty()) {
            product.setStock(sumStock(skuList));
        }

        productMapper.update(product);

        productAttrsMapper.deleteByProductId(productDTO.getId());

        List<ProductAttr> attrs = productDTO.getAttrs();
        if (attrs != null && !attrs.isEmpty()){
            attrs.forEach(productAttr -> productAttr.setProductId(productDTO.getId()));
            productAttrsMapper.insertBatch(attrs);
        }

        if (skuList != null) {
            productSkuMapper.deleteByProductId(productDTO.getId());
            saveSkus(productDTO.getId(), skuList);
        }

        //样式图整体替换；未传图片相关字段时保留原样式图
        if (productDTO.isImageProvided() || productDTO.isImagesProvided()) {
            productImageMapper.deleteByProductIdAndType(productDTO.getId(), ProductImage.TYPE_STYLE);
            saveImages(productDTO.getId(), images, ProductImage.TYPE_STYLE);
        }

        //详情长图整体替换；未传该字段时保留原详情长图
        if (productDTO.isDetailImagesProvided()) {
            productImageMapper.deleteByProductIdAndType(productDTO.getId(), ProductImage.TYPE_DETAIL);
            saveImages(productDTO.getId(), detailImages, ProductImage.TYPE_DETAIL);
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
     * 根据分类id查询商品
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

    /**
     * 条件查询商品和规格属性
     * @param product 查询条件
     * @return 商品列表
     */
    @Override
    public List<ProductVO> listWithAttr(Product product) {
        List<Product> productList = productMapper.list(product);

        List<ProductVO> productVOList = new ArrayList<>();

        for (Product p : productList) {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(p, productVO);

            //根据商品id查询对应的规格属性
            List<ProductAttr> attrs = productAttrsMapper.getById(p.getId());

            productVO.setAttrs(attrs);
            productVOList.add(productVO);
        }

        return productVOList;
    }

    /**
     * 校验并补全SKU默认值
     * @param skuList 前端提交的SKU列表，null表示修改时未传该字段
     * @param defaultPrice 商品价格，SKU未单独定价时使用
     * @return 处理后的SKU列表
     */
    private List<ProductSku> normalizeSkus(List<ProductSku> skuList, BigDecimal defaultPrice) {
        if (skuList == null) {
            return null;
        }
        for (ProductSku sku : skuList) {
            if (sku == null) {
                throw new ProductStyleBusinessException("SKU信息不能为空");
            }
            //无规格商品也允许有一个默认SKU，此时属性为空对象
            if (!StringUtils.hasText(sku.getAttrs())) {
                sku.setAttrsValue("{}");
            }
            if (sku.getPrice() == null) {
                sku.setPrice(defaultPrice);
            }
            if (sku.getPrice() == null) {
                throw new ProductStyleBusinessException("SKU价格不能为空");
            }
            if (sku.getStock() == null) {
                sku.setStock(0);
            }
            if (sku.getStatus() == null) {
                sku.setStatus(StatusConstant.ENABLE);
            }
            if (!StringUtils.hasText(sku.getSkuCode())) {
                sku.setSkuCode(null);
            }
        }
        return skuList;
    }

    /**
     * 保存商品SKU
     * @param productId 商品id
     * @param skuList SKU列表
     */
    private void saveSkus(Long productId, List<ProductSku> skuList) {
        if (skuList == null || skuList.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProductSku sku : skuList) {
            sku.setId(null);
            sku.setProductId(productId);
            sku.setCreateTime(now);
            sku.setUpdateTime(now);
            productSkuMapper.insert(sku);
        }
    }

    /**
     * 商品总库存 = 启用状态SKU库存之和
     * @param skuList SKU列表
     * @return 总库存
     */
    private int sumStock(List<ProductSku> skuList) {
        int total = 0;
        for (ProductSku sku : skuList) {
            if (StatusConstant.ENABLE.equals(sku.getStatus()) && sku.getStock() != null) {
                total += sku.getStock();
            }
        }
        return total;
    }

    /**
     * 保存商品图片
     * @param productId 商品id
     * @param images 图片地址列表
     */
    private void saveImages(Long productId, List<String> images, Integer type) {
        if (images == null || images.isEmpty()) {
            return;
        }
        List<ProductImage> productImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            productImages.add(ProductImage.builder()
                    .productId(productId)
                    .url(images.get(i))
                    .type(type)
                    .sort(i)
                    .build());
        }
        productImageMapper.insertBatch(productImages);
    }

    /**
     * 查询指定类型的商品图片地址
     * @param productId 商品id
     * @param type 图片类型 1样式图 2详情长图
     * @return 图片地址列表
     */
    private List<String> getImageUrls(Long productId, Integer type) {
        List<String> images = new ArrayList<>();
        List<ProductImage> productImages = productImageMapper.getByProductIdAndType(productId, type);
        if (productImages != null) {
            productImages.forEach(productImage -> images.add(productImage.getUrl()));
        }
        return images;
    }

    /**
     * 整理样式图列表：去空、去重，封面图优先
     * @param productDTO 商品信息
     * @return 图片地址列表
     */
    private List<String> normalizeStyleImages(ProductDTO productDTO) {
        Set<String> imageSet = new LinkedHashSet<>();
        String cover = trimToNull(productDTO.getImage());
        if (cover != null) {
            imageSet.add(cover);
        }
        if (productDTO.getImages() != null) {
            for (String image : productDTO.getImages()) {
                String url = trimToNull(image);
                if (url != null) {
                    imageSet.add(url);
                }
            }
        }
        return new ArrayList<>(imageSet);
    }

    /**
     * 整理图片地址列表：去空、去重
     * @param images 图片地址列表
     * @return 图片地址列表
     */
    private List<String> normalizeImageList(List<String> images) {
        Set<String> imageSet = new LinkedHashSet<>();
        if (images != null) {
            for (String image : images) {
                String url = trimToNull(image);
                if (url != null) {
                    imageSet.add(url);
                }
            }
        }
        return new ArrayList<>(imageSet);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

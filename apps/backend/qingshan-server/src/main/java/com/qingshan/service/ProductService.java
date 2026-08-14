package com.qingshan.service;

import com.qingshan.dto.ProductDTO;
import com.qingshan.dto.ProductPageQueryDTO;
import com.qingshan.entity.Product;
import com.qingshan.result.PageResult;
import com.qingshan.vo.ProductVO;

import java.util.List;

public interface ProductService {

    void saveWithAttr(ProductDTO productDTO);

    PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO);

    void deleteBatch(List<Long> ids);

    ProductVO getByidWithAttr(Long id);

    void updateWithAtrr(ProductDTO productDTO);

    void startOrStop(Integer status, Long id);

    List<Product> list(Long categoryId);

    List<ProductVO> listWithAttr(Product product);
}

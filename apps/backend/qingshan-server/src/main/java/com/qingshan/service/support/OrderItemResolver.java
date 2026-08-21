package com.qingshan.service.support;

import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.entity.Bundle;
import com.qingshan.entity.Product;
import com.qingshan.entity.ProductSku;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.mapper.BundleMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.ProductSkuMapper;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.service.ProductStyleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 商品、套装和 SKU 的解析与校验
 * 购物车加购和下单共用，保证价格、规格、库存口径一致
 */
@Service
public class OrderItemResolver {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private BundleMapper bundleMapper;

    @Autowired
    private ProductStyleService productStyleService;

    /**
     * 解析并校验待下单商品
     * @param productId 商品id
     * @param bundleId 套装id
     * @param skuId SKU id，可为空
     * @param productStyle 前端提交的已选属性JSON
     * @param number 数量
     * @return 以服务端数据为准的商品信息
     */
    public ResolvedOrderItem resolve(Long productId, Long bundleId, Long skuId,
                                     String productStyle, Integer number) {
        //商品和套装必须二选一
        if ((productId == null) == (bundleId == null)) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_BUNDLE_INCOMPLETE);
        }

        //未传数量时默认1件，小于1按1件处理，超过上限直接拦截
        int buyNumber = number == null ? 1 : number;
        if (buyNumber < 1) {
            buyNumber = 1;
        }
        if (buyNumber > NumConstant.PRODUCT_MAX_BUY) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_BUY_LIMIT);
        }

        //前端没有真实SKU时会传0，统一按“无SKU”处理
        Long resolvedSkuId = skuId != null && skuId > 0 ? skuId : null;

        ResolvedOrderItem.ResolvedOrderItemBuilder builder = ResolvedOrderItem.builder()
                .productId(productId)
                .bundleId(bundleId)
                .skuId(resolvedSkuId)
                .number(buyNumber);

        BigDecimal price;
        if (productId != null) {
            Product product = productMapper.getById(productId);
            if (product == null) {
                throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
            }
            if (!StatusConstant.ENABLE.equals(product.getStatus())) {
                throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_OFF_SHELF);
            }
            builder.name(product.getName())
                    .image(product.getImage())
                    .stock(product.getStock());
            price = product.getPrice();

            //有SKU时价格、图片、库存和已选属性都以SKU为准
            ProductSku sku = null;
            if (resolvedSkuId != null) {
                sku = productSkuMapper.getById(resolvedSkuId);
                if (sku == null || !Objects.equals(sku.getProductId(), productId)) {
                    throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
                }
                if (StatusConstant.DISABLE.equals(sku.getStatus())) {
                    throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_STYLE_OFF_SHELF);
                }
                if (sku.getPrice() != null) {
                    price = sku.getPrice();
                }
                if (sku.getStock() != null) {
                    builder.stock(sku.getStock());
                }
                if (StringUtils.hasText(sku.getImage())) {
                    builder.image(sku.getImage());
                }
            }
            builder.productStyle(productStyleService.resolve(productId, sku, productStyle));
        } else {
            //套装没有SKU，名称、图片、价格取套装本身的信息
            Bundle bundle = bundleMapper.getById(bundleId);
            if (bundle == null) {
                throw new ShoppingCartBusinessException(MessageConstant.BUNDLE_NOT_FOUND);
            }
            if (!StatusConstant.ENABLE.equals(bundle.getStatus())) {
                throw new ShoppingCartBusinessException(MessageConstant.BUNDLE_OFF_SHELF);
            }
            builder.name(bundle.getName())
                    .image(bundle.getImage())
                    .stock(null);
            price = bundle.getPrice();
            //套装规格暂不支持属性校验，沿用前端提交的已选文案
            builder.productStyle(StringUtils.hasText(productStyle) ? productStyle.trim() : null);
        }

        if (price == null) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_PRICE);
        }

        return builder.price(price)
                .amount(price.multiply(BigDecimal.valueOf(buyNumber))
                        .setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP))
                .build();
    }
}

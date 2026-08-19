package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.ShoppingCartDTO;
import com.qingshan.dto.ShoppingCartEditDTO;
import com.qingshan.entity.Bundle;
import com.qingshan.entity.Product;
import com.qingshan.entity.ProductSku;
import com.qingshan.entity.ShoppingCart;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.exception.UserNotLoginException;
import com.qingshan.mapper.BundleMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.ProductSkuMapper;
import com.qingshan.mapper.ShoppingCartMapper;
import com.qingshan.service.ProductStyleService;
import com.qingshan.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private static final int MAX_NUMBER = 999;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private BundleMapper bundleMapper;

    @Autowired
    private ProductStyleService productStyleService;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO 加购参数：商品/套装、SKU、已选属性、数量
     */
    @Override
    @Transactional
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 1、获取当前登录用户，未登录不能加购
        Long userId = getCurrentUserId();

        // 商品和套装必须二选一：都为空或都不为空都视为参数错误
        Long productId = shoppingCartDTO.getProductId();
        Long bundleId = shoppingCartDTO.getBundleId();
        if ((productId == null) == (bundleId == null)) {
            throw new ShoppingCartBusinessException("商品或套装信息不完整");
        }

        // 未传数量时默认购买1件，小于1按1件处理，超过上限直接拦截
        int number = shoppingCartDTO.getNumber() == null ? 1 : shoppingCartDTO.getNumber();
        if (number < 1) {
            number = 1;
        }
        if (number > MAX_NUMBER) {
            throw new ShoppingCartBusinessException("单个商品最多购买" + MAX_NUMBER + "件");
        }

        // 当前版本前端没有真实SKU时会传0，统一按“无SKU”处理
        Long skuId = shoppingCartDTO.getSkuId();
        if (skuId != null && skuId <= 0) {
            skuId = null;
        }

        // 以下字段最终写入购物车记录，由商品/套装和SKU信息计算得出
        String name;
        String image;
        BigDecimal price;
        Integer stock = null;
        String productStyle;

        if (productId != null) {
            // 2、商品：先取商品基础信息，存在SKU时再用SKU信息覆盖
            Product product = productMapper.getById(productId);
            if (product == null) {
                throw new ShoppingCartBusinessException("商品不存在");
            }
            if (!StatusConstant.ENABLE.equals(product.getStatus())) {
                throw new ShoppingCartBusinessException("商品已下架");
            }
            name = product.getName();
            image = product.getImage();
            price = product.getPrice();
            stock = product.getStock();

            // 3、SKU是具体规格组合，价格、库存、图片和已选属性都以它为准
            ProductSku sku = null;
            if (skuId != null) {
                sku = productSkuMapper.getById(skuId);
                if (sku == null || !Objects.equals(sku.getProductId(), productId)) {
                    throw new ShoppingCartBusinessException("商品规格不存在");
                }
                if (StatusConstant.DISABLE.equals(sku.getStatus())) {
                    throw new ShoppingCartBusinessException("该规格已下架");
                }
                if (sku.getPrice() != null) {
                    price = sku.getPrice();
                }
                if (sku.getStock() != null) {
                    stock = sku.getStock();
                }
                if (StringUtils.hasText(sku.getImage())) {
                    image = sku.getImage();
                }
            }
            // 4、有SKU时直接取SKU属性；无SKU时校验前端已选属性并生成规范JSON
            productStyle = productStyleService.resolve(productId, sku, shoppingCartDTO.getProductStyle());
        } else {
            // 套装没有SKU，名称、图片、价格取套装本身的信息
            Bundle bundle = bundleMapper.getById(bundleId);
            if (bundle == null) {
                throw new ShoppingCartBusinessException("套装不存在");
            }
            if (!StatusConstant.ENABLE.equals(bundle.getStatus())) {
                throw new ShoppingCartBusinessException("套装已下架");
            }
            name = bundle.getName();
            image = bundle.getImage();
            price = bundle.getPrice();
            // 套装规格暂不支持属性校验，沿用前端提交的已选文案
            productStyle = StringUtils.hasText(shoppingCartDTO.getProductStyle())
                    ? shoppingCartDTO.getProductStyle().trim() : null;
        }

        // 价格缺失时无法计算购物车金额
        if (price == null) {
            throw new ShoppingCartBusinessException("商品价格异常");
        }

        // 5、查询当前用户是否已经加购过同一商品/套装、同一SKU、同一规格
        ShoppingCart query = ShoppingCart.builder()
                .userId(userId)
                .productId(productId)
                .bundleId(bundleId)
                .skuId(skuId)
                .build();
        // SQL 只过滤非空条件，这里再按商品/套装、SKU、规格精确匹配一次，避免误合并
        ShoppingCart exist = null;
        for (ShoppingCart cart : shoppingCartMapper.list(query)) {
            if (Objects.equals(cart.getProductId(), productId)
                    && Objects.equals(cart.getBundleId(), bundleId)
                    && Objects.equals(cart.getSkuId(), skuId)
                    && Objects.equals(cart.getProductStyle(), productStyle)) {
                exist = cart;
                break;
            }
        }

        // 已存在则合并数量，并重新校验单商品上限和库存
        int existNumber = exist == null || exist.getNumber() == null ? 0 : exist.getNumber();
        int totalNumber = existNumber + number;
        if (totalNumber > MAX_NUMBER) {
            throw new ShoppingCartBusinessException("单个商品最多购买" + MAX_NUMBER + "件");
        }
        if (stock != null && totalNumber > stock) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 金额 = 单价 × 合并后的总数量
        BigDecimal amount = price.multiply(BigDecimal.valueOf(totalNumber));

        if (exist != null) {
            // 已在购物车中：只更新数量和金额
            exist.setNumber(totalNumber);
            exist.setAmount(amount);
            shoppingCartMapper.updateNumber(exist);
            return;
        }

        // 不在购物车中：新增一条加购记录
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .name(name)
                .image(image)
                .userId(userId)
                .productId(productId)
                .bundleId(bundleId)
                .skuId(skuId)
                .productStyle(productStyle)
                .number(totalNumber)
                .amount(amount)
                .createTime(LocalDateTime.now())
                .build();
        shoppingCartMapper.insert(shoppingCart);
    }

    /**
     * 查询当前用户的购物车
     * @return 购物车列表
     */
    @Override
    public List<ShoppingCart> list() {
        Long userId = getCurrentUserId();
        List<ShoppingCart> list = shoppingCartMapper.list(ShoppingCart.builder().userId(userId).build());
        // 填充当前库存，前端据此限制可修改的数量并提示库存不足
        for (ShoppingCart shoppingCart : list) {
            shoppingCart.setStock(getStock(shoppingCart.getSkuId(), shoppingCart.getProductId()));
        }
        return list;
    }

    /**
     * 查询购物车商品当前库存：有SKU取SKU库存，否则取商品库存
     * @return 库存，套装等无库存概念的商品返回null
     */
    private Integer getStock(Long skuId, Long productId) {
        if (skuId != null) {
            ProductSku sku = productSkuMapper.getById(skuId);
            if (sku != null) {
                return sku.getStock();
            }
        }
        if (productId != null) {
            Product product = productMapper.getById(productId);
            if (product != null) {
                return product.getStock();
            }
        }
        return null;
    }

    /**
     * 修改购物车商品数量，数量小于等于0时删除该记录
     * @param shoppingCartEditDTO 购物车记录id及修改后的数量
     */
    @Override
    @Transactional
    public void editShoppingCart(ShoppingCartEditDTO shoppingCartEditDTO) {
        Long userId = getCurrentUserId();
        if (shoppingCartEditDTO.getId() == null) {
            throw new ShoppingCartBusinessException("购物车记录不存在");
        }
        ShoppingCart shoppingCart = shoppingCartMapper.getById(shoppingCartEditDTO.getId());
        // 只能操作自己的购物车记录
        if (shoppingCart == null || !Objects.equals(shoppingCart.getUserId(), userId)) {
            throw new ShoppingCartBusinessException("购物车记录不存在");
        }

        int number = shoppingCartEditDTO.getNumber() == null ? 0 : shoppingCartEditDTO.getNumber();
        if (number <= 0) {
            shoppingCartMapper.deleteById(shoppingCart.getId());
            return;
        }
        if (number > MAX_NUMBER) {
            throw new ShoppingCartBusinessException("单个商品最多购买" + MAX_NUMBER + "件");
        }

        // 校验库存：有SKU取SKU库存，否则取商品库存
        Integer stock = null;
        if (shoppingCart.getSkuId() != null) {
            ProductSku sku = productSkuMapper.getById(shoppingCart.getSkuId());
            if (sku != null) {
                stock = sku.getStock();
            }
        } else if (shoppingCart.getProductId() != null) {
            Product product = productMapper.getById(shoppingCart.getProductId());
            if (product != null) {
                stock = product.getStock();
            }
        }
        if (stock != null && number > stock) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 根据原金额和原数量还原单价，再按新数量计算金额
        int oldNumber = shoppingCart.getNumber() == null || shoppingCart.getNumber() <= 0 ? 1 : shoppingCart.getNumber();
        BigDecimal oldAmount = shoppingCart.getAmount() == null ? BigDecimal.ZERO : shoppingCart.getAmount();
        BigDecimal unitPrice = oldAmount.divide(BigDecimal.valueOf(oldNumber), 2, RoundingMode.HALF_UP);

        shoppingCart.setNumber(number);
        shoppingCart.setAmount(unitPrice.multiply(BigDecimal.valueOf(number)));
        shoppingCartMapper.updateNumber(shoppingCart);
    }

    /**
     * 批量删除当前用户购物车中的商品
     * @param ids 购物车记录id列表
     */
    @Override
    @Transactional
    public void deleteShoppingCart(List<Long> ids) {
        Long userId = getCurrentUserId();
        if (ids == null || ids.isEmpty()) {
            throw new ShoppingCartBusinessException("请选择要删除的商品");
        }
        // SQL 同时按用户id过滤，只能删除自己的购物车记录
        shoppingCartMapper.deleteByIds(ids, userId);
    }

    /**
     * 统计当前用户购物车中的商品条目数
     * @return 条目数
     */
    @Override
    public Integer count() {
        Long userId = getCurrentUserId();
        return shoppingCartMapper.countByUserId(userId);
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

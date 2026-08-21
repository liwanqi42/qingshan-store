package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.ShoppingCartDTO;
import com.qingshan.dto.ShoppingCartEditDTO;
import com.qingshan.entity.Product;
import com.qingshan.entity.ProductSku;
import com.qingshan.entity.ShoppingCart;
import com.qingshan.exception.ShoppingCartBusinessException;
import com.qingshan.exception.UserNotLoginException;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.mapper.ProductSkuMapper;
import com.qingshan.mapper.ShoppingCartMapper;
import com.qingshan.model.ResolvedOrderItem;
import com.qingshan.service.ShoppingCartService;
import com.qingshan.service.support.OrderItemResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private OrderItemResolver orderItemResolver;

    /**
     * 添加购物车
     * @param shoppingCartDTO 加购参数：商品/套装、SKU、已选属性、数量
     */
    @Override
    @Transactional
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 1、获取当前登录用户，未登录不能加购
        Long userId = getCurrentUserId();

        // 2、商品/套装、SKU、价格、库存和已选属性统一由 OrderItemResolver 校验
        ResolvedOrderItem item = orderItemResolver.resolve(
                shoppingCartDTO.getProductId(),
                shoppingCartDTO.getBundleId(),
                shoppingCartDTO.getSkuId(),
                shoppingCartDTO.getProductStyle(),
                shoppingCartDTO.getNumber());

        // 3、查询当前用户是否已经加购过同一商品/套装、同一SKU、同一规格
        ShoppingCart query = ShoppingCart.builder()
                .userId(userId)
                .productId(item.getProductId())
                .bundleId(item.getBundleId())
                .skuId(item.getSkuId())
                .build();
        // SQL 只过滤非空条件，这里再按商品/套装、SKU、规格精确匹配一次，避免误合并
        ShoppingCart exist = null;
        for (ShoppingCart cart : shoppingCartMapper.list(query)) {
            if (Objects.equals(cart.getProductId(), item.getProductId())
                    && Objects.equals(cart.getBundleId(), item.getBundleId())
                    && Objects.equals(cart.getSkuId(), item.getSkuId())
                    && Objects.equals(cart.getProductStyle(), item.getProductStyle())) {
                exist = cart;
                break;
            }
        }

        // 4、已存在则合并数量，并重新校验单商品上限和库存
        int existNumber = exist == null || exist.getNumber() == null ? 0 : exist.getNumber();
        int totalNumber = existNumber + item.getNumber();
        if (totalNumber > NumConstant.PRODUCT_MAX_BUY) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_BUY_LIMIT);
        }
        if (item.getStock() != null && totalNumber > item.getStock()) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 金额 = 单价 × 合并后的总数量
        BigDecimal amount = item.getPrice().multiply(BigDecimal.valueOf(totalNumber));

        if (exist != null) {
            // 已在购物车中：只更新数量和金额
            exist.setNumber(totalNumber);
            exist.setAmount(amount);
            shoppingCartMapper.updateNumber(exist);
            return;
        }

        // 5、不在购物车中：新增一条加购记录
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .name(item.getName())
                .image(item.getImage())
                .userId(userId)
                .productId(item.getProductId())
                .bundleId(item.getBundleId())
                .skuId(item.getSkuId())
                .productStyle(item.getProductStyle())
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
        if (number > NumConstant.PRODUCT_MAX_BUY) {
            throw new ShoppingCartBusinessException(MessageConstant.PRODUCT_BUY_LIMIT);
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

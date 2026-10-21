package com.qingshan.controller.user;

import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.ProductPageQueryDTO;
import com.qingshan.entity.Product;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.ProductService;
import com.qingshan.service.support.ProductCacheManager;
import com.qingshan.utils.PageUtil;
import com.qingshan.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userProductController")
@RequestMapping("/user/product")
@Slf4j
@Api(tags = "C端-商品浏览接口")
public class ProductController {

    private static final long CACHE_TTL_MINUTES = 30;

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductCacheManager productCacheManager;

    /**
     * 根据分类id查询商品
     *
     * @param categoryId 商品分类id
     * @return 商品列表（含规格属性）
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询商品")
    public Result<List<ProductVO>> list(Long categoryId) {

        String key = "product_v" + productCacheManager.currentVersion() + "_" + categoryId;

        List<ProductVO> list = getCache(key);
        if (list != null && ! list.isEmpty()){
            return Result.success(list);
        }

        Product product = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)//查询上架中的商品
                .build();

        list = productService.listWithAttr(product);
        putCache(key, list);

        return Result.success(list);
    }

    /**
     * 商品分页查询
     *
     * @param productPageQueryDTO 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    @ApiOperation("商品分页查询")
    public Result<PageResult> page(ProductPageQueryDTO productPageQueryDTO) {
        //用户端只查询上架商品
        productPageQueryDTO.setStatus(StatusConstant.ENABLE);
        // 先归一化分页参数，避免用超大 pageSize 生成无效缓存 key
        productPageQueryDTO.setPage(PageUtil.pageNum(productPageQueryDTO.getPage()));
        productPageQueryDTO.setPageSize(PageUtil.pageSize(productPageQueryDTO.getPageSize()));

        String key = "product_page_v" + productCacheManager.currentVersion() + "_"
                + (productPageQueryDTO.getCategoryId() == null ? "all" : productPageQueryDTO.getCategoryId()) + "_"
                + (productPageQueryDTO.getName() == null ? "" : productPageQueryDTO.getName()) + "_"
                + productPageQueryDTO.getPage() + "_"
                + productPageQueryDTO.getPageSize();

        PageResult pageResult = getCache(key);
        if (pageResult == null) {
            pageResult = productService.pageQuery(productPageQueryDTO);
            putCache(key, pageResult);
        }
        return Result.success(pageResult);
    }

    /**
     * 根据id查询商品详情
     *
     * @param id 商品id
     * @return 商品信息（含规格属性）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询商品详情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO productVO = productService.getByIdWithAttr(id);
        return Result.success(productVO);
    }

    /**
     * 读取商品缓存。缓存对象来自旧版本的类时反序列化会失败，
     * 此时删除脏缓存并返回 null，让调用方回源数据库。
     * @param key 缓存key
     * @return 缓存值，缓存不可用时返回null
     */
    @SuppressWarnings("unchecked")
    private <T> T getCache(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (SerializationException e) {
            log.warn("商品缓存反序列化失败，删除脏缓存：{}", key, e);
            try {
                redisTemplate.delete(key);
            } catch (Exception deleteException) {
                log.warn("删除脏缓存失败：{}", key, deleteException);
            }
            return null;
        } catch (Exception e) {
            // Redis 不可用时回源数据库，不影响商品浏览
            log.warn("读取商品缓存失败，本次回源数据库：{}", key, e);
            return null;
        }
    }

    /**
     * 写入商品缓存，失败时忽略，由下次请求回源或 TTL 兜底
     */
    private void putCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商品缓存失败：{}", key, e);
        }
    }

}

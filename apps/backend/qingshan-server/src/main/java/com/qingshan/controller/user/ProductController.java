package com.qingshan.controller.user;

import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.ProductPageQueryDTO;
import com.qingshan.entity.Product;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.ProductService;
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

@RestController("userProductController")
@RequestMapping("/user/product")
@Slf4j
@Api(tags = "C端-商品浏览接口")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询商品
     *
     * @param categoryId 商品分类id
     * @return 商品列表（含规格属性）
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询商品")
    public Result<List<ProductVO>> list(Long categoryId) {

        String key = "product_" +categoryId;

        List<ProductVO> list = getCache(key);
        if (list != null && ! list.isEmpty()){
            return Result.success(list);
        }

        Product product = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)//查询上架中的商品
                .build();

        list = productService.listWithAttr(product);
        redisTemplate.opsForValue().set(key, list);

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

        String key = "product_page_"
                + (productPageQueryDTO.getCategoryId() == null ? "all" : productPageQueryDTO.getCategoryId()) + "_"
                + (productPageQueryDTO.getName() == null ? "" : productPageQueryDTO.getName()) + "_"
                + productPageQueryDTO.getPage() + "_"
                + productPageQueryDTO.getPageSize();

        PageResult pageResult = getCache(key);
        if (pageResult == null) {
            pageResult = productService.pageQuery(productPageQueryDTO);
            redisTemplate.opsForValue().set(key, pageResult);
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
    private <T> T getCache(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (SerializationException e) {
            log.warn("商品缓存反序列化失败，删除脏缓存：{}", key, e);
            redisTemplate.delete(key);
            return null;
        }
    }

}

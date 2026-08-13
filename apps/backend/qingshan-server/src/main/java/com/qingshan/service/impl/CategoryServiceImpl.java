package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.CategoryDTO;
import com.qingshan.dto.CategoryPageQueryDTO;
import com.qingshan.entity.Category;
import com.qingshan.exception.DeletionNotAllowedException;
import com.qingshan.mapper.BundleMapper;
import com.qingshan.mapper.CategoryMapper;
import com.qingshan.mapper.ProductMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BundleMapper bundleMapper;

    /**
     * 新增分类
     * @param categoryDTO 分类信息
     */
    @Override
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);



        categoryMapper.insert(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO 分页查询参数
     * @return 分页查询结果
     */
    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        try (Page<Category> page = PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize())) {
            //下一条sql进行分页，自动加入limit关键字分页
            List<Category> records = categoryMapper.pageQuery(categoryPageQueryDTO);
            return new PageResult(page.getTotal(), records);
        }
    }

    /**
     * 根据id删除分类
     * @param id 分类id
     */
    @Override
    public void deleteById(Long id) {
        //查询当前分类是否关联了商品，如果关联了就抛出业务异常
        Integer count = productMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有商品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_PRODUCT);
        }

        //查询当前分类是否关联了套装，如果关联了就抛出业务异常
        count = bundleMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有套装，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_BUNDLE);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO 分类信息
     */
    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        categoryMapper.update(category);
    }

    /**
     * 启用、禁用分类
     * @param status 分类状态，0禁用 1启用
     * @param id 分类id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
//                .updateTime(LocalDateTime.now())
//                .updateUser(BaseContext.getCurrentId())
                .build();
        categoryMapper.update(category);
    }

    /**
     * 根据类型查询分类
     * @param type 分类类型，1商品分类 2套装分类
     * @return 分类列表
     */
    @Override
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }
}

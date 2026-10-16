package com.qingshan.service;

import com.qingshan.dto.SysMenuDTO;
import com.qingshan.vo.SysMenuVO;

import java.util.List;

public interface MenuService {

    /**
     * 查询全部菜单配置
     */
    List<SysMenuVO> list();

    void save(SysMenuDTO menuDTO);

    void update(SysMenuDTO menuDTO);

    void delete(Long id);

    /**
     * 删除全部菜单配置并恢复内置默认数据
     */
    void reset();
}

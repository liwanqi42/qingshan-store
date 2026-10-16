package com.qingshan.service;

import com.qingshan.dto.DictItemDTO;
import com.qingshan.dto.DictTypeDTO;
import com.qingshan.vo.DictTypeVO;

import java.util.List;

public interface DictService {

    /**
     * 管理端查询全部字典（含禁用项）
     */
    List<DictTypeVO> listForAdmin();

    /**
     * 用户端查询启用状态的字典
     */
    List<DictTypeVO> listForUser();

    void saveType(DictTypeDTO dictTypeDTO);

    void updateType(DictTypeDTO dictTypeDTO);

    void deleteType(Long id);

    void saveItem(DictItemDTO dictItemDTO);

    void updateItem(DictItemDTO dictItemDTO);

    void deleteItem(Long id);

    /**
     * 删除全部字典并恢复内置默认数据
     */
    void reset();
}

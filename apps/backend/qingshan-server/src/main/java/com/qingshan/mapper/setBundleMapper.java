package com.qingshan.mapper;

import com.qingshan.annotation.AutoFill;
import com.qingshan.entity.Bundle;
import com.qingshan.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface setBundleMapper {

    /**
     * 根据主键更新套装
     * @param bundle
     */
    @AutoFill(OperationType.UPDATE)
    void update(Bundle bundle);
}

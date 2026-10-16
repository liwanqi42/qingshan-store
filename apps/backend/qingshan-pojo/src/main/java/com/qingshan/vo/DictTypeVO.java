package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 字典类型视图，包含类型下的字典项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String type;

    private String name;

    private Integer status;

    private Integer sort;

    private Integer isSystem;

    private List<DictItemVO> items;
}

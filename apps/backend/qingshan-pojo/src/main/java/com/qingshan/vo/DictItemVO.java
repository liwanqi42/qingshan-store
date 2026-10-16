package com.qingshan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字典项视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String label;

    private String value;

    private String tagType;

    private Integer sort;

    private Integer status;
}

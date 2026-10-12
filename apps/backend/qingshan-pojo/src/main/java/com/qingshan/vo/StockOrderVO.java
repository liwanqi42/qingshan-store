package com.qingshan.vo;

import com.qingshan.entity.StockOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 入库/出库单据详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    //类型 1入库 2出库
    private Integer type;

    //状态 0待审核 1已审核
    private Integer status;

    private Integer totalQuantity;

    private Integer totalKinds;

    private String remark;

    private LocalDateTime auditTime;

    private Long auditUser;

    private String auditUserName;

    private LocalDateTime createTime;

    private Long createUser;

    private String createUserName;

    private LocalDateTime updateTime;

    @Builder.Default
    private List<StockOrderItem> items = new ArrayList<>();
}

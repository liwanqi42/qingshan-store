package com.qingshan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入库/出库单据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 入库 */
    public static final int TYPE_IN = 1;

    /** 出库 */
    public static final int TYPE_OUT = 2;

    /** 待审核 */
    public static final int STATUS_PENDING = 0;

    /** 已审核 */
    public static final int STATUS_AUDITED = 1;

    private Long id;

    //单据编号，RK 入库 / CK 出库
    private String orderNo;

    //类型 1入库 2出库
    private Integer type;

    //状态 0待审核 1已审核
    private Integer status;

    //总数量
    private Integer totalQuantity;

    //商品种数（按商品+SKU去重）
    private Integer totalKinds;

    //备注
    private String remark;

    //审核时间
    private LocalDateTime auditTime;

    //审核人
    private Long auditUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}

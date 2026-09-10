package com.qingshan.service;

import com.qingshan.vo.BusinessDataVO;
import com.qingshan.vo.OrderOverViewVO;
import com.qingshan.vo.SaleOverViewVO;

import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询商品总览
     * @return
     */
    SaleOverViewVO getProductOverView();

    /**
     * 查询套装总览
     * @return
     */
    SaleOverViewVO getBundleOverView();

}

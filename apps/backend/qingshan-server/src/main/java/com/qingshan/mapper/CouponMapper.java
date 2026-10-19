package com.qingshan.mapper;

import com.github.pagehelper.Page;
import com.qingshan.annotation.AutoFill;
import com.qingshan.dto.CouponPageQueryDTO;
import com.qingshan.entity.Coupon;
import com.qingshan.enumeration.OperationType;
import com.qingshan.vo.CouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface CouponMapper {

    /**
     * 管理端分页查询优惠券，附带已使用数量与剩余数量
     */
    Page<CouponVO> pageQuery(CouponPageQueryDTO couponPageQueryDTO);

    @Select("select * from coupon where id = #{id}")
    Coupon getById(Long id);

    /**
     * 领券前锁定券行，串行化同一张券的领取，避免限领校验并发穿透
     */
    @Select("select * from coupon where id = #{id} for update")
    Coupon getByIdForUpdate(Long id);

    @AutoFill(OperationType.INSERT)
    void insert(Coupon coupon);

    @AutoFill(OperationType.UPDATE)
    void update(Coupon coupon);

    /**
     * 只允许删除尚未被领取的优惠券
     */
    int deleteById(Long id);

    @AutoFill(OperationType.UPDATE)
    int updateStatus(Coupon coupon);

    /**
     * 领券时条件增加已领数量：仅上架、有效期内、未领完时成功
     */
    int increaseReceived(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 用户端可领券列表：上架、有效期内、未领完且未达个人限领
     */
    Page<Coupon> listAvailable(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}

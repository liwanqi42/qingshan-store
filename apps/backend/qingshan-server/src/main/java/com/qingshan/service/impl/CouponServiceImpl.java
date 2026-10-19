package com.qingshan.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.NumConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.dto.CouponDTO;
import com.qingshan.dto.CouponPageQueryDTO;
import com.qingshan.entity.Coupon;
import com.qingshan.entity.UserCoupon;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.CouponMapper;
import com.qingshan.mapper.UserCouponMapper;
import com.qingshan.result.PageResult;
import com.qingshan.service.CouponService;
import com.qingshan.service.support.OrderAmountCalculator;
import com.qingshan.utils.PageUtil;
import com.qingshan.vo.CouponVO;
import com.qingshan.vo.UserCouponVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务实现
 */
@Service
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderAmountCalculator orderAmountCalculator;

    public CouponServiceImpl(CouponMapper couponMapper,
                             UserCouponMapper userCouponMapper,
                             OrderAmountCalculator orderAmountCalculator) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.orderAmountCalculator = orderAmountCalculator;
    }

    @Override
    public PageResult pageQuery(CouponPageQueryDTO couponPageQueryDTO) {
        int page = PageUtil.pageNum(couponPageQueryDTO.getPage());
        int pageSize = PageUtil.pageSize(couponPageQueryDTO.getPageSize());
        PageHelper.startPage(page, pageSize);
        Page<CouponVO> pageResult = couponMapper.pageQuery(couponPageQueryDTO);
        return new PageResult(pageResult.getTotal(), pageResult.getResult());
    }

    @Override
    public void save(CouponDTO couponDTO) {
        Coupon coupon = validateAndBuild(couponDTO, null);
        coupon.setReceivedCount(0);
        couponMapper.insert(coupon);
    }

    @Override
    public void update(CouponDTO couponDTO) {
        if (couponDTO == null || couponDTO.getId() == null) {
            throw new BaseException(MessageConstant.COUPON_NOT_FOUND);
        }
        Coupon existing = requireCoupon(couponDTO.getId());
        Coupon coupon = validateAndBuild(couponDTO, existing);
        coupon.setId(existing.getId());
        couponMapper.update(coupon);
    }

    @Override
    public void delete(Long id) {
        Coupon existing = requireCoupon(id);
        int received = existing.getReceivedCount() == null ? 0 : existing.getReceivedCount();
        if (received > 0) {
            throw new BaseException(MessageConstant.COUPON_RECEIVED_PROTECTED);
        }
        int updated = couponMapper.deleteById(id);
        if (updated == 0) {
            throw new BaseException(MessageConstant.COUPON_RECEIVED_PROTECTED);
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        requireCoupon(id);
        if (!Coupon.STATUS_ON.equals(status) && !Coupon.STATUS_OFF.equals(status)) {
            throw new BaseException(MessageConstant.COUPON_STATUS_INVALID);
        }
        couponMapper.updateStatus(Coupon.builder().id(id).status(status).build());
    }

    @Override
    public PageResult listAvailable(int page, int size) {
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(PageUtil.pageNum(page), PageUtil.pageSize(size));
        Page<Coupon> pageResult = couponMapper.listAvailable(userId, LocalDateTime.now());
        List<Coupon> records = pageResult.getResult();
        return new PageResult(pageResult.getTotal(), records);
    }

    @Override
    @Transactional
    public void take(Long couponId) {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();

        // 锁券行：同一张券的领取串行执行，限领与库存校验不会被并发穿透
        Coupon coupon = couponMapper.getByIdForUpdate(couponId);
        if (coupon == null) {
            throw new BaseException(MessageConstant.COUPON_NOT_FOUND);
        }
        checkAvailable(coupon, now);

        int limit = coupon.getLimitPerUser() == null ? 1 : coupon.getLimitPerUser();
        int received = userCouponMapper.countByUserAndCoupon(userId, couponId);
        if (received >= limit) {
            throw new BaseException(MessageConstant.COUPON_LIMIT_REACHED);
        }

        // 条件更新库存，最后一张券只会被一个请求领走
        int updated = couponMapper.increaseReceived(couponId, now);
        if (updated == 0) {
            throw new BaseException(MessageConstant.COUPON_SOLD_OUT);
        }

        userCouponMapper.insert(UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .receiveTime(now)
                .status(UserCoupon.STATUS_UNUSED)
                .build());
    }

    @Override
    public PageResult listMine(Integer status, int page, int size) {
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(PageUtil.pageNum(page), PageUtil.pageSize(size));
        Page<UserCouponVO> pageResult = userCouponMapper.pageByUser(userId, status, LocalDateTime.now());
        return new PageResult(pageResult.getTotal(), pageResult.getResult());
    }

    @Override
    public BigDecimal resolveDiscount(Long userCouponId, Long userId, BigDecimal goodsAmount) {
        UserCoupon userCoupon = userCouponMapper.getById(userCouponId);
        if (userCoupon == null) {
            throw new BaseException(MessageConstant.COUPON_NOT_FOUND);
        }
        if (!userCoupon.getUserId().equals(userId)) {
            throw new BaseException(MessageConstant.COUPON_NOT_OWNED);
        }
        if (!UserCoupon.STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BaseException(MessageConstant.COUPON_ALREADY_USED);
        }

        Coupon coupon = couponMapper.getById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BaseException(MessageConstant.COUPON_NOT_FOUND);
        }
        checkAvailable(coupon, LocalDateTime.now());
        if (!orderAmountCalculator.isUsable(coupon, goodsAmount)) {
            throw new BaseException(MessageConstant.COUPON_THRESHOLD_NOT_MET);
        }
        return orderAmountCalculator.calculateDiscount(coupon, goodsAmount);
    }

    @Override
    public void lockForOrder(Long userCouponId, Long userId, Long orderId) {
        int updated = userCouponMapper.lockForOrder(userCouponId, userId, orderId, LocalDateTime.now());
        if (updated == 0) {
            throw new BaseException(MessageConstant.COUPON_LOCK_FAILED);
        }
    }

    @Override
    public void markUsedByOrder(Long orderId) {
        int updated = userCouponMapper.markUsedByOrder(orderId, LocalDateTime.now());
        if (updated == 0) {
            // 无券订单或重复回调，均视为正常
            log.debug("订单{}没有待核销的优惠券", orderId);
        }
    }

    @Override
    public void returnForOrder(Long orderId) {
        int updated = userCouponMapper.returnByOrder(orderId, LocalDateTime.now());
        if (updated > 0) {
            log.info("订单{}取消/退款，退回优惠券{}张", orderId, updated);
        }
    }

    private Coupon requireCoupon(Long id) {
        Coupon coupon = id == null ? null : couponMapper.getById(id);
        if (coupon == null) {
            throw new BaseException(MessageConstant.COUPON_NOT_FOUND);
        }
        return coupon;
    }

    private void checkAvailable(Coupon coupon, LocalDateTime now) {
        boolean statusOn = Coupon.STATUS_ON.equals(coupon.getStatus());
        boolean inPeriod = coupon.getStartTime() != null && coupon.getEndTime() != null
                && !now.isBefore(coupon.getStartTime()) && !now.isAfter(coupon.getEndTime());
        if (!statusOn || !inPeriod) {
            throw new BaseException(MessageConstant.COUPON_NOT_AVAILABLE);
        }
    }

    private Coupon validateAndBuild(CouponDTO dto, Coupon existing) {
        if (dto == null) {
            throw new BaseException(MessageConstant.COUPON_NOT_FOUND);
        }
        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new BaseException(MessageConstant.COUPON_NAME_REQUIRED);
        }
        if (name.length() > 64) {
            throw new BaseException(MessageConstant.COUPON_NAME_TOO_LONG);
        }
        Integer type = dto.getType();
        if (type == null || type < Coupon.TYPE_FULL_REDUCTION || type > Coupon.TYPE_FULL_DISCOUNT) {
            throw new BaseException(MessageConstant.COUPON_TYPE_INVALID);
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BaseException(MessageConstant.COUPON_TIME_INVALID);
        }

        BigDecimal fullMoney = money(dto.getFullMoney());
        BigDecimal minus = money(dto.getMinus());
        BigDecimal rate = dto.getRate() == null
                ? BigDecimal.ONE : dto.getRate().setScale(2, RoundingMode.HALF_UP);
        if (fullMoney.signum() < 0) {
            throw new BaseException(MessageConstant.COUPON_THRESHOLD_INVALID);
        }

        switch (type) {
            case 1:
                if (minus.signum() <= 0) {
                    throw new BaseException(MessageConstant.COUPON_AMOUNT_INVALID);
                }
                break;
            case 2:
                checkRate(rate);
                break;
            case 3:
                if (minus.signum() <= 0) {
                    throw new BaseException(MessageConstant.COUPON_AMOUNT_INVALID);
                }
                break;
            case 4:
                if (fullMoney.signum() <= 0) {
                    throw new BaseException(MessageConstant.COUPON_THRESHOLD_INVALID);
                }
                checkRate(rate);
                break;
            default:
                throw new BaseException(MessageConstant.COUPON_TYPE_INVALID);
        }

        int totalCount = dto.getTotalCount() == null ? 0 : dto.getTotalCount();
        if (totalCount < 0) {
            throw new BaseException(MessageConstant.COUPON_TOTAL_INVALID);
        }
        if (totalCount > 0 && existing != null
                && existing.getReceivedCount() != null && totalCount < existing.getReceivedCount()) {
            throw new BaseException(MessageConstant.COUPON_TOTAL_TOO_SMALL);
        }
        int limitPerUser = dto.getLimitPerUser() == null ? 1 : dto.getLimitPerUser();
        if (limitPerUser < 1) {
            throw new BaseException(MessageConstant.COUPON_LIMIT_INVALID);
        }
        Integer status = dto.getStatus() == null
                ? (existing == null ? Coupon.STATUS_OFF : existing.getStatus())
                : dto.getStatus();
        if (!Coupon.STATUS_ON.equals(status) && !Coupon.STATUS_OFF.equals(status)) {
            throw new BaseException(MessageConstant.COUPON_STATUS_INVALID);
        }

        return Coupon.builder()
                .name(name)
                .type(type)
                .fullMoney(fullMoney)
                .minus(minus)
                .rate(rate)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .totalCount(totalCount)
                .limitPerUser(limitPerUser)
                .status(status)
                .build();
    }

    private BigDecimal money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return amount.setScale(NumConstant.AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private void checkRate(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new BaseException(MessageConstant.COUPON_RATE_INVALID);
        }
    }
}

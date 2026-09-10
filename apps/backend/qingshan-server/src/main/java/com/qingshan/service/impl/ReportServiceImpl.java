package com.qingshan.service.impl;

import com.qingshan.entity.Orders;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.service.ReportService;
import com.qingshan.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //构造从begin到end的日期列表（包含首尾两天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate date = begin;
        while (!date.isAfter(end)) {
            dateList.add(date);
            date = date.plusDays(1);
        }

        //统计每天已完成订单的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate day : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(day, LocalTime.MAX);
            Double turnover = orderMapper.sumByCondition(beginTime, endTime, Orders.STATUS_COMPLETED);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(dateList.stream().map(LocalDate::toString).collect(Collectors.joining(",")))
                .turnoverList(turnoverList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
    }
}

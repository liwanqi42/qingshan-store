package com.qingshan.service.impl;

import com.qingshan.dto.GoodsSalesDTO;
import com.qingshan.entity.Orders;
import com.qingshan.mapper.OrderMapper;
import com.qingshan.mapper.UserMapper;
import com.qingshan.service.ReportService;
import com.qingshan.service.WorkspaceService;
import com.qingshan.vo.BusinessDataVO;
import com.qingshan.vo.OrderReportVO;
import com.qingshan.vo.SalesTop10ReportVO;
import com.qingshan.vo.TurnoverReportVO;
import com.qingshan.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    /**
     * 导出报表统计的天数
     */
    private static final int EXPORT_DAYS = 30;

    private static final String EXPORT_TEMPLATE = "template/运营数据报表模板.xlsx";

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        //统计每天已完成订单的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate day : dateList) {
            Double turnover = orderMapper.sumByCondition(
                    LocalDateTime.of(day, LocalTime.MIN),
                    LocalDateTime.of(day, LocalTime.MAX),
                    Orders.STATUS_COMPLETED);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(join(dateList))
                .turnoverList(join(turnoverList))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        //存放每天的总用户数量和新增用户数量
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate day : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(day, LocalTime.MAX);

            //总用户数量：当天结束前注册的用户
            totalUserList.add(userMapper.countByCondition(null, endTime));
            //新增用户数量：当天注册的用户
            newUserList.add(userMapper.countByCondition(beginTime, endTime));
        }

        return UserReportVO.builder()
                .dateList(join(dateList))
                .totalUserList(join(totalUserList))
                .newUserList(join(newUserList))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        //存放每天的订单总数和有效订单数
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate day : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(day, LocalTime.MAX);

            Integer orderCount = orderMapper.countByCondition(beginTime, endTime, null);
            Integer validCount = orderMapper.countByCondition(beginTime, endTime, Orders.STATUS_COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validCount);
            totalOrderCount += orderCount;
            validOrderCount += validCount;
        }

        //订单完成率
        double orderCompletionRate = totalOrderCount == 0
                ? 0.0
                : (double) validOrderCount / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(join(dateList))
                .orderCountList(join(orderCountList))
                .validOrderCountList(join(validOrderCountList))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream()
                .map(GoodsSalesDTO::getName)
                .collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream()
                .map(GoodsSalesDTO::getNumber)
                .collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(join(names))
                .numberList(join(numbers))
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(EXPORT_DAYS);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessData = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(EXPORT_TEMPLATE)) {
            if (in == null) {
                throw new IllegalStateException("运营数据报表模板不存在：" + EXPORT_TEMPLATE);
            }

            try (XSSFWorkbook excel = new XSSFWorkbook(in)) {
                XSSFSheet sheet = excel.getSheet("Sheet1");

                //填充概览数据
                sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

                XSSFRow row = sheet.getRow(3);
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(6).setCellValue(businessData.getNewUsers());

                row = sheet.getRow(4);
                row.getCell(2).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getUnitPrice());

                //填充每日明细数据
                for (int i = 0; i < EXPORT_DAYS; i++) {
                    LocalDate date = dateBegin.plusDays(i);
                    BusinessDataVO dailyData = workspaceService.getBusinessData(
                            LocalDateTime.of(date, LocalTime.MIN),
                            LocalDateTime.of(date, LocalTime.MAX));

                    row = sheet.getRow(7 + i);
                    row.getCell(1).setCellValue(date.toString());
                    row.getCell(2).setCellValue(dailyData.getTurnover());
                    row.getCell(3).setCellValue(dailyData.getValidOrderCount());
                    row.getCell(4).setCellValue(dailyData.getOrderCompletionRate());
                    row.getCell(5).setCellValue(dailyData.getUnitPrice());
                    row.getCell(6).setCellValue(dailyData.getNewUsers());
                }

                //通过输出流将Excel文件下载到客户端浏览器
                String fileName = URLEncoder.encode("运营数据报表.xlsx", StandardCharsets.UTF_8.name());
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

                try (ServletOutputStream out = response.getOutputStream()) {
                    excel.write(out);
                }
            }
        } catch (IOException e) {
            log.error("导出运营数据报表失败", e);
            throw new RuntimeException("导出运营数据报表失败", e);
        }
    }

    /**
     * 构造从begin到end的日期列表（包含首尾两天）
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate date = begin;
        while (!date.isAfter(end)) {
            dateList.add(date);
            date = date.plusDays(1);
        }
        return dateList;
    }

    /**
     * 将集合按逗号拼接为字符串
     */
    private String join(List<?> list) {
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}

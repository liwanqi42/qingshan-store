package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.dto.LogisticsTraceDTO;
import com.qingshan.result.Result;
import com.qingshan.service.LogisticsService;
import com.qingshan.vo.OrderLogisticsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端手工物流轨迹
 */
@RestController
@RequestMapping("/admin/order/logistics")
@Api(tags = "物流轨迹管理接口")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @GetMapping("/trace")
    @ApiOperation("查询订单物流轨迹")
    public Result<OrderLogisticsVO> trace(@RequestParam Long orderId) {
        return Result.success(logisticsService.getForAdmin(orderId));
    }

    @PostMapping("/trace")
    @RequireRole
    @ApiOperation("新增物流轨迹")
    public Result<Long> addTrace(@RequestBody LogisticsTraceDTO traceDTO) {
        return Result.success(logisticsService.addTrace(traceDTO));
    }

    @DeleteMapping("/trace/{id}")
    @RequireRole
    @ApiOperation("删除物流轨迹")
    public Result deleteTrace(@PathVariable Long id) {
        logisticsService.deleteTrace(id);
        return Result.success();
    }
}

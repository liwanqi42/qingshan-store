package com.qingshan.controller.user;

import com.qingshan.dto.BrowseClearDTO;
import com.qingshan.dto.BrowseRecordDTO;
import com.qingshan.result.PageResult;
import com.qingshan.result.Result;
import com.qingshan.service.BrowseHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/browse")
@Api(tags = "C端-商品浏览历史接口")
@Slf4j
public class BrowseController {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    /**
     * 记录商品浏览
     * @param browseRecordDTO 商品id
     * @return
     */
    @PostMapping("/record")
    @ApiOperation("记录商品浏览")
    public Result record(@RequestBody BrowseRecordDTO browseRecordDTO) {
        browseHistoryService.record(browseRecordDTO.getProductId());
        return Result.success();
    }

    /**
     * 分页查询历史浏览
     * @param page 页码
     * @param size 每页数量
     * @return 浏览历史分页结果
     */
    @GetMapping("/list")
    @ApiOperation("分页查询历史浏览")
    public Result<PageResult> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(browseHistoryService.pageQuery(page, size));
    }

    /**
     * 删除历史浏览
     * @param browseClearDTO 要删除的浏览记录id列表
     * @return
     */
    @PostMapping("/clear")
    @ApiOperation("删除历史浏览")
    public Result clear(@RequestBody BrowseClearDTO browseClearDTO) {
        browseHistoryService.clear(browseClearDTO.getIds());
        return Result.success();
    }
}

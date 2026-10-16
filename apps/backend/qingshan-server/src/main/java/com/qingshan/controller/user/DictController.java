package com.qingshan.controller.user;

import com.qingshan.result.Result;
import com.qingshan.service.DictService;
import com.qingshan.vo.DictTypeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端字典查询，只返回启用状态的类型和字典项
 */
@RestController("UserDictController")
@RequestMapping("/user/dict")
@Api(tags = "用户端字典接口")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/list")
    @ApiOperation("查询启用字典")
    public Result<List<DictTypeVO>> list() {
        return Result.success(dictService.listForUser());
    }
}

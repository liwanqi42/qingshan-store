package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
import com.qingshan.result.Result;
import com.qingshan.service.support.ImageUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    private final ImageUploadService imageUploadService;

    public CommonController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /**
     * 文件上传
     * @param file 上传的文件
     * @param type 图片类型：1封面图 2样式图 3详情长图，不传时上传到根目录
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    @RequireRole
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file, @RequestParam(required = false) Integer type) {
        return Result.success(imageUploadService.uploadImage(file, imageUploadService.productDir(type)));
    }
}

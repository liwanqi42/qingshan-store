package com.qingshan.controller.user;

import com.qingshan.result.Result;
import com.qingshan.service.support.ImageUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端通用接口
 */
@RestController("UserCommonController")
@RequestMapping("/user/common")
@Api(tags = "用户端通用接口")
public class CommonController {

    private final ImageUploadService imageUploadService;

    public CommonController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /**
     * 用户头像上传
     * @param file 上传的图片
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    @ApiOperation("用户端图片上传")
    public Result<String> upload(MultipartFile file) {
        return Result.success(imageUploadService.uploadImage(file, "user/avatar/"));
    }
}

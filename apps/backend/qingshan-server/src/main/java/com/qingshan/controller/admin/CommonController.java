package com.qingshan.controller.admin;

import com.qingshan.constant.MessageConstant;
import com.qingshan.result.Result;
import com.qingshan.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    private final AliOssUtil aliOssUtil;

    public CommonController(AliOssUtil aliOssUtil) {this.aliOssUtil = aliOssUtil;}

    /**
     * 文件上传
     * @param file 上传的文件
     * @param type 图片类型：1封面图 2样式图 3详情长图，不传时上传到根目录
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file, @RequestParam(required = false) Integer type){
        log.info("文件上传:{}, type:{}", file, type);

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error(MessageConstant.UPLOAD_FAILED);
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = resolveUploadDir(type) + UUID.randomUUID() + extension;

            //文件请求路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);
        } catch (Exception e) {
            log.error("文件上传失败", e);
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

    /**
     * 按图片用途分目录存放，便于在OSS中区分封面图、样式图和详情长图
     * @param type 图片类型
     * @return OSS对象目录前缀
     */
    private String resolveUploadDir(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case 1:
                return "product/cover/";
            case 2:
                return "product/style/";
            case 3:
                return "product/detail/";
            default:
                return "";
        }
    }
}

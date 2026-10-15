package com.qingshan.controller.admin;

import com.qingshan.annotation.RequireRole;
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

import java.util.*;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

    private final AliOssUtil aliOssUtil;

    public CommonController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
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
        if (file == null || file.isEmpty()) {
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error(MessageConstant.UPLOAD_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.error(MessageConstant.UPLOAD_SIZE_EXCEEDED);
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error(MessageConstant.UPLOAD_TYPE_NOT_ALLOWED);
        }

        try {
            byte[] bytes = file.getBytes();
            // 以文件头识别的真实类型为准，客户端 Content-Type 和扩展名都不可信
            String imageType = detectImageType(bytes);
            if (imageType == null || !extensionMatches(extension, imageType)) {
                return Result.error(MessageConstant.UPLOAD_TYPE_NOT_ALLOWED);
            }

            String objectName = resolveUploadDir(type) + UUID.randomUUID() + "." + imageType;
            String filePath = aliOssUtil.upload(bytes, objectName, contentTypeOf(imageType));
            return Result.success(filePath);
        } catch (Exception e) {
            log.error("文件上传失败，filename={}", originalFilename, e);
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

    /**
     * 按文件头识别真实图片类型
     */
    private String detectImageType(byte[] bytes) {
        if (bytes.length < 12) {
            return null;
        }
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "png";
        }
        // WebP: RIFF....WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private boolean extensionMatches(String extension, String imageType) {
        if ("jpg".equals(imageType)) {
            return "jpg".equals(extension) || "jpeg".equals(extension);
        }
        return imageType.equals(extension);
    }

    private String contentTypeOf(String imageType) {
        switch (imageType) {
            case "jpg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            default:
                return null;
        }
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

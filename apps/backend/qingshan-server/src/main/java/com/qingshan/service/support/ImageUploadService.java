package com.qingshan.service.support;

import com.qingshan.constant.MessageConstant;
import com.qingshan.exception.BaseException;
import com.qingshan.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传校验与 OSS 上传，管理端和用户端共用
 */
@Service
@Slf4j
public class ImageUploadService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

    private final AliOssUtil aliOssUtil;

    public ImageUploadService(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    /**
     * 校验图片并上传
     * @param file 上传的文件
     * @param objectDir OSS 目录前缀，如 product/cover/
     * @return 文件访问路径
     */
    public String uploadImage(MultipartFile file, String objectDir) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BaseException(MessageConstant.UPLOAD_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BaseException(MessageConstant.UPLOAD_SIZE_EXCEEDED);
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(MessageConstant.UPLOAD_TYPE_NOT_ALLOWED);
        }

        try {
            byte[] bytes = file.getBytes();
            // 以文件头识别的真实类型为准，客户端 Content-Type 和扩展名都不可信
            String imageType = detectImageType(bytes);
            if (imageType == null || !extensionMatches(extension, imageType)) {
                throw new BaseException(MessageConstant.UPLOAD_TYPE_NOT_ALLOWED);
            }

            String prefix = objectDir == null ? "" : objectDir;
            String objectName = prefix + UUID.randomUUID() + "." + imageType;
            return aliOssUtil.upload(bytes, objectName, contentTypeOf(imageType));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败，filename={}", originalFilename, e);
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
    }

    /**
     * 管理端商品图片目录
     * @param type 图片类型：1封面图 2样式图 3详情长图，不传时上传到根目录
     * @return OSS 对象目录前缀
     */
    public String productDir(Integer type) {
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
}

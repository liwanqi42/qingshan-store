package com.qingshan.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        return upload(bytes, objectName, null);
    }

    /**
     * 文件上传，并设置浏览器/CDN 使用的 Content-Type
     */
    public String upload(byte[] bytes, String objectName, String contentType) {

        // 创建OSSClient实例。
        // TODO(prod): 不要使用主账号 AccessKey，改用只有该 Bucket 写权限的 RAM 子账号，
        //   条件允许时改用 STS 临时凭证；Bucket 建议私有读 + CDN 签名，并配置防盗链
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentType != null) {
                metadata.setContentType(contentType);
            }
            metadata.setCacheControl("public, max-age=31536000");
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes), metadata);
        } catch (OSSException oe) {
            log.error("OSS 上传失败：code={}, requestId={}, message={}",
                    oe.getErrorCode(), oe.getRequestId(), oe.getErrorMessage());
            throw new IllegalStateException("文件上传失败", oe);
        } catch (ClientException ce) {
            log.error("OSS 客户端异常", ce);
            throw new IllegalStateException("文件上传失败", ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}

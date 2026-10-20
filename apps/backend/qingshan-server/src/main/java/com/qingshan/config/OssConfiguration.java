package com.qingshan.config;

import com.qingshan.properties.AliOssProperties;
import com.qingshan.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){
        // 只记录非敏感配置，禁止把 AccessKeySecret 打进日志
        log.info("初始化阿里云 OSS：endpoint={}, bucket={}",
                aliOssProperties.getEndpoint(), aliOssProperties.getBucketName());
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName()
                );
    }
}

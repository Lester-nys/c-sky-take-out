package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类：用于创建AliOssUtil对象
 * 因为util这个工具类需要先配置好各种属性（服务器的地址，访问服务器的钥匙和密码）
 * 就通过这个configuration来进行配置
 * */
@Configuration
@Slf4j
public class OssConfiguration {
    @Bean                       //  将这个注册为bean  否则后续无法读取
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties)
    {
        log.info("开始创建阿里云文件上传工具类对象：{}",aliOssProperties);
        // 提前把需要的属性都配置好
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName()
        );
    }
}

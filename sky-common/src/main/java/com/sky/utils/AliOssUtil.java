
package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 上传图片到OSS
     *
     * @param bytes 图片字节数据
     * @param objectName 图片文件名
     * @return 后端图片访问地址
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(
                endpoint,
                accessKeyId,
                accessKeySecret
        );

        try {
            // 将图片上传到OSS
            ossClient.putObject(
                    bucketName,
                    objectName,
                    new ByteArrayInputStream(bytes)
            );

            // 不再返回OSS的私有文件地址
            // 返回前端能够访问的后端图片接口地址
            String filePath = "/api/common/image/" + objectName;

            log.info("OSS上传成功，文件名：{}", objectName);

            return filePath;

        } catch (OSSException e) {
            log.error("OSS上传失败", e);
            throw e;

        } catch (ClientException e) {
            log.error("OSS客户端异常", e);
            throw e;

        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 从OSS读取图片
     *
     * @param objectName 图片文件名
     * @return 图片字节数组
     */
    public byte[] download(String objectName) throws IOException {

        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(
                endpoint,
                accessKeyId,
                accessKeySecret
        );

        try {
            // 从OSS中获取图片对象
            OSSObject ossObject = ossClient.getObject(
                    bucketName,
                    objectName
            );

            // 读取图片内容
            try (InputStream inputStream =
                         ossObject.getObjectContent()) {

                return inputStream.readAllBytes();
            }

        } finally {
            // 关闭OSS客户端
            ossClient.shutdown();
        }
    }
}

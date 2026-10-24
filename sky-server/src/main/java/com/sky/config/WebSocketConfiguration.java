package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 *
 * 作用：
 * 将 @ServerEndpoint 标记的 WebSocket 服务端
 * 注册到当前内嵌的 Web 容器中
 */
@Configuration
public class WebSocketConfiguration {

    /**
     * @Bean：
     * 将方法返回的对象交给 Spring IoC 容器管理
     *
     * ServerEndpointExporter：
     * 负责注册 WebSocket 端点
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

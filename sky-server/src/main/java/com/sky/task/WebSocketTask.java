
package com.sky.task;

import com.sky.websocket.WebSocketServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket 定时推送任务
 */
@Component
public class WebSocketTask {

    /**
     * 注入 WebSocketServer 对象
     *
     * 通过它来调用群发消息的方法
     */
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 每隔5秒向全部客户端推送消息
     *
     * cron 表达式：
     * 0/5 * * * * ?
     *
     * 表示每分钟的第0、5、10、15...秒执行
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void sendMessageToClient() {

        // 获取当前时间，并格式化为 HH:mm:ss
        String time = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .format(LocalDateTime.now());

        // 组装推送消息
        String message = "这是来自服务端的消息：" + time;

        // 主动向所有已连接的客户端推送消息
        webSocketServer.sendToAllClient(message);
    }
}

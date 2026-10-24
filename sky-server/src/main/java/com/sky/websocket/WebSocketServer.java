
package com.sky.websocket;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 服务端
 * 功能：
 * 1. 管理客户端连接
 * 2. 接收客户端发送的消息
 * 3. 主动向客户端推送消息
 */
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    /**
     * 保存所有客户端的 WebSocket 会话
     *
     * key：客户端的唯一标识 sid
     * value：客户端对应的 Session 对象
     *
     * Session 可以理解为服务器与某个客户端之间的连接会话。
     */
    private static Map<String, Session> sessionMap = new HashMap<>();

    /**
     * 客户端连接建立成功后，自动执行
     *
     * @OnOpen：监听连接建立事件
     *
     * @param session 当前客户端的会话对象
     * @param sid 客户端的唯一标识
     * 浏览器A ── SessionA ──┐
     * 浏览器B ── SessionB ──┼── WebSocketServer
     * 浏览器C ── SessionC ──┘
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {

        System.out.println("客户端：" + sid + "建立连接");

        // 保存客户端对应的 Session
        // 后续服务端可以通过 Session 向客户端发送消息
        sessionMap.put(sid, session);
    }

    /**
     * 接收到客户端消息时，自动执行
     *
     * @OnMessage：监听客户端发送消息的事件
     *
     * @param message 客户端发送过来的消息
     * @param sid 客户端唯一标识
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {

        System.out.println(
                "收到来自客户端：" + sid + "的信息：" + message
        );
    }

    /**
     * 客户端断开连接时，自动执行
     *
     * @OnClose：监听连接关闭事件
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {

        System.out.println("连接断开：" + sid);

        // 删除已经断开的客户端会话
        sessionMap.remove(sid);
    }

    /**
     * 向所有已连接的客户端群发消息
     *
     * @param message 需要发送的消息内容
     */
    public void sendToAllClient(String message) {

        // 获取 Map 中保存的所有 Session
        Collection<Session> sessions = sessionMap.values();

        // 遍历全部连接
        for (Session session : sessions) {
            try {
                // 获取发送通道并发送文本消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

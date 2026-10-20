package com.qingshan.websocket;

import com.alibaba.fastjson2.JSON;
import com.qingshan.entity.Orders;
import com.qingshan.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    //存放会话对象，onOpen/onClose 与群发在不同线程执行，必须用线程安全的Map
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 每个连接由WebSocket容器新建实例，Spring注入的依赖需放到静态字段供所有实例共用
     */
    private static OrderMapper orderMapper;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        WebSocketServer.orderMapper = orderMapper;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("客户端：{}建立连接", sid);
        sessionMap.put(sid, session);
        // 没有员工在线期间支付的订单不会再推送，登录建立连接后补发一次待接单提醒
        sendPendingOrderNotice(session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端：{}的信息:{}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("连接断开:{}", sid);
        sessionMap.remove(sid);
    }

    /**
     * 向刚建立连接的客户端补发待接单提醒
     * 消息格式：{"type":2,"count":3,"content":"有3个订单待接单"}
     */
    private void sendPendingOrderNotice(Session session) {
        try {
            Integer pending = orderMapper.countStatus(Orders.STATUS_AWAITING_SHIPMENT);
            if (pending == null || pending <= 0) {
                return;
            }
            Map<String, Object> message = new HashMap<>();
            message.put("type", 2);
            message.put("count", pending);
            message.put("content", "有" + pending + "个订单待接单");
            session.getAsyncRemote().sendText(JSON.toJSONString(message));
        } catch (Exception e) {
            log.warn("推送待接单提醒失败", e);
        }
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        // getBasicRemote 不是线程安全的，多线程群发时用异步发送
        sessionMap.forEach((sid, session) -> {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                // 发送失败说明会话已失效，移除避免后续重复报错
                sessionMap.remove(sid, session);
            }
        });
    }

}

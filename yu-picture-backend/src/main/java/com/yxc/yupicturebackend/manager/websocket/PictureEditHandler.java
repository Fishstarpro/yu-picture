package com.yxc.yupicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.yxc.yupicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.yxc.yupicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.yxc.yupicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.yxc.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yxc.yupicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: PictureEditHandler
 * Package: com.yxc.yupicturebackend.manager.websocket
 * Description:
 *
 * @Author fishstar
 * @Create 2025/4/24 17:38
 * @Version 1.0
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();
    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        //1.保存会话到集合中
        User user = (User) session.getAttributes().get("user");

        Long pictureId = (Long) session.getAttributes().get("pictureId");

        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());

        pictureSessions.get(pictureId).add(session);
        //2.构造响应,给所有用户发送当前用户加入编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();

        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());

        String message = String.format("用户 %s 加入编辑", user.getUserName());

        pictureEditResponseMessage.setMessage(message);

        pictureEditResponseMessage.setUser(userService.getUserVO(user));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        //1.获取消息
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);

        User user = (User) session.getAttributes().get("user");

        Long pictureId = (Long) session.getAttributes().get("pictureId");
        //2.通过消息生产者将消息发送到Disruptor环形队列中
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    /**
     * 处理用户加入编辑
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //1.没有用户编辑图片,才能进入编辑状态
        if (!pictureEditingUsers.containsKey(pictureId)) {
            pictureEditingUsers.put(pictureId, user.getId());
            //2.构造响应,广播给所有用户
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            broadcastToUser(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理编辑操作
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //1.发送编辑操作请求的用户必须是正在编辑的用户
        Long editUserId = pictureEditingUsers.get(pictureId);

        String editAction = pictureEditRequestMessage.getEditAction();

        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);

        if (pictureEditActionEnum == null) {
            log.error("无效的编辑动作");

            return;
        }

        if (editUserId != null && editUserId.equals(user.getId())) {
            //2.构造响应,广播给所有用户
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();

            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());

            String message = String.format("%s 执行 %s", user.getUserName(), pictureEditActionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastToUser(pictureId, pictureEditResponseMessage, session);
        }

    }

    /**
     * 退出编辑状态
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //1.退出编辑状态的用户必须是正在编辑的用户
        Long editUserId = pictureEditingUsers.get(pictureId);

        if (editUserId != null && editUserId.equals(user.getId())) {
            pictureEditingUsers.remove(pictureId);
            //2.构造响应,广播给所有用户
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();

            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());

            String message = String.format("用户 %s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            broadcastToUser(pictureId, pictureEditResponseMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        //1.从集合中移除会话,并广播给所有用户
        Long pictureId = (Long) session.getAttributes().get("pictureId");

        User user = (User) session.getAttributes().get("user");
        //移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);

        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);

        if (webSocketSessions != null) {
            webSocketSessions.remove(session);

            if (CollUtil.isEmpty(webSocketSessions)) {
                pictureSessions.remove(pictureId);
            }
        }

        // 通知其他用户，该用户已经离开编辑
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToUser(pictureId, pictureEditResponseMessage);
    }

    /**
     * 广播给该图片的所有用户（支持排除掉某个 Session）
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     */
    private void broadcastToUser(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        //1.获取该图片所有连接的会话
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);

        if (CollUtil.isNotEmpty(sessions)) {
            //2.解决发送给前端时的Long精度丢失问题
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            //3.遍历发送消息
            for (WebSocketSession session : sessions) {
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }

                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播给该图片的所有用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToUser(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToUser(pictureId, pictureEditResponseMessage, null);
    }
}


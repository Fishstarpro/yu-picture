package com.yxc.yupicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.yxc.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yxc.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yxc.yupicturebackend.model.entity.Picture;
import com.yxc.yupicturebackend.model.entity.Space;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yxc.yupicturebackend.service.PictureService;
import com.yxc.yupicturebackend.service.SpaceService;
import com.yxc.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * ClassName: WsHandshakeInterceptor
 * Package: com.yxc.yupicturebackend.manager.websocket
 * Description:WebSocket拦截器,建立连接前先校验
 *
 * @Author fishstar
 * @Create 2025/4/24 15:10
 * @Version 1.0
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //1.从请求中获取pictureId
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            String pictureId = httpServletRequest.getParameter("pictureId");

            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数,拒绝握手");

                return false;
            }
            //2.判断当前登录用户是否有图片编辑权限
            User loginUser = userService.getLoginUser(httpServletRequest);

            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录,拒绝握手");

                return false;
            }

            Picture picture = pictureService.getById(pictureId);

            if (ObjUtil.isEmpty(picture)) {
                log.error("图片不存在,拒绝握手");

                return false;
            }

            Long spaceId = picture.getSpaceId();

            Space space;

            if (ObjUtil.isEmpty(spaceId)) {
                log.error("公共图库,拒绝握手");

                return false;
            } else {
                space = spaceService.getById(spaceId);

                if (ObjUtil.isEmpty(space)) {
                    log.error("图片空间不存在,拒绝握手");

                    return false;
                }

                if (!space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                    log.error("非团队空间,拒绝握手");

                    return false;
                }

                List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);

                if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                    log.error("无编辑权限,拒绝握手");

                    return false;
                }
                //3.将用户信息设置到attributes中
                attributes.put("user", loginUser);
                attributes.put("userId", loginUser.getId());
                attributes.put("pictureId", Long.valueOf(pictureId));
            }

            return true;
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}

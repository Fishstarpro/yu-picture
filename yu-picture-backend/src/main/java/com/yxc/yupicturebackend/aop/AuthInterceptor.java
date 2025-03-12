package com.yxc.yupicturebackend.aop;

import com.yxc.yupicturebackend.annotation.AuthCheck;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.entity.enums.UserRoleEnum;
import com.yxc.yupicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * ClassName: AuthInterceptor
 * Package: com.yxc.yupicturebackend.aop
 * Description:
 *
 * @Author fishstar
 * @Create 2025/3/12 13:41
 * @Version 1.0
 */
/**
 * 权限拦截器
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doAuthCheck(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //1.获取请求
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();

        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //2.获取当前用户
        User loginUser = userService.getLoginUser(request);
        //3.如果不需要权限,直接放行
        if (authCheck.mustRole().equals("")) {
            return joinPoint.proceed();
        }
        //4.判断是否有相关权限
        //用户无任何权限,直接报错
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //需要管理员权限,但用户不具有管理员权限,直接报错
        if (authCheck.mustRole().equals("admin") && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //5,通过权限校验,放行
        return joinPoint.proceed();
    }
}

package com.yxc.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxc.yupicturebackend.constant.UserConstant;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.exception.ThrowUtils;
import com.yxc.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.entity.enums.UserRoleEnum;
import com.yxc.yupicturebackend.model.vo.LoginUserVO;
import com.yxc.yupicturebackend.model.vo.UserVO;
import com.yxc.yupicturebackend.service.UserService;
import com.yxc.yupicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author fishstar
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-03-11 00:14:32
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "参数为空");

        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");

        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");

        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");
        //2.判断账户是否和数据库已有的冲突
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("userAccount", userAccount);

        long count = this.count(queryWrapper);

        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号已存在");
        //3.对密码加密
        String encryptPassword = this.getEncryptPassword(userPassword);
        //4.插入数据到数据库
        User user = new User();

        user.setUserAccount(userAccount);

        user.setUserPassword(encryptPassword);

        user.setUserName("无名");

        user.setUserRole(UserRoleEnum.USER.getValue());

        boolean saveResult = this.save(user);

        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败");
        //5.返回用户id
        return user.getId();
    }

    /**
     * 密码加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String password) {
        //加盐
        String SALT = "fishstar";

        //加密
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空");

        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");

        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        //2.获得加密后的密码
        String encryptPassword = this.getEncryptPassword(userPassword);
        //3.查找是否存在用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(User::getUserAccount, userAccount).eq(User::getUserPassword, encryptPassword);

        User user = this.getOne(queryWrapper);

        if (user == null) {
            log.info("user login failed, userAccount is not exist or password is wrong");

            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        //4.保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        return this.getLoginUesrVO(user);
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUesrVO(User user) {
        if (user == null) {
            return null;
        }

        LoginUserVO loginUserVO = new LoginUserVO();

        BeanUtil.copyProperties(user, loginUserVO);

        return loginUserVO;
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();

        BeanUtil.copyProperties(user, userVO);

        return userVO;    }

    /**
     * 获取脱敏的用户信息列表
     *
     * @param users 用户列表
     * @return 脱敏后的用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> users) {
        if (CollUtil.isEmpty(users)) {
            return new ArrayList<>();
        }

        return users.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //1.从session中获取用户信息
        User user = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        //2.判断是否为空
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //3.从数据库中再查一遍(防止用户信息被篡改)
        Long userId = user.getId();

        user = this.getById(userId);

        //用户可能违规被删除
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //4.返回用户信息
        return user;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        User user = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);

        if (user == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }

        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        return true;
    }

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);

        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);

        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);

        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);

        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);

        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }


}





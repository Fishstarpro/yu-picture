package com.yxc.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;


/**
 * 用户登录请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 6813265812413484594L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}

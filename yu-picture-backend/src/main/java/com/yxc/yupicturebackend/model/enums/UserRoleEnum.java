package com.yxc.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.HashMap;

/**
 * ClassName: UserRoleEnum
 * Package: com.yxc.yupicturebackend.model.entity.enums
 * Description:
 *
 * @Author fishstar
 * @Create 2025/3/11 0:26
 * @Version 1.0
 */

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    private static HashMap<String, UserRoleEnum> map = new HashMap<>();

    //类加载时将对象放入map中
    static {
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            map.put(userRoleEnum.getValue(), userRoleEnum);
        }
    }

    UserRoleEnum(String text, String value) {
        this.text = text;

        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }

        //当对象过多时,这种查找效率低
        /*for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.getValue().equals(value)) {
                return userRoleEnum;
            }
        }*/

        //将对象放入一个map中,通过map判断value返回对象
        if (map.containsKey(value)) {
            return map.get(value);
        }

        return null;
    }
}

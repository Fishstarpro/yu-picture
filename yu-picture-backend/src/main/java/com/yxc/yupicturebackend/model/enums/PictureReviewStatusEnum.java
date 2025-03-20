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
 * 图片审核状态枚举
 */
@Getter
public enum PictureReviewStatusEnum {

    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;

    private final int value;

    private static HashMap<Integer, PictureReviewStatusEnum> map = new HashMap<>();

    //类加载时将对象放入map中
    static {
        for (PictureReviewStatusEnum userRoleEnum : PictureReviewStatusEnum.values()) {
            map.put(userRoleEnum.getValue(), userRoleEnum);
        }
    }

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;

        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
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

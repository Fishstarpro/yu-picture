package com.yxc.yupicturebackend.model.dto.picture;

/**
 * ClassName: PictureReviewRequest
 * Package: com.yxc.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author fishstar
 * @Create 2025/3/18 0:25
 * @Version 1.0
 */

import lombok.Data;

import java.io.Serializable;

/**
 * 图片审核请求
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;
}
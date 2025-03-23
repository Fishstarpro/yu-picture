package com.yxc.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxc.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yxc.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author yxc63
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-03-15 15:42:15
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 校验图片
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 获取图片包装类(单条)
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类(分页)
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 图片批量抓取和上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 从缓存中获取图片分页
     * @param pictureQueryRequest
     * @param request
     * @param current
     * @param size
     * @return
     */
    Page<PictureVO> getPictureVOPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request, long current, long size);

    /**
     * 清除图片文件
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);
}

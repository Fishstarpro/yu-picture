package com.yxc.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxc.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yxc.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author yxc63
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-03-15 15:42:15
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param file
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile file, PictureUploadRequest pictureUploadRequest, User loginUser);

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
}

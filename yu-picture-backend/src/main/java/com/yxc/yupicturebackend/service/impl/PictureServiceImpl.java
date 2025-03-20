package com.yxc.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.exception.ThrowUtils;
import com.yxc.yupicturebackend.manager.FileManager;
import com.yxc.yupicturebackend.manager.upload.FilePictureUpload;
import com.yxc.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yxc.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yxc.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yxc.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yxc.yupicturebackend.model.entity.Picture;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yxc.yupicturebackend.model.vo.PictureVO;
import com.yxc.yupicturebackend.model.vo.UserVO;
import com.yxc.yupicturebackend.service.PictureService;
import com.yxc.yupicturebackend.mapper.PictureMapper;
import com.yxc.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author yxc63
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-03-15 15:42:15
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{


    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        //2.根据用户id划分目录
        String prefix = String.format("public/%s", loginUser.getId());
        //3.上传图片
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;

        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, prefix);
        //4.封装存入数据库的图片信息
        Picture picture = new Picture();

        picture.setUrl(uploadPictureResult.getUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();

        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //补充审核参数
        fillReviewParams(picture, loginUser);
        //5.判断是新增还是更新
        //**更新
        if (pictureUploadRequest.getId() != null) {
            //获取老图
            Picture oldPicture = this.getById(pictureUploadRequest.getId());

            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //如果不是本人或者不是管理员不能修改
            if (!loginUser.getId().equals(pictureUploadRequest.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            picture.setId(pictureUploadRequest.getId());

            picture.setEditTime(new Date());
        }

        boolean result = this.saveOrUpdate(picture);

        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "数据库上传图片失败");
        //返回图片信息
        return PictureVO.objToVo(picture);
    }

    /**
     * 校验图片
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        //取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //id不为空
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");        //有url则校验
        //有url则校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
            //有introduction则校验
            if (StrUtil.isNotBlank(introduction)) {
                ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
            }
        }
    }

    /**
     * 获取图片包装类(单条)
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //获取封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联查询并设置用户信息
        Long userId = pictureVO.getUserId();

        if (userId != null) {
            User user = userService.getById(userId);

            UserVO userVO = userService.getUserVO(user);

            pictureVO.setUser(userVO);
        }
        //返回包装信息
        return pictureVO;

    }

    /**
     * 获取图片包装类(分页)
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        //获取包装后的图片列表
        List<Picture> pictureList = picturePage.getRecords();

        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());

        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //为每个图片设置用户信息
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 如果一条一条query，可能会有性能问题,所以一次性查完,放到一个map中
        Set<Long> userIdSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());

        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        pictureVOList.stream().forEach(pictureVO -> {
            if (userIdUserListMap.containsKey(pictureVO.getUserId())) {
                User user = userIdUserListMap.get(pictureVO.getUserId()).get(0);

                UserVO userVO = userService.getUserVO(user);

                pictureVO.setUser(userVO);
            }
        });
        //返回
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    /**
     * 获取查询条件
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        //管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);

        Long id = pictureReviewRequest.getId();

        Integer reviewStatus = pictureReviewRequest.getReviewStatus();

        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        // 如果id为null或审核状态不存在抛异常
        if (id == null || reviewStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);

        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //判断审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //数据库操作
        Picture picture = new Picture();

        BeanUtil.copyProperties(pictureReviewRequest, picture);

        picture.setReviewerId(loginUser.getId());

        picture.setReviewTime(new Date());

        boolean result = this.updateById(picture);

        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 图片批量抓取和上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();

        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多获取30张图片");
        //名称前缀默认等于搜索词
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        //2.抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);

        Document document;

        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失");
        }
        //3.解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        //4.上传图片
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;

        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");

            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            //处理图片地址,防止转义或和对象存储冲突,把?后面的去掉
            int index = fileUrl.indexOf("?");

            if (index > -1) {
                fileUrl = fileUrl.substring(0, index);
            }

            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();

            pictureUploadRequest.setFileUrl(fileUrl);

            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));

            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);

                log.info("图片上传成功，id = {}", pictureVO.getId());

                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);

                continue;
            }

            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }
}





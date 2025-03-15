package com.yxc.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.exception.ThrowUtils;
import com.yxc.yupicturebackend.manager.FileManager;
import com.yxc.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yxc.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yxc.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yxc.yupicturebackend.model.entity.Picture;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.vo.PictureVO;
import com.yxc.yupicturebackend.model.vo.UserVO;
import com.yxc.yupicturebackend.service.PictureService;
import com.yxc.yupicturebackend.mapper.PictureMapper;
import com.yxc.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     * @param file
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile file, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        //根据用户id划分目录
        String prefix = String.format("public/%s", loginUser.getId());
        //上传图片
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(file, prefix);
        //封装存入数据库的图片信息
        Picture picture = new Picture();

        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 判断是新增还是更新
        //   更新
        if (pictureUploadRequest.getId() != null) {
            //判断图片是否存在
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureUploadRequest.getId()).exists();

            ThrowUtils.throwIf(exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

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
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
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
}





package com.yxc.yupicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.exception.ThrowUtils;
import com.yxc.yupicturebackend.mapper.SpaceMapper;
import com.yxc.yupicturebackend.model.dto.space.analyze.*;
import com.yxc.yupicturebackend.model.entity.Picture;
import com.yxc.yupicturebackend.model.entity.Space;
import com.yxc.yupicturebackend.model.entity.User;
import com.yxc.yupicturebackend.model.vo.space.analyze.*;
import com.yxc.yupicturebackend.service.PictureService;
import com.yxc.yupicturebackend.service.SpaceAnalyzeService;
import com.yxc.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: SpaceAnalyzeServiceImpl
 * Package: com.yxc.yupicturebackend.service.impl
 * Description:
 *
 * @Author fishstar
 * @Create 2025/4/13 17:56
 * @Version 1.0
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 空间分析权限校验
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        //1.如果是公共空间或全空间分析,只能管理员操作
        if (spaceAnalyzeRequest.isQueryPublic() || spaceAnalyzeRequest.isQueryAll()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }
        //2.如果是私有空间,只能本人或管理员操作
        else {
            Long spaceId = spaceAnalyzeRequest.getSpaceId();

            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);

            Space space = this.getById(spaceId);

            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            if (!userService.isAdmin(loginUser) && !space.getUserId().equals(loginUser.getId())) {
                ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 填充查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        //1.全空间分析
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        } else if (spaceAnalyzeRequest.isQueryPublic()) {
            //2.公共图库
            queryWrapper.isNull("spaceId");

            return;
        } else {
            //3.私有图库
            Long spaceId = spaceAnalyzeRequest.getSpaceId();

            if (spaceId != null) {
                queryWrapper.eq("spaceId", spaceId);

                return;
            }
        }

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 空间资源使用分析
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //1.校验权限
        this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        //2.如果为公共图库或全空间分析,就去picture表查询
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();

        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            //填充查询条件
            this.fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);

            queryWrapper.select("picSize");

            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);

            long usedSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();

            long usedCount = pictureObjList.size();

            spaceUsageAnalyzeResponse.setUsedSize(usedSize);

            spaceUsageAnalyzeResponse.setUsedCount(usedCount);

            return spaceUsageAnalyzeResponse;
        }
        //3.如果为私有图库,就去space表查询
        else {
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();

            Space space = this.getById(spaceId);

            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());

            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());

            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());

            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            //计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();

            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();

            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);

            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
        }
        //4.返回结果
        return spaceUsageAnalyzeResponse;
    }

    /**
     * 空间分类分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        //1.校验权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        //2.构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        this.fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        //3.返回结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(map -> {
                    SpaceCategoryAnalyzeResponse spaceCategoryAnalyzeResponse = new SpaceCategoryAnalyzeResponse();

                    spaceCategoryAnalyzeResponse.setCategory((String) map.get("category"));

                    spaceCategoryAnalyzeResponse.setCount(((Number) map.get("count")).longValue());

                    spaceCategoryAnalyzeResponse.setTotalSize(((Number) map.get("totalSize")).longValue());

                    return spaceCategoryAnalyzeResponse;
                })
                .collect(Collectors.toList());
    }

    /**
     * 空间标签分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        //1.校验权限
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        //2.构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        this.fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        queryWrapper.select("tags");
        //3.返回结果
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        Map<String, Long> tagCountMap = tagJsonList.stream()
                //["tag1","tag2"], ["tag3"] => ["tag1","tag2","tag3"]
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        return tagCountMap.entrySet()
                .stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 空间图片大小分析
     *
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        //1.校验权限
        this.checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        //2.构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        this.fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        queryWrapper.select("picSize");
        //3.返回结果
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> (Long) size)
                .collect(Collectors.toList());
        //定义分段范围,使用有序的map
        LinkedHashMap<String, Long> sizeRanges = new LinkedHashMap<>();

        sizeRanges.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());

        sizeRanges.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());

        sizeRanges.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());

        sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 空间用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        //1.校验权限
        this.checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //2.构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        this.fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        queryWrapper.eq(ObjUtil.isNotNull(spaceUserAnalyzeRequest.getUserId()), "userId", spaceUserAnalyzeRequest.getUserId());
        //3.返回结果
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();

        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
                break;
        }

        queryWrapper.groupBy("period");

        return pictureService.getBaseMapper().selectMaps(queryWrapper)
               .stream()
               .map(map -> new SpaceUserAnalyzeResponse(map.get("period").toString(), (Long) map.get("count")))
               .collect(Collectors.toList());
    }

    /**
     * 空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */ 
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        //1.校验权限
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        //2.构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();

        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("limit " + spaceRankAnalyzeRequest.getTopN());
        //3.返回结果
        return this.list(queryWrapper);
    }
}

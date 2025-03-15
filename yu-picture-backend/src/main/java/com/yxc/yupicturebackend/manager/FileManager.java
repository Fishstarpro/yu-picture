package com.yxc.yupicturebackend.manager;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yxc.yupicturebackend.common.ResultUtils;
import com.yxc.yupicturebackend.config.CosClientConfig;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.exception.ThrowUtils;
import com.yxc.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * ClassName: FileManager
 * Package: com.yxc.yupicturebackend.manager
 * Description:
 *
 * @Author fishstar
 * @Create 2025/3/15 15:43
 * @Version 1.0
 */
@Component
@Slf4j
public class FileManager {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     * @param multipartFile
     * @param prefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String prefix) {
        //校验图片
        validPicture(multipartFile);
        //设置图片的路径
        // 自定义文件名称
        String uuid = RandomUtil.randomString(16);

        String originalFilename = multipartFile.getOriginalFilename();

        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        //  自定义路径
        String filePath = String.format("/%s/%s", prefix, fileName);
        //上传图片
        // 创建临时文件
        File file = null;

        try {
            file = File.createTempFile(filePath, null);

            multipartFile.transferTo(file);
            //  上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(filePath, file);
            //封装图片返回信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            String format = imageInfo.getFormat();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            int quality = imageInfo.getQuality();
            String ave = imageInfo.getAve();
            int orientation = imageInfo.getOrientation();
            int frameCount = imageInfo.getFrameCount();

            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();

            uploadPictureResult.setUrl(cosClientConfig.getHost() + filePath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);

            return uploadPictureResult;
        } catch (IOException e) {
            log.error("file upload fail", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file upload fail");
        } finally {
            //删除临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验图片
     * @param file
     */
    private void validPicture(MultipartFile file) {
        //文件不能为空
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //校验文件大小
        long fileSize = file.getSize();

        final long ONE_MB = 1024 * 1024;

        ThrowUtils.throwIf(fileSize > ONE_MB * 2, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        // 允许的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "gif", "webp");

        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    public void deleteTempFile(File file) {
        if (file != null) {
            boolean isDelete = file.delete();

            if (!isDelete) {
                log.error("file delete fail, filePath = {}", file.getAbsolutePath());
            }
        }
    }
}

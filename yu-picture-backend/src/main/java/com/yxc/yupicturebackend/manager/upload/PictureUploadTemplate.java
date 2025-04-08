package com.yxc.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yxc.yupicturebackend.config.CosClientConfig;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.manager.CosManager;
import com.yxc.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
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
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     *
     * @param inputSource
     * @param prefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String prefix) {
        //1.校验输入源
        validPicture(inputSource);
        //2.设置图片的路径
        String uuid = RandomUtil.randomString(16);
        // 获取原始文件名
        String originalFilename = getOriginFilename(inputSource);
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", prefix, uploadFilename);
        //3.上传图片
        //创建临时文件
        File file = null;

        try {
            file = File.createTempFile(uploadPath, null);

            //设置文件内容
            setFileContent(inputSource, file);
            //上传文件到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取压缩后的文件
            List<CIObject> ciObjectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();

            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            if (CollUtil.isNotEmpty(ciObjectList)) {
                CIObject compressedCiObject = ciObjectList.get(0);

                CIObject thumbnailCiObject = compressedCiObject;

                if (ciObjectList.size() > 1) {
                    thumbnailCiObject = ciObjectList.get(1);
                }
                //封装图片返回信息
                return getUploadPictureResult(imageInfo, originalFilename, compressedCiObject, thumbnailCiObject);
            }
            //封装图片返回信息
            return getUploadPictureResult(imageInfo, originalFilename, file, uploadPath);
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
     *
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);


    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 设置文件内容
     *
     * @param inputSource
     * @param file
     * @throws IOException
     */
    protected abstract void setFileContent(Object inputSource, File file) throws IOException;

    /**
     * 封装图片返回信息
     *
     * @param imageInfo
     * @param originalFilename
     * @param file
     * @param uploadPath
     * @return
     */
    private UploadPictureResult getUploadPictureResult(ImageInfo imageInfo, String originalFilename, File file, String uploadPath) {
        String format = imageInfo.getFormat();
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();

        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicColor(imageInfo.getAve());

        return uploadPictureResult;
    }

    /**
     * 封装图片返回信息
     *
     * @param imageInfo
     * @param originalFilename
     * @param compressedCiObject
     * @param thumbnailCiObject
     * @return
     */
    private UploadPictureResult getUploadPictureResult(ImageInfo imageInfo, String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject) {
        String format = compressedCiObject.getFormat();
        int width = compressedCiObject.getWidth();
        int height = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();

        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" +  compressedCiObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicColor(imageInfo.getAve());

        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file != null) {
            boolean isDelete = file.delete();

            if (!isDelete) {
                log.error("file delete fail, filePath = {}", file.getAbsolutePath());
            }
        }
    }
}

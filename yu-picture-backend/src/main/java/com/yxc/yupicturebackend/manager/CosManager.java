package com.yxc.yupicturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yxc.yupicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * ClassName: CosManager
 * Package: com.yxc.yupicturebackend.manager
 * Description:
 *
 * @Author fishstar
 * @Create 2025/3/14 22:31
 * @Version 1.0
 */
@Component
public class CosManager {
    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传文件
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载文件
     * @param key
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片(返回图片信息)
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        // 设置返回图片信息
        PicOperations picOperations = new PicOperations();

        // 开启返回图片信息
        picOperations.setIsPicInfo(1);

        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

}

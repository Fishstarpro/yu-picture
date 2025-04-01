package com.yxc.yupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yxc.yupicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        //1.设置返回图片信息
        PicOperations picOperations = new PicOperations();

        //开启返回图片信息
        picOperations.setIsPicInfo(1);
        //2.设置图片压缩
        List<PicOperations.Rule> list = new ArrayList<>();

        PicOperations.Rule compressRule = new PicOperations.Rule();
        //设置图片名称
        compressRule.setFileId(FileUtil.mainName(key) + ".webp");
        //设置存储桶
        compressRule.setBucket(cosClientConfig.getBucket());
        //设置图片处理规则
        compressRule.setRule("imageMogr2/format/webp");

        list.add(compressRule);
        //3.设置缩略图
        //仅对大于20KB的图片进行缩略
        if (file.length() > 20 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();

            thumbnailRule.setFileId(FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key));

            thumbnailRule.setBucket(cosClientConfig.getBucket());

            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));

            list.add(thumbnailRule);
        }
        //使用规则
        picOperations.setRules(list);

        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除文件
     * @param key
     * @return
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}

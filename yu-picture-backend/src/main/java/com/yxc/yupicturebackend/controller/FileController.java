package com.yxc.yupicturebackend.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ByteUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.yxc.yupicturebackend.annotation.AuthCheck;
import com.yxc.yupicturebackend.common.BaseResponse;
import com.yxc.yupicturebackend.common.ResultUtils;
import com.yxc.yupicturebackend.constant.UserConstant;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import com.yxc.yupicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * ClassName: FileController
 * Package: com.yxc.yupicturebackend.controller
 * Description:
 *
 * @Author fishstar
 * @Create 2025/3/14 22:37
 * @Version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试上传文件
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //设置文件上传目录
        String originalFilename = multipartFile.getOriginalFilename();

        String filePath = String.format("/test/%s", originalFilename);

        //创建临时文件
        File file = null;

        try {
            file = File.createTempFile(filePath, null);

            multipartFile.transferTo(file);
        //上传文件
            cosManager.putObject(filePath, file);
        //返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("file upload fail", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file upload fail");
        } finally {
            //删除临时文件
            if (file != null) {
                boolean isDelete = file.delete();

                if (!isDelete) {
                    log.error("file delete fail, filePath = {}", filePath);
                }
            }
        }

    }

    /**
     * 测试下载文件
     * @param filePath
     * @param response
     * @throws IOException
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInputStream = null;

        try {
            //下载文件
            COSObject cosObject = cosManager.getObject(filePath);

            cosObjectInputStream = cosObject.getObjectContent();

            byte[] bytes = IoUtil.readBytes(cosObjectInputStream);
            //写入response
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);

            response.getOutputStream().write(bytes);
        } catch (Exception e) {
            log.error("file download fail", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file download fail");
        } finally {
            //关闭流
            if (cosObjectInputStream != null) {
                cosObjectInputStream.close();
            }
        }


    }

}

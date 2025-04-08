package com.yxc.yupicturebackend.api.imageSearch.sub;

/**
 * ClassName: GetImagePageUrlApi
 * Package: com.yxc.yupicturebackend.api.imageSearch.sub
 * Description:
 *
 * @Author fishstar
 * @Create 2025/4/2 15:04
 * @Version 1.0
 */

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址(step 1)
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        //1.构造请求参数
        // image: https%3A%2F%2Fwww.codefather.cn%2Flogo.png
        //tn: pc
        //from: pc
        //image_source: PC_UPLOAD_URL
        //sdkParams:
        Map<String, Object> formData = new HashMap<>();

        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        String acsToken = "1743568158203_1743653215755_Jj6lOJqaQigkYhPqmpvLkJwSBwzWPnCG+2weWx8XRY5ytJtMEczUTmcUStdTVGPVpH8ZjmrgCsj1rCDY5JMP91spOmV6mL8nIDKDX6CkjxgosvlIC/Zm85s15S3C2haxhX7em8Au5zELeMgInSWEl/9/1PUn+tUy69T3G1uKii7tT3DJhct2Zs/9gohAbFES0z1w7MYxBoV/CX42PVELmILpYZ5aMhhYsT45SSjqu7tvmhmZNB1BXd//0WuePKyhZNtVVQFIYn8L7RQbutR0TKMR62U/sEk2TIHxgahnBEKkJvTBEegoLZ3WoZOdvSvDtPA8qaUYNehgfJV4oCjSyfV+OOuAGai1qB8DlzbL2FTcCJBTvuWJHT2LGp3W5zF3C3PnShp6BUXWNLs/QqMnTtn99E2bAP8ffJGaK4lCRj8=";
        //获取当前时间戳
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        //2.发送请求
        try {
            HttpResponse httpResponse = HttpRequest.post(url).form(formData).header("Acs-Token", acsToken).timeout(10000).execute();
            //3.校验响应
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // {"status":0,"msg":"Success","data":{"url":"https://graph.baidu.com/sc","sign":"1262fe97cd54acd88139901734784257"}}
            String body = httpResponse.body();

            Map<String, Object> result = JSONUtil.toBean(body, Map.class);

            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            //4.获取结果
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            //对url进行解码
            String rawUrl = (String) data.get("url");

            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }
            //5.返回结果
            return searchResultUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + searchResultUrl);
    }
}

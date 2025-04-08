package com.yxc.yupicturebackend.api.imageSearch.sub;

/**
 * ClassName: GetImageFirstUrlApi
 * Package: com.yxc.yupicturebackend.api.imageSearch.sub
 * Description:
 *
 * @Author fishstar
 * @Create 2025/4/2 15:20
 * @Version 1.0
 */

import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取图片列表接口的Api(step 2)
 */
@Slf4j
public class GetImageFirstUrlApi {

    /**
     * 获取图片列表接口的Api
     * @param url
     * @return 图片列表接口的Api
     */
    public static String getImageFirstUrl(String url) {
        try {
            // 使用 Jsoup 获取 HTML 内容
            Document document = Jsoup.connect(url)
                    .timeout(5000)
                    .get();

            // 获取所有 <script> 标签
            Elements scriptElements = document.getElementsByTag("script");

            // 遍历找到包含 `firstUrl` 的脚本内容
            for (Element script : scriptElements) {
                String scriptContent = script.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    // 正则表达式提取 firstUrl 的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        // 处理转义字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到 url");
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }
}

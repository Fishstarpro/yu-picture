package com.yxc.yupicturebackend.api.imageSearch;

import com.yxc.yupicturebackend.api.imageSearch.model.ImageSearchResult;
import com.yxc.yupicturebackend.api.imageSearch.sub.GetImageFirstUrlApi;
import com.yxc.yupicturebackend.api.imageSearch.sub.GetImageListApi;
import com.yxc.yupicturebackend.api.imageSearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ClassName: ImageSearchApiFacade
 * Package: com.yxc.yupicturebackend.api.imageSearch
 * Description:
 *
 * @Author fishstar
 * @Create 2025/4/2 15:26
 * @Version 1.0
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }
}

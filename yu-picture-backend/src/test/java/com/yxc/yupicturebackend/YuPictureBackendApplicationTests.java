package com.yxc.yupicturebackend;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YuPictureBackendApplicationTests {

    @Test
    void contextLoads() {
        String longUrl = "https://fishstar-1329389649.cos.ap-chongqing.myqcloud.com/public/1/example.txt";
        String mainName = FileUtil.mainName(longUrl);
        System.out.println(mainName);

    }

}

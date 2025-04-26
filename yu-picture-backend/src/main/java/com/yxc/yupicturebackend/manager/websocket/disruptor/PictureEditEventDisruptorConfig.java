package com.yxc.yupicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * ClassName: PictureEditEventDisruptorConfig
 * Package: com.yxc.yupicturebackend.manager.websocket.disruptor
 * Description:
 *
 * @Author fishstar
 * @Create 2025/4/25 0:47
 * @Version 1.0
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 定义 ringBuffer 的大小
        int bufferSize = 1024 * 256;
        // 创建 disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 启动 disruptor
        disruptor.start();
        return disruptor;
    }
}

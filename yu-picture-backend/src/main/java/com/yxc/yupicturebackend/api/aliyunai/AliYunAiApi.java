package com.yxc.yupicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yxc.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yxc.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yxc.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yxc.yupicturebackend.exception.BusinessException;
import com.yxc.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";


    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        //1.校验参数
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "扩图参数为空");
        }
        //2.发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        //3.处理响应
        //try()保证了在try语句块执行完毕后，会自动关闭HttpResponse对象，释放资源。
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());

                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);

            if (createOutPaintingTaskResponse.getCode() != null) {
                String message = createOutPaintingTaskResponse.getMessage();

                log.error("请求异常：{}", message);

                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败, " + message);
            }

            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 查询创建的任务结果
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        //1.校验参数
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 为空");
        }
        //2.发送请求
        HttpRequest httpRequest = HttpRequest
                .get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey);
        //3.处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());

                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            GetOutPaintingTaskResponse getOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);

            return getOutPaintingTaskResponse;
        }
    }
}
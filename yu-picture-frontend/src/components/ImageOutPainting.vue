<template>
  <a-modal
    class="image-out-painting"
    v-model:visible="visible"
    title="AI 扩图"
    :footer="false"
    @cancel="closeModal"
  >
    <a-row gutter="16">
      <a-col span="12">
        <h4>原始图片</h4>
        <img :src="picture?.url" :alt="picture?.name" style="max-width: 100%" />
      </a-col>
      <a-col span="12">
        <h4>扩图结果</h4>
        <img
          v-if="resultImageUrl"
          :src="resultImageUrl"
          :alt="picture?.name"
          style="max-width: 100%"
        />
      </a-col>
    </a-row>
    <div style="margin-bottom: 16px" />
    <a-flex justify="center" gap="16">
      <a-button type="primary" :loading="!!taskId" ghost @click="createTask">生成图片</a-button>
      <a-button v-if="resultImageUrl" type="primary" :loading="uploadLoading" @click="handleUpload">
        应用结果
      </a-button>
    </a-flex>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  createPictureOutPaintingTaskUsingPost,
  getPictureOutPaintingTaskUsingGet,
  uploadPictureByUrlUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  picture: API.PictureVO
  spaceId: number
  onSuccess: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

//任务ID
const taskId = ref<string>()

//上传状态
const uploadLoading = ref(false)

//结果图片URL
const resultImageUrl = ref<string>()

//创建扩图任务
const createTask = async () => {
  //1.构造参数
  if (!props.picture?.id) {
    return
  }

  const params: API.CreatePictureOutPaintingTaskRequest = {
    pictureId: props.picture.id,
    parameters: {
      xScale: 2,
      yScale: 2,
    },
  }
  //2.调用接口
  const res = await createPictureOutPaintingTaskUsingPost(params)
  //3.处理结果
  if (res.data.code === 0 && res.data.data) {
    message.success('创建任务成功，请耐心等待，不要退出界面')

    console.log(res.data.data.output.taskId)

    taskId.value = res.data.data.output.taskId
    // 开启轮询
    startPolling()
  } else {
    message.error('图片任务失败，' + res.data.message)
  }
}

//轮询定时器
let pollingTimer: NodeJS.Timeout = null

//开启轮询
const startPolling = () => {
  if (!taskId.value) {
    return
  }

  pollingTimer = setInterval(async () => {
    //1.构造参数
    const params: API.getPictureOutPaintingTaskUsingGETParams = {
      taskId: taskId.value,
    }
    //2.调用接口
    try {
      const res = await getPictureOutPaintingTaskUsingGet(params)
      //3.处理结果
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output

        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('扩图任务执行成功')

          resultImageUrl.value = taskResult.outputImageUrl //停止轮询
          //停止轮询
          clearPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('扩图任务失败le')
          //停止轮询
          clearPolling()
        }
      }
    } catch (error) {
      console.error('扩图任务轮询失败', error)

      message.error('扩图任务轮询失败，' + error.message)
      //停止轮询
      clearPolling()
    }
  }, 3000) //3秒轮询一次
}

//停止轮询
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    taskId.value = null
  }
}

//上传结果图片
const handleUpload = async () => {
  uploadLoading.value = true
  try {
    //1.构造参数
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    //2.调用接口
    const res = await uploadPictureByUrlUsingPost(params)
    //3.处理结果
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      // 关闭弹窗
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }
  uploadLoading.value = false
}
//是否可见
const visible = ref(false)

//打开分享弹窗
const openModal = () => {
  visible.value = true
}

//关闭分享弹窗
const closeModal = () => {
  visible.value = false
}

//暴露方法给父组件调用
defineExpose({
  openModal,
})
</script>

<style scoped></style>

<template>
  <div id="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
    >
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">点击或拖拽图片上传</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

const loading = ref<boolean>(false)

/**
 * 上传图片
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true

  try {
    //获取参数
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}

    params.spaceId = props.spaceId
    //像后端发送请求返回结果
    const res = await uploadPictureUsingPost(params, {}, file)
    //将结果传递给父组件
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')

      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败, ' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }

  loading.value = false
}

/**
 * 上传前校验
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('不支持该格式的图片上传, 推荐jpg或png格式')
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('不能上传超过10M的图片')
  }
  return isJpgOrPng && isLt10M
}
</script>
<style scoped>
#picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}

#picture-upload img {
  max-width: 100%;
  max-height: 480px;
}

.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>

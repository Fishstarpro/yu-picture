<template>
  <div id="url-picture-upload">
    <div class="url-input-container">
      <a-input-search
        v-model:value="url"
        placeholder="请输入图片URL"
        enter-button="上传"
        @search="handleUrlUpload"
      />
    </div>
    <div v-if="picture?.url" class="preview-container">
      <img v-lazy="picture.url" alt="preview" />
    </div>
    <div v-else class="empty-container">
      <picture-outlined />
      <div class="empty-text">请输入图片URL并点击上传</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const loading = ref<boolean>(false)
const url = ref<string>('')

/**
 * 处理URL上传
 */
const handleUrlUpload = async () => {
  if (!url.value) {
    message.warning('请输入图片URL')
    return
  }

  loading.value = true
  try {
    const params = props.picture ? { id: props.picture.id } : {}
    const res = await uploadPictureUsingPost(params, { url: url.value })
    
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败, ' + res.data.message)
    }
  } catch (error: any) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#url-picture-upload {
  width: 100%;
}

.url-input-container {
  margin-bottom: 16px;
}

.preview-container {
  width: 100%;
  min-height: 152px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
}

.preview-container img {
  max-width: 100%;
  max-height: 480px;
  object-fit: contain;
}

.empty-container {
  width: 100%;
  min-height: 152px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
}

.empty-container i {
  font-size: 32px;
  color: #999;
  margin-bottom: 8px;
}

.empty-text {
  color: #666;
}
</style>
  
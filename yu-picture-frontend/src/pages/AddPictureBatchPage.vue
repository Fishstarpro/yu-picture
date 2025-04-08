<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">批量创建图片</h2>

    <!-- 图片信息表单 -->
    <a-form name="formdata" layout="vertical" :model="formdata" @finish="handleSubmit">
      <a-form-item name="searchText" label="搜索词">
        <a-input v-model:value="formdata.searchText" placeholder="请输入搜索词" allow-clear />
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <a-input-number
          v-model:value="formdata.count"
          placeholder="请输入数量"
          :min="1"
          :max="30"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <a-input v-model:value="formdata.namePrefix" placeholder="请输入名称前缀" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          执行任务
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureByBatchUsingPost } from '@/api/pictureController.ts'
import { useRouter } from 'vue-router'

//任务执行状态
const loading = ref(false)

// 图片信息表单
const formdata = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
})

const router = useRouter()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true

  const params = {
    ...formdata,
  }
  const res = await uploadPictureByBatchUsingPost(params)
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('创建成功')

    loading.value = false
    // 跳转到主页
    router.push({
      path: `/`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
}
</script>

<style scoped>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}

.preview-container {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 8px;
  overflow: hidden;
  margin: 0 auto;
  min-height: 300px;
}

.preview-container img {
  max-width: 100%;
  max-height: 480px;
  object-fit: contain;
}
</style>

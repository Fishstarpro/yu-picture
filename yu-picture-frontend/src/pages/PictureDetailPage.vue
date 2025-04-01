<template>
  <div id="pictureDetailPage">
    <a-row :gutter="[16, 16]">
      <!-- 图片预览 -->
      <a-col :sm="24" :md="15" :xl="17">
        <a-card title="图片预览" class="preview-card" :bodyStyle="{ padding: '12px', textAlign: 'center' }">
          <div class="image-wrapper">
            <a-image
              :src="picture.url"
              :preview="{ maskClassName: 'preview-mask' }"
              :lazy="true"
            />
          </div>
        </a-card>
      </a-col>
      <!-- 图片信息区域 -->
      <a-col :sm="24" :md="9" :xl="7">
        <a-card title="图片信息" class="info-card" :bodyStyle="{ padding: '8px 12px' }">
          <a-descriptions :column="1" class="info-descriptions">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="28" :src="picture.user?.userAvatar" />
                <div style="font-weight: 500; color: #333;">{{ picture.user?.userName }}</div>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">{{ picture.name ?? '未命名' }}</a-descriptions-item>
            <a-descriptions-item label="简介">{{ picture.introduction ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="分类">{{ picture.category ?? '默认' }}</a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-space size="8">
                <a-tag
                  v-for="tag in picture.tags"
                  :key="tag"
                  class="tag-item"
                >
                  {{ tag }}
                </a-tag>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="格式">{{ picture.picFormat ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="宽度">{{ picture.picWidth ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="高度">{{ picture.picHeight ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="宽高比">{{ picture.picScale ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="大小">{{ formatSize(picture.picSize) }}</a-descriptions-item>
          </a-descriptions>
          <a-space wrap>

            <!-- 图片操作 -->
              <a-button
                type="primary"
                @click="doDownload"
                class="action-btn"
              >
                <template #icon>
                  <DownloadOutlined />
                </template>
                下载
              </a-button>
              <a-button
                v-if="isAllow"
                :icon="h(EditOutlined)"
                type="link"
                @click="doEdit"
                class="action-btn"
              >
                编辑
              </a-button>
              <a-button
                v-if="isAllow"
                :icon="h(DeleteOutlined)"
                danger
                @click="doDelete"
                class="action-btn"
              >
                删除
              </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { DeleteOutlined, DownloadOutlined, EditOutlined } from '@ant-design/icons-vue'
import { computed, h, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { downloadImage, formatSize } from '../utils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useRouter } from 'vue-router'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({})

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})

//下载图片
const doDownload = () => {
  downloadImage(picture.value.url, picture.value.name)
}

const loginUserStore = useLoginUserStore()

//校验权限
const isAllow = computed(() => {
  //拿到登录用户
  const loginUser = loginUserStore.loginUser
  //未登录不可编辑
  if (!loginUser.id) {
    return false
  }
  //不是本人并且不是管理员返回false
  if (loginUser?.id !== picture.value.user?.id && loginUser?.userRole !== 'admin') {
    return false
  }
  return true
})

const router = useRouter()

// 编辑
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

// 删除数据
const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')

    router.push('/')
  } else {
    message.error('删除失败')
  }
}
</script>

<style scoped>
#pictureDetailPage {
  padding: 16px;
  max-width: 1600px;
  margin: 0 auto;
}

.preview-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  height: 100%;
}

.image-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  max-height: 650px;
  overflow: hidden;
}

.image-wrapper :deep(.ant-image) {
  max-height: 650px;
  width: 100%;
}

.image-wrapper :deep(.ant-image-img) {
  object-fit: contain;
  max-height: 650px;
  border-radius: 4px;
}

.info-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  background: #ffffff;
  height: 100%;
}

.info-descriptions {
  padding: 8px 0;
}

.info-descriptions :deep(.ant-descriptions-item) {
  padding-bottom: 8px;
}

.info-descriptions :deep(.ant-descriptions-item-label) {
  color: #666;
  font-weight: 500;
  width: 70px;
}

.tag-item {
  background: #ebf7ff;
  color: #1890ff;
  font-size: 12px;
  margin: 2px;
}

.action-btn {
  min-width: 80px;
  height: 36px;
  border-radius: 6px;
  font-size: 14px;
  margin-top: 8px;
}

.action-btn.ant-btn-primary {
  background: #1890ff;
  border-color: #1890ff;
}

.action-btn.ant-btn-link {
  color: #1890ff;
  padding: 0;
}

.action-btn.ant-btn-danger {
  background: #ff4d4f;
  border-color: #ff4d4f;
}
</style>

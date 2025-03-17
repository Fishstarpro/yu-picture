<template>
  <div id="pictureDetailPage">
    <a-row :gutter="[24, 24]">
      <!-- 图片预览 -->
      <a-col :sm="24" :md="16" :xl="18">
        <a-card title="图片预览" class="preview-card">
          <a-image
            :src="picture.url"
            style="max-height: 650px; object-fit: contain; border-radius: 8px;"
          />
        </a-card>
      </a-col>
      <!-- 图片信息区域 -->
      <a-col :sm="24" :md="8" :xl="6">
        <a-card title="图片信息" class="info-card">
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
            <div class="action-group">
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
            </div>
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
  router.push('/add_picture?id=' + picture.value.id)
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
.pictureDetailPage {
  padding: 24px;
}

.preview-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.info-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  background: #f8f9fa;
}

.info-descriptions {
  padding: 16px;
}

.info-descriptions .ant-descriptions-item-label {
  color: #666;
  font-weight: 500;
}

.tag-item {
  background: #ebf7ff;
  color: #1890ff;
  font-size: 14px;
}

.action-group {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.action-btn {
  min-width: 80px;
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
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

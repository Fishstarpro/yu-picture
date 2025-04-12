<template>
  <div id="picture-list">
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 8, xs: 1, sm: 2, md: 3, lg: 3, xl: 5, xxl: 5 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item>
          <!-- 单张图片 -->
          <a-card hoverable @click="doClickPicture(picture)" class="fade-in-card">
            <template #cover>
              <div class="image-container">
                <img
                  :alt="picture.name"
                  v-lazy="picture.thumbnailUrl || picture.url"
                  style="height: 250px; width: 100%; object-fit: fill"
                  :preview="{ maskClassName: 'preview-mask' }"
                  :lazy="true"
                />
                <!--                <div class="author-info">
                  <span>{{ picture.user?.userName || '未知作者' }}</span>
                </div>-->
                <!-- 悬停效果覆盖层 -->
                <div class="hover-overlay">
                  <div class="detail-button">
                    <span>Preview</span>
                  </div>
                </div>
              </div>
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>

            <template v-if="showOp" #actions>
              <a-space @click="(e) => doShare(picture, e)">
                <ShareAltOutlined />
                分享
              </a-space>
              <a-space @click="(e) => doSearch(picture, e)">
                <SearchOutlined />
                搜索
              </a-space>
              <a-space @click="(e) => doEdit(picture, e)">
                <EditOutlined />
                编辑
              </a-space>
              <a-space @click="(e) => doDelete(picture, e)">
                <DeleteOutlined />
                删除
              </a-space>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>
<script setup lang="ts">
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message, Modal } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'
import { ref } from 'vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
})

const router = useRouter()

// 编辑
const doEdit = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除数据
const doDelete = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }

  // 添加确认弹窗
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除图片 ${picture.name}？`,
    async onOk() {
      const res = await deletePictureUsingPost({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        props.onReload?.()
      } else {
        message.error('删除失败')
      }
    },
  })
}

// 点击图片跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

//以图搜图
const doSearch = (picture, e) => {
  //1.阻止冒泡
  e.stopPropagation()
  //2.打开一个新页面展示
  window.open(`/search_picture?pictureId=${picture.id}`)
}

//分享图片
const shareModalRef = ref()

const shareLink = ref<string>('')

const doShare = (picture, e) => {
  //1.阻止冒泡
  e.stopPropagation()
  //2.修改shareLink值
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  //3.调用子组件
  shareModalRef.value.openModal()
}
</script>

<style scoped>
.image-container {
  position: relative;
  overflow: hidden;
  height: 250px;
  width: 100%;
  border-radius: 8px;
  background-color: #f0f0f0;
}

.author-info {
  position: absolute;
  bottom: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 8px;
  font-size: 12px;
  border-top-right-radius: 4px;
}

/* 悬停效果样式 */
.hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-container:hover .hover-overlay {
  opacity: 1;
}

.detail-button {
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.detail-button:hover {
  transform: scale(1.05);
}

.fade-in-card {
  animation: fadeIn 0.8s ease-in-out;
  opacity: 0;
  animation-fill-mode: forwards;
  margin-bottom: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
}

.fade-in-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 为每个卡片添加延迟，创造错落有致的效果 */
.fade-in-card:nth-child(1) {
  animation-delay: 0.1s;
}
.fade-in-card:nth-child(2) {
  animation-delay: 0.2s;
}
.fade-in-card:nth-child(3) {
  animation-delay: 0.3s;
}
.fade-in-card:nth-child(4) {
  animation-delay: 0.4s;
}
.fade-in-card:nth-child(5) {
  animation-delay: 0.5s;
}
.fade-in-card:nth-child(6) {
  animation-delay: 0.6s;
}
</style>

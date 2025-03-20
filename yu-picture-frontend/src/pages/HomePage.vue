<template>
  <div id="homePage">
    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从海量图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>
    <!-- 分类和标签筛选 -->
    <a-tabs v-model:active-key="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :pagination="pagination"
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
                  :src="picture.url"
                  style="height: 180px; width: 100%; object-fit: cover"
                />
                <div class="author-info">
                  <span>{{ picture.user?.userName || '未知作者' }}</span>
                </div>
                <!-- 悬停效果覆盖层 -->
                <div class="hover-overlay">
                  <div class="detail-button">
                    <EyeOutlined />
                    <span>查看详情</span>
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
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>
<script setup lang="ts">
// 数据列表
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { EyeOutlined } from '@ant-design/icons-vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'

const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(false)
// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  //*转换参数
  const params = {
    ...searchParams,
   tags: [] as string[],
  }

  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  //**[true, false, true] => [tag1, tag3]
  selectedTagList.value.forEach((value, index) => {
    if (value) {
      params.tags.push(tagList.value[index])
    }
  })

  // 向后端发送请求获取数据
  const res = await listPictureVoByPageUsingPost(params)
  // 判断数据是否为空
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []

    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }
  loading.value = false
}

// 页面加载时请求一次数据
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    total: total.value,
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    //这里不是表格,所以单独写了一个函数
    onChange: (page: number, pageSize: number) => {
      searchParams.current = page
      searchParams.pageSize = pageSize
      fetchData()
    },
  }
})

// 搜索数据
const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

const router = useRouter()

// 点击图片跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const selectedTagList = ref<boolean[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}

#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}

#homePage .tag-bar {
  margin-bottom: 16px;
  text-align: center;
}

/* 添加tabs居中样式 */
#homePage :deep(.ant-tabs) .ant-tabs-nav {
  display: flex;
  justify-content: center;
}

#homePage :deep(.ant-tabs-nav-wrap) {
  justify-content: center;
}
.image-container {
  position: relative;
  overflow: hidden;
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
  background-color: rgba(0, 0, 0, 0.7);
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
  background-color: rgba(24, 144, 255, 0.8);
  padding: 8px 16px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.detail-button:hover {
  background-color: rgba(24, 144, 255, 1);
}

.fade-in-card {
  animation: fadeIn 0.8s ease-in-out;
  opacity: 0;
  animation-fill-mode: forwards;
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
.fade-in-card:nth-child(1) { animation-delay: 0.1s; }
.fade-in-card:nth-child(2) { animation-delay: 0.2s; }
.fade-in-card:nth-child(3) { animation-delay: 0.3s; }
.fade-in-card:nth-child(4) { animation-delay: 0.4s; }
.fade-in-card:nth-child(5) { animation-delay: 0.5s; }
.fade-in-card:nth-child(6) { animation-delay: 0.6s; }
</style>

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
    <PIctureList :dataList="dataList" :loading="loading" :show-op="false" />
    <!-- 加载更多提示 -->
    <div v-if="dataList.length > 0" class="load-more-container" ref="loadMoreTriggerRef">
      <div v-if="loading" class="loading-indicator">
        <a-spin />
        <span style="margin-left: 8px">加载中...</span>
      </div>
      <div v-else-if="!hasMore" class="no-more-text">
        没有更多图片了
      </div>
      <div v-else>
        <!-- 下滑加载更多提示 -->
        <div class="load-more-hint">
          <a-icon-down class="down-arrow-icon" />
          <span>下滑加载更多</span>
        </div>
        <div class="load-more-trigger">
          <!-- 这是一个隐藏的触发元素，用于IntersectionObserver监测 -->
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
// 数据列表
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { EyeOutlined, DownOutlined } from '@ant-design/icons-vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import PIctureList from '@/components/PIctureList.vue'

const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(false)
const hasMore = ref(true)
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
    // 如果是第一页，替换数据，否则追加数据
    if (searchParams.current === 1) {
      dataList.value = res.data.data.records ?? []
    } else {
      dataList.value = [...dataList.value, ...(res.data.data.records ?? [])]
    }

    total.value = res.data.data.total ?? 0

    // 判断是否还有更多数据
    const currentPageRecords = res.data.data.records ?? [];
    hasMore.value = currentPageRecords.length === searchParams.pageSize && dataList.value.length < total.value;
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }
  loading.value = false
}

// 页面加载时请求一次数据
onMounted(() => {
  fetchData()
})

// 加载更多数据
const loadMore = () => {
  if (loading.value || !hasMore.value) return
  searchParams.current += 1
  fetchData()
}

// 监听滚动事件，实现自动加载
const handleScroll = () => {
  // 如果正在加载或没有更多数据，则不处理
  if (loading.value || !hasMore.value) return

  // 获取滚动位置信息
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight
  const clientHeight = document.documentElement.clientHeight

  // 当滚动到距离底部200px时，加载更多数据
  if (scrollTop + clientHeight >= scrollHeight - 200) {
    loadMore()
  }
}

// 搜索数据
const doSearch = () => {
  searchParams.current = 1
  hasMore.value = true
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

// 使用IntersectionObserver监测底部元素是否可见
const loadMoreObserver = ref<IntersectionObserver | null>(null)
const loadMoreTriggerRef = ref<HTMLElement | null>(null)

// 初始化IntersectionObserver
const initIntersectionObserver = () => {
  if (loadMoreObserver.value) return

  // 创建观察器实例
  loadMoreObserver.value = new IntersectionObserver((entries) => {
    // 如果底部元素进入视口，且不在加载中，且还有更多数据
    if (entries[0].isIntersecting && !loading.value && hasMore.value) {
      loadMore()
    }
  }, {
    // 设置根元素为null，表示视口
    root: null,
    // 设置根元素的margin，提前触发
    rootMargin: '0px 0px 200px 0px',
    // 设置阈值，当目标元素0%可见时触发回调
    threshold: 0
  })

  // 如果底部触发元素存在，开始观察
  if (loadMoreTriggerRef.value) {
    loadMoreObserver.value.observe(loadMoreTriggerRef.value)
  }
}

onMounted(() => {
  getTagCategoryOptions()
  // 添加滚动事件监听
  window.addEventListener('scroll', handleScroll)
  // 初始化IntersectionObserver
  initIntersectionObserver()
})

onUnmounted(() => {
  // 移除滚动事件监听
  window.removeEventListener('scroll', handleScroll)
  // 断开IntersectionObserver连接
  if (loadMoreObserver.value) {
    loadMoreObserver.value.disconnect()
    loadMoreObserver.value = null
  }
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

/* 未选中的标签跳动动画 */
#homePage :deep(.ant-tag-checkable:not(.ant-tag-checkable-checked)):hover {
  animation: tagBounce 0.6s ease;
}

/* 未选中的分类跳动动画 */
#homePage :deep(.ant-tabs-tab:not(.ant-tabs-tab-active)):hover {
  animation: tagBounce 0.6s ease;
}

@keyframes tagBounce {
  0%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-5px);
  }
  60% {
    transform: translateY(-3px);
  }
}
/* 加载更多样式 */
.load-more-container {
  text-align: center;
  margin: 20px 0;
  padding: 10px;
  min-height: 60px; /* 确保容器有足够高度被观察到 */
}

.loading-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  color: #1890ff;
}

/* 移除了手动加载相关样式，只保留自动加载样式 */

.no-more-text {
  color: #999;
  padding: 8px;
}

/* 触发元素样式 */
.load-more-trigger {
  height: 1px;
  margin-top: 10px;
  visibility: hidden; /* 隐藏但仍占据空间 */
}

/* 下滑加载更多提示样式 */
.load-more-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #1890ff;
  margin-bottom: 10px;
  animation: bounce 1.5s infinite;
  cursor: pointer;
}

.down-arrow-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}
</style>

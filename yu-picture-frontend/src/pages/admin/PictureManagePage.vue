<template>
  <div id="pictureManagePage">
    <a-flex justify="space-between">
      <h2>图片管理</h2>
      <a-space>
        <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost
          >+ 批量创建图片</a-button
        >
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 搜索条件 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜索"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="类型">
        <a-input v-model:value="searchParams.category" placeholder="请输入类型" allow-clear />
      </a-form-item>
      <a-form-item label="标签">
        <a-input
          v-model:value="searchParams.tags"
          placeholder="请输入标签"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="reviewStatus" label="审核状态">
        <a-select
          v-model:value="searchParams.reviewStatus"
          style="min-width: 180px"
          placeholder="请选择审核状态"
          :options="PIC_REVIEW_STATUS_OPTIONS"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>

    <div style="margin-bottom: 16px"></div>
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image 
            :src="record.url" 
            :width="100" 
            :preview="{
              src: record.url,
              mask: '点击预览'
            }" 
          />
        </template>

        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">{{ tag }}</a-tag>
          </a-space>
        </template>

        <template v-if="column.dataIndex === 'picInfo'">
          <div>格式：{{ record.picFormat }}</div>
          <div>宽度：{{ record.picWidth }}</div>
          <div>高度：{{ record.picHeight }}</div>
          <div>宽高比：{{ record.picScale }}</div>
          <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
        </template>

        <template v-if="column.dataIndex === 'reviewStatus'">
          <div>审核状态：{{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}</div>
          <div>审核信息：{{ record.reviewMessage }}</div>
          <div>审核人：{{ record.reviewerId }}</div>
          <div v-if="record.reviewTime">
            审核时间：{{ dayjs(record.reviewTime).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
        </template>

        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
              @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
              type="link"
              >通过</a-button
            >

            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
              danger
              @click="showRejectModal(record)"
            >
              拒绝
            </a-button>

            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">
              编辑
            </a-button>
            <a-popconfirm
              title="确认删除吗?"
              ok-text="Yes"
              cancel-text="No"
              @confirm="doDelete(record.id)"
            >
              <a-button danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>

  <!-- 拒绝原因输入对话框 -->
  <a-modal v-model:visible="rejectModalVisible" title="请输入拒绝原因" @ok="handleRejectConfirm">
    <a-textarea v-model:value="rejectMessage" placeholder="请输入拒绝原因" :rows="4" />
  </a-modal>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '../../constants/picture.ts'
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '审核信息',
    dataIndex: 'reviewStatus',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 数据列表
const dataList = ref<API.Picture[]>([])
const total = ref(0)

// 拒绝对话框相关
const rejectModalVisible = ref(false)
const rejectMessage = ref('')
const currentRecord = ref<API.Picture | null>(null)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  // 向后端发送请求获取数据
  const res = await listPictureByPageUsingPost({
    ...searchParams,
  })
  // 判断数据是否为空
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []

    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }
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
    showSizeChanger: true,
    showTotal: (total) => `共${total}条`,
  }
})

// 表格变化之后, 重新请求数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }

  const res = await deletePictureUsingPost({ id })

  if (res.data.code === 0) {
    message.success('删除成功')
  } else {
    message.error('删除失败')
  }

  fetchData()
}

// 显示拒绝对话框
const showRejectModal = (record: API.Picture) => {
  currentRecord.value = record
  rejectMessage.value = ''
  rejectModalVisible.value = true
}

// 处理拒绝确认
const handleRejectConfirm = async () => {
  if (!currentRecord.value) return

  await handleReview(currentRecord.value, PIC_REVIEW_STATUS_ENUM.REJECT, rejectMessage.value)
  rejectModalVisible.value = false
}

// 审核图片
const handleReview = async (record: API.Picture, reviewStatus: number, customMessage?: string) => {
  // 设置审核信息
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '审核通过' : customMessage || '审核不通过'

  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })

  if (res.data.code === 0) {
    message.success('审核成功')
    //重新获取数据
    fetchData()
  } else {
    message.error('审核失败')
  }
}
</script>

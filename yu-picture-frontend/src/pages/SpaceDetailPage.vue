<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}（私有空间）</h2>
      <a-space size="middle">
        <a-button v-if="canUploadPicture" :href="`/add_picture?spaceId=${id}`" target="_blank" type="primary">
          + 创建图片
        </a-button>
        <a-button
          v-if="canManageSpaceUser && space.spaceType === SPACE_TYPE_ENUM.TEAM"
          :icon="h(TeamOutlined)"
          :href="`/spaceUserManage/${id}`"
          target="_blank"
          type="primary"
        >
          成员管理
        </a-button>
        <a-button
          v-if="canManageSpaceUser"
          :href="`/space_analyze?spaceId=${id}`"
          :icon="h(BarChartOutlined)"
          ghost
          target="_blank"
          type="primary"
        >
          空间分析
        </a-button>
        <a-button v-if="canEditPicture" :icon="h(EditOutlined)" @click="doBatchEdit"> 批量编辑</a-button>
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
            :size="42"
            type="circle"
          />
        </a-tooltip>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 搜索表单 -->
    <PictureSearchForm :onSearch="onSearch" />
    <div style="margin-bottom: 16px" />
    <!-- 按颜色搜索，跟其他搜索条件独立 -->
    <a-form-item label="按颜色搜索">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>
    <!-- 图片列表 -->
    <PictureList
      :can-delete="canDeletePicture"
      :can-edit="canEditPicture"
      :dataList="dataList"
      :loading="loading"
      :onReload="onReload"
      :showOp="true"
    />
    <!-- 分页 -->
    <a-pagination
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      style="text-align: right"
      @change="onPageChange"
    />
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :onSuccess="onBatchEditPictureSuccess"
      :pictureList="dataList"
      :spaceId="id"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed, h, onMounted, ref, watch } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { BarChartOutlined, EditOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM } from '@/constants/space.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// -------- 获取空间详情 --------
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})

// --------- 获取图片列表 --------

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value,
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

//删除图片时,会触发此方法
const onReload = () => {
  fetchData()
  fetchSpaceDetail()
}
//搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  //合并搜索参数
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  //重新请求
  fetchData()
}

//颜色搜索
const onColorChange = async (color: string) => {
  loading.value = true
  //请求后端接口
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })

  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []

    dataList.value = data

    total.value = data.length
  } else {
    message.error('颜色搜索失败，' + res.data.message)
  }

  loading.value = false
}

//批量编辑
const batchEditPictureModalRef = ref()

const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 空间 id 改变时，必须重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)
</script>

<style scoped>
#spaceDetailPage {
  margin-bottom: 16px;
}
</style>

<template>
  <div>
    <a-modal v-model:visible="visible" title="批量编辑图片" :footer="false" @cancel="closeModal">
      <!-- 图片信息表单 -->
      <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
        <a-form-item name="category" label="分类">
          <a-auto-complete
            v-model:value="formData.category"
            placeholder="请输入分类"
            :options="categoryOptions"
            @change="handleCategoryChange"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="tags" label="标签">
          <a-select
            v-model:value="formData.tags"
            mode="tags"
            placeholder="请输入标签"
            :options="tagOptions"
            @change="handleTagChange"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="nameRule" label="命名规则">
          <a-input
            v-model:value="formData.nameRule"
            placeholder="请输入命名规则，输入 {序号} 可动态生成"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" style="width: 100%">提交</a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  editPictureByBatchUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'

const formData = reactive<API.PictureEditByBatchRequest>({
  category: '',
  tags: [],
  nameRule: '',
})

interface Props {
  pictureList: API.PictureVO[]
  spaceId: number
  onSuccess: () => void
}

const props = withDefaults(defineProps<Props>(), {})

//是否可见
const visible = ref(false)

// 打开分享弹窗
const openModal = () => {
  visible.value = true
}

// 关闭分享弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露方法给父组件调用
defineExpose({
  openModal,
})

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  //1.校验参数
  if (!props.pictureList) {
    return
  }
  //2.调用后端接口
  const res = await editPictureByBatchUsingPost({
    pictureIdList: props.pictureList.map((picture) => picture.id),
    spaceId: props.spaceId,
    ...values,
  })

  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    closeModal()
    props.onSuccess?.()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

/**
 * 处理分类变更，如果输入的分类不在预设选项中，则清空
 * @param value 用户输入的分类值
 */
const handleCategoryChange = (value: string) => {
  if (value) {
    // 检查输入的值是否在预设选项中
    const exists = categoryOptions.value.some((option) => {
      return typeof option === 'string' ? option === value : option.value === value
    })

    // 如果不存在于预设选项中，则清空输入
    if (!exists) {
      formData.category = ''
      message.warning('请选择系统提供的分类选项')
    }
  }
}

/**
 * 处理标签变更，如果输入的标签不在预设选项中，则移除该标签
 * @param values 用户选择的标签数组
 */
const handleTagChange = (values: string[]) => {
  if (values && values.length > 0) {
    // 过滤出不在预设选项中的标签
    const invalidTags = values.filter((tag) => {
      // 检查标签是否在预设选项中
      const exists = tagOptions.value.some((option) => {
        return typeof option === 'string' ? option === tag : option.value === tag
      })
      return !exists
    })

    // 如果有不在预设选项中的标签，则移除它们
    if (invalidTags.length > 0) {
      // 过滤掉无效标签，只保留有效标签
      formData.tags = values.filter((tag) => !invalidTags.includes(tag))
      message.warning('请选择系统提供的标签选项')
    }
  }
}
</script>

<style scoped></style>

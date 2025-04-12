<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
    </a-typography-paragraph>
    <!-- 上传方式切换 -->
    <div v-if="!route.query?.id">
      <a-radio-group v-model:value="uploadType" style="margin-bottom: 16px">
        <a-radio value="local">本地上传</a-radio>
        <a-radio value="url">URL上传</a-radio>
      </a-radio-group>
      <!-- 图片上传组件 -->
      <PictureUpload
        v-if="uploadType === 'local'"
        :spaceId="spaceId"
        :picture="picture"
        :onSuccess="onSuccess"
      />
      <UrlPictureUpload v-else :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
    </div>

    <!-- 修改图片时显示当前图片预览 -->
    <div
      v-if="route.query?.id && picture?.url"
      class="preview-container"
      style="margin-bottom: 16px"
    >
      <img v-lazy="picture.url" alt="当前图片" />
    </div>

    <!-- 图片编辑 -->
    <div v-if="picture" class="edit-bar">
      <a-space size="middle">
        <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
        <a-button type="primary" :icon="h(FullscreenOutlined)" @click="doImagePainting">
          AI 扩图
        </a-button>
      </a-space>
    </div>

    <!-- 图片裁剪组件 -->
    <ImageCropper
      ref="imageCropperRef"
      :imageUrl="picture?.url"
      :picture="picture"
      :spaceId="spaceId"
      :onSuccess="onCropSuccess"
    />

    <!-- 图片扩图组件 -->
    <ImageOutPainting
      ref="imageOutPaintingRef"
      :picture="picture"
      :spaceId="spaceId"
      :onSuccess="onImageOutPaintingSuccess"
    />

    <!-- 图片信息表单 -->
    <a-form
      v-if="picture"
      name="pictureForm"
      layout="vertical"
      :model="pictureForm"
      @finish="handleSubmit"
    >
      <a-form-item name="name" label="名称">
        <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
      </a-form-item>
      <a-form-item name="introduction" label="简介">
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="请输入简介"
          :auto-size="{ minRows: 2, maxRows: 5 }"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="pictureForm.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          @change="handleCategoryChange"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          placeholder="请输入标签"
          :options="tagOptions"
          @change="handleTagChange"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">{{
          route.query?.id ? '修改图片' : '创建图片'
        }}</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})

const uploadType = ref<'local' | 'url'>('local')

const router = useRouter()
const route = useRoute()

//空间id
const spaceId = computed(() => route.query?.spaceId)

// 监听上传方式变化，仅在新建图片时生效
watch(uploadType, () => {
  // 如果是修改图片（id存在），则不清空图片和表单
  if (route.query?.id) {
    return
  }

  // 新建图片时，清空已上传的图片和表单
  picture.value = undefined
  pictureForm.name = ''
  pictureForm.introduction = ''
  pictureForm.category = ''
  pictureForm.tags = []
})

/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  console.log(values)
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }

  const params = {
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  }
  const res = await editPictureUsingPost(params)
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success(route.query?.id ? '修改成功' : '创建成功')
    // 跳转到图片详情页
    router.push({
      path: `/picture/${pictureId}`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
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

// 获取老数据
const getOldPicture = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}

onMounted(() => {
  getOldPicture()
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
      pictureForm.category = ''
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
      pictureForm.tags = values.filter((tag) => !invalidTags.includes(tag))
      message.warning('请选择系统提供的标签选项')
    }
  }
}

//编辑图片
const imageCropperRef = ref()

const doEditPicture = async () => {
  imageCropperRef.value?.openModal()
}

const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

//AI 扩图
const imageOutPaintingRef = ref()

const doImagePainting = async () => {
  imageOutPaintingRef.value?.openModal()
}

const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
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

#addPicturePage .edit-bar {
  text-align: center;
  margin: 16px 0;
}
</style>

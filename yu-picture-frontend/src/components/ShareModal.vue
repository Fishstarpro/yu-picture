<template>
  <div>
    <a-modal v-model:visible="visible" :title="title" :footer="false" @cancel="closeModal">
      <h4>复制分享链接</h4>
      <a-typography-link copyable>
        {{ link }}
      </a-typography-link>
      <div style="margin-bottom: 16px" />
      <h4>手机扫码查看</h4>
      <a-qrcode :value="link" />
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  title: string
  link: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '分享图片',
  link: '',
})

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
</script>

<style scoped></style>

<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

//刷新页面时重新获取登录用户信息
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()

//改为在权限校验文件中获取
</script>

<style scoped></style>

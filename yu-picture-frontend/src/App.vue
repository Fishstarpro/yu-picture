<template>
  <a-config-provider :locale="zhCN">
  <div id="app">
    <BasicLayout />
  </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');

//刷新页面时重新获取登录用户信息
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()

//改为在权限校验文件中获取
</script>

<style scoped></style>

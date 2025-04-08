<template>
  <div id="userRegisterPage">
    <h2 class="title">图库-注册</h2>
    <div class="desc">企业级智能协同云图库</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于8位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码' },
          { min: 8, message: '确认密码长度不能小于8位' },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
      </a-form-item>

      <div class="tips">
        已有帐号?
        <RouterLink to="/user/login">登录</RouterLink>
      </div>

      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { userRegisterUsingPost } from '@/api/userController.ts'
import router from '@/router'
import { message } from 'ant-design-vue'

// 定义表单数据
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})
const handleSubmit = async (values: any) => {
  // 向后端发送注册请求
  const res = await userRegisterUsingPost(values)
  // 注册成功,前端保存登陆状态
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    // 跳转到首页
    router.push({
      path: `/`,
      replace: true,
    })
  } else {
    message.error('注册失败, ' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  margin-bottom: 16px;
  color: #bbb;
}

.tips {
  text-align: right;
  margin-bottom: 16px;
  color: #bbb;
}
</style>

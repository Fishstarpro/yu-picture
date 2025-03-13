import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'

let firstFetchLoginUser = true;


router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()

  let loginUser = loginUserStore.loginUser

  //确保页面刷新时,首次加载时,能等待后端返回登陆信息在校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()

    loginUser = loginUserStore.loginUser

    firstFetchLoginUser = false;
  }

  //可以自定义权限校验
  const toUrl = to.path

  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error("没有权限访问")

      next(`/user/login?redirect=${to.fullPath}`)
    }
  }

  next()

});

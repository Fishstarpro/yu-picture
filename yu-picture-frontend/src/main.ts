import { createApp } from 'vue'
import { createPinia } from 'pinia'
import VueLazyLoad from 'vue3-lazyload'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import '@/access.ts'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(VueLazyLoad, {
  // 添加加载中和加载失败的默认图片
  loading: '/src/assets/loading.svg', // 加载中显示的图片
  error: '/src/assets/loading.svg',   // 加载失败显示的图片
  attempt: 3,                         // 尝试加载次数
  preLoad: 1.3,                       // 预加载高度比例
  observer: true,                     // 使用IntersectionObserver
  observerOptions: {
    rootMargin: '0px',
    threshold: 0.1
  }
})

app.mount('#app')



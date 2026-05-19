import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.js'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const app = createApp(App)
// 掛上 Pinia
const pinia = createPinia()
app.use(pinia)

// 初始化 userStore
import { useUserStore } from './stores/user'
const userStore = useUserStore()

await userStore.initFromLocalStorage()

// 強制將購物車數量歸零，避免看到上一個人的紀錄
if (!userStore.token) {
  userStore.cartCount = 0
}

app.use(router)

app.mount('#app')

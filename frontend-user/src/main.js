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
userStore.initFromLocalStorage()


app.use(router)

app.mount('#app')

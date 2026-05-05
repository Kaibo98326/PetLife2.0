import './assets/main.css'
import '@fortawesome/fontawesome-free/css/all.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import { createPinia } from 'pinia'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import Vant from 'vant';
import 'vant/lib/index.css';

import './assets/css/Dashboard.css'
//引入 Bootstrap 的 CSS 與 JS
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'


const app = createApp(App)

app.use(createPinia())
app.use(Vant);
app.use(router)
app.use(ElementPlus)
app.mount('#app')

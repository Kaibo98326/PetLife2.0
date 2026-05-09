import './assets/main.css'
import '@fortawesome/fontawesome-free/css/all.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import './assets/css/Dashboard.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useEmployeeStore } from '@/stores/employee'

import App from './App.vue'
import router from './router'


import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import Vant from 'vant';
import 'vant/lib/index.css';

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)

const employeeStore = useEmployeeStore()
employeeStore.initFromLocalStorage()




app.use(Vant);
app.use(router)
app.use(ElementPlus)
app.mount('#app')

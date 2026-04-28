import './assets/main.css'
import '@fortawesome/fontawesome-free/css/all.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Vant from 'vant';
import 'vant/lib/index.css';



const app = createApp(App)

app.use(createPinia())
app.use(Vant);
app.use(router)

app.mount('#app')

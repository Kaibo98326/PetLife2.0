import { createRouter, createWebHistory } from 'vue-router'
import LoginEmp from '@/views/LoginEmp.vue'


const routes = [
  {   
    path: '/',
    name: 'LoginEmp',
    component: LoginEmp 

  },
  {
    path: '/admin/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  }
  
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

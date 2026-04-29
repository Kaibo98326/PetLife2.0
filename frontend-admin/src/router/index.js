import { createRouter, createWebHistory } from 'vue-router'
import LoginEmp from '@/views/LoginEmp.vue'
import MainLayout from '@/layout/MainLayout.vue' // 引入你的外殼組件

const routes = [
  // 登入頁面 (不需要側邊欄和頂欄)
  {   
    path: '/',
    name: 'LoginEmp',
    component: LoginEmp 
  },
  
  // 後台管理區域 (使用 MainLayout 作為外殼)
  {
    path: '/admin',
    component: MainLayout,
    children: [
      {
        path: 'dashboard', // 對應 /admin/dashboard
        name: '首頁',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'category', // 對應 /admin/category
        name: '商品類別管理',
        component: () => import('@/views/Category.vue')
      },
      {
        path: 'product', // 對應 /admin/product
        name: '商品管理',
        component: () => import('@/views/Product.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
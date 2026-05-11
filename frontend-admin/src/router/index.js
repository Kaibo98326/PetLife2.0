import { createRouter, createWebHistory } from 'vue-router'
import LoginEmp from '@/views/LoginEmp.vue'
import MainLayout from '@/layout/MainLayout.vue' // 引入你的外殼組件
import OrderAdminView from '@/views/OrderAdminView.vue'

const routes = [
  // 登入頁面 (不需要側邊欄和頂欄)
  {
    path: '/',
    name: 'LoginEmp',
    component: LoginEmp,
  },

  // 後台管理區域 (使用 MainLayout 作為外殼)
  {
    path: '/admin',
    component: MainLayout,
    children: [
      {
        path: 'dashboard', // 對應 /admin/dashboard
        name: '',
        component: () => import('@/views/Dashboard.vue'),
      },
      {
        path: 'category', // 對應 /admin/category
        name: '商品類別管理',
        component: () => import('@/views/Category.vue'),
      },
      {
        path: 'product', // 對應 /admin/product
        name: '商品管理',
        component: () => import('@/views/Product.vue'),
      },
      {
        path: 'member/list', // 對應 /admin/members
        name: '現有會員',
        component: () => import('@/views/AdminMemberList.vue'),
      },
      {
        path: 'member/analysis',
        name: '會員狀態分析',
        component: () => import('@/views/AdminMemberAnalysis.vue'),
      },
      {
        path: 'product/edit/:id', // 對應 /admin/product/edit/:id
        name: 'ProductEdit',
        component: () => import('@/views/ProductEdit.vue'),
      },
      {
        path: 'product/add', // 對應 /admin/product/add
        name: 'ProductAdd',
        component: () => import('@/views/ProductEdit.vue'),
      },
      {
        path: 'order',
        name: '訂單管理',
        component: OrderAdminView,
      },
      {
        path: 'employee',
        name: '員工管理',
        component: () => import('@/views/AdminEmployeeList.vue'),
      },
      {
        path: 'pet',
        name: '寵物管理',
        component: () => import('@/views/AdminPetList.vue'),
      },
      {
  path: '/admin/discount',  // 或是 /admin/discounts
  name: 'DiscountManagement',
  component: () => import('../views/DiscountView.vue')// 確保組件路徑正確
}
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

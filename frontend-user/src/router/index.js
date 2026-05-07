import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserLayout from '@/views/UserLayout.vue'
import LoginMember from '@/views/LoginMember.vue'
import MemberCenter from '@/views/MemberCenter.vue'
import ProfileView from '@/views/ProfileView.vue'
import ProductDetailView from '@/views/ProductDetailView.vue'
import PetListView from '@/views/PetListView.vue'
import AddPetView from '@/views/AddPetView.vue'

const routes = [
  {
    // ── 商城主框架（Header + Nav 由 UserLayout 提供） ──
    path: '/',
    component: UserLayout,
    children: [
      {
        // 商城首頁（輪播 + 商品列表）
        path: '',
        name: 'home',
        component: HomeView
      },
      {
        // 商品詳情
        path: 'product/:id',
        name: 'productDetail',
        component: ProductDetailView
      },
      {
        // 會員中心
        path: 'member/center',
        name: 'MemberCenter',
        component: MemberCenter,
        children: [
          { path: 'profile' , component: ProfileView   },
          { path: 'pets'    , component: PetListView   },
          { path: 'pets/add', component: AddPetView    },
        ]
      }
    ]
  },
  {
    // 登入頁（獨立頁面，不套用 UserLayout）
    path: '/login',
    name: 'login',
    component: LoginMember
  },
  {
    path: '/set-password',
    name: 'SetPassword',
    component: () => import('@/views/SetPassword.vue'),
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router

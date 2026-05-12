import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserLayout from '@/views/UserLayout.vue'
import LoginMember from '@/views/LoginMember.vue'
import MemberCenter from '@/views/MemberCenter.vue'
import ProfileView from '@/views/ProfileView.vue'
import CartView from '@/views/CartView.vue'
import CheckoutView from '@/views/CheckoutView.vue'
import { useUserStore } from '@/stores/user'
import CheckoutSuccessView from '@/views/CheckoutSuccessView.vue'
import OrderHistoryView from '@/views/OrderHistoryView.vue'
import ProductDetailView from '@/views/ProductDetailView.vue'
import PetListView from '@/views/PetListView.vue'
import AddPetView from '@/views/AddPetView.vue'
import HeartView from '@/views/HeartView.vue'

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
        component: HomeView,
      },
      {
        // 商品詳情
        path: 'product/:id',
        name: 'productDetail',
        component: ProductDetailView,
      },
    ],
  },
  // 購物車的router
  {
    path: '/cart',
    name: 'cart',
    component: CartView,
    /*守衛，看會員有沒有登入*/
    meta: { requiresAuth: true },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: CheckoutView,
  },
  {
    path: '/checkoutsuccess',
    name: 'checkoutsuccess',
    component: CheckoutSuccessView,
  },
  {
    path: '/orderhistory',
    component: OrderHistoryView,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'productorders',
        component: () => import('@/views/ProductOrderView.vue'),
      },
      {
        path: 'prettyorders',
        name: 'prettyorders',
        component: () => import('@/views/PrettyOrderView.vue'),
      },
      {
        path: 'stayorders',
        name: 'stayorders',
        component: () => import('@/views/StayOrderView.vue'),
      },
    ],
  },
  {
    // 會員中心
    path: '/member/center',
    name: 'MemberCenter',
    component: MemberCenter,
    children: [{ path: 'profile', component: ProfileView }],
  },
  {
    // 登入頁（獨立頁面，不套用 UserLayout）
    path: '/login',
    name: 'login',
    component: LoginMember,
  },
  {
    path: '/set-password',
    name: 'SetPassword',
    component: () => import('@/views/SetPassword.vue'),
  },
  {
    // 會員中心
    path: '/member/center',
    name: 'MemberCenter',
    component: MemberCenter,
    children: [
      { path: 'profile', component: ProfileView },
      { path: 'pets', component: PetListView },
      { path: 'pets/add', component: AddPetView },
    ],
  },
    {
    path: '/heart',
    name: 'heart',
    component: HeartView,
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 掛在router上的守衛
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 檢查有沒有 memberId
  if (to.matched.some((record) => record.meta.requiresAuth) && !userStore.memberId) {
    next('/login')
  } else {
    next()
  }
})

export default router

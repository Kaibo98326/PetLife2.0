import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginMember from '@/views/LoginMember.vue'
import MemberCenter from '@/views/MemberCenter.vue'
import ProfileView from '@/views/ProfileView.vue'
import CartView from '@/views/CartView.vue'
import CheckoutView from '@/views/CheckoutView.vue'
import { useUserStore } from '@/stores/user'
import CheckoutSuccessView from '@/views/CheckoutSuccessView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/login',
    name: 'login',
    component: LoginMember,
  },
  {
    path: '/member/center',
    name: 'MemberCenter',
    component: MemberCenter,
    children: [{ path: 'profile', component: ProfileView }],
  },
  // 購物車的router
  {
    path: '/cart',
    name: 'cart',
    component: CartView,
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: CheckoutView,
    /*守衛，看會員有沒有登入*/
    meta: { requiresAuth: true },
  },
  {
    path: '/checkoutsuccess',
    name: 'checkoutsuccess',
    component: CheckoutSuccessView,
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

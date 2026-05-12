import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserLayout from '@/views/UserLayout.vue'
import LoginMember from '@/views/LoginMember.vue'
import MemberCenter from '@/views/MemberCenter.vue'
import ProfileView from '@/views/ProfileView.vue'
import CartView from '@/views/CartView.vue'
import CheckoutView from '@/views/CheckoutView.vue'
import CheckoutSuccessView from '@/views/CheckoutSuccessView.vue'
import OrderHistoryView from '@/views/OrderHistoryView.vue'
import ProductDetailView from '@/views/ProductDetailView.vue'
import PetListView from '@/views/PetListView.vue'
import AddPetView from '@/views/AddPetView.vue'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: UserLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: HomeView,
      },
      {
        path: 'product/:id',
        name: 'productDetail',
        component: ProductDetailView,
      },
      {
        path: 'beauty-booking',
        name: 'BeautyItems',
        component: () => import('@/views/BeautyItemsView.vue'),
      },
      {
        path: 'beauty-booking/reserve',
        name: 'BeautyBooking',
        component: () => import('@/views/BeautyBookingView.vue'),
      },
      {
        path: 'beauty-booking/appointments/:appointmentId',
        name: 'BeautyAppointmentDetail',
        component: () => import('@/views/BeautyAppointmentDetailView.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
  {
    path: '/cart',
    name: 'cart',
    component: CartView,
    meta: { requiresAuth: true },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: CheckoutView,
    meta: { requiresAuth: true },
  },
  {
    path: '/checkoutsuccess',
    name: 'checkoutsuccess',
    component: CheckoutSuccessView,
    meta: { requiresAuth: true },
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
    path: '/member/center',
    name: 'MemberCenter',
    component: MemberCenter,
    meta: { requiresAuth: true },
    children: [
      { path: 'profile', component: ProfileView },
      { path: 'pets', component: PetListView },
      { path: 'pets/add', component: AddPetView },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: LoginMember,
  },
  {
    path: '/set-password',
    name: 'SetPassword',
    component: () => import('@/views/SetPassword.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.matched.some((record) => record.meta.requiresAuth) && !userStore.memberId) {
    return '/login'
  }
})

export default router

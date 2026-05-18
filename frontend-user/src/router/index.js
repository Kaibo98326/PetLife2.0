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
import HeartView from '@/views/HeartView.vue'
import favoritesView from '@/views/favoritesView.vue'

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
      // 5/8 路徑stay/:房型ID  要有UserLayout
      {
        path: 'stay/:roomTypeId',
        name: 'StayRoomTypeDetail',
        component: () => import('@/views/stay/StayRoomTypeDetail.vue'),
      },
      // 5/9 路徑stay/:房型ID/calendar  要有UserLayout
      {
        path: 'stay/:roomTypeId/calendar',
        name: 'StayCalendar',
        component: () => import('@/views/stay/StayCalendar.vue'),
      },
      // 5/9 路徑stay/:房型ID/booking
      {
        path: 'stay/:roomTypeId/booking',
        name: 'StayBooking',
        component: () => import('@/views/stay/StayBooking.vue'),
      },
      // 5/13  訂單成功頁面
      {
        path: 'stay/booking-success',
        name: 'StayBookingSuccess',
        component: () => import('@/views/stay/StayBookingSuccess.vue'),
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
      },
      {
        path: '/heart',
        name: 'heart',
        component: HeartView,
        meta: { requiresAuth: true },
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
    ],
  },
  // 5/8 stay 路徑 不希望有UserLayout
  {
    path: '/stay',
    name: 'StayRoomTypeList',
    component: () => import('@/views/stay/StayRoomTypeList.vue'),
  },
  {
    // 購物車的router
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
        path: 'prettyorders/:appointmentId',
        name: 'prettyOrderDetail',
        component: () => import('@/views/BeautyAppointmentDetailView.vue'),
      },
      {
        path: 'stayorders',
        name: 'stayorders',
        component: () => import('@/views/StayOrderView.vue'),
      },
    ],
  },
  {
<<<<<<< HEAD
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
=======
    // 登入頁（獨立頁面，不套用 UserLayout）
>>>>>>> main
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
      { path: 'favorites', component: favoritesView },
    ],
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/ResetPassword.vue'),
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

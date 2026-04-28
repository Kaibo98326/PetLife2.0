import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginMember from '@/views/LoginMember.vue'
import MemberCenter from '@/views/MemberCenter.vue'
import ProfileView from '@/views/ProfileView.vue'


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
    children:[
      {path: 'profile', component: ProfileView},
      
    ]
  },

]


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router

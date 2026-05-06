import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginMember from '@/views/LoginMember.vue'
import MemberCenter from '@/views/MemberCenter.vue'
import ProfileView from '@/views/ProfileView.vue'
import PetListView from '@/views/PetListView.vue'
import AddPetView from '@/views/AddPetView.vue'


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
      {path: 'pets' , component: PetListView},
      {path: 'pets/add',component: AddPetView},
      
    ]
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

<script setup>
import { ref } from 'vue'
import logo from '@/assets/images/logo01.png'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'

const keyword = ref('')
const cartCount = ref(0) // 之後從小吉的 Pinia 或 API 拿
const userStore = useUserStore()
userStore.initFromLocalStorage()

function searchProducts() {
  console.log('搜尋:', keyword.value)
  // 這裡可以透過 router 跳轉到搜尋結果頁
}

const handleLogout = () => {
  Swal.fire({
    icon: 'warning',
    title: '確定要登出嗎？',
    text: '登出後需要重新登入才能使用會員功能',
    showCancelButton: true,
    confirmButtonText: '是的，登出',
    cancelButtonText: '取消'
  }).then((result) => {
    if (result.isConfirmed) {
      userStore.logout()
      Swal.fire({
        icon: 'success',
        title: '已登出',
        text: '期待您再次回來！',
        confirmButtonText: '回首頁'
      }).then(() => {
        window.location.href = '/'
      })
    }
  })
}
</script>

<template>
  <div class="user-layout">
    <header class="shop-header sticky-top">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-2">
            <router-link to="/" class="shop-logo">
              <img :src="logo" alt="PetLife Logo" class="img-fluid" />
            </router-link>
          </div>
          <div class="col px-lg-5">
            <form @submit.prevent="searchProducts" class="shop-search-form d-flex">
              <input v-model="keyword" type="text" class="form-control search-input" placeholder="搜尋毛孩好物..." />
              <button type="submit" class="btn search-submit-btn"><i class="fas fa-search"></i></button>
            </form>
          </div>
          <div class="col-auto">
            <nav class="shop-user-nav d-flex align-items-center">
              <router-link to="/cart" class="nav-icon-item me-3">
                <i class="fas fa-shopping-cart"></i>
                <span>購物車</span>
                <span v-if="cartCount > 0" class="badge bg-danger">{{ cartCount }}</span>
              </router-link>
              <router-link v-if="!userStore.memberId" to="/login" class="nav-icon-item">
                <i class="far fa-user-circle"></i>
                <span>登入/註冊</span>
              </router-link>
              <div v-else class="user-action-zone">
                <span class="welcome-tag"><i class="fas fa-paw"></i>{{ userStore.memberName }} 你好!</span>
                <router-link to="/member/center" class="member-link">會員中心</router-link>
                <button @click="handleLogout" class="btn btn-link text-danger">登出</button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </header>

    <nav class="header-nav mt-3">
      <div class="container">
        <ul class="nav nav-pills nav-menu-list d-flex justify-content-center">
          <li class="nav-item"><router-link to="/products" class="nav-link">全部商品</router-link></li>
          <li class="nav-item"><router-link to="/products/cat" class="nav-link">🐱 貓貓專區</router-link></li>
          <li class="nav-item"><router-link to="/products/dog" class="nav-link">🐶 狗狗專區</router-link></li>
          <li class="nav-item"><router-link to="/beauty" class="nav-link">🛁 寵物美容</router-link></li>
          <li class="nav-item"><router-link to="/hotel" class="nav-link">🏠 寵物旅館</router-link></li>
        </ul>
      </div>
    </nav>

    <router-view />

    <footer class="container text-center py-4 border-top mt-5">
      <p>© 2026 PetLife 寵物複合式商店</p>
    </footer>
  </div>
</template>
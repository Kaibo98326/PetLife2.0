<script setup>
import { ref } from 'vue'
import logo from '@/assets/images/logo01.png'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'


// 搜尋框 & 購物車
const keyword = ref('')
const cartCount = ref(0)
function searchProducts() {
  console.log('搜尋:', keyword.value)
}

// Pinia store
const userStore = useUserStore()
userStore.initFromLocalStorage()

// 模擬商品資料
const products = ref([
  { id: 1, name: '貓咪飼料', price: 500, image: '/images/products/catfood.jpg', category: '飼料' },
  { id: 2, name: '狗狗玩具', price: 300, image: '/images/products/dogtoy.jpg', category: '玩具' }
])

// 輪播圖片
import ad01 from '@/assets/images/ad01.jpg'
import ad02 from '@/assets/images/ad02.jpg'
import ad03 from '@/assets/images/ad03.jpg'
import ad04 from '@/assets/images/ad04.jpg'
import ad05 from '@/assets/images/ad05.jpg'
import ad06 from '@/assets/images/ad06.jpg'

const carouselImages = ref([
  { src: ad01, alt: '廣告輪播01' },
  { src: ad02, alt: '廣告輪播02' },
  { src: ad03, alt: '廣告輪播03' },
  { src: ad04, alt: '廣告輪播04' },
  { src: ad05, alt: '廣告輪播05' },
  { src: ad06, alt: '廣告輪播06' }
])

const handleLogout = () =>{
  Swal.fire({
    icon: 'warning',
    title: '確定要登出嗎？',
    text: '登出後需要重新登入才能使用會員功能',
    showCancelButton: true,
    confirmButtonText: '是的，登出',
    cancelButtonText: '取消'
  }).then((result) => {
    if(result.isConfirmed){
      userStore.logout()
      Swal.fire({
        icon: 'success',
        title: '已登出',
        text: '期待您再次回來！',
        confirmButtonText: '回首頁'
      }).then(() =>{
        window.location.href = '/'
        
      } )
    }
  })
}
</script>

<template>
  <div class="home">
    <!-- Header -->
    <header class="shop-header sticky-top">
      <div class="container">
        <div class="row align-items-center">
          <!-- LOGO -->
          <div class="col-2">
            <router-link to="/" class="shop-logo">
              <img :src="logo" alt="PetLife Logo" class="img-fluid" />
            </router-link>
          </div>

          <!-- 搜尋框 -->
          <div class="col px-lg-5">
            <form @submit.prevent="searchProducts" class="shop-search-form d-flex">
              <input v-model="keyword" type="text" class="form-control search-input"
                placeholder="搜尋毛孩好物..." />
              <button type="submit" class="btn search-submit-btn">
                <i class="fas fa-search"></i>
              </button>
            </form>
          </div>

          <!-- 右側選單 -->
          <div class="col-auto">
            <nav class="shop-user-nav d-flex align-items-center">
              <!-- 購物車 -->
              <router-link to="/cart" class="nav-icon-item me-3">
                <i class="fas fa-shopping-cart"></i>
                <span>購物車</span>
                <span v-if="cartCount > 0" class="badge bg-danger">{{ cartCount }}</span>
              </router-link>

              <!-- 登入/會員 -->
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

    <!-- 分類選單 -->
    <nav class="header-nav mt-3">
      <div class="container">
        <ul class="nav nav-pills nav-menu-list d-flex justify-content-center">
          <li class="nav-item"><router-link to="/products/all" class="nav-link">全部商品</router-link></li>
          <li class="nav-item"><router-link to="/products/cat" class="nav-link">🐱 貓貓專區</router-link></li>
          <li class="nav-item"><router-link to="/products/dog" class="nav-link">🐶 狗狗專區</router-link></li>
          <li class="nav-item"><router-link to="/beauty" class="nav-link">🛁 寵物美容</router-link></li>
          <li class="nav-item"><router-link to="/hotel" class="nav-link">🏠 寵物旅館</router-link></li>
          <li class="nav-item"><router-link to="/sale" class="nav-link">🔥 限時特惠</router-link></li>
        </ul>
      </div>
    </nav>

    <!-- 廣告輪播 + Sidebar -->
    <div class="container d-flex">
      <aside class="category-sidebar shadow-sm me-3">
        <div class="sidebar-title"><i class="fas fa-bars me-2"></i>毛孩商品總覽</div>
        <nav class="sidebar-nav">
          <router-link to="/products/all" class="category-item">全部商品</router-link>
          <router-link to="/products/cat" class="category-item">貓貓專區</router-link>
          <router-link to="/products/dog" class="category-item">狗狗專區</router-link>
        </nav>
      </aside>

      <div id="shopCarousel" class="carousel slide shadow-sm rounded-3 overflow-hidden flex-grow-1"
        data-bs-ride="carousel">
        <div class="carousel-indicators">
          <button v-for="(img, index) in carouselImages" :key="index" type="button"
            data-bs-target="#shopCarousel" :data-bs-slide-to="index" :class="{ active: index === 0 }"></button>
        </div>
        <div class="carousel-inner">
          <div v-for="(img, index) in carouselImages" :key="index" class="carousel-item"
            :class="{ active: index === 0 }" data-bs-interval="2000">
            <img :src="img.src" :alt="img.alt" class="d-block w-100 img-fluid"
              style="object-fit: cover; max-height: 500px;">
          </div>
        </div>
        <button class="carousel-control-prev" type="button" data-bs-target="#shopCarousel" data-bs-slide="prev">
          <span class="carousel-control-prev-icon"></span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#shopCarousel" data-bs-slide="next">
          <span class="carousel-control-next-icon"></span>
        </button>
      </div>
    </div>

    <!-- 商品卡片列表 -->
    <section class="container product-section py-5">
      <div class="row row-cols-2 row-cols-md-4 g-4">
        <div v-for="p in products" :key="p.id" class="col">
          <article class="product-card shadow-sm">
            <div class="product-category-badge">{{ p.category }}</div>
            <router-link :to="`/product/${p.id}`" class="text-decoration-none">
              <div class="product-img-wrapper">
                <img :src="p.image" :alt="p.name" />
              </div>
              <div class="product-info">
                <h6 class="product-name">{{ p.name }}</h6>
              </div>
            </router-link>
            <div class="product-footer">
              <span class="product-price">$ {{ p.price }}</span>
              <button type="button" class="btn add-to-cart-btn">
                <i class="fas fa-shopping-basket"></i>
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

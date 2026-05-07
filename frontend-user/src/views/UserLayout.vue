<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import logo from '@/assets/images/logo01.png'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'

// ── Pinia Store ───────────────────────────────────────────────────────────
const userStore = useUserStore()
const router = useRouter()

// ── 搜尋 ──────────────────────────────────────────────────────────────────
const keyword = ref('')

/** 搜尋後跳回首頁並帶 query，讓 HomeView 接收 */
function searchProducts() {
  router.push({ path: '/', query: { keyword: keyword.value } })
}

/** 回到首頁並重置狀態 (LOGO 使用) */
function goHome() {
  keyword.value = ''
  router.push('/')
}

/** 導向全部商品 (隱藏輪播) */
function goToAllProducts() {
  keyword.value = ''
  router.push({ path: '/', query: { view: 'all' } })
}

// ── 登出 ──────────────────────────────────────────────────────────────────
const handleLogout = () => {
  Swal.fire({
    icon: 'warning',
    title: '確定要登出嗎？',
    text: '登出後需要重新登入才能使用會員功能',
    showCancelButton: true,
    confirmButtonText: '是的，登出',
    cancelButtonText: '取消',
    confirmButtonColor: '#e67e22'
  }).then((result) => {
    if (result.isConfirmed) {
      userStore.logout()
      Swal.fire({
        icon: 'success',
        title: '已登出',
        text: '期待您再次回來！',
        confirmButtonText: '回首頁',
        confirmButtonColor: '#e67e22'
      }).then(() => {
        router.push('/')
      })
    }
  })
}

// ── 初始化：讀取 LocalStorage 中的 Token ──────────────────────────────────
onMounted(async () => {
  userStore.initFromLocalStorage()
  if (userStore.token) {
    await userStore.fetchUser()
  }
})
</script>

<template>
  <div class="user-layout">
    <!-- ========== Header 頂端導覽 ========== -->
    <header class="shop-header sticky-top">
      <div class="container-fluid px-lg-5">
        <div class="row align-items-center">
          <div class="col-auto">
            <a href="#" class="shop-logo" @click.prevent="goHome">
              <img :src="logo" alt="PetLife Logo" />
            </a>
          </div>

          <!-- 搜尋框 -->
          <div class="col px-lg-5">
            <form @submit.prevent="searchProducts" class="shop-search-form">
              <div class="input-group">
                <input
                  v-model="keyword"
                  type="text"
                  class="form-control search-input"
                  placeholder="搜尋毛孩好物..."
                />
                <button type="submit" class="btn search-submit-btn">
                  <i class="fas fa-search text-muted"></i>
                </button>
              </div>
            </form>
          </div>

          <!-- 頂端右側 -->
          <div class="col-auto">
            <nav class="shop-user-nav d-flex align-items-center">
              <!-- 購物車 -->
              <router-link
                to="/cart"
                class="nav-icon-item position-relative d-flex flex-column align-items-center text-decoration-none"
              >
                <i class="fas fa-shopping-cart"></i>
                <span style="font-size: 13px; color: #5a4a42;">購物車</span>
                <span
                  v-if="userStore.cartCount > 0"
                  class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
                  style="font-size: 0.7rem;"
                >{{ userStore.cartCount }}</span>
              </router-link>

              <!-- 聊聊 -->
              <a
                href="#"
                class="nav-icon-item d-flex flex-column align-items-center text-decoration-none"
              >
                <i class="far fa-comment-dots" style="font-size: 1.2rem; color: #666;"></i>
                <span style="font-size: 13px; color: #5a4a42;">聊聊</span>
              </a>

              <!-- 登入判斷 -->
              <div class="user-action-zone">
                <!-- 已登入 -->
                <div v-if="userStore.token" class="d-flex align-items-center">
                  <div class="user-greeting-box me-3 text-end">
                    <span class="welcome-tag">
                      <i class="fas fa-paw"></i>
                      <strong>{{ userStore.user?.memberName }}</strong> 你好！
                    </span>
                    <router-link to="/member/center" class="member-link">會員中心</router-link>
                  </div>
                  <a
                    href="#"
                    class="nav-icon-item d-flex flex-column align-items-center text-decoration-none text-danger"
                    @click.prevent="handleLogout"
                  >
                    <i class="fas fa-sign-out-alt" style="font-size: 1.2rem;"></i>
                    <span style="font-size: 13px;">登出</span>
                  </a>
                </div>
                <!-- 尚未登入 -->
                <div v-else>
                  <router-link
                    to="/login"
                    class="nav-icon-item d-flex flex-column align-items-center text-decoration-none"
                  >
                    <i class="far fa-user-circle" style="font-size: 1.2rem; color: #666;"></i>
                    <span style="font-size: 13px; color: #5a4a42;">登入 / 註冊</span>
                  </router-link>
                </div>
              </div>
            </nav>
          </div>
        </div>

        <!-- 上層分類目錄 -->
        <nav class="header-nav mt-3">
          <div class="container-fluid px-lg-5">
            <ul class="nav-menu-list">
              <li><a href="#" class="nav-menu-link" @click.prevent="goToAllProducts">全部商品</a></li>
              <li><router-link :to="{ path: '/', query: { catId: 1 } }" class="nav-menu-link">🐱 貓貓專區</router-link></li>
              <li><router-link :to="{ path: '/', query: { catId: 2 } }" class="nav-menu-link">🐶 狗狗專區</router-link></li>
              <li><router-link to="/beauty-booking" class="nav-menu-link">🛁 寵物美容</router-link></li>
              <li><router-link to="/hotel" class="nav-menu-link">🏠 寵物旅館</router-link></li>
              <li><a href="#" class="nav-menu-link">🔥 限時特惠</a></li>
            </ul>
          </div>
        </nav>
      </div>
    </header>

    <!-- ========== 主內容區（子路由渲染） ========== -->
    <router-view />

    <!-- ========== Footer ========== -->
    <footer class="container-fluid text-center py-4 border-top mt-5">
      <p class="mb-0">© 2026 PetLife 寵物複合式商店</p>
    </footer>
  </div>
</template>

<style>
/* 引入商城主樣式 — 不加 scoped，讓 ShopPanel.css 的全域樣式生效 */
@import '@/assets/css/ShopPanel.css';
</style>
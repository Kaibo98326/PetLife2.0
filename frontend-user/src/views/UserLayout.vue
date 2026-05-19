<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from '@/axios.js'
import logo from '@/assets/images/logo01.png'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'
// 引入新組件
import ChatBot from '@/components/ChatBot.vue'

// ── Pinia Store ───────────────────────────────────────────────────────────
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

// ── 搜尋相關 ──────────────────────────────────────────────────────────────
const keyword = ref('')
const hotKeywords = ref([])

// ── 聊天機器人連動 ────────────────────────────────────────────────────────
const isChatOpen = ref(false)
const chatBotRef = ref(null)

function toggleChat() {
  chatBotRef.value?.toggleChat()
}

// ── 導覽分類選單 ──────────────────────────────────────────────────────────
// ── 分類選單資料 ──────────────────────────────────────────────────────────
const categories = ref([])

async function fetchCategories() {
  try {
    const res = await axios.get('/shop/categories')
    categories.value = (res.data || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) {
    console.error('取得導覽分類失敗', e)
  }
}

// 動態大專區 (Type 2)
const mainAreas = computed(() => {
  return categories.value.filter(c => c.categoryType === 2)
})

// // 動態大專區 (Type 2)
// const mainAreas = computed(() => categories.value.filter((c) => c.categoryType === 2))



// ✨ 上方選單優惠活動下拉式徽章 (拆分父標題與子選項)
const activityParent = computed(() => {
  return categories.value.find(c => c.categoryType === 3 && c.categoryId === 3)
})

const activityChildren = computed(() => {
  return categories.value.filter(c => c.categoryType === 3 && c.categoryId !== 3)
})

/** ✨分類跳轉功能 (供下拉選單使用) */
function selectCategory(categoryId) {
  keyword.value = ''
  router.push({ path: '/', query: { catId: categoryId } })
}



// ── 搜尋邏輯 ──────────────────────────────────────────────────────────────
async function fetchHotKeywords() {
  try {
    const res = await axios.get('/shop/search/hot')
    hotKeywords.value = res.data || []
  } catch (e) {
    console.error('取得熱門關鍵字失敗', e)
  }
}

function clickHotKeyword(kw) {
  keyword.value = kw
  searchProducts()
}

watch(
  () => route.query.keyword,
  (newVal) => {
    keyword.value = newVal || ''
  },
  { immediate: true },
)

watch(
  () => route.query.keyword,
  (newVal) => {
    keyword.value = newVal || ''
  },
  { immediate: true },
)

/** 搜尋後跳回首頁並帶 query，讓 HomeView 接收 */
function searchProducts() {
  router.push({ path: '/', query: { keyword: keyword.value } })
}

function goHome() {
  keyword.value = ''
  router.push('/')
}

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
    confirmButtonColor: '#e67e22',
  }).then((result) => {
    if (result.isConfirmed) {
      userStore.logout()
      Swal.fire({
        icon: 'success',
        title: '已登出',
        text: '期待您再次回來！',
        confirmButtonText: '回首頁',
        confirmButtonColor: '#e67e22',
      }).then(() => router.push('/'))
    }
  })
}

// ── 初始化 ────────────────────────────────────────────────────────────────
onMounted(async () => {
  userStore.initFromLocalStorage()
  if (userStore.token) {
    await userStore.fetchUser()
  }
  await fetchCategories()
  await fetchHotKeywords()
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
          <div class="col position-relative">
            <form @submit.prevent="searchProducts" class="shop-search-form">
              <div class="input-group">
                <input
                  v-model="keyword"
                  type="text"
                  class="form-control search-input"
                  placeholder="請輸入關鍵字"
                />
                <button type="submit" class="btn search-submit-btn">
                  <i class="fas fa-search text-muted"></i>
                </button>
              </div>
            </form>
            <!-- 熱門關鍵字 -->
            <div class="hot-keywords-row d-none d-lg-flex">
              <div class="hot-keyword-list">
                <a
                  v-for="item in hotKeywords"
                  :key="item.id"
                  href="#"
                  class="hot-keyword-item"
                  @click.prevent="clickHotKeyword(item.keyword)"
                >
                  {{ item.keyword }}
                </a>
              </div>
            </div>
          </div>
          <!-- 頂端右側 -->
          <div class="col-auto">
            <nav class="shop-user-nav d-flex align-items-center">
              <!-- 收藏清單 -->
              <router-link
                to="/heart"
                class="nav-icon-item d-flex flex-column align-items-center text-decoration-none"
              >
                <i class="fas fa-heart"></i>
                <span>收藏清單</span>
              </router-link>
              <!-- 購物車 -->
              <router-link
                to="/cart"
                class="nav-icon-item position-relative d-flex flex-column align-items-center text-decoration-none"
              >
                <i class="fas fa-shopping-cart"></i>
                <span>購物車</span>
                <span
                  v-if="userStore.cartCount > 0"
                  class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger text-white"
                  style="font-size: 0.7rem"
                  >{{ userStore.cartCount }}</span
                >
              </router-link>

              <!-- 聊聊 (透過組件控制) -->
              <a
                href="#"
                class="nav-icon-item d-flex flex-column align-items-center text-decoration-none"
                :class="{ active: isChatOpen }"
                @click.prevent="toggleChat"
              >
                <i class="far fa-comment-dots"></i>
                <span>聊聊</span>
              </a>
              <!-- 登入判斷 -->
              <div class="user-action-zone">
                <div v-if="userStore.token" class="d-flex align-items-center">
                  <div class="user-greeting-box me-3 text-end">
                    <span class="welcome-tag" style="color: orange"
                      ><i class="fas fa-paw"></i
                      ><strong>{{ userStore.user?.memberName }}</strong> 你好！</span
                    >
                    <router-link to="/member/center" class="member-link">會員中心</router-link>
                  </div>
                  <a
                    href="#"
                    class="nav-icon-item logout-btn d-flex flex-column align-items-center text-decoration-none"
                    @click.prevent="handleLogout"
                  >
                    <i class="fas fa-sign-out-alt"></i><span>登出</span>
                  </a>
                </div>
                <div v-else>
                  <router-link
                    to="/login"
                    class="nav-icon-item d-flex flex-column align-items-center text-decoration-none"
                  >
                    <i class="far fa-user-circle"></i><span>登入 / 註冊</span>
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
              <li v-for="area in mainAreas" :key="area.categoryId">
                <router-link
                  :to="{ path: '/', query: { catId: area.categoryId } }"
                  class="nav-menu-link"
                  >{{ area.categoryName }}</router-link
                >
              </li>
              <li v-if="activityParent" class="nav-item dropdown activity-nav-dropdown">
                <div 
                  class="nav-menu-link activity-hover-toggle"
                >
                  {{ activityParent.categoryName }} <i class="fas fa-caret-down ms-2"></i>
                </div>
                <ul class="dropdown-menu shadow border-0 dropdown-menu-hover">
                  <!-- 動態活動標籤 (Type 3) -->
                  <li v-for="tag in activityChildren" :key="tag.categoryId">
                    <a class="dropdown-item py-2 text-dark" href="#" @click.prevent="selectCategory(tag.categoryId)">
                      {{ tag.categoryName }}
                    </a>
                  </li>
                </ul>
              </li>
              <li>
                <router-link to="/beauty-booking" class="nav-menu-link">🛁 寵物美容</router-link>
              </li>
              <li><router-link to="/stay" class="nav-menu-link">🏠 寵物旅館</router-link></li>
            </ul>
          </div>
        </nav>
      </div>
    </header>

    <!-- ========== 主內容區 ========== -->
    <router-view />
    <footer class="container-fluid text-center py-4 border-top mt-5">
      <p class="mb-0">© 2026 PetLife 寵物複合式商店</p>
    </footer>

    <!-- ── 聊天機器人組件 ── -->
    <ChatBot ref="chatBotRef" v-model:isOpen="isChatOpen" />
  </div>
</template>

<style>
@import '@/assets/css/UserLayout.css';
@import '@/assets/css/ShopPanel.css';
</style>

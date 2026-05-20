<template>
  <div class="wishlist-page">
    <div class="top-accent-line"></div>

    <div class="container py-5">
      <!-- 標題導航區 -->
      <div class="header-wrapper mb-5">
        <div class="row align-items-end">
          <div class="col-md-7 text-start">
            <p class="wishlist-count">
              目前已收藏 <span>{{ favoriteList.length }}</span> 件商品
            </p>
          </div>
          <div class="col-md-5 text-md-end d-flex justify-content-md-end gap-3 pb-2">
            <router-link to="/cart" class="nav-solid-btn">
              前往結帳 <i class="bi bi-cart-check ms-1"></i>
            </router-link>
          </div>
        </div>
      </div>

      <!-- 讀取狀態 -->
      <div v-if="loading" class="loader-container text-center py-5">
        <div class="warm-spinner mx-auto"></div>
        <p class="mt-3 text-muted">正在搬運您的收藏清單...</p>
      </div>

      <!-- 列表區 -->
      <div v-else-if="favoriteList.length > 0">
        <!-- 卡片一排 5 個 -->
        <div class="row g-3 row-cols-2 row-cols-md-4 row-cols-lg-5">
          <div v-for="item in paginatedList" :key="item.heartId" class="col">
            <div class="pet-card clickable-card" @click="goToProduct(item.product.productId)">
              <div class="img-wrapper">
                <img
                  :src="
                    item.product.productImage
                      ? `http://localhost:8082${item.product.productImage.startsWith('/') ? '' : '/'}${item.product.productImage}`
                      : '/default-img.png'
                  "
                  class="product-img"
                  alt="Product Image"
                />
                <div class="category-tag">{{ item.product.categoryName || '精品' }}</div>
              </div>

              <div class="product-details">
                <h3 class="product-name">{{ item.product.productName }}</h3>
                <div class="price-val">NT$ {{ item.product.productPrice.toLocaleString() }}</div>

                <div class="action-group">
                  <!-- ⚠️ 按鈕皆加上 .stop 以防點擊按鈕時也跑去商品頁 -->
                  <button class="action-btn cart-btn-orange" @click.stop="addToCart(item.product)">
                    <i class="bi bi-basket2-fill"></i> 加入購物車
                  </button>
                  <button
                    class="action-btn remove-btn-minimal"
                    @click.stop="handleRemove(item.product.productId)"
                  >
                    <i class="bi bi-trash3"></i> 取消收藏
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分頁器 -->
        <nav class="pagination-container mt-5">
          <ul class="luxury-pagination">
            <!-- 頁碼 -->
            <li v-for="page in totalPages" :key="page">
              <button
                :class="['page-num-btn', { active: currentPage === page }]"
                @click="changePage(page)"
              >
                {{ String(page).padStart(2, '0') }}
              </button>
            </li>
          </ul>
        </nav>
      </div>

      <!-- 3. 空白狀態 -->
      <div v-else class="empty-state text-center py-5">
        <div class="empty-visual mb-4">
          <i class="bi bi-suit-heart heart-bg"></i>
          <i class="bi bi-plus heart-plus"></i>
        </div>
        <h3 class="fw-bold">收藏清單還是空的喔！</h3>
        <router-link to="/shop" class="nav-solid-btn mt-3 px-5 py-2 d-inline-block"
          >開始購物</router-link
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const favoriteList = ref([])
const loading = ref(true)
const currentPage = ref(1)
const itemsPerPage = 10

// --- 分頁邏輯修正 ---
const totalPages = computed(() => {
  const total = Math.ceil(favoriteList.value.length / itemsPerPage)
  // 強制最少要有1頁，這樣01按鈕才會出現
  return total > 0 ? total : 1
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return favoriteList.value.slice(start, end)
})

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

watch(totalPages, (newTotal) => {
  if (currentPage.value > newTotal) currentPage.value = newTotal || 1
})

// --- 導頁功能 ---
const goToProduct = (productId) => {
  // 對齊 HomeView.vue 的 /product/:id 格式
  router.push(`/product/${productId}`)
}

// --- API 與功能邏輯 ---
const fetchWishlist = async () => {
  if (!userStore.memberId) {
    loading.value = false
    return
  }
  try {
    loading.value = true
    const res = await axios.get('/heart/list', { params: { memberId: userStore.memberId } })
    favoriteList.value = res.data
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleRemove = async (productId) => {
  const result = await Swal.fire({
    title: '移除收藏？',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#ff8c00',
    confirmButtonText: '確定',
    cancelButtonText: '取消',
  })
  if (result.isConfirmed) {
    try {
      await axios.delete('/heart/remove', { params: { memberId: userStore.memberId, productId } })
      favoriteList.value = favoriteList.value.filter((i) => i.product.productId !== productId)
    } catch (err) {
      console.error(err)
      Swal.fire('錯誤', '操作失敗', 'error')
    }
  }
}

const addToCart = async (product) => {
  if (!userStore.token) {
    Swal.fire({ icon: 'warning', title: '請先登入', confirmButtonColor: '#ff8c00' })
    return
  }
  try {
    await axios.post(`/cart/add/${userStore.memberId}`, {
      productId: product.productId,
      quantity: 1,
    })
    if (userStore.updateCartCount) userStore.updateCartCount()
    Swal.fire({ icon: 'success', title: '已加入購物車', timer: 1000, showConfirmButton: false })
  } catch (e) {
    console.error(e)
    Swal.fire({ icon: 'error', title: '加入失敗' })
  }
}

onMounted(fetchWishlist)
</script>

<style scoped>
@import '@/assets/css/HeartView.css';
</style>

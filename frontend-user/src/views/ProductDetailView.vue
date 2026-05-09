<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── 後端圖片基礎 URL ──────────────────────────────────────────────────────
const IMG_BASE = 'http://localhost:8082'

/** 組合商品圖片完整 URL */
function getImageUrl(imagePath) {
  if (!imagePath || imagePath === 'default_product.jpg') {
    return `${IMG_BASE}/images/products/default_product.jpg`
  }
  return `${IMG_BASE}/${imagePath}`
}

// ── 商品資料 ──────────────────────────────────────────────────────────────
const product = ref(null)
const loading = ref(false)
const errorMsg = ref('')
/** 購買數量 */
const quantity = ref(1)

// ── 取得商品詳情 ──────────────────────────────────────────────────────────
async function fetchProduct() {
  loading.value = true
  errorMsg.value = ''
  try {
    const id = route.params.id
    const res = await axios.get(`/products/detail/${id}`)
    product.value = res.data
    // 若商品不存在或已下架
    if (!product.value || product.value.productStatus !== 1) {
      errorMsg.value = '此商品不存在或已下架'
      product.value = null
    }
  } catch (e) {
    console.error('取得商品詳情失敗', e)
    errorMsg.value = '商品載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}

// ── 數量控制 ──────────────────────────────────────────────────────────────
function increaseQty() {
  if (product.value && quantity.value < product.value.productStock) {
    quantity.value++
  }
}
function decreaseQty() {
  if (quantity.value > 1) quantity.value--
}

// ── 返回商城首頁 ──────────────────────────────────────────────────────────
function goBack() {
  router.push('/')
}

// ── 登入檢查（共用） ──────────────────────────────────────────────────────
async function requireLogin() {
  if (!userStore.token) {
    await Swal.fire({
      icon: 'warning',
      title: '請先登入',
      text: '登入後才能進行購買或加入購物車喔！',
      confirmButtonText: '前往登入',
      confirmButtonColor: '#e67e22'
    })
    router.push('/login')
    return false
  }
  return true
}

// ── 立即購買（加入購物車後跳轉到購物車頁） ─────────────────────────────────
async function directBuy() {
  if (!(await requireLogin())) return
  if (!product.value || quantity.value <= 0) return

  try {
    await axios.post(`/api/cart/add/${userStore.memberId}`, {
      productId: product.value.productId,
      quantity: quantity.value
    })
    // 直接跳到購物車頁
    router.push('/cart')
  } catch (e) {
    console.error('購買失敗', e)
    Swal.fire({
      icon: 'error',
      title: '購買失敗',
      text: '請稍後再試',
      confirmButtonColor: '#e67e22'
    })
  }
}

// ── 加入購物車 ────────────────────────────────────────────────────────────
async function addToCart() {
  if (!(await requireLogin())) return
  if (quantity.value <= 0) {
    Swal.fire({ icon: 'warning', title: '數量必須大於 0', confirmButtonColor: '#e67e22' })
    return
  }

  try {
    await axios.post(`/api/cart/add/${userStore.memberId}`, {
      productId: product.value.productId,
      quantity: quantity.value
    })
    Swal.fire({
      icon: 'success',
      title: '加入成功！',
      text: `已將 ${quantity.value} 件「${product.value.productName}」放進購物車`,
      timer: 1500,
      showConfirmButton: false
    })
  } catch (e) {
    console.error('加入購物車失敗', e)
    Swal.fire({
      icon: 'error',
      title: '加入購物車失敗',
      text: '請稍後再試',
      confirmButtonColor: '#e67e22'
    })
  }
}

// ── 初始化 ────────────────────────────────────────────────────────────────
onMounted(fetchProduct)
</script>

<template>
  <div class="container">
    <!-- 商品詳情卡片 -->
    <div class="product-detail-container">
      <!-- 載入中 -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-warning" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="mt-3 text-muted">商品載入中...</p>
      </div>

      <!-- 錯誤訊息 -->
      <div v-else-if="errorMsg" class="alert alert-warning text-center py-5">
        <i class="fas fa-exclamation-circle fa-2x mb-3 d-block"></i>
        {{ errorMsg }}
        <div class="mt-3">
          <button class="btn btn-outline-secondary rounded-pill" @click="goBack">回到商城</button>
        </div>
      </div>

      <!-- 商品詳情內容 -->
      <div v-else-if="product" class="row g-5">
        <!-- 左側：商品圖片 -->
        <div class="col-md-6">
          <div class="main-image-wrapper">
            <img
              :src="getImageUrl(product.productImage)"
              :alt="product.productName"
              class="img-fluid rounded"
              @error="$event.target.src = `${IMG_BASE}/images/products/default_product.jpg`"
            />
          </div>
        </div>

        <!-- 右側：商品資訊 -->
        <div class="col-md-6">
          <!-- 麵包屑 -->
          <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
              <li class="breadcrumb-item">
                <a href="#" class="text-decoration-none" style="color: #999;" @click.prevent="goBack">首頁</a>
              </li>
              <li class="breadcrumb-item active">{{ product.categoryName || '商品' }}</li>
            </ol>
          </nav>

          <!-- 分類標籤 -->
          <span class="product-category-tag"># {{ product.categoryName || '商品' }}</span>

          <!-- 商品名稱 -->
          <h1 class="product-title">{{ product.productName }}</h1>

          <!-- 價格 -->
          <div class="product-price-large">
            $ {{ Number(product.productPrice).toLocaleString() }}
          </div>

          <!-- 規格選擇 -->
          <div class="mb-4">
            <label class="form-label fw-bold">規格選擇</label>
            <select class="form-select rounded-pill">
              <option>預設規格</option>
            </select>
          </div>

          <!-- 數量選擇 -->
          <label class="form-label fw-bold">數量</label>
          <div class="quantity-control d-flex align-items-center gap-2">
            <button
              type="button"
              class="btn btn-outline-secondary btn-qty"
              @click="decreaseQty"
              :disabled="quantity <= 1"
            >-</button>
            <input
              type="number"
              v-model.number="quantity"
              min="1"
              :max="product.productStock"
              class="form-control text-center rounded-pill"
              style="width: 80px;"
            />
            <button
              type="button"
              class="btn btn-outline-secondary btn-qty"
              @click="increaseQty"
              :disabled="product.productStock <= 0 || quantity >= product.productStock"
            >+</button>
          </div>

          <!-- 立即購買 + 加入購物車（並排） -->
          <div class="d-flex gap-3 mt-4">
            <button
              type="button"
              class="btn btn-warning w-100 rounded-pill fw-bold"
              :disabled="product.productStock <= 0"
              @click="directBuy"
            >
              {{ product.productStock > 0 ? '立即購買' : '缺貨中' }}
            </button>
            <button
              type="button"
              class="btn btn-outline-dark w-100 rounded-pill fw-bold"
              :disabled="product.productStock <= 0"
              @click="addToCart"
            >
              加入購物車
            </button>
          </div>

          <!-- 商品描述 -->
          <div class="product-description-section mt-5" v-if="product.productDescription">
            <h5 class="desc-title border-bottom pb-2">商品介紹</h5>
            <p class="text-muted mt-3" style="line-height: 1.8; white-space: pre-wrap;">
              {{ product.productDescription }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* 引入商品詳情專用樣式 */
@import '@/assets/css/Product.css';
</style>

<style scoped>
/* ── 數量輸入框微調 ── */
.quantity-control input[type="number"]::-webkit-inner-spin-button,
.quantity-control input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.quantity-control input[type="number"] {
  -moz-appearance: textfield;
}
</style>

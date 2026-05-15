<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'
import '@/assets/css/ShopPanel.css'
import { Carousel } from 'bootstrap/dist/js/bootstrap.bundle.min'


// ── 使用者 Store（登入判斷、購物車） ──────────────────────────────────────
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// ── 輪播圖片 ──────────────────────────────────────────────────────────────
import ad01 from '@/assets/images/ad01.jpg'
import ad02 from '@/assets/images/ad02.jpg'
import ad03 from '@/assets/images/ad03.jpg'
import ad04 from '@/assets/images/ad04.jpg'
import ad05 from '@/assets/images/ad05.jpg'
import ad06 from '@/assets/images/ad06.jpg'
import { ca } from 'vuetify/locale'

const carouselImages = ref([
  { src: ad01, alt: '廣告輪播01' },
  { src: ad02, alt: '廣告輪播02' },
  { src: ad03, alt: '廣告輪播03' },
  { src: ad04, alt: '廣告輪播04' },
  { src: ad05, alt: '廣告輪播05' },
  { src: ad06, alt: '廣告輪播06' },
])

// ── 後端資料狀態 ──────────────────────────────────────────────────────────
const products = ref([]) // 商品列表（原始資料）
const categories = ref([]) // 分類列表
const selectedCategoryId = ref(null) // 選中的分類 ID
const searchKeyword = ref('') // 搜尋關鍵字
const currentPage = ref(1) // 目前頁碼
const totalPages = ref(1) // 總頁數
const totalElements = ref(0) // 商品總數
const loading = ref(false) // 載入狀態
const errorMsg = ref('') // 錯誤訊息
const viewHistory = ref([]) // 瀏覽紀錄
const top10Products = ref([]) // TOP10 熱銷商品

// ── 排序與狀態 ────────────────────────────────────────────────────────────
const sortBy = ref('newest') // 預設：最新上架
const pageSize = ref(25) // 每頁顯示筆數
const viewMode = ref('grid') // 檢視模式
const priceDirection = ref('asc') // 價格排序方向

/** 切換排序（價格按鈕會 toggle 方向） */
function setSort(key) {
  if (key === 'price') {
    if (sortBy.value === 'price_asc') {
      sortBy.value = 'price_desc'
      priceDirection.value = 'desc'
    } else {
      sortBy.value = 'price_asc'
      priceDirection.value = 'asc'
    }
  } else {
    sortBy.value = key
  }
}

/** 是否顯示輪播區域（搜尋、分類或點擊「全部商品」時隱藏） */
const showCarousel = computed(() => {
  return selectedCategoryId.value === null && !searchKeyword.value && route.query.view !== 'all'
})

/** TOP10 熱銷排行 (改用 API 取得) */
// const top10Products = computed(() => {
//   return [...products.value].slice(0, 10)
// })

/** 麵包屑導覽路徑 */
const breadcrumbs = computed(() => {
  const crumbs = [{ label: '', id: null }]

  if (searchKeyword.value) {
    crumbs.push({ label: `搜尋關鍵字：「${searchKeyword.value}」`, id: 'search' })
    return crumbs
  }

  if (selectedCategoryId.value) {
    const current = categories.value.find((c) => c.categoryId === selectedCategoryId.value)
    if (current) {
      // 如果有父分類 (大專區)，先放進去
      if (current.parentId) {
        const parent = categories.value.find((c) => c.categoryId === current.parentId)
        if (parent) {
          crumbs.push({ label: parent.categoryName, id: parent.categoryId })
        }
      }
      crumbs.push({ label: current.categoryName, id: current.categoryId })
    }
    return crumbs
  }

  if (route.query.view === 'all') {
    crumbs.push({ label: '全部商品', id: 'all' })
  }

  return crumbs
})

/** 頁面標題文字 */
const pageTitle = computed(() => {
  const lastCrumb = breadcrumbs.value[breadcrumbs.value.length - 1]
  return lastCrumb ? lastCrumb.label : '精選好物'
})

// ── 後端圖片基礎 URL ──────────────────────────────────────────────────────
const IMG_BASE = 'http://localhost:8082'

/** 組合商品圖片完整 URL */
function getImageUrl(imagePath) {
  if (!imagePath || imagePath === 'default.jpg' || imagePath === 'default_product.jpg') {
    return `${IMG_BASE}/images/products/default.jpg`
  }
  return `${IMG_BASE}/${imagePath}`
}

// ── 取得分類列表 ──────────────────────────────────────────────────────────
async function fetchCategories() {
  try {
    const res = await axios.get('/shop/categories')
    // 確保原始資料就先照 sortOrder 排序
    categories.value = (res.data || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) {
    console.error('取得分類失敗', e)
  }
}

/**
 * 核心：將扁平分類轉換為樹狀結構 (同步後台排序)
 * 結構：[ { ...area, children: [ ...physical ] }, ...activityTags ]
 */
const categoryTree = computed(() => {
  const all = categories.value
  const tree = []

  // 1. 先找出所有「大專區」(Type 2)
  const mainAreas = all.filter((c) => c.categoryType === 2)

  mainAreas.forEach((area) => {
    // 2. 找出屬於該專區的「實體分類」(Type 1)
    const children = all.filter((c) => c.parentId === area.categoryId && c.categoryType === 1)
    tree.push({
      ...area,
      children: children,
    })
  })

  // 3. 處理「活動標籤」(Type 3) - 獨立成一個區塊
  const activityTags = all.filter((c) => c.categoryType === 3)

  return {
    shopTree: tree,
    activityTags: activityTags,
  }
})

// ── 取得商品列表 ──────────────────────────────────────────────────────────
async function fetchProducts(page = 1) {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = {
      cp: page,
      pageSize: pageSize.value,
      sort: sortBy.value, // 將排序方式傳給後端
    }
    if (searchKeyword.value) {
      params.searchKeyword = searchKeyword.value
    }
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }

    const res = await axios.get('/shop/products', { params })
    const data = res.data
    // 只顯示上架商品 (productStatus === 1)
    products.value = (data.productList || []).filter((p) => p.productStatus === 1)
    currentPage.value = data.currentPage || 1
    totalPages.value = data.totalPages || 1
    totalElements.value = data.totalElements || 0
  } catch (e) {
    console.error('取得商品失敗', e)
    errorMsg.value = '商品載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}

// ── 取得 TOP10 熱銷排行 ──────────────────────────────────────────────────
async function fetchTop10() {
  try {
    const res = await axios.get('/shop/products/top10')
    top10Products.value = res.data || []
  } catch (e) {
    console.error('取得 TOP10 失敗', e)
  }
}

// ── 取得瀏覽紀錄 ──────────────────────────────────────────────────────────
async function fetchHistory() {
  if (!userStore.token || !userStore.memberId) return
  try {
    const res = await axios.get(`/history/${userStore.memberId}`)
    viewHistory.value = res.data || []
  } catch (e) {
    console.error('取得歷史紀錄失敗', e)
  }
}

// ── 分類篩選 ──────────────────────────────────────────────────────────────
function selectCategory(categoryId) {
  if (categoryId === null) {
    // 點擊「全部商品」
    router.push({ path: '/', query: { view: 'all' } })
  } else {
    // 點擊特定分類
    router.push({ path: '/', query: { catId: categoryId } })
  }
}

watch(
  () => [selectedCategoryId.value, searchKeyword.value, sortBy.value],
  () => {
    fetchProducts(1)
  },
)

// ── 搜尋（由 Header 觸發，透過 route.query） ──────────────────────────────
function handleSearch() {
  selectedCategoryId.value = null
  sortBy.value = 'default'
  fetchProducts(1)
}

// ── 分頁切換 ──────────────────────────────────────────────────────────────
function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  fetchProducts(page)
  const section = document.querySelector('.product-section')
  if (section) section.scrollIntoView({ behavior: 'smooth' })
}

// ── 頁碼列表（最多顯示 5 頁） ─────────────────────────────────────────────
const pageRange = computed(() => {
  const pages = []
  const half = 2
  let start = Math.max(1, currentPage.value - half)
  let end = Math.min(totalPages.value, currentPage.value + half)
  if (end - start < 4) {
    if (start === 1) end = Math.min(totalPages.value, start + 4)
    else start = Math.max(1, end - 4)
  }
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// ── 加入購物車 ────────────────────────────────────────────────────────────
async function addToCart(product) {
  // 未登入彈出提示
  if (!userStore.token) {
    await Swal.fire({
      icon: 'warning',
      title: '請先登入',
      text: '登入後才能將毛孩好物加入購物車喔！',
      confirmButtonText: '前往登入',
      confirmButtonColor: '#e67e22',
    })
    window.location.href = '/login'
    return
  }
  try {
    const cartData = {
      productId: product.productId,
      quantity: 1,
    }

    await axios.post(`/cart/add/${userStore.memberId}`, cartData)
    userStore.updateCartCount()

    Swal.fire({
      icon: 'success',
      title: '加入成功！',
      text: `已將「${product.productName}」放進購物車`,
      timer: 1500,
      showConfirmButton: false,
    })
    if (userStore.updateCartCount) {
      userStore.updateCartCount()
    }
  } catch (e) {
    console.error('加入購物車失敗', e)
    const errorText = e.response?.data || '請稍後再試'
    console.log(errorText)
    Swal.fire({
      icon: 'error',
      title: '加入失敗',
      text: '請稍後再試',
      confirmButtonColor: '#e67e22',
    })
  }
}

// ── 監聽 URL query 變化（Header 搜尋 / 分類連結點擊） ─────────────────────
watch(
  () => route.query,
  (query) => {
    if (query.keyword) {
      searchKeyword.value = query.keyword
      selectedCategoryId.value = null
      fetchProducts(1)
    } else if (query.catId) {
      selectedCategoryId.value = parseInt(query.catId)
      searchKeyword.value = ''
      fetchProducts(1)
    } else if (query.view === 'all') {
      selectedCategoryId.value = null
      searchKeyword.value = ''
      fetchProducts(1)
    } else {
      // 點 LOGO 回首頁時，query 是空的
      selectedCategoryId.value = null
      searchKeyword.value = ''
      fetchProducts(1)
    }
  },
)

// ── 初始化 ────────────────────────────────────────────────────────────────
onMounted(async () => {
  await fetchCategories()

  // 檢查 URL 是否帶有查詢參數
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
  }
  if (route.query.catId) {
    selectedCategoryId.value = parseInt(route.query.catId)
  }
  if (userStore.token && userStore.memberId) {
    userStore.updateCartCount()
  }

  await fetchProducts(1)
  fetchTop10() // 【新增】取得真實熱銷排行
  fetchHistory()

  const carouselElement = document.getElementById('shopCarousel')

  if (carouselElement) {
    const carousel = new Carousel(carouselElement, {
      interval: 2000,
      ride: 'carousel',
    })
  }
})
</script>

<template>
  <div class="shop-content container-fluid py-4 px-lg-5">
    <div class="row g-3">
      <!-- ========== 左側欄：選單 + 未來歷史紀錄 ========== -->
      <aside class="col-lg-2">
        <div class="sticky-sidebar">
          <div class="category-sidebar">
            <nav class="sidebar-nav">
              <!-- 全部商品 -->
              <a
                href="#"
                class="nav-item all-products-link"
                :class="{ active: route.query.view === 'all' }"
                @click.prevent="selectCategory(null)"
              >
                全部商品
              </a>

              <!-- 商城分類結構 (大專區 + 實體分類) -->
              <div v-for="area in categoryTree.shopTree" :key="area.categoryId" class="nav-group">
                <div
                  class="group-title"
                  :class="{ active: selectedCategoryId === area.categoryId }"
                  @click="selectCategory(area.categoryId)"
                >
                  {{ area.categoryName }}
                </div>
                <div class="group-content">
                  <a
                    v-for="sub in area.children"
                    :key="sub.categoryId"
                    href="#"
                    class="sub-item"
                    :class="{ active: selectedCategoryId === sub.categoryId }"
                    @click.prevent="selectCategory(sub.categoryId)"
                  >
                    {{ sub.categoryName }}
                  </a>
                </div>
              </div>

              <!-- 活動專區 -->
              <div v-if="categoryTree.activityTags.length > 0" class="nav-group mt-4">
                <div class="group-title text-danger">
                  <i class="fas fa-bullhorn me-2"></i>活動特報 (功能尚未完成)
                </div>
                <!-- <div class="group-content">
                  <a v-for="tag in categoryTree.activityTags" 
                     :key="tag.categoryId" 
                     href="#" 
                     class="sub-item activity-item"
                     :class="{ 'active': selectedCategoryId === tag.categoryId }"
                     @click.prevent="selectCategory(tag.categoryId)"
                  >
                    <i class="fas fa-tag me-2"></i>{{ tag.categoryName }}
                  </a>
                </div> -->
              </div>
            </nav>
          </div>

          <!-- 這裡留給您未來放置「歷史紀錄」 -->
          <div v-if="userStore.token && viewHistory.length > 0" class="recent-history-section mt-5">
            <h6 class="history-title mb-3">
              最近看過 ...
            </h6>
            <div class="history-list">
              <router-link 
                v-for="h in viewHistory.slice(0, 10)" 
                :key="h.productId" 
                :to="`/product/${h.productId}`"
                class="history-item d-flex align-items-center text-decoration-none mb-3"
              >
                <div class="history-img-box me-2">
                  <img :src="getImageUrl(h.productImage)" :alt="h.productName" />
                </div>
                <div class="history-info">
                  <p class="history-name mb-0">{{ h.productName }}</p>
                  <p class="history-price mb-0">$ {{ Number(h.productPrice).toLocaleString() }}</p>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </aside>

      <!-- ========== 右側主內容區 ========== -->
      <main class="col-lg-10">
        <!-- 麵包屑 -->
        <nav v-if="!showCarousel" aria-label="breadcrumb" class="mb-3">
          <ol class="breadcrumb">
            <li
              v-for="(crumb, index) in breadcrumbs"
              :key="index"
              class="breadcrumb-item"
              :class="{ active: index === breadcrumbs.length - 1 }"
            >
              <a
                v-if="index < breadcrumbs.length - 1"
                href="#"
                class="text-decoration-none text-muted"
                @click.prevent="
                  selectCategory(crumb.id === 'search' || crumb.id === 'all' ? null : crumb.id)
                "
              >
                {{ crumb.label }}
              </a>
              <span v-else>{{ crumb.label }}</span>
            </li>
          </ol>
        </nav>

        <!-- 廣告輪播 -->
        <div
          v-if="showCarousel"
          id="shopCarousel"
          class="carousel slide mb-4"
          data-bs-interval="2000"
          data-bs-ride="carousel"
        >
          <div class="carousel-indicators">
            <button
              v-for="(img, index) in carouselImages"
              :key="index"
              type="button"
              data-bs-target="#shopCarousel"
              :data-bs-slide-to="index"
              :class="{ active: index === 0 }"
            ></button>
          </div>
          <div class="carousel-inner">
            <div
              v-for="(img, index) in carouselImages"
              :key="index"
              class="carousel-item"
              :class="{ active: index === 0 }"
              data-bs-interval="2000"
            >
              <img :src="img.src" :alt="img.alt" class="d-block w-100 img-fluid" />
            </div>
          </div>
          <button
            class="carousel-control-prev"
            type="button"
            data-bs-target="#shopCarousel"
            data-bs-slide="prev"
          >
            <span class="carousel-control-prev-icon"></span>
          </button>
          <button
            class="carousel-control-next"
            type="button"
            data-bs-target="#shopCarousel"
            data-bs-slide="next"
          >
            <span class="carousel-control-next-icon"></span>
          </button>
        </div>

        <!-- TOP 5 熱銷排行 (固定展示) -->
        <section
          v-if="!loading && top10Products.length > 0 && showCarousel"
          class="top10-section mb-5"
        >
          <h4 class="section-title mb-4">
            <i class="fas fa-crown text-warning me-2"></i>TOP 5 熱門精選
          </h4>
          <div class="row row-cols-1 row-cols-md-3 row-cols-lg-5 g-3">
            <div v-for="(p, idx) in top10Products.slice(0, 5)" :key="p.productId" class="col">
              <div class="top10-card h-100 shadow-sm border-0">
                <div class="rank-badge-static">TOP {{ idx + 1 }}</div>
                <router-link
                  :to="`/product/${p.productId}`"
                  class="text-decoration-none"
                  style="color: inherit"
                >
                  <div class="top10-img">
                    <img :src="getImageUrl(p.productImage)" :alt="p.productName" loading="lazy" />
                  </div>
                  <div class="top10-info">
                    <p class="top10-name">{{ p.productName }}</p>
                  </div>
                </router-link>
                <div class="top10-footer">
                  <span class="top10-price">$ {{ Number(p.productPrice).toLocaleString() }}</span>
                  <button class="btn add-to-cart-btn" @click.stop.prevent="addToCart(p)">
                    <i class="fas fa-shopping-basket"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 商品列表 -->
        <section class="product-section">
          <div class="section-header d-flex justify-content-between align-items-center mb-2">
            <h4 class="section-title mb-0">
              <span>{{ pageTitle }}</span>
            </h4>
          </div>

          <!-- 排序工具列 -->
          <div class="sort-toolbar">
            <div class="sort-left">
              <button
                class="sort-btn"
                :class="{ active: sortBy === 'default' }"
                @click="setSort('default')"
              >
                推薦
              </button>
              <button
                class="sort-btn"
                :class="{ active: sortBy === 'sales' }"
                @click="setSort('sales')"
              >
                熱銷
              </button>
              <button
                class="sort-btn"
                :class="{ active: sortBy === 'newest' }"
                @click="setSort('newest')"
              >
                最新
              </button>
              <button
                class="sort-btn"
                :class="{ active: sortBy.startsWith('price') }"
                @click="setSort('price')"
              >
                價格
                <i class="fas fa-sort" v-if="!sortBy.startsWith('price')"></i>
                <i class="fas fa-sort-up" v-else-if="priceDirection === 'asc'"></i>
                <i class="fas fa-sort-down" v-else></i>
              </button>
            </div>
            <div class="sort-right">
              <select v-model.number="pageSize" class="page-size-select" @change="fetchProducts(1)">
                <option :value="10">顯示10筆/頁</option>
                <option :value="25">顯示25筆/頁</option>
                <option :value="50">顯示50筆/頁</option>
              </select>
              <div class="view-toggle ms-2">
                <button
                  class="view-btn"
                  :class="{ active: viewMode === 'grid' }"
                  @click="viewMode = 'grid'"
                >
                  <i class="fas fa-th"></i>
                </button>
                <button
                  class="view-btn"
                  :class="{ active: viewMode === 'list' }"
                  @click="viewMode = 'list'"
                >
                  <i class="fas fa-th-list"></i>
                </button>
              </div>
            </div>
          </div>

          <!-- 商品卡片列表 -->
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-warning" role="status"></div>
          </div>
          <div v-else-if="products.length === 0" class="text-center py-5">
            <p class="text-muted">查無商品</p>
          </div>
          <div
            v-else
            class="row g-2"
            :class="
              viewMode === 'grid'
                ? 'row-cols-2 row-cols-md-3 row-cols-lg-4 row-cols-xl-5'
                : 'row-cols-1 list-mode'
            "
          >
            <!-- 使用 products.value 渲染，因為後端已經排好序了 -->
            <div v-for="p in products" :key="p.productId" class="col">
              <article class="product-card shadow-sm">
                <div class="product-category-badge">{{ p.categoryName || '寵物好物' }}</div>

                <!-- 主要連結：包含圖片與名稱 -->
                <router-link
                  :to="`/product/${p.productId}`"
                  class="product-main-area text-decoration-none"
                  style="color: inherit"
                >
                  <div class="product-img-wrapper">
                    <img :src="getImageUrl(p.productImage)" :alt="p.productName" loading="lazy" />
                  </div>
                  <div class="product-info product-name-area">
                    <h6 class="product-name">{{ p.productName }}</h6>
                  </div>
                </router-link>

                <!-- 側邊或下方區域：包含價格與按鈕 -->
                <div class="product-info product-action-area pt-0">
                  <div class="product-footer">
                    <span class="product-price"
                      >$ {{ Number(p.productPrice).toLocaleString() }}</span
                    >
                    <button class="btn add-to-cart-btn" @click.stop.prevent="addToCart(p)">
                      <i class="fas fa-shopping-basket"></i>
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <!-- 分頁 -->
          <nav v-if="totalPages > 1" class="mt-5 d-flex justify-content-center">
            <ul class="pagination pagination-shop">
              <li class="page-item" :class="{ disabled: currentPage <= 1 }">
                <button class="page-link" @click="goToPage(currentPage - 1)">
                  <i class="fas fa-chevron-left"></i>
                </button>
              </li>
              <li
                v-for="page in pageRange"
                :key="page"
                class="page-item"
                :class="{ active: page === currentPage }"
              >
                <button class="page-link" @click="goToPage(page)">{{ page }}</button>
              </li>
              <li class="page-item" :class="{ disabled: currentPage >= totalPages }">
                <button class="page-link" @click="goToPage(currentPage + 1)">
                  <i class="fas fa-chevron-right"></i>
                </button>
              </li>
            </ul>
          </nav>
        </section>
      </main>
    </div>
  </div>
</template>

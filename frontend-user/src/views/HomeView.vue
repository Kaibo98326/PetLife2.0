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
const products = ref([]) 
const categories = ref([]) 
const selectedCategoryId = ref(null) 
const searchKeyword = ref('') 
const currentPage = ref(1) 
const totalPages = ref(1) 
const totalElements = ref(0) 
const loading = ref(false) 
const errorMsg = ref('') 

// ── 排序與狀態 ────────────────────────────────────────────────────────────
const sortBy = ref('newest') 
const pageSize = ref(25) 
const viewMode = ref('grid') 
const priceDirection = ref('asc') 

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

/** TOP10 熱銷排行 */
const top10Products = computed(() => {
  return [...products.value].slice(0, 10)
})

/** 麵包屑導覽路徑 */
const breadcrumbs = computed(() => {
  // ✨ 新增/修改：將起始點設定為「首頁」
  const crumbs = [{ label: '首頁', id: null }]

  if (searchKeyword.value) {
    crumbs.push({ label: `搜尋關鍵字：「${searchKeyword.value}」`, id: 'search' })
    return crumbs
  }

  if (selectedCategoryId.value) {
    const current = categories.value.find((c) => c.categoryId === selectedCategoryId.value)
    if (current) {
      // ✨ 新增/修改：如果是活動標籤 (Type 3 且不是核心容器 ID 3)，強制安插「核心容器」作為父層麵包屑
      if (current.categoryType === 3 && current.categoryId !== 3) {
        const container = categories.value.find(c => c.categoryId === 3)
        const containerName = container ? container.categoryName : '🔥優惠活動'
        // 將 ID 設為 3，後續 template 會判斷此 ID 將其設為純文字不可點擊
        crumbs.push({ label: containerName, id: 3 })
      }
      // 如果有父分類 (大專區)，先放進去
      else if (current.parentId) {
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

function getImageUrl(imagePath) {
  if (!imagePath || imagePath === 'default_product.jpg') {
    return `${IMG_BASE}/images/products/default_product.jpg`
  }
  return `${IMG_BASE}/${imagePath}`
}

// ── 取得分類列表 ──────────────────────────────────────────────────────────
async function fetchCategories() {
  try {
    const res = await axios.get('/shop/categories')
    categories.value = (res.data || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) {
    console.error('取得分類失敗', e)
  }
}

/**
 * 核心：將扁平分類轉換為樹狀結構 (同步後台排序)
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
  // ✨ 新增/修改：動態提取系統核心容器 (ID: 3)，並將其餘 Type 3 視為子項目
  const systemContainer = all.find(c => c.categoryId === 3) || { categoryName: '🔥優惠活動', categoryId: 3 }
  const activityTags = all.filter((c) => c.categoryType === 3 && c.categoryId !== 3)

  return {
    shopTree: tree,
    systemContainer: systemContainer, // 導出供 Template 的群組標題使用
    activityTags: activityTags,
  }
})

// ── 取得商品列表 ──────────────────────────────────────────────────────────
async function fetchProducts(page = 1) {
  loading.value = true
  errorMsg.value = ''
  try {
    const params = { cp: page, pageSize: pageSize.value, sort: sortBy.value }
    if (searchKeyword.value) params.searchKeyword = searchKeyword.value
    if (selectedCategoryId.value) params.categoryId = selectedCategoryId.value

    const res = await axios.get('/shop/products', { params })
    const data = res.data
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

// ── 分類篩選 ──────────────────────────────────────────────────────────────
function selectCategory(categoryId) {
  if (categoryId === null) {
    router.push({ path: '/', query: { view: 'all' } })
  } else {
    router.push({ path: '/', query: { catId: categoryId } })
  }
}

watch(
  () => [selectedCategoryId.value, searchKeyword.value, sortBy.value],
  () => { fetchProducts(1) }
)

// ── 搜尋（由 Header 觸發） ──────────────────────────────
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

// ── 頁碼列表 ─────────────────────────────────────────────
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
  if (!userStore.token) {
    await Swal.fire({ icon: 'warning', title: '請先登入', text: '登入後才能將毛孩好物加入購物車喔！', confirmButtonText: '前往登入', confirmButtonColor: '#e67e22' })
    window.location.href = '/login'
    return
  }
  try {
    const cartData = { productId: product.productId, quantity: 1 }
    await axios.post(`/cart/add/${userStore.memberId}`, cartData)
    userStore.updateCartCount()
    Swal.fire({ icon: 'success', title: '加入成功！', text: `已將「${product.productName}」放進購物車`, timer: 1500, showConfirmButton: false })
  } catch (e) {
    console.error('加入購物車失敗', e)
    Swal.fire({ icon: 'error', title: '加入失敗', text: '請稍後再試', confirmButtonColor: '#e67e22' })
  }
}

// ── 監聽 URL query 變化 ─────────────────────
watch(
  () => route.query,
  (query) => {
    if (query.keyword) {
      searchKeyword.value = query.keyword; selectedCategoryId.value = null; fetchProducts(1)
    } else if (query.catId) {
      selectedCategoryId.value = parseInt(query.catId); searchKeyword.value = ''; fetchProducts(1)
    } else if (query.view === 'all') {
      selectedCategoryId.value = null; searchKeyword.value = ''; fetchProducts(1)
    } else {
      selectedCategoryId.value = null; searchKeyword.value = ''; fetchProducts(1)
    }
  }
)

// ── 初始化 ────────────────────────────────────────────────────────────────
onMounted(async () => {
  await fetchCategories()
  if (route.query.keyword) searchKeyword.value = route.query.keyword
  if (route.query.catId) selectedCategoryId.value = parseInt(route.query.catId)
  if (userStore.token && userStore.memberId) userStore.updateCartCount()
  await fetchProducts(1)
  const carouselElement = document.getElementById('shopCarousel')
  if (carouselElement) new Carousel(carouselElement, { interval: 2000, ride: 'carousel' })
})
</script>

<template>
  <div class="shop-content container-fluid py-4 px-lg-5">
    <div class="row g-3">
      <aside class="col-lg-2">
        <div class="sticky-sidebar">
          <div class="category-sidebar">
            <nav class="sidebar-nav">
              <a href="#" class="nav-item all-products-link" :class="{ active: route.query.view === 'all' }" @click.prevent="selectCategory(null)">
                全部商品
              </a>

              <div v-for="area in categoryTree.shopTree" :key="area.categoryId" class="nav-group">
                <div class="group-title" :class="{ active: selectedCategoryId === area.categoryId }" @click="selectCategory(area.categoryId)">
                  {{ area.categoryName }}
                </div>
                <div class="group-content">
                  <a v-for="sub in area.children" :key="sub.categoryId" href="#" class="sub-item" :class="{ active: selectedCategoryId === sub.categoryId }" @click.prevent="selectCategory(sub.categoryId)">
                    {{ sub.categoryName }}
                  </a>
                </div>
              </div>

              <div v-if="categoryTree.activityTags.length > 0" class="nav-group mt-4">
                <div class="group-title text-danger" style="cursor: default;">
                  <i class="fas fa-bullhorn me-2"></i>{{ categoryTree.systemContainer.categoryName }}
                </div>
                <div class="group-content">
                  <a v-for="tag in categoryTree.activityTags" 
                     :key="tag.categoryId" 
                     href="#" 
                     class="sub-item activity-item"
                     :class="{ 'active': selectedCategoryId === tag.categoryId }"
                     @click.prevent="selectCategory(tag.categoryId)"
                  >
                    <i class="fas fa-tag me-2"></i>{{ tag.categoryName }}
                  </a>
                </div>
              </div>
            </nav>
          </div>
          <div class="history-placeholder mt-4"></div>
        </div>
      </aside>

      <main class="col-lg-10">
        <nav v-if="!showCarousel" aria-label="breadcrumb" class="mb-3">
          <ol class="breadcrumb">
            <li v-for="(crumb, index) in breadcrumbs" :key="index" class="breadcrumb-item" :class="{ active: index === breadcrumbs.length - 1 }">
              <span v-if="crumb.id === 3 || index === breadcrumbs.length - 1" :class="{'text-muted': crumb.id === 3}">
                {{ crumb.label }}
              </span>
              <a v-else href="#" class="text-decoration-none text-muted" @click.prevent="selectCategory(crumb.id === 'search' || crumb.id === 'all' ? null : crumb.id)">
                {{ crumb.label }}
              </a>
            </li>
          </ol>
        </nav>

        <div v-if="showCarousel" id="shopCarousel" class="carousel slide mb-4" data-bs-interval="2000" data-bs-ride="carousel">
          <div class="carousel-indicators">
            <button v-for="(img, index) in carouselImages" :key="index" type="button" data-bs-target="#shopCarousel" :data-bs-slide-to="index" :class="{ active: index === 0 }"></button>
          </div>
          <div class="carousel-inner">
            <div v-for="(img, index) in carouselImages" :key="index" class="carousel-item" :class="{ active: index === 0 }" data-bs-interval="2000">
              <img :src="img.src" :alt="img.alt" class="d-block w-100 img-fluid" />
            </div>
          </div>
          <button class="carousel-control-prev" type="button" data-bs-target="#shopCarousel" data-bs-slide="prev"><span class="carousel-control-prev-icon"></span></button>
          <button class="carousel-control-next" type="button" data-bs-target="#shopCarousel" data-bs-slide="next"><span class="carousel-control-next-icon"></span></button>
        </div>

        <section v-if="!loading && top10Products.length > 0 && showCarousel" class="top10-section mb-4">
          <h4 class="section-title"><i class="fas fa-fire text-danger me-2"></i>TOP10 熱銷排行</h4>
          <div class="top10-scroll-wrapper">
            <div class="top10-track">
              <div v-for="(p, idx) in top10Products" :key="'a-' + p.productId" class="top10-card">
                <div class="rank-badge"><i class="fas fa-fire"></i></div>
                <router-link :to="`/product/${p.productId}`" class="text-decoration-none" style="color: inherit">
                  <div class="top10-img"><img :src="getImageUrl(p.productImage)" :alt="p.productName" loading="lazy" /></div>
                  <div class="top10-info"><p class="top10-name">{{ p.productName }}</p></div>
                </router-link>
                <div class="top10-footer">
                  <span class="top10-price">$ {{ Number(p.productPrice).toLocaleString() }}</span>
                  <button class="btn add-to-cart-btn" @click.stop.prevent="addToCart(p)"><i class="fas fa-shopping-basket"></i></button>
                </div>
              </div>
              <div v-for="(p, idx) in top10Products" :key="'b-' + p.productId" class="top10-card" aria-hidden="true">
                <div class="rank-badge"><i class="fas fa-fire"></i></div>
                <router-link :to="`/product/${p.productId}`" class="text-decoration-none" style="color: inherit" tabindex="-1">
                  <div class="top10-img"><img :src="getImageUrl(p.productImage)" :alt="p.productName" loading="lazy" /></div>
                  <div class="top10-info"><p class="top10-name">{{ p.productName }}</p></div>
                </router-link>
                <div class="top10-footer">
                  <span class="top10-price">$ {{ Number(p.productPrice).toLocaleString() }}</span>
                  <button class="btn add-to-cart-btn" @click.stop.prevent="addToCart(p)"><i class="fas fa-shopping-basket"></i></button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="product-section">
          <div class="section-header d-flex justify-content-between align-items-center mb-2">
            <h4 class="section-title mb-0"><span>{{ pageTitle }}</span></h4>
          </div>

          <div class="sort-toolbar">
            <div class="sort-left">
              <button class="sort-btn" :class="{ active: sortBy === 'default' }" @click="setSort('default')">推薦</button>
              <button class="sort-btn" :class="{ active: sortBy === 'sales' }" @click="setSort('sales')">熱銷</button>
              <button class="sort-btn" :class="{ active: sortBy === 'newest' }" @click="setSort('newest')">最新</button>
              <button class="sort-btn" :class="{ active: sortBy.startsWith('price') }" @click="setSort('price')">
                價格 <i class="fas fa-sort" v-if="!sortBy.startsWith('price')"></i><i class="fas fa-sort-up" v-else-if="priceDirection === 'asc'"></i><i class="fas fa-sort-down" v-else></i>
              </button>
            </div>
            <div class="sort-right">
              <select v-model.number="pageSize" class="page-size-select" @change="fetchProducts(1)">
                <option :value="10">顯示10筆/頁</option><option :value="25">顯示25筆/頁</option><option :value="50">顯示50筆/頁</option>
              </select>
              <div class="view-toggle ms-2">
                <button class="view-btn" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'"><i class="fas fa-th"></i></button>
                <button class="view-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'"><i class="fas fa-th-list"></i></button>
              </div>
            </div>
          </div>

          <div v-if="loading" class="text-center py-5"><div class="spinner-border text-warning" role="status"></div></div>
          <div v-else-if="products.length === 0" class="text-center py-5"><p class="text-muted">查無商品</p></div>
          <div v-else class="row g-2" :class="viewMode === 'grid' ? 'row-cols-2 row-cols-md-3 row-cols-lg-4 row-cols-xl-5' : 'row-cols-1 list-mode'">
            <div v-for="p in products" :key="p.productId" class="col">
              <article class="product-card shadow-sm position-relative h-100 d-flex flex-column">
                <div class="product-category-badge">{{ p.categoryName || '寵物好物' }}</div>
                <router-link :to="`/product/${p.productId}`" class="product-main-area text-decoration-none flex-grow-1" style="color: inherit">
                  <div class="product-img-wrapper"><img :src="getImageUrl(p.productImage)" :alt="p.productName" loading="lazy" /></div>
                  <div class="product-info product-name-area"><h6 class="product-name">{{ p.productName }}</h6></div>
                </router-link>
                <div class="product-info product-action-area pt-0 mt-auto">
                  <div class="product-footer">
                    <span class="product-price">$ {{ Number(p.productPrice).toLocaleString() }}</span>
                    <button class="btn add-to-cart-btn" @click.stop.prevent="addToCart(p)"><i class="fas fa-shopping-basket"></i></button>
                  </div>
                </div>
                </article>
            </div>
          </div>

          <nav v-if="totalPages > 1" class="mt-5 d-flex justify-content-center">
            <ul class="pagination pagination-shop">
              <li class="page-item" :class="{ disabled: currentPage <= 1 }"><button class="page-link" @click="goToPage(currentPage - 1)"><i class="fas fa-chevron-left"></i></button></li>
              <li v-for="page in pageRange" :key="page" class="page-item" :class="{ active: page === currentPage }"><button class="page-link" @click="goToPage(page)">{{ page }}</button></li>
              <li class="page-item" :class="{ disabled: currentPage >= totalPages }"><button class="page-link" @click="goToPage(currentPage + 1)"><i class="fas fa-chevron-right"></i></button></li>
            </ul>
          </nav>
        </section>
      </main>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, watchEffect, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request' // 1. 改用你的全域配置
import Swal from 'sweetalert2'
import { useProductStore } from '@/stores/product'
import '@/assets/css/Product.css'

const router = useRouter()
const route = useRoute()
const productStore = useProductStore()

// 分類類型配色 (與 Category.vue 保持同步)
const typeMap = {
  1: { label: '實體分類', color: '#795548' }, // 溫暖褐色
  2: { label: '專區', color: '#e67e22' },    // 活力橘色
  3: { label: '活動標籤', color: '#d81b60' }     // 質感桃紅
}

// --- 資料狀態 ---
const productList = ref([])
const categories = ref([])
const pagination = ref({ currentPage: 1, totalPages: 1, totalElements: 0 })
const pageSize = ref(10) // 新增：每頁顯示筆數
const searchKeyword = ref('')
const selectedIds = ref([]) 

const mode = ref('list')
const currentProduct = ref({}) 
const previewUrl = ref(null)
const isLowStockFilter = ref(false) // 新增：庫存預警篩選狀態
const showFilterModal = ref(false) // 複合式篩選彈窗狀態
const showExportModal = ref(false) // 匯出欄位選擇彈窗狀態

// --- 匯出欄位設定 ---
const exportFields = ref([
  { key: 'productId', label: '商品ID', checked: true },
  { key: 'productName', label: '商品名稱', checked: true },
  { key: 'productPrice', label: '價格', checked: true },
  { key: 'productStock', label: '庫存', checked: true },
  { key: 'categoryName', label: '商品分類', checked: true },
  { key: 'productLocation', label: '儲位編號', checked: true },
  { key: 'productStatus', label: '狀態', checked: true },
  { key: 'productDescription', label: '商品描述', checked: false }
])

// --- 複合式篩選條件 ---
const filterParams = ref({
  categoryId: 0,
  status: -1, // -1 代表全部
  minPrice: null,
  maxPrice: null,
  minStock: null,
  maxStock: null
})

const isFilterActive = computed(() => {
  return filterParams.value.categoryId !== 0 || 
         filterParams.value.status !== -1 || 
         filterParams.value.minPrice !== null || 
         filterParams.value.maxPrice !== null || 
         filterParams.value.minStock !== null || 
         filterParams.value.maxStock !== null
})


// --- 排序狀態 ---
const sortKey = ref('')
const sortOrder = ref('') // 'asc' | 'desc' | ''

const toggleSort = (key) => {
  if (sortKey.value === key) {
    // 循環切換：asc → desc → 無排序
    if (sortOrder.value === 'asc') {
      sortOrder.value = 'desc'
    } else if (sortOrder.value === 'desc') {
      sortOrder.value = ''
      sortKey.value = ''
    } else {
      sortOrder.value = 'asc'
    }
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }
}

const sortedProductList = computed(() => {
  if (!sortKey.value || !sortOrder.value) return productList.value

  const list = [...productList.value]
  const key = sortKey.value
  const order = sortOrder.value === 'asc' ? 1 : -1

  return list.sort((a, b) => {
    let valA = a[key]
    let valB = b[key]

    // 字串比較
    if (typeof valA === 'string') {
      return valA.localeCompare(valB, 'zh-Hant') * order
    }
    // 數字比較
    return ((valA ?? 0) - (valB ?? 0)) * order
  })
})

// --- 全選邏輯 ---
const selectAllRef = ref(null)

const isAllSelected = computed(() => {
  return sortedProductList.value.length > 0 && 
         sortedProductList.value.every(p => selectedIds.value.includes(p.productId))
})

const isIndeterminate = computed(() => {
  return selectedIds.value.length > 0 && !isAllSelected.value
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = sortedProductList.value.map(p => p.productId)
  }
}

watchEffect(() => {
  if (selectAllRef.value) {
    selectAllRef.value.indeterminate = isIndeterminate.value
  }
})

// 導向商品編輯頁
const goEdit = (productId) => {
  router.push({ name: '編輯商品', params: { id: productId } })
}

// 當圖片載入失敗時，自動替換成一張預設的預覽圖
const handleImgError = (e) => {
  e.target.src = 'https://placehold.co/60x60?text=No+Img'; 
}

// --- API 呼叫 ---
const fetchProducts = async (page = 1) => {
  try {
    // 強制等候 DOM 與變數同步
    await import('vue').then(v => v.nextTick())
    
    // 確保關鍵字已經完全同步
    const currentKeyword = searchKeyword.value ? searchKeyword.value.trim() : ''
    
    const params = { 
      cp: page, 
      ps: pageSize.value, // 加入每頁筆數參數
      searchKeyword: currentKeyword,
      lowStock: isLowStockFilter.value, 
      categoryId: filterParams.value.categoryId !== 0 ? filterParams.value.categoryId : (route.query.categoryId || null),
      status: filterParams.value.status !== -1 ? filterParams.value.status : null,
      minPrice: filterParams.value.minPrice,
      maxPrice: filterParams.value.maxPrice,
      minStock: filterParams.value.minStock,
      maxStock: filterParams.value.maxStock
    }

    // 對齊後端路徑: /api/products/list
    const res = await request.get('/api/products/list', { params })
    // 根據你 Java 回傳的 Map 結構抓資料
    productList.value = res.data.productList
    pagination.value.currentPage = res.data.currentPage
    pagination.value.totalPages = res.data.totalPages
    pagination.value.totalElements = res.data.totalElements || 0
    // 同步全域低庫存計數
    if (res.data.lowStockCount !== undefined) {
      productStore.lowStockCount = res.data.lowStockCount
    }
  } catch (error) {
    console.error("讀取失敗", error)
  }
}

// 監聽網址參數變化 (例如點擊分類跳轉過來時)
watch(() => route.query.categoryId, () => {
  fetchProducts(1)
})

// 返回上一步 (或是清除搜尋/過濾)
const goBack = () => {
  if (isLowStockFilter.value) {
    isLowStockFilter.value = false
    fetchProducts(1)
  } else if (isFilterActive.value) {
    resetFilters()
  } else if (route.query.categoryId) {
    router.back()
  } else {
    searchKeyword.value = ''
    fetchProducts(1)
  }
}

const resetFilters = () => {
  filterParams.value = {
    categoryId: 0,
    status: -1,
    minPrice: null,
    maxPrice: null,
    minStock: null,
    maxStock: null
  }
  fetchProducts(1)
}

const applyFilters = () => {
  showFilterModal.value = false
  fetchProducts(1)
}


// --- 匯出 CSV (處理自定義欄位) ---
const handleExportCSV = () => {
  const selectedFields = exportFields.value.filter(f => f.checked)
  
  if (selectedFields.length === 0) {
    Swal.fire('提示', '請至少選擇一個匯出欄位', 'warning')
    return
  }

  // 決定資料來源
  const targetData = selectedIds.value.length > 0
    ? productList.value.filter(p => selectedIds.value.includes(p.productId))
    : productList.value

  if (targetData.length === 0) {
    Swal.fire('提示', '目前沒有資料可供匯出', 'info')
    return
  }

  // 生成表頭
  const headers = selectedFields.map(f => f.label)
  
  // 生成資料列
  const rows = targetData.map(p => {
    return selectedFields.map(f => {
      if (f.key === 'productStatus') {
        return p.productStatus === 1 ? '上架中' : '已下架'
      }
      // 如果 key 是 categoryName 且 p.categories 存在，則合併分類名稱 (雙重保險)
      if (f.key === 'categoryName' && p.categories) {
        return p.categories.map(c => c.categoryName).join(', ') || p.categoryName || ''
      }
      return p[f.key] || ''
    })
  })
  
  let csvContent = "data:text/csv;charset=utf-8,\uFEFF" 
  csvContent += headers.join(",") + "\n"
  rows.forEach(row => {
    // 處理資料內容可能含逗號的問題
    const escapedRow = row.map(val => `"${String(val).replace(/"/g, '""')}"`)
    csvContent += escapedRow.join(",") + "\n"
  })

  const fileName = selectedIds.value.length > 0 
    ? `自定義商品清單(選取)_${new Date().toLocaleDateString()}.csv`
    : `自定義商品清單(全部)_${new Date().toLocaleDateString()}.csv`

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement("a")
  link.setAttribute("href", encodedUri)
  link.setAttribute("download", fileName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  showExportModal.value = false // 關閉彈窗
}


const toggleLowStock = () => {
  isLowStockFilter.value = !isLowStockFilter.value
  fetchProducts(1)
}

// 取得分類清單
const fetchCategories = async () => {
  const res = await request.get('/api/categories')
  categories.value = res.data
}

// 獲取分類名稱 (用於標籤顯示)
const getCategoryName = (id) => {
  if (!id) return '';
  const cat = categories.value.find(c => c.categoryId === Number(id));
  return cat ? cat.categoryName : '未知分類';
};

// 圖片處理
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    currentProduct.value.tempFile = file // 暫存檔案
    previewUrl.value = URL.createObjectURL(file)
  }
}

// 執行新增/修改
const saveProduct = async () => {
  const formData = new FormData()
  
  // 按照 Java @ModelAttribute 的需求塞入欄位
  formData.append('productName', currentProduct.value.productName || '')
  if (currentProduct.value.categoryIds && currentProduct.value.categoryIds.length > 0) {
    currentProduct.value.categoryIds.forEach(id => formData.append('categoryIds', id))
  } else {
    formData.append('categoryIds', '')
  }
  formData.append('productPrice', currentProduct.value.productPrice || 0)
  formData.append('productStock', currentProduct.value.productStock || 0)
  formData.append('productDescription', currentProduct.value.productDescription || '')
  
  if (currentProduct.value.tempFile) {
    formData.append('file', currentProduct.value.tempFile) // 對應 Java 的 @RequestParam("file")
  }
  
  if (mode.value === 'edit') {
    formData.append('productId', currentProduct.value.productId)
    formData.append('oldImage', currentProduct.value.productImage) // 傳回舊圖路徑
  }

  try {
    const url = mode.value === 'add' ? '/api/products/insert' : '/api/products/update'
    // 注意：修改圖片通常用 POST 或 PUT，你 Java 寫 @PostMapping("/update")
    await request.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    Swal.fire('成功', '資料已更新', 'success')
    mode.value = 'list'
    fetchProducts(pagination.value.currentPage)
  } catch (error) {
    Swal.fire('錯誤', '操作失敗', 'error')
  }
}

// 批次上下架（含確認對話框）
const batchStatus = async (status) => {
  if (selectedIds.value.length === 0) return Swal.fire('提示', '請先勾選商品', 'info')

  const action = status === 1 ? '上架' : '下架'
  const result = await Swal.fire({
    title: `確認批次${action}？`,
    text: `即將對 ${selectedIds.value.length} 項商品執行批次${action}`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: `確認${action}`,
    cancelButtonText: '取消',
    confirmButtonColor: status === 1 ? '#1abc9c' : '#e74c3c',
  })
  if (!result.isConfirmed) return

  try {
    await request.post('/api/products/batchUpdateStatus', { 
      ids: selectedIds.value, 
      status 
    })
    Swal.fire('成功', `已成功${action} ${selectedIds.value.length} 項商品`, 'success')
    selectedIds.value = []
    fetchProducts(pagination.value.currentPage)
  } catch (error) {
    console.error(error)
    Swal.fire('錯誤', '批次操作失敗', 'error')
  }
}

onMounted(() => {
  // 檢查是否有來自 Dashboard 的庫存預警篩選請求
  if (route.query.lowStock === 'true') {
    isLowStockFilter.value = true
  }
  fetchProducts(1)
  fetchCategories()
})
</script>

<template>
  <!-- 匯出欄位選擇彈窗 -->
  <div v-if="showExportModal" class="filter-modal-overlay" @click.self="showExportModal = false">
    <div class="filter-modal-content export-mini">
      <div class="filter-modal-header">
        <h5><i class="fas fa-file-export me-2"></i>選擇匯出欄位</h5>
        <button class="btn-close-modal" @click="showExportModal = false">&times;</button>
      </div>
      <div class="filter-modal-body">
        <p class="export-tip">請勾選您想要匯出至 CSV 的欄位：</p>
        <div class="export-fields-grid">
          <label v-for="field in exportFields" :key="field.key" class="export-field-item">
            <input type="checkbox" v-model="field.checked">
            <span>{{ field.label }}</span>
          </label>
        </div>
      </div>
      <div class="filter-modal-footer">
        <button @click="exportFields.forEach(f => f.checked = true)" class="btn-filter-reset">全選</button>
        <button @click="handleExportCSV" class="btn-filter-apply">確認匯出</button>
      </div>
    </div>
  </div>
  <!-- <div class="admin-container"> -->
    <div v-if="mode === 'list'" class="main-card">
      
      <div class="toolbar-v5">
        <!-- 左：數據與導航 -->
        <div class="toolbar-left-v5">
          <button 
            v-if="route.query.categoryId || searchKeyword || isLowStockFilter || isFilterActive" 
            @click="goBack" 
            class="btn-back-v5" 
            title="返回全部"
          >
            <i class="fas fa-arrow-left"></i>
          </button>

          <div class="info-capsule-v5">
          <span class="count-label">共 <strong>{{ pagination.totalElements }}</strong> 筆</span>
          <div class="inner-divider"></div>
          <select v-model="pageSize" class="ps-select-v5" @change="fetchProducts(1)">
            <option :value="10">10 筆/頁</option>
            <option :value="20">20 筆/頁</option>
            <option :value="50">50 筆/頁</option>
            <option :value="100">100 筆/頁</option>
          </select>
        </div>

        <!-- 新增：已選取膠囊 -->
        <div v-if="selectedIds.length > 0" class="selection-capsule-v5 animate__animated animate__fadeInLeft">
          <span>已選取 <strong>{{ selectedIds.length }}</strong> 筆</span>
          <i class="fas fa-times ms-2 clear-selection" @click="selectedIds = []" title="取消全選"></i>
        </div>
      </div>

        <!-- 中：功能開關與批次 -->
        <div class="toolbar-center-v5">
          <div class="control-group-v5">
            <button 
              @click="toggleLowStock" 
              :class="['btn-alert-v5', { 'active': isLowStockFilter }]"
            >
              <i class="fas fa-exclamation-triangle"></i>
              <span>庫存預警</span>
            </button>
            <div class="inner-divider"></div>
            <div class="batch-tools-v5">
          <button class="btn-batch-v5 up" @click="batchStatus(1)" title="批次上架">
            <i class="fas fa-arrow-up"></i>
          </button>
          <button class="btn-batch-v5 down" @click="batchStatus(0)" title="批次下架">
            <i class="fas fa-arrow-down"></i>
          </button>
        </div>
      </div>
        </div>

        <!-- 右：搜尋與主要動作 -->
        <div class="toolbar-right-v5">
          <form class="search-box-v5" @submit.prevent="fetchProducts(1)">
            <i class="fas fa-search" @click="fetchProducts(1)" style="cursor: pointer;"></i>
            <input 
              v-model="searchKeyword" 
              type="search"
              placeholder="搜尋商品..."
            >
            <input type="submit" style="display: none;"> <!-- 隱藏的提交按鈕，確保 Enter 有效 -->
          </form>

          <button 
            @click="showFilterModal = true" 
            :class="['btn-action-v5', { 'active': isFilterActive }]" 
            title="進階篩選"
          >
            <i class="fas fa-sliders-h"></i>
          </button>

          <button @click="router.push('/admin/product/add')" class="btn-main-v5">
            <i class="fas fa-plus"></i> 新增
          </button>

          <button @click="showExportModal = true" class="btn-action-v5 export" title="匯出 CSV">
            <i class="fas fa-file-csv"></i>
          </button>
        </div>
      </div>

      <!-- 啟用中的篩選標籤 (如有) -->
      <div v-if="route.query.categoryId || searchKeyword || isLowStockFilter || isFilterActive" class="active-filters-bar">
        <span class="filter-label">當前篩選：</span>
        <div class="filter-tags">
          <!-- 關鍵字 -->
          <div v-if="searchKeyword" class="filter-tag">
            <span>搜尋: {{ searchKeyword }}</span>
            <i class="fas fa-times" @click="searchKeyword = ''; fetchProducts(1)"></i>
          </div>

          <!-- 分類 -->
          <div v-if="filterParams.categoryId > 0" class="filter-tag">
            <span>分類: {{ getCategoryName(filterParams.categoryId) }}</span>
            <i class="fas fa-times" @click="filterParams.categoryId = 0; applyFilters()"></i>
          </div>

          <!-- 狀態 -->
          <div v-if="filterParams.status !== -1" class="filter-tag">
            <span>狀態: {{ filterParams.status === 1 ? '上架中' : '已下架' }}</span>
            <i class="fas fa-times" @click="filterParams.status = -1; applyFilters()"></i>
          </div>

          <!-- 價格 -->
          <div v-if="filterParams.minPrice || filterParams.maxPrice" class="filter-tag">
            <span>價格: {{ filterParams.minPrice || 0 }} ~ {{ filterParams.maxPrice || '∞' }}</span>
            <i class="fas fa-times" @click="filterParams.minPrice = null; filterParams.maxPrice = null; applyFilters()"></i>
          </div>

          <!-- 庫存 -->
          <div v-if="filterParams.minStock || filterParams.maxStock" class="filter-tag">
            <span>庫存: {{ filterParams.minStock || 0 }} ~ {{ filterParams.maxStock || '∞' }}</span>
            <i class="fas fa-times" @click="filterParams.minStock = null; filterParams.maxStock = null; applyFilters()"></i>
          </div>

          <!-- 庫存預警 -->
          <div v-if="isLowStockFilter" class="filter-tag warning">
            <span><i class="fas fa-exclamation-triangle me-1"></i>庫存預警中</span>
            <i class="fas fa-times" @click="toggleLowStock"></i>
          </div>

          <button class="btn-clear-all" @click="goBack">清除全部</button>
        </div>
      </div>

      <table class="custom-table align-middle">
        <thead>
          <tr>
            <th style="width: 50px;"><input type="checkbox" ref="selectAllRef" :checked="isAllSelected" @change="toggleSelectAll" class="form-check-input"></th>
            <th style="width: 110px;" class="sortable-th" @click="toggleSort('productStatus')">
              <div class="th-sort-wrap">
                <span>狀態</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productStatus' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productStatus' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 110px;" class="sortable-th" @click="toggleSort('productId')">
              <div class="th-sort-wrap">
                <span>商品</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productId' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productId' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th class="th-name sortable-th" @click="toggleSort('productName')">
              <div class="th-sort-wrap">
                <span>名稱</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productName' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productName' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 120px;" class="sortable-th" @click="toggleSort('productPrice')">
              <div class="th-sort-wrap">
                <span>單價</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productPrice' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productPrice' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 120px;" class="sortable-th" @click="toggleSort('productStock')">
              <div class="th-sort-wrap">
                <span>庫存</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productStock' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productStock' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 180px;" class="sortable-th" @click="toggleSort('categoryName')">
              <div class="th-sort-wrap">
                <span>分類</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'categoryName' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'categoryName' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 120px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in sortedProductList" :key="p.productId">
            <td><input type="checkbox" :value="p.productId" v-model="selectedIds" class="form-check-input"></td>
            <td>
              <span :class="['status-pill', p.productStatus === 1 ? 'status-active' : 'status-inactive']">
                {{ p.productStatus === 1 ? '上架中' : '已下架' }}
              </span>
            </td>
            <td class="td-img-cell">
              <div class="prod-img-wrapper">
                <img :src="`http://localhost:8082/${p.productImage}`" class="prod-img" @error="handleImgError">
                <span class="prod-id-badge">#{{ p.productId }}</span>
                <div class="prod-hover-preview">
                  <img :src="`http://localhost:8082/${p.productImage}`" @error="handleImgError">
                </div>
              </div>
            </td>
            <td class="fw-bold" style="color: #5d4037;">{{ p.productName }}</td>
            <td style="color: #e67e22; font-weight: 600;">${{ p.productPrice }}</td>
            <td class="td-stock-cell text-center">
              <span :class="['stock-num', {'text-danger fw-bold': p.lowStock !== -1 && p.productStock <= (p.lowStock || 10)}]">
                {{ p.productStock }}
              </span>
            </td>
            <td class="td-category-cell">
              <div class="category-tags-wrapper">
                <span v-for="cat in p.categories" :key="cat.categoryId" class="category-tag" 
                      :style="{ 
                        backgroundColor: (typeMap[cat.categoryType]?.color || '#999') + '15', 
                        color: typeMap[cat.categoryType]?.color || '#999',
                        borderColor: (typeMap[cat.categoryType]?.color || '#999') + '40'
                      }">
                  <i v-if="cat.categoryType !== 2" class="fas fa-tag me-1" style="font-size: 0.6rem;"></i>{{ cat.categoryName }}
                </span>
                <!-- 如果沒有物件資料，才顯示原本的字串 (保險起見) -->
                <span v-if="(!p.categories || p.categories.length === 0) && p.categoryName" class="category-tag">
                  {{ p.categoryName }}
                </span>
              </div>
            </td>
            <td>
              <button @click="goEdit(p.productId)" 
                      class="btn-edit-link">修改</button>
            </td>
          </tr>
        </tbody>
      </table>

     <!-- 分頁區塊修正 -->
<div class="pagination-container mt-5 ">
  <div class="pagination-wrapper" style="display: flex; align-items:center;justify-content: center; gap: 1rem;">
    <button 
      class="page-btn" 
      :disabled="pagination.currentPage === 1" 
      @click="fetchProducts(pagination.currentPage - 1)"
    >
      上一頁
    </button>
    
    <div class="page-info">
      第 {{ pagination.currentPage }} 頁 / 共 {{ pagination.totalPages }} 頁
    </div>
    
    <button 
      class="page-btn" 
      :disabled="pagination.currentPage === pagination.totalPages" 
      @click="fetchProducts(pagination.currentPage + 1)"
    >
      下一頁
    </button>
  </div>
</div>
    </div>
  <!-- </div> -->

  <!-- 複合式篩選彈窗 -->
  <div v-if="showFilterModal" class="filter-modal-overlay" @click.self="showFilterModal = false">
    <div class="filter-modal-content">
      <div class="filter-modal-header">
        <h5><i class="fas fa-filter me-2"></i>複合式篩選</h5>
        <button class="btn-close-modal" @click="showFilterModal = false">&times;</button>
      </div>
      <div class="filter-modal-body">
        <div class="filter-form-grid">
          <!-- 分類 -->
          <div class="filter-item">
            <label>商品分類</label>
            <select v-model="filterParams.categoryId" class="filter-select">
              <option :value="0">所有分類</option>
              <option v-for="cat in categories" :key="cat.categoryId" :value="cat.categoryId">
                {{ cat.categoryName }}
              </option>
            </select>
          </div>

          <!-- 狀態 -->
          <div class="filter-item">
            <label>商品狀態</label>
            <select v-model="filterParams.status" class="filter-select">
              <option :value="-1">全部</option>
              <option :value="1">上架中</option>
              <option :value="0">已下架</option>
            </select>
          </div>

          <!-- 價格區間 -->
          <div class="filter-item full-width">
            <label>價格區間</label>
            <div class="range-inputs">
              <input v-model.number="filterParams.minPrice" type="number" placeholder="最低價" class="filter-input">
              <span class="range-sep">~</span>
              <input v-model.number="filterParams.maxPrice" type="number" placeholder="最高價" class="filter-input">
            </div>
          </div>

          <!-- 庫存區間 -->
          <div class="filter-item full-width">
            <label>庫存區間</label>
            <div class="range-inputs">
              <input v-model.number="filterParams.minStock" type="number" placeholder="最小庫存" class="filter-input">
              <span class="range-sep">~</span>
              <input v-model.number="filterParams.maxStock" type="number" placeholder="最大庫存" class="filter-input">
            </div>
          </div>
        </div>
      </div>
      <div class="filter-modal-footer">
        <button @click="resetFilters" class="btn-filter-reset">重置</button>
        <button @click="applyFilters" class="btn-filter-apply">套用篩選</button>
      </div>
    </div>
  </div>
</template>
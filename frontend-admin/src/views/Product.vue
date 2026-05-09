<script setup>
import { ref, computed, onMounted, watchEffect, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request' // 1. 改用你的全域配置
import Swal from 'sweetalert2'
import '@/assets/css/Product.css'

const router = useRouter()
const route = useRoute()

// 分類類型配色 (與 Category.vue 保持同步)
const typeMap = {
  1: { label: '實體分類', color: '#795548' }, // 溫暖褐色
  2: { label: '專區', color: '#e67e22' },    // 活力橘色
  3: { label: '活動標籤', color: '#d81b60' }     // 質感桃紅
}

// --- 資料狀態 ---
const productList = ref([])
const categories = ref([])
const pagination = ref({ currentPage: 1, totalPages: 1 })
const searchKeyword = ref('')
const selectedIds = ref([]) 

const mode = ref('list')
const currentProduct = ref({}) 
const previewUrl = ref(null)

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
    const params = { 
      cp: page, 
      searchKeyword: searchKeyword.value 
    }
    // 如果網址有帶 categoryId，就加入搜尋條件
    if (route.query.categoryId) {
      params.categoryId = route.query.categoryId
    }

    // 對齊後端路徑: /api/products/list
    const res = await request.get('/api/products/list', { params })
    // 根據你 Java 回傳的 Map 結構抓資料
    productList.value = res.data.productList
    pagination.value.currentPage = res.data.currentPage
    pagination.value.totalPages = res.data.totalPages
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
  if (route.query.categoryId) {
    router.back()
  } else {
    searchKeyword.value = ''
    fetchProducts(1)
  }
}

// 取得分類清單
const fetchCategories = async () => {
  const res = await request.get('/api/categories')
  categories.value = res.data
}

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
  fetchProducts()
  fetchCategories()
})
</script>

<template>
  <!-- <div class="admin-container"> -->
    <div v-if="mode === 'list'" class="main-card">
      
      <div class="toolbar-group">
  <div class="btn-left">
    <button @click="router.push('/admin/product/add')" class="btn-action btn-add">＋ 新增商品</button>
    <span class="toolbar-divider"></span>
    <button @click="batchStatus(1)" class="btn-action btn-batch-up">▲ 批次上架</button>
    <button @click="batchStatus(0)" class="btn-action btn-batch-down">▼ 批次下架</button>
    <span v-if="selectedIds.length > 0" class="selected-count-badge">
      已選 {{ selectedIds.length }} 項
    </span>
  </div>

  <div class="search-wrapper">
    <span class="search-icon">🔍</span>
    <input 
      v-model="searchKeyword" 
      @keyup.enter="fetchProducts(1)" 
      class="search-input" 
      placeholder="搜尋商品名稱..."
    >
    <button v-if="route.query.categoryId || searchKeyword" @click="goBack" class="btn-back ms-2">
      <i class="fas fa-arrow-left"></i>返回
    </button>
  </div>
</div>

      <table class="custom-table align-middle">
        <thead>
          <tr>
            <th style="width: 10px;"><input type="checkbox" ref="selectAllRef" :checked="isAllSelected" @change="toggleSelectAll" class="form-check-input"></th>
            <th style="width: 80px;" class="sortable-th" @click="toggleSort('productStatus')">
              <div class="th-sort-wrap">
                <span>狀態</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productStatus' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productStatus' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 60px;" class="sortable-th" @click="toggleSort('productId')">
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
            <th style="width: 80px;" class="sortable-th" @click="toggleSort('productPrice')">
              <div class="th-sort-wrap">
                <span>單價</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productPrice' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productPrice' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 80px;" class="sortable-th" @click="toggleSort('productStock')">
              <div class="th-sort-wrap">
                <span>庫存</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'productStock' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'productStock' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 150px;" class="sortable-th" @click="toggleSort('categoryName')">
              <div class="th-sort-wrap">
                <span>分類</span>
                <span class="sort-arrows">
                  <span :class="['arrow-up', { active: sortKey === 'categoryName' && sortOrder === 'asc' }]">▲</span>
                  <span :class="['arrow-down', { active: sortKey === 'categoryName' && sortOrder === 'desc' }]">▼</span>
                </span>
              </div>
            </th>
            <th style="width: 80px;">操作</th>
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
            <td>
              <span :class="{'text-danger fw-bold': p.productStock < 10}">{{ p.productStock }}</span>
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
</template>
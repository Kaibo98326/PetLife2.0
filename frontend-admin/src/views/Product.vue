<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request' // 1. 改用你的全域配置
import Swal from 'sweetalert2'
import '@/assets/css/Product.css'

// --- 資料狀態 ---
const productList = ref([])
const categories = ref([])
const pagination = ref({ currentPage: 1, totalPages: 1 })
const searchKeyword = ref('')
const selectedIds = ref([]) 

const mode = ref('list')
const currentProduct = ref({}) 
const previewUrl = ref(null)

// 當圖片載入失敗時，自動替換成一張預設的預覽圖
const handleImgError = (e) => {
  e.target.src = 'https://placehold.co/60x60?text=No+Img'; 
}

// --- API 呼叫 ---
const fetchProducts = async (page = 1) => {
  try {
    // 對齊後端路徑: /api/products/list
    const res = await request.get('/api/products/list', {
      params: { 
        cp: page, 
        searchKeyword: searchKeyword.value 
      }
    })
    // 根據你 Java 回傳的 Map 結構抓資料
    productList.value = res.data.productList
    pagination.value.currentPage = res.data.currentPage
    pagination.value.totalPages = res.data.totalPages
  } catch (error) {
    console.error("讀取失敗", error)
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
  formData.append('categoryId', currentProduct.value.categoryId || '')
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

// 批次上下架
const batchStatus = async (status) => {
  if (selectedIds.value.length === 0) return Swal.fire('提示', '請先勾選商品', 'info')
  try {
    // 對齊 Java: /api/product/batchUpdateStatus
    await request.post('/api/products/api/product/batchUpdateStatus', { 
      ids: selectedIds.value, 
      status 
    })
    Swal.fire('成功', '批次更新完成', 'success')
    fetchProducts(pagination.value.currentPage)
  } catch (error) {
    console.error(error)
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
      
      <div class="toolbar-group d-flex align-items-center justify-content-between flex-wrap gap-3">
  
  <div class="btn-left d-flex gap-2">
    <button @click="mode = 'add'" class="btn-action btn-add">＋ 新增商品</button>
    <button @click="batchStatus(1)" class="btn-action btn-outline">批次上架</button>
    <button @click="batchStatus(0)" class="btn-action btn-outline">批次下架</button>
  </div>

  <div class="search-wrapper ms-auto">
    <span class="search-icon">🔍</span>
    <input 
      v-model="searchKeyword" 
      @keyup.enter="fetchProducts(1)" 
      class="search-input" 
      placeholder="搜尋商品名稱..."
    >
  </div>
</div>

      <table class="custom-table align-middle">
        <thead>
          <tr>
            <th style="width: 10px;"><input type="checkbox" class="form-check-input"></th>
      <th style="width: 80px;">狀態</th>
      <th style="width: 60px;">商品</th>
      <th class="th-name">名稱</th> <!-- ✨ 不給寬度，讓它自動佔滿剩餘空間 -->
      <th style="width: 80px;">單價</th>
      <th style="width: 80px;">庫存</th>
      <th style="width: 90px;">分類</th>
      <th style="width: 50px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in productList" :key="p.productId">
            <td><input type="checkbox" :value="p.productId" v-model="selectedIds" class="form-check-input"></td>
            <td>
              <span :class="['status-pill', p.productStatus === 1 ? 'status-active' : 'status-inactive']">
                {{ p.productStatus === 1 ? '上架中' : '已下架' }}
              </span>
            </td>
            <td>
              <div class="prod-img-wrapper">
                <img :src="`http://localhost:8082/${p.productImage}`" class="prod-img" @error="handleImgError">
                <span class="prod-id-badge">#{{ p.productId }}</span>
              </div>
            </td>
            <td class="fw-bold" style="color: #5d4037;">{{ p.productName }}</td>
            <td style="color: #e67e22; font-weight: 600;">${{ p.productPrice }}</td>
            <td>
              <span :class="{'text-danger fw-bold': p.productStock < 10}">{{ p.productStock }}</span>
            </td>
            <td><span class="category-tag">{{ p.categoryName || '預設分類' }}</span></td>
            <td>
              <button @click="mode = 'edit'; currentProduct = { ...p }; previewUrl = `http://localhost:8082/${p.productImage}`" 
                      class="btn btn-sm btn-link text-decoration-none" style="color: #2c3e50;">修改</button>
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
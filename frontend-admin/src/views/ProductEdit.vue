<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import '@/assets/css/ProductEdit.css'

const route = useRoute()
const router = useRouter()

const productId = computed(() => route.params.id)
const categories = ref([])
const product = ref({
  productId: '',
  productName: '',
  categoryId: '',
  productPrice: 0,
  productStock: 0,
  productDescription: '',
  productImage: '',
  productStatus: 1,
})
const previewUrl = ref(null)
const tempFile = ref(null)
const loading = ref(true)

// 取得商品資料
const fetchProduct = async () => {
  try {
    const res = await request.get(`/api/products/${productId.value}`)
    product.value = res.data
    if (product.value.productImage) {
      previewUrl.value = `http://localhost:8082/${product.value.productImage}`
    }
  } catch (error) {
    console.error('讀取商品失敗', error)
    Swal.fire('錯誤', '找不到該商品資料', 'error')
  } finally {
    loading.value = false
  }
}

// 取得分類清單
const fetchCategories = async () => {
  const res = await request.get('/api/categories')
  categories.value = res.data
}

// 圖片預覽處理
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    tempFile.value = file
    previewUrl.value = URL.createObjectURL(file)
  }
}

// 儲存修改
const saveProduct = async () => {
  const formData = new FormData()

  formData.append('productId', product.value.productId)
  formData.append('productName', product.value.productName || '')
  formData.append('categoryId', product.value.categoryId || '')
  formData.append('productPrice', product.value.productPrice || 0)
  formData.append('productStock', product.value.productStock || 0)
  formData.append('productDescription', product.value.productDescription || '')
  formData.append('oldImage', product.value.productImage || '')

  if (tempFile.value) {
    formData.append('file', tempFile.value)
  }

  try {
    await request.post('/api/products/update', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    Swal.fire('成功', '商品資料已更新', 'success').then(() => {
      router.push('/admin/product')
    })
  } catch (error) {
    Swal.fire('錯誤', '更新失敗', 'error')
  }
}

// 返回商品列表
const goBack = () => {
  router.push('/admin/product')
}

// 刪除商品（此操作不可逆，僅供緊急用途）
const deleteProduct = async () => {
  const first = await Swal.fire({
    title: '確定要刪除此商品嗎？',
    text: '此操作無法復原，商品資料將永久刪除！',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: '確認刪除',
    cancelButtonText: '取消',
    confirmButtonColor: '#e74c3c',
  })
  if (!first.isConfirmed) return

  // 二次確認以防止誤操作
  const second = await Swal.fire({
    title: '再次確認',
    text: `即將永久刪除商品「${product.value.productName}」，是否繼續？`,
    icon: 'error',
    showCancelButton: true,
    confirmButtonText: '永久刪除',
    cancelButtonText: '取消',
    confirmButtonColor: '#c0392b',
  })
  if (!second.isConfirmed) return

  try {
    await request.delete(`/api/products/${productId.value}`)
    Swal.fire('已刪除', '商品已成功刪除', 'success').then(() => {
      router.push('/admin/product')
    })
  } catch (error) {
    Swal.fire('錯誤', '刪除失敗，請稍後再試', 'error')
  }
}

// 當圖片載入失敗時，自動替換成一張預設的預覽圖
const handleImgError = (e) => {
  e.target.src = 'https://placehold.co/200x200?text=No+Img'
}

onMounted(() => {
  fetchProduct()
  fetchCategories()
})
</script>

<template>
  <div class="edit-container">
    <!-- 頂部導航列 -->
    <div class="edit-header">
      <button class="btn-back" @click="goBack">
        <span class="back-arrow">←</span> 返回商品列表
      </button>
      <h2 class="edit-title">編輯商品</h2>
      <div class="edit-id-badge">#{{ product.productId }}</div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>載入商品資料中...</p>
    </div>

    <div v-else class="edit-body">
      <!-- 左側：圖片區 -->
      <div class="edit-left">
        <div class="image-card">
          <div class="image-preview-box">
            <img 
              v-if="previewUrl" 
              :src="previewUrl" 
              alt="商品圖片" 
              class="preview-img"
              @error="handleImgError"
            />
            <div v-else class="no-image">
              <span class="no-image-icon">🖼️</span>
              <span>尚未上傳圖片</span>
            </div>
          </div>
          <label class="upload-btn">
            <span>📁 更換圖片</span>
            <input type="file" accept="image/*" @change="handleFileChange" hidden />
          </label>
        </div>
      </div>

      <!-- 右側：表單區 -->
      <div class="edit-right">
        <div class="form-card">
          <div class="form-group">
            <label class="form-label">商品名稱</label>
            <input v-model="product.productName" type="text" class="form-input" placeholder="請輸入商品名稱" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">分類</label>
              <select v-model="product.categoryId" class="form-input">
                <option value="" disabled>請選擇分類</option>
                <option v-for="cat in categories" :key="cat.categoryId" :value="cat.categoryId">
                  {{ cat.categoryName }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">狀態</label>
              <select v-model="product.productStatus" class="form-input">
                <option :value="1">上架中</option>
                <option :value="0">已下架</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">單價 (NT$)</label>
              <input v-model.number="product.productPrice" type="number" class="form-input" min="0" />
            </div>

            <div class="form-group">
              <label class="form-label">庫存</label>
              <input v-model.number="product.productStock" type="number" class="form-input" min="0" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">商品描述</label>
            <textarea v-model="product.productDescription" class="form-input form-textarea" rows="5" placeholder="請輸入商品描述..."></textarea>
          </div>

          <div class="form-actions">
            <button class="btn-delete" @click="deleteProduct">🗑️ 刪除商品</button>
            <div class="form-actions-right">
              <button class="btn-cancel" @click="goBack">取消</button>
              <button class="btn-save" @click="saveProduct">💾 儲存修改</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

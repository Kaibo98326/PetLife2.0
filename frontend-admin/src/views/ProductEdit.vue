<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import { useProductStore } from '@/stores/product'
import '@/assets/css/ProductEdit.css'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()

const productId = computed(() => route.params.id)
const categories = ref([])

const typeMap = {
  2: { label: '專區', color: '#e67e22', icon: 'fas fa-sitemap' },
  1: { label: '實體分類', color: '#795548', icon: 'fas fa-tag' },
  3: { label: '活動標籤', color: '#d81b60', icon: 'fas fa-thumbtack' }
}

const product = ref({
  productId: '',
  productName: '',
  categoryIds: [],
  productPrice: 0,
  productStock: 0,
  lowStock: 10,
  storagePosition: '',
  productDescription: '',
  productImage: '',
  productStatus: 1,
})
const previewUrl = ref(null)
const tempFile = ref(null)

// ── 多張細節圖相關狀態 ──
const tempExtraFiles = ref([])
const extraPreviewUrls = ref([])
const existingExtraImages = ref([])

const loading = ref(true)
const isIgnoreWarning = ref(false)

// 過濾分類
const bigCategories = computed(() => categories.value.filter(c => c.categoryType === 2))
const activityLabels = computed(() => categories.value.filter(c => c.categoryType === 3))

// 取得當前選中的專區
const selectedBigCatIds = computed(() => {
  return product.value.categoryIds.filter(id => {
    const cat = categories.value.find(c => c.categoryId === id)
    return cat && cat.categoryType === 2
  })
})

// 根據選中的專區，取得對應的實體分類
const getChildrenByParent = (parentId) => {
  return categories.value.filter(c => c.categoryType === 1 && c.parentId === parentId)
}

const fetchProduct = async () => {
  if (!productId.value) {
    loading.value = false
    return
  }
  try {
    const res = await request.get(`/api/products/detail/${productId.value}`)
    product.value = res.data
    if (product.value.categories) {
      product.value.categoryIds = product.value.categories.map(c => c.categoryId)
    } else if (!product.value.categoryIds) {
      product.value.categoryIds = []
    }
    
    // 處理忽略預警的 UI 狀態
    if (product.value.lowStock === -1) {
      isIgnoreWarning.value = true
    }
    if (product.value.productImage) {
      previewUrl.value = `http://localhost:8082/${product.value.productImage}`
    }
    
    // 載入已有的細節圖 (過濾掉主圖)
    if (product.value.images && product.value.images.length > 0) {
      existingExtraImages.value = product.value.images
        .filter(img => img.imageUrl !== product.value.productImage)
        .map(img => `http://localhost:8082/${img.imageUrl}`)
    }
  } catch (error) {
    console.error('讀取失敗', error)
    Swal.fire('錯誤', '找不到商品', 'error')
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  const res = await request.get('/api/categories')
  categories.value = res.data
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    tempFile.value = file
    previewUrl.value = URL.createObjectURL(file)
  }
}

const handleExtraFilesChange = (e) => {
  const files = Array.from(e.target.files)
  files.forEach(file => {
    tempExtraFiles.value.push(file)
    extraPreviewUrls.value.push(URL.createObjectURL(file))
  })
  // 清空 input 讓下次選同檔名也能觸發
  e.target.value = ''
}

const removeExtraFile = (index) => {
  tempExtraFiles.value.splice(index, 1)
  extraPreviewUrls.value.splice(index, 1)
}


const saveProduct = async () => {
  // --- 表單必填驗證 ---
  if (!product.value.productName || !product.value.productName.trim()) {
    return Swal.fire('提示', '請填寫商品名稱', 'warning')
  }
  if (!product.value.productPrice || product.value.productPrice <= 0) {
    return Swal.fire('提示', '請輸入正確的商品單價', 'warning')
  }
  if (!product.value.categoryIds || product.value.categoryIds.length === 0) {
    return Swal.fire('提示', '請至少選擇一個商品分類', 'warning')
  }
  if (!product.value.productImage && !tempFile.value) {
    return Swal.fire('提示', '請上傳商品主圖', 'warning')
  }

  // --- 庫存為 0 的提醒 ---
  if (product.value.productStock === 0 || !product.value.productStock) {
    const confirmZero = await Swal.fire({
      title: '提醒：庫存為 0',
      text: '目前的庫存數量為 0，確定要繼續儲存商品嗎？',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#f39c12',
      cancelButtonText: '取消',
      confirmButtonText: '確定儲存'
    })
    if (!confirmZero.isConfirmed) return
  }

  const formData = new FormData()
  if (productId.value) formData.append('productId', product.value.productId)
  formData.append('productName', product.value.productName || '')
  if (product.value.categoryIds) {
    product.value.categoryIds.forEach(id => formData.append('categoryIds', id))
  }
  formData.append('productPrice', product.value.productPrice || 0)
  formData.append('productStock', product.value.productStock || 0)
  // 如果勾選忽略預警，傳送 -1 給後端
  formData.append('lowStock', isIgnoreWarning.value ? -1 : (product.value.lowStock || 10))
  formData.append('storagePosition', product.value.storagePosition || '')
  formData.append('productStatus', product.value.productStatus ?? 1)
  formData.append('productDescription', product.value.productDescription || '')
  formData.append('oldImage', product.value.productImage || '')
  if (tempFile.value) formData.append('file', tempFile.value)

  // 加入多張細節圖
  if (tempExtraFiles.value.length > 0) {
    tempExtraFiles.value.forEach(file => {
      formData.append('extraFiles', file)
    })
  }

  try {
    const url = productId.value ? '/api/products/update' : '/api/products/insert'
    const res = await request.post(url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.data === 'success') {
      productStore.fetchLowStockCount()
      Swal.fire('成功', '商品資料已儲存', 'success').then(() => router.push('/admin/product'))
    } else {
      throw new Error(res.data)
    }
  } catch (error) {
    Swal.fire('錯誤', `儲存失敗: ${error.message}`, 'error')
  }
}

// 執行刪除
const deleteProduct = async () => {
  const result = await Swal.fire({
    title: '確定刪除？',
    text: `即將永久刪除「${product.value.productName}」，此操作無法復原！`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#e74c3c',
    cancelButtonText: '取消',
    confirmButtonText: '確定刪除'
  })
  
  if (result.isConfirmed) {
    try {
      // 對應 InnerProductController 的 @DeleteMapping("/delete/{id}")
      await request.delete(`/api/products/delete/${productId.value}`)
      Swal.fire('已刪除', '商品已成功移除', 'success').then(() => {
        router.push('/admin/product')
      })
    } catch (error) {
      console.error(error)
      Swal.fire('失敗', '刪除失敗', 'error')
    }
  }
}


// 當取消勾選專區時，自動取消其下的所有實體分類
watch(() => [...product.value.categoryIds], (newVal, oldVal) => {
  if (newVal.length < oldVal.length) {
    const removedId = oldVal.find(id => !newVal.includes(id))
    const removedCat = categories.value.find(c => c.categoryId === removedId)
    
    // 如果取消的是專區，則取消其所有子項目
    if (removedCat && removedCat.categoryType === 2) {
      const childrenIds = categories.value
        .filter(c => c.parentId === removedId)
        .map(c => c.categoryId)
      
      product.value.categoryIds = product.value.categoryIds.filter(id => !childrenIds.includes(id))
    }
  }
}, { deep: true })

const autofillProduct = () => {
  product.value.productName = "【Dog Says 狗狗說】早點蛋條 (護心好健康蛋)｜雞蛋+魚油 15g x 4入"
  product.value.productPrice = 39
  product.value.productStock = 120
  product.value.lowStock = 15
  product.value.storagePosition = "D-03-24"
  product.value.productStatus = 1
  product.value.productDescription = `【Dog Says 狗狗說】早點蛋條系列 — 護心好健康蛋（雞蛋+魚油）\n專為狗狗設計的健康機能美味零食！\n\n💡 產品特色：\n• 軟嫩蒸蛋質地：高溫高壓精心烹煮，口感如布丁般 Q 彈細緻，好消化、好吸收，愛不釋口。\n• 雙重營養守護：優質雞蛋 ＋ 黃金魚油（富含 Omega-3），保護心血管健康並維持毛髮亮麗。\n• 鮮美滴雞湯基底：採用濃郁雞白湯與熬煮雞湯製作，香氣四溢，適口性極佳，挑嘴毛孩也瘋狂。\n• 全齡犬適用：3 個月以上幼犬、成犬及熟齡犬皆可安心食用。\n\n📦 產品規格：\n• 規格：15g x 4 條 / 包\n• 產地：台灣研發與製造\n\n🔬 營養成分分析（每 100g）：\n• 粗蛋白質 10.8% 以上\n• 粗脂肪 12.0% 以上\n• 水分 73.7% 以下\n• 熱量約 158.8 Kcal`
  
  // 智慧篩選匹配狗狗/零食分類
  const autoSelectedIds = []
  const dogZone = categories.value.find(c => c.categoryType === 2 && (c.categoryName.includes("狗") || c.categoryName.includes("犬")))
  if (dogZone) {
    autoSelectedIds.push(dogZone.categoryId)
    const subCats = categories.value.filter(c => c.categoryType === 1 && c.parentId === dogZone.categoryId)
    const targetSub = subCats.find(c => c.categoryName.includes("零食") || c.categoryName.includes("罐頭") || c.categoryName.includes("食品")) || subCats[0]
    if (targetSub) {
      autoSelectedIds.push(targetSub.categoryId)
    }
  }
  if (autoSelectedIds.length === 0 && categories.value.length > 0) {
    if (categories.value[0]) autoSelectedIds.push(categories.value[0].categoryId)
    if (categories.value[1]) autoSelectedIds.push(categories.value[1].categoryId)
  }
  product.value.categoryIds = autoSelectedIds
  
  Swal.fire({
    title: '已自動輸入資料！',
    text: '已為您自動填充【狗狗說早點蛋條】的完整商品資訊！',
    icon: 'success',
    timer: 1500,
    showConfirmButton: false
  })
}

onMounted(() => {
  fetchProduct()
  fetchCategories()
})
</script>

<template>
  <div class="edit-container">
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在準備商品資料...</p>
    </div>

    <div v-else class="edit-grid">
      <!-- Left Column -->
      <aside class="grid-left">
        <section class="form-card image-upload-wrapper">
          <div class="section-title"><i class="fas fa-image"></i> 商品照片 <span class="text-danger ms-2">*</span></div>
          <div class="image-preview-container">
            <img v-if="previewUrl" :src="previewUrl" class="preview-img" @error="e => e.target.src = 'https://placehold.co/400x400?text=無圖片'" />
            <div v-else style="color: #eee5d8; font-size: 3rem;"><i class="fas fa-camera"></i></div>
          </div>
          <label class="btn-primary-custom" style="display: block; text-align: center;">
            <i class="fas fa-cloud-upload-alt me-2"></i> 上傳主圖
            <input type="file" @change="handleFileChange" hidden />
          </label>
        </section>

        <section class="form-card image-upload-wrapper mt-3">
          <div class="section-title"><i class="fas fa-images"></i> 商品細節圖 (可選多張)</div>
          
          <!-- 已存在的細節圖預覽 -->
          <div class="existing-extras mb-2" v-if="existingExtraImages.length > 0">
            <label style="font-size: 0.85rem; color: #888;">目前已有的細節圖：</label>
            <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px;">
              <div v-for="(imgUrl, idx) in existingExtraImages" :key="idx" class="extra-preview-box">
                <img :src="imgUrl" class="preview-img-small" />
              </div>
            </div>
          </div>

          <!-- 新選擇的細節圖預覽 -->
          <div class="new-extras mb-3" v-if="extraPreviewUrls.length > 0">
            <label style="font-size: 0.85rem; color: #e67e22;">新選擇的圖片 (儲存後生效)：</label>
            <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px;">
              <div v-for="(url, idx) in extraPreviewUrls" :key="idx" class="extra-preview-box">
                <img :src="url" class="preview-img-small" />
                <button type="button" class="remove-btn" @click.stop="removeExtraFile(idx)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>
          </div>

          <label class="btn-secondary-custom" style="display: block; text-align: center;">
            <i class="fas fa-plus me-2"></i> 選擇細節圖
            <input type="file" multiple @change="handleExtraFilesChange" hidden />
          </label>
        </section>

        <section class="form-card">
          <div class="section-title"><i class="fas fa-tasks"></i> 狀態與庫存</div>
          <div class="form-group">
            <label class="form-label">銷售狀態</label>
            <select v-model="product.productStatus" class="form-select">
              <option :value="1">上架中</option>
              <option :value="0">已下架</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">儲位編號</label>
            <input v-model="product.storagePosition" type="text" class="form-control" placeholder="A-01-01" />
          </div>
        </section>
      </aside>

      <!-- Right Column -->
      <main class="grid-right">
        <section class="form-card">
          <div class="section-title d-flex justify-content-between align-items-center">
            <span><i class="fas fa-edit"></i> 基本資訊</span>
            <button v-if="!productId" type="button" class="btn-autofill-demo" @click="autofillProduct">
              <i class="fas fa-magic me-1"></i> 一鍵輸入
            </button>
          </div>
          <div class="form-group">
            <label class="form-label">商品名稱 <span class="text-danger">*</span></label>
            <input v-model="product.productName" type="text" class="form-control" placeholder="請輸入完整商品名稱" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">單價 (NT$) <span class="text-danger">*</span></label>
              <input v-model.number="product.productPrice" type="number" class="form-control" />
            </div>
            <div class="form-group">
              <label class="form-label">目前庫存量</label>
              <input v-model.number="product.productStock" type="number" class="form-control" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">庫存預警設定</label>
            <div class="warning-setting-box">
              <el-checkbox v-model="isIgnoreWarning" class="me-3">不需預警提醒</el-checkbox>
              <div v-if="!isIgnoreWarning" class="input-with-label animate__animated animate__fadeIn">
                <span class="sub-label">低於</span>
                <input v-model.number="product.lowStock" type="number" class="form-control small-input" />
                <span class="sub-label">件時提醒</span>
              </div>
              <span v-else class="text-muted" style="font-size: 0.9rem;">(已關閉此商品的預警功能)</span>
            </div>
          </div>
        </section>

        <!-- 分類選取 (階層化) -->
        <section class="form-card">
          <div class="section-title"><i class="fas fa-layer-group"></i> 分類選取 <span class="text-danger ms-2">*</span></div>
          <div class="category-pills-container">
            
            <!-- 1. 第一步：選擇專區 -->
            <div class="type-section">
              <span class="type-label" :style="{ color: typeMap[2].color }">
                <i :class="typeMap[2].icon + ' me-1'"></i> STEP 1. 請先勾選專區
              </span>
              <div class="pill-grid">
                <label v-for="cat in bigCategories" :key="cat.categoryId" 
                  class="category-pill" :class="{ 'active': product.categoryIds.includes(cat.categoryId) }">
                  <input type="checkbox" v-model="product.categoryIds" :value="cat.categoryId" />
                  {{ cat.categoryName }}
                </label>
              </div>
            </div>

            <!-- 2. 第二步：根據選中的專區顯示對應的實體分類 -->
            <div v-if="selectedBigCatIds.length > 0" class="sub-category-wrapper mt-4">
               <div v-for="bigId in selectedBigCatIds" :key="bigId" class="parent-group-section mb-3">
                  <span class="type-label" style="color: #8d6e63; font-size: 0.8rem; opacity: 0.8;">
                    <i class="fas fa-level-up-alt fa-rotate-90 me-2"></i> 
                    屬於「{{ categories.find(c => c.categoryId === bigId)?.categoryName }}」的子分類
                  </span>
                  <div class="pill-grid">
                    <label v-for="sub in getChildrenByParent(bigId)" :key="sub.categoryId" 
                      class="category-pill" :class="{ 'active': product.categoryIds.includes(sub.categoryId) }">
                      <input type="checkbox" v-model="product.categoryIds" :value="sub.categoryId" />
                      {{ sub.categoryName }}
                    </label>
                    <div v-if="getChildrenByParent(bigId).length === 0" class="no-sub-hint">
                      (此專區目前無子分類)
                    </div>
                  </div>
               </div>
            </div>
            <div v-else class="empty-sub-hint mt-3">
               <i class="fas fa-info-circle me-1"></i> 請先勾選上方專區，以顯示可選的子分類
            </div>

            <hr class="my-4" style="border: none; border-top: 1px dashed #eee5d8;">

            <!-- 3. 第三步：活動標籤 (獨立選取) -->
            <div class="type-section">
              <span class="type-label" :style="{ color: typeMap[3].color }">
                <i :class="typeMap[3].icon + ' me-1'"></i> 其他活動標籤
              </span>
              <div class="pill-grid">
                <label v-for="cat in activityLabels" :key="cat.categoryId" 
                  class="category-pill" :class="{ 'active': product.categoryIds.includes(cat.categoryId) }">
                  <input type="checkbox" v-model="product.categoryIds" :value="cat.categoryId" />
                  {{ cat.categoryName }}
                </label>
              </div>
            </div>

          </div>
        </section>

        <section class="form-card">
          <div class="section-title"><i class="fas fa-align-left"></i> 商品描述</div>
          <textarea v-model="product.productDescription" class="form-control" style="min-height: 150px;" placeholder="輸入商品詳細介紹..."></textarea>
        </section>

        <div class="form-actions" style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px;">
          <button v-if="productId" class="btn-danger-custom" @click="deleteProduct"><i class="fas fa-trash-alt"></i> 刪除商品資料</button>
          <div v-else></div>
          <div style="display: flex; gap: 15px;">
            <button class="btn-secondary-custom" @click="router.push('/admin/product')">取消返回</button>
            <button class="btn-primary-custom" @click="saveProduct">確認儲存</button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.btn-autofill-demo {
  background: linear-gradient(135deg, #e67e22, #d35400);
  color: white;
  border: none;
  padding: 6px 14px;
  font-size: 0.85rem;
  font-weight: 600;
  border-radius: 20px;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(230, 126, 34, 0.2);
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
}

.btn-autofill-demo:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(230, 126, 34, 0.4);
  background: linear-gradient(135deg, #f39c12, #e67e22);
}

.btn-autofill-demo:active {
  transform: translateY(0);
}
</style>


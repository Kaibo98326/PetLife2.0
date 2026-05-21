<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
// 1. 改引入你自己寫的全域配置
import request from '@/utils/request' 
import Swal from 'sweetalert2'

// 引入獨立的 CSS 樣式檔 (暖心奶油專業版風格)
import '@/assets/css/Category.css'

const router = useRouter()

const categories = ref([])
const isEditing = ref(false)

const newCategory = ref({
  categoryName: '',
  categoryType: 1,
  parentId: null
})

const editItem = ref({
  categoryId: null,
  categoryName: '',
  categoryType: 1,
  parentId: null
})

// 分類類型定義
const typeMap = {
  1: { label: '實體分類', class: 'type-physical', color: '#795548' }, // 溫暖褐色
  2: { label: '專區', class: 'type-area', color: '#e67e22' },    // 活力橘色
  3: { label: '活動標籤', class: 'type-tag', color: '#d81b60' }     // 質感桃紅
}

// 1. 初始化讀取清單
const fetchCategories = async () => {
  try {
    // 使用 request 就不需要寫 http://localhost:8082 了
    const res = await request.get('/api/categories')
    categories.value = res.data 
    console.log("抓取到的分類：", categories.value)
  } catch (error) {
    console.error("讀取失敗", error)
  }
}



// 取得特定群組的樹狀結構 (用來區分「商城分類(Type 1,2)」與「活動標籤(Type 3)」)
const getTree = (filterFn) => {
  const result = []
  const map = {}
  
  // 過濾並存入 map
  const filtered = categories.value.filter(filterFn)
  filtered.forEach(cat => {
    map[cat.categoryId] = { ...cat, children: [] }
  })
  
  // 建立樹狀結構
  const roots = []
  filtered.forEach(cat => {
    if (cat.parentId && map[cat.parentId]) {
      map[cat.parentId].children.push(map[cat.categoryId])
    } else {
      roots.push(map[cat.categoryId])
    }
  })
  
  // 依照 sortOrder 排序
  const sortItems = (items) => {
    items.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    items.forEach((item, index) => {
      // 紀錄自己在同層級中的相對位置
      item.isFirstSibling = index === 0;
      item.isLastSibling = index === items.length - 1;
      
      if (item.children.length > 0) sortItems(item.children)
    })
  }
  sortItems(roots)
  
  // 遞迴展開為扁平列表以便顯示
  const flatten = (items, depth = 0) => {
    items.forEach((item) => {
      result.push({ 
        ...item, 
        depth, 
        // 視覺上的上一個項目，用來做 makeSubcategory (抓取結果陣列的最後一個元素)
        visualPrevId: result.length > 0 ? result[result.length - 1].categoryId : null
      })
      if (item.children.length > 0) {
        flatten(item.children, depth + 1)
      }
    })
  }
  
  flatten(roots)
  return result
}

// 區分兩個區塊
const mainCategories = computed(() => getTree(cat => cat.categoryType === 1 || cat.categoryType === 2))

// 活動標籤不需要階層，直接扁平化顯示，並隱藏名稱為「活動標籤」的虛擬父節點
const activityCategories = computed(() => {
  const tags = categories.value
    .filter(cat => cat.categoryType === 3 && cat.categoryName !== '活動標籤')
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    
  return tags.map((cat, index) => ({
    ...cat,
    depth: 0,
    isFirstSibling: index === 0,
    isLastSibling: index === tags.length - 1
  }))
})

// ========================
// 階層與排序操作
// ========================

const updateCategoryField = async (catId, fieldUpdateObj) => {
  const target = categories.value.find(c => c.categoryId === catId)
  if (!target) return
  const updated = { ...target, ...fieldUpdateObj }
  try {
    await request.put(`/api/categories/${catId}`, updated)
    // 成功後更新本地資料
    Object.assign(target, fieldUpdateObj)
  } catch (error) {
    Swal.fire('失敗', '更新失敗', 'error')
  }
}

const makeSubcategory = async (cat) => {
  if (!cat.visualPrevId) return
  await updateCategoryField(cat.categoryId, { parentId: cat.visualPrevId, categoryType: 1 })
  fetchCategories()
}

const makeMainCategory = async (cat) => {
  await updateCategoryField(cat.categoryId, { parentId: null, categoryType: 2 })
  fetchCategories()
}

const moveUp = async (cat) => {
  // 找出所有同層級的兄弟節點
  let siblings = []
  if (cat.categoryType === 3) {
    // 活動標籤：忽略 parentId，統一看作一個群組，並排除虛擬父節點
    siblings = categories.value
      .filter(c => c.categoryType === 3 && c.categoryName !== '活動標籤')
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } else {
    // 商城分類：嚴格比對同層級
    siblings = categories.value
      .filter(c => c.parentId == cat.parentId && c.categoryType == cat.categoryType)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  }
    
  const index = siblings.findIndex(c => c.categoryId === cat.categoryId)
  if (index > 0) {
    // 為了避免大家都是 0 換不出結果，先強制賦予有間距的排序值
    siblings.forEach((s, i) => { s.sortOrder = i * 10 })
    
    // 交換兩者
    const prevCat = siblings[index - 1]
    const currentOrder = cat.sortOrder
    cat.sortOrder = prevCat.sortOrder
    prevCat.sortOrder = currentOrder
    
    // 更新到資料庫
    await updateCategoryField(cat.categoryId, { sortOrder: cat.sortOrder })
    await updateCategoryField(prevCat.categoryId, { sortOrder: prevCat.sortOrder })
    fetchCategories()
  }
}

const moveDown = async (cat) => {
  let siblings = []
  if (cat.categoryType === 3) {
    siblings = categories.value
      .filter(c => c.categoryType === 3 && c.categoryName !== '活動標籤')
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } else {
    siblings = categories.value
      .filter(c => c.parentId == cat.parentId && c.categoryType == cat.categoryType)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  }
    
  const index = siblings.findIndex(c => c.categoryId === cat.categoryId)
  if (index !== -1 && index < siblings.length - 1) {
    siblings.forEach((s, i) => { s.sortOrder = i * 10 })
    
    const nextCat = siblings[index + 1]
    const currentOrder = cat.sortOrder
    cat.sortOrder = nextCat.sortOrder
    nextCat.sortOrder = currentOrder
    
    await updateCategoryField(cat.categoryId, { sortOrder: cat.sortOrder })
    await updateCategoryField(nextCat.categoryId, { sortOrder: nextCat.sortOrder })
    fetchCategories()
  }
}


// 2. 執行新增
const submitAdd = async () => {
  if (!newCategory.value.categoryName.trim()) {
    Swal.fire('提示', '請填寫分類名稱', 'warning')
    return
  }
  if (newCategory.value.categoryType === 1 && !newCategory.value.parentId) {
    Swal.fire('提示', '實體分類必須隸屬於一個專區', 'warning')
    return
  }
  if (newCategory.value.categoryType !== 1) {
    newCategory.value.parentId = null
  }
  try {
    await request.post('/api/categories', newCategory.value)
    newCategory.value = { categoryName: '', categoryType: 1, parentId: null }
    Swal.fire('成功', '分類已新增', 'success')
    fetchCategories() 
  } catch (error) {
    Swal.fire('失敗', '新增失敗', 'error')
  }
}

// 3. 準備修改 (打開編輯模式)
const startEdit = async (cat) => {
  editItem.value = { ...cat } 
  isEditing.value = true
  
  // 等待 Vue 將編輯區塊渲染到畫面上後，再將該區塊捲動到可視範圍
  await nextTick()
  const editContainer = document.querySelector('.category-container')
  if (editContainer) {
    editContainer.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 4. 執行更新
const submitUpdate = async () => {
  if (!editItem.value.categoryName.trim()) {
    Swal.fire('提示', '請填寫分類名稱', 'warning')
    return
  }
  if (editItem.value.categoryType === 1 && !editItem.value.parentId) {
    Swal.fire('提示', '實體分類必須隸屬於一個專區', 'warning')
    return
  }
  if (editItem.value.categoryType !== 1) {
    editItem.value.parentId = null
  }
  try {
    await request.put(`/api/categories/${editItem.value.categoryId}`, editItem.value)
    isEditing.value = false
    Swal.fire('成功', '分類已更新', 'success')
    fetchCategories()
  } catch (error) {
    Swal.fire('失敗', '更新失敗', 'error')
  }
}

// 5. 執行刪除
const confirmDelete = (cat) => {
  if (cat.productCount > 0) {
    Swal.fire('無法刪除', `此分類還有 ${cat.productCount} 件商品`, 'warning')
    return
  }
  Swal.fire({
    title: '確定刪除？',
    text: `刪除後將無法恢復「${cat.categoryName}」`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
    confirmButtonText: '刪除',
    cancelButtonText: '取消'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await request.delete(`/api/categories/${cat.categoryId}`)
        Swal.fire('已刪除', '分類已成功移除', 'success')
        fetchCategories()
      } catch (error) {
        Swal.fire('失敗', '刪除失敗', 'error')
      }
    }
  })
}

// 6. 前往商品列表並過濾分類
const viewProducts = (categoryId) => {
  router.push({ path: '/admin/product', query: { categoryId } })
}

// 7. 一鍵輸入專區：🪲爬蟲專區
const autofillReptileZone = () => {
  newCategory.value.categoryName = '🪲爬蟲專區'
  newCategory.value.categoryType = 2
  newCategory.value.parentId = null
}

// 8. 一鍵輸入實體分類：爬寵物品
const autofillReptileItems = () => {
  newCategory.value.categoryName = '爬寵物品'
  newCategory.value.categoryType = 1
  
  // 智慧配對：若列表中已建立「🪲爬蟲專區」，自動將下拉選單選取該專區
  const parent = categories.value.find(c => c.categoryType === 2 && c.categoryName === '🪲爬蟲專區')
  if (parent) {
    newCategory.value.parentId = parent.categoryId
  } else {
    newCategory.value.parentId = null
  }
}

onMounted(fetchCategories)
</script>

<template>
  <div class="category-container">
    
    <!-- 新增區塊 (恢復為原本的區塊，不使用 Modal 避免破圖) -->
    <div v-if="!isEditing" class="add-card shadow-sm mb-4">
      <div class="custom-form-row">
        <div class="form-group">
          <label class="form-label">分類名稱</label>
          <input v-model="newCategory.categoryName" type="text" class="form-control" placeholder="例如：貓砂、熱銷促銷">
        </div>
        <div class="form-group">
          <label class="form-label">分類類型</label>
          <select v-model="newCategory.categoryType" class="form-select">
            <template v-for="(info, type) in typeMap" :key="type">
              <option v-if="Number(type) !== 3" :value="Number(type)">
                {{ info.label }}
              </option>
            </template>
          </select>
        </div>
        <div class="form-group" v-if="newCategory.categoryType === 1">
          <label class="form-label">隸屬專區</label>
          <select v-model="newCategory.parentId" class="form-select">
            <option :value="null" disabled>-- 請選擇專區 --</option>
            <option v-for="cat in mainCategories.filter(c => c.depth === 0)" :key="cat.categoryId" :value="cat.categoryId">
              {{ cat.categoryName }}
            </option>
          </select>
        </div>
        <div class="form-group submit-group">
          <button @click="submitAdd" class="btn-primary-custom w-100">+ 新增分類</button>
        </div>
      </div>
      
      <!-- 快捷一鍵新增區 -->
      <div class="quick-actions-row mt-4 pt-3 border-top d-flex gap-3 align-items-center">
        <span class="text-muted fw-bold" style="font-size: 0.85rem;"><i class="fas fa-bolt text-warning me-1"></i> 快捷操作：</span>
        <button @click="autofillReptileZone" class="btn btn-outline-warning rounded-pill px-3 py-1 fw-bold" style="font-size: 0.85rem; border-color: #ffe0b2; color: #e67e22;">
          一鍵輸入 專區 (🪲爬蟲專區)
        </button>
        <button @click="autofillReptileItems" class="btn btn-outline-warning rounded-pill px-3 py-1 fw-bold" style="font-size: 0.85rem; border-color: #ffe0b2; color: #e67e22;">
          一鍵輸入 實體分類 (爬寵物品)
        </button>
      </div>
    </div>

    <!-- 編輯區塊 (保留 Inline Edit 彈出效果) -->
    <div v-if="isEditing" class="edit-card shadow-sm animate__animated animate__fadeIn">
      <h5 class="text-warning mb-4 fw-bold"><i class="fas fa-edit me-2"></i>編輯分類 (ID: {{ editItem.categoryId }})</h5>
      <div class="custom-form-row">
        <div class="form-group">
          <label class="form-label">分類名稱</label>
          <input v-model="editItem.categoryName" type="text" class="form-control">
        </div>
        <div class="form-group">
          <label class="form-label">分類類型</label>
          <select v-model="editItem.categoryType" class="form-select">
            <template v-for="(info, type) in typeMap" :key="type">
              <option v-if="Number(type) !== 3" :value="Number(type)">
                {{ info.label }}
              </option>
            </template>
          </select>
        </div>
        <div class="form-group" v-if="editItem.categoryType === 1">
          <label class="form-label">隸屬專區</label>
          <select v-model="editItem.parentId" class="form-select">
            <option :value="null" disabled>-- 請選擇專區 --</option>
            <option v-for="cat in mainCategories.filter(c => c.depth === 0)" :key="cat.categoryId" :value="cat.categoryId" v-show="cat.categoryId !== editItem.categoryId">
              {{ cat.categoryName }}
            </option>
          </select>
        </div>
        <div class="form-group submit-group d-flex gap-2">
          <button @click="submitUpdate" class="btn btn-warning flex-grow-1 text-white fw-bold border-0" style="border-radius: 12px; padding: 12px;">儲存</button>
          <button @click="isEditing = false" class="btn btn-light" style="border-radius: 12px; padding: 12px; border: 1.5px solid #eee5d8;">取消</button>
        </div>
      </div>
    </div>

    <!-- 區塊一：活動標籤 -->
    <!-- <div class="category-section">
      <h4 class="section-title"><i class="fas fa-tags" style="color: #d81b60;"></i> 活動標籤 【 備註：等昀翔完成後，要再修改這裡 】</h4>
      <ul class="category-list">
        <li v-for="cat in activityCategories" :key="cat.categoryId" class="category-item">
          
          <div class="hierarchy-controls">
            <button class="btn-arrow" @click="moveUp(cat)" :disabled="cat.isFirstSibling"><i class="fas fa-arrow-up"></i></button>
            <button class="btn-arrow" @click="moveDown(cat)" :disabled="cat.isLastSibling"><i class="fas fa-arrow-down"></i></button>
          </div>

          <div class="category-name-area">
            <span class="cat-name fw-bold text-danger">{{ cat.categoryName }}</span>
            <span class="product-count-badge clickable-badge ms-2" v-if="cat.productCount > 0" @click="viewProducts(cat.categoryId)" title="點擊查看此分類商品">
              {{ cat.productCount }} 件商品
            </span>
          </div>

          <div class="action-area">
            <button @click="startEdit(cat)" class="btn btn-action-edit">編輯</button>
            <button @click="confirmDelete(cat)" class="btn btn-action-delete">刪除</button>
          </div>
        </li>
      </ul>
      <div v-if="activityCategories.length === 0" class="text-center text-muted p-4 border rounded mt-3 bg-light">沒有資料</div>
    </div> -->

    <!-- 區塊二：分類結構 -->
    <div class="category-section">
      <h4 class="section-title"><i class="fas fa-sitemap"></i> 分類結構 (專區 / 實體分類)</h4>
      <ul class="category-list">
        <li v-for="cat in mainCategories" :key="cat.categoryId" class="category-item">
          <!-- 左側：縮排控制 -->
          <div v-for="n in cat.depth" :key="n" class="indent-space"></div>
          
          <div class="hierarchy-controls">
            <!-- 上下排序 -->
            <button class="btn-arrow" @click="moveUp(cat)" :disabled="cat.isFirstSibling" title="向上移動">
              <i class="fas fa-arrow-up"></i>
            </button>
            <button class="btn-arrow" @click="moveDown(cat)" :disabled="cat.isLastSibling" title="向下移動">
              <i class="fas fa-arrow-down"></i>
            </button>
          </div>

          <!-- 中間：名稱與資訊 -->
          <div class="category-name-area">
            <span :class="['cat-name', { 'is-main': cat.depth === 0 }]">{{ cat.categoryName }}</span>
            <span class="type-badge" :style="{ backgroundColor: (typeMap[cat.categoryType]?.color || '#999') + '15', color: typeMap[cat.categoryType]?.color || '#999' }">
              <i class="fas fa-tag me-2" style="font-size: 0.65rem;"></i>{{ typeMap[cat.categoryType]?.label || '未知' }}
            </span>
            <span class="product-count-badge clickable-badge ms-2" v-if="cat.productCount > 0" @click="viewProducts(cat.categoryId)" title="點擊查看此分類商品">
              {{ cat.productCount }} 件商品
            </span>
          </div>

          <!-- 右側：操作按鈕 -->
          <div class="action-area">
            <button @click="startEdit(cat)" class="btn btn-action-edit">編輯</button>
            <button @click="confirmDelete(cat)" class="btn btn-action-delete">刪除</button>
          </div>
        </li>
      </ul>
      <div v-if="mainCategories.length === 0" class="text-center text-muted p-4 border rounded mt-3 bg-light">沒有資料</div>
    </div>

  </div>

</template>
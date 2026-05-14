<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request' 
import Swal from 'sweetalert2'

import '@/assets/css/Category.css'

const emit = defineEmits(['success'])

const newCategory = ref({
  categoryName: '',
  categoryType: 3, 
  parentId: null
})

const isAdding = ref(false)
const tagsList = ref([])

const fetchTags = async () => {
  try {
    const res = await request.get('/api/categories')
    // 只保留 categoryType === 3 且過濾掉名為「活動標籤」的虛擬父節點
    tagsList.value = res.data.filter(c => c.categoryType === 3 && c.categoryName !== '活動標籤')
  } catch (error) {
    console.error("無法取得標籤清單:", error)
  }
}

const submitAdd = async () => {
  if (!newCategory.value.categoryName.trim()) {
    Swal.fire('提示', '請填寫標籤名稱', 'warning')
    return
  }
  
  isAdding.value = true
  try {
    await request.post('/api/categories', newCategory.value)
    Swal.fire({ icon: 'success', title: '新增成功', text: `標籤「${newCategory.value.categoryName}」已建立`, timer: 1500, showConfirmButton: false })
    const addedName = newCategory.value.categoryName
    newCategory.value.categoryName = '' 
    await fetchTags()
    emit('success', addedName) 
  } catch (error) {
    console.error("新增標籤失敗:", error)
    Swal.fire('失敗', '新增失敗，請檢查網路或後端狀態', 'error')
  } finally {
    isAdding.value = false
  }
}

const confirmDelete = (tag) => {
  if (tag.productCount > 0) {
    Swal.fire('無法刪除', `此標籤還有 ${tag.productCount} 件商品關聯，請先至商品管理移除關聯！`, 'warning')
    return
  }

  Swal.fire({
    title: '確定刪除？', text: `刪除後將無法恢復標籤「${tag.categoryName}」`, icon: 'warning',
    showCancelButton: true, confirmButtonColor: '#d33', cancelButtonColor: '#3085d6', confirmButtonText: '刪除', cancelButtonText: '取消'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await request.delete(`/api/categories/${tag.categoryId}`)
        Swal.fire('已刪除', '標籤已成功移除', 'success')
        await fetchTags()
        emit('success')
      } catch (error) {
        console.error("刪除標籤失敗:", error)
        Swal.fire('失敗', '刪除失敗，請檢查網路或後端狀態', 'error')
      }
    }
  })
}

onMounted(() => {
  fetchTags()
})
</script>

<template>
  <div class="quick-add-section animate__animated animate__fadeIn">
    <div class="add-card shadow-sm mb-4">
      <div class="custom-form-row align-items-end">
        <div class="form-group flex-grow-1">
          <label class="form-label fw-bold"><i class="fas fa-plus-circle me-1"></i>快速新增活動標籤</label>
          <input v-model="newCategory.categoryName" type="text" class="form-control" placeholder="例如：夏季特賣、雙11優惠" @keyup.enter="submitAdd" :disabled="isAdding">
        </div>
        <div class="form-group submit-group" style="min-width: 160px;">
          <button @click="submitAdd" class="btn-primary-custom w-100" :disabled="isAdding">
            <span v-if="isAdding" class="spinner-border spinner-border-sm me-1"></span>{{ isAdding ? '處理中...' : '+ 新增標籤' }}
          </button>
        </div>
      </div>

      <div class="tags-cloud-container mt-4 pt-3 border-top">
        <label class="form-label fw-bold text-muted mb-3"><i class="fas fa-tags me-1"></i>目前已建立的活動標籤 (點擊 <i class="fas fa-times mx-1"></i> 刪除)</label>
        <div class="d-flex flex-wrap gap-2">
          <div v-if="tagsList.length === 0" class="text-muted small">尚無活動標籤資料</div>
          <span v-for="tag in tagsList" :key="tag.categoryId" class="badge-chip shadow-sm">
            {{ tag.categoryName }}
            <span v-if="tag.productCount > 0" class="tag-count" title="關聯商品數">({{ tag.productCount }})</span>
            
            <i v-if="tag.categoryId !== 3" class="fas fa-times ms-2 delete-icon" @click="confirmDelete(tag)" title="刪除標籤"></i>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.quick-add-section { max-width: 100%; }
.border-top { border-top: 1.5px dashed #eee5d8 !important; }
.badge-chip { display: inline-flex; align-items: center; background-color: rgba(216, 27, 96, 0.08); color: #d81b60; border: 1px solid rgba(216, 27, 96, 0.2); padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; transition: all 0.2s ease; }
.badge-chip:hover { background-color: rgba(216, 27, 96, 0.15); transform: translateY(-1px); }
.tag-count { margin-left: 4px; font-size: 0.75rem; opacity: 0.7; }
.delete-icon { cursor: pointer; opacity: 0.5; transition: all 0.2s; padding: 2px; font-size: 0.9rem; }
.delete-icon:hover { opacity: 1; color: #c2185b; transform: scale(1.2); }
</style>
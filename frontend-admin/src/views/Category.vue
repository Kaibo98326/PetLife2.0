<script setup>
import { ref, onMounted } from 'vue'
// 1. 改引入你自己寫的全域配置
import request from '@/utils/request' 
import Swal from 'sweetalert2'

const categories = ref([])
const newCategoryName = ref('')
const isEditing = ref(false)
const editItem = ref({ categoryId: null, categoryName: '' })

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



// 2. 執行新增
const submitAdd = async () => {
  if (!newCategoryName.value.trim()) return
  try {
    await request.post('/api/categories', { 
      categoryName: newCategoryName.value 
    })
    newCategoryName.value = ''
    Swal.fire('成功', '分類已新增', 'success')
    fetchCategories() 
  } catch (error) {
    Swal.fire('失敗', '新增失敗', 'error')
  }
}

// 3. 準備修改 (打開編輯模式)
const startEdit = (cat) => {
  editItem.value = { ...cat } 
  isEditing.value = true
}

// 4. 執行更新
const submitUpdate = async () => {
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

onMounted(fetchCategories)
</script>

<template>
  <div class="category-wrapper p-4">
    <div class="card shadow-sm p-3 mb-4">
      <div class="d-flex gap-2">
        <input v-model="newCategoryName" type="text" class="form-control" placeholder="輸入新分類名稱">
        <button @click="submitAdd" class="btn btn-primary px-4">執行新增</button>
      </div>
    </div>

    <div v-if="isEditing" class="card shadow-sm p-3 mb-4 bg-light border-warning">
      <h5 class="text-warning mb-3">✏️ 修改分類 (編號: {{ editItem.categoryId }})</h5>
      <div class="d-flex gap-2">
        <input v-model="editItem.categoryName" type="text" class="form-control">
        <button @click="submitUpdate" class="btn btn-warning">儲存</button>
        <button @click="isEditing = false" class="btn btn-secondary">取消</button>
      </div>
    </div>

    <div class="card shadow-sm p-3">
      <table class="table table-hover align-middle">
        <thead class="table-light">
          <tr>
            <th>編號</th>
            <th>分類名稱</th>
            <th>商品數量</th>
            <th>管理操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="categories.length === 0">
            <td colspan="4" class="text-center text-muted py-4">目前沒有分類資料</td>
          </tr>
          <tr v-for="cat in categories" :key="cat.categoryId">
            <td>{{ cat.categoryId }}</td>
            <td class="fw-bold">{{ cat.categoryName }}</td>
            <td><span class="badge bg-info text-dark">{{ cat.productCount || 0 }} 件商品</span></td>
            <td>
              <button @click="startEdit(cat)" class="btn btn-sm btn-outline-primary me-2">修改</button>
              <button @click="confirmDelete(cat)" class="btn btn-sm btn-outline-danger">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.btn-primary {
  background-color: #ff7a00;
  border-color: #ff7a00;
}
.btn-primary:hover {
  background-color: #e66e00;
  border-color: #e66e00;
}
</style>
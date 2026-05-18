<script setup>
// ✨ 修改：引入 computed 以支援標籤狀態分流與單獨提取大標題物件
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request' 
import Swal from 'sweetalert2'

import '@/assets/css/Category.css'

const emit = defineEmits(['success', 'tag-select']) // 加入 tag-select 事件供主頁面接收


const newCategory = ref({
  categoryName: '',
  categoryType: 3, 
  parentId: null
})

const isAdding = ref(false)
const tagsList = ref([])
const activeTagId = ref('all') // 用來追蹤紀錄目前正被高亮選中篩選的標籤 ID

// ✨ 修改：更新分流計算屬性，將大補帖特權標籤 (categoryId === 3) 徹底排除在普通標籤海之外，避免印在下方盒子裡
const activeTags = computed(() => tagsList.value.filter(tag => !tag.isHiddenInFront && tag.categoryId !== 3))
const inactiveTags = computed(() => tagsList.value.filter(tag => tag.isHiddenInFront && tag.categoryId !== 3))

// ✨ 修改：新增單獨提取大標題物件的計算屬性 (categoryId === 3)，專供頂部標題列獨立靜態渲染
const mainHeaderTag = computed(() => tagsList.value.find(tag => tag.categoryId === 3))

// ✨ 新增：點擊整個標籤執行連動過濾邏輯
const handleTagClick = (tag) => {
  if (activeTagId.value === tag.categoryId) {
    activeTagId.value = 'all'; // 再次點擊相同標籤則取消過濾，還原為全部
  } else {
    activeTagId.value = tag.categoryId; // 設為當前過濾條件
  }
  emit('tag-select', activeTagId.value); // 向上傳遞事件
};

// ✨ 新增：清除標籤篩選按鈕觸發函數
const clearTagFilter = () => {
  activeTagId.value = 'all';
  emit('tag-select', 'all');
};

// ✨ 新增：點擊小筆 icon 編輯標籤名稱功能 (串接後端 PUT API)
const handleEditTag = async (tag) => {
  const { value: newName } = await Swal.fire({
    title: '修改活動標籤名稱',
    input: 'text',
    inputValue: tag.categoryName,
    showCancelButton: true,
    confirmButtonText: '儲存修改',
    cancelButtonText: '取消',
    inputValidator: (value) => {
      if (!value || !value.trim()) return '標籤名稱不可為空！';
    }
  });

  if (newName && newName.trim() !== tag.categoryName) {
    try {
      // 封裝符合後端 Category 模型的物件
      const updatedCategory = { categoryName: newName.trim(), categoryType: 3, parentId: tag.parentId };
      // 呼叫 InnerCategoryController.java 內建的更新 API
      await request.put(`/api/categories/${tag.categoryId}`, updatedCategory);
      Swal.fire({ icon: 'success', title: '修改成功', text: '標籤名稱已成功更新', timer: 1500, showConfirmButton: false });
      
      await fetchTags();
      emit('success'); // 觸發成功提醒，保持所有元件下拉資料即時同步
    } catch (error) {
      console.error("修改標籤失敗:", error);
      Swal.fire('失敗', '修改失敗，請檢查網路或後端狀態', 'error');
    }
  }
};

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
    <div class="tag-system-unified-card">
      
      <div class="row align-items-end g-2">
        <div class="col-md-8">
          <label class="form-label fw-bold text-muted small"><i class="fas fa-plus-circle me-1"></i>建立新標籤</label>
          <input type="text" class="form-control border-2" v-model.trim="newCategory.categoryName" placeholder="請輸入活動標籤名稱 (例如: 春季特賣)" @keyup.enter="submitAdd">
        </div>
        <div class="col-md-4">
          <button class="btn btn-warm-solid w-100 fw-bold shadow-sm" @click="submitAdd" :disabled="isAdding">
            <span v-if="isAdding" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="fas fa-plus me-1"></i>
            {{ isAdding ? '建立中...' : '新增標籤' }}
          </button>
        </div>
      </div>

     <div class="tags-cloud-container mt-4 pt-3 border-top">
        <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
          <label class="form-label fw-bold text-muted mb-0"><i class="fas fa-tags me-1"></i>目前已建立的活動標籤 (點擊標籤可過濾活動表格，點擊 <i class="fas fa-times mx-1"></i> 刪除)</label>
          
          <span v-if="mainHeaderTag" class="badge-chip shadow-sm" style="cursor: default; user-select: none;">
            {{ mainHeaderTag.categoryName }}
            <i class="fas fa-edit ms-2 edit-icon" @click.stop="handleEditTag(mainHeaderTag)" title="修改大標題名稱"></i>
          </span>
        </div>
        
        <div v-if="tagsList.length === 0" class="text-muted small">尚無活動標籤資料</div>
        
        <div v-else class="row g-4">
          
          <div class="col-md-6">
            <div class="fw-bold text-dark mb-2 small" style="font-size: 0.9rem;">
              <i class="fas fa-fire text-danger me-1"></i>使用中 / 上架中標籤 ({{ activeTags.length }})
            </div>
            <div class="tag-scroll-box shadow-sm">
              <div class="d-flex flex-wrap gap-2 align-items-center">
                <div v-if="activeTags.length === 0" class="text-muted small">尚無上架中標籤</div>
                <span v-for="tag in activeTags" :key="tag.categoryId" 
                      class="badge-chip shadow-sm" 
                      :class="{'active-tag-chip': activeTagId === tag.categoryId}"
                      @click="handleTagClick(tag)"
                      style="cursor: pointer; user-select: none;">
                  {{ tag.categoryName }}
                  <span v-if="tag.productCount > 0" class="tag-count" title="關聯商品數">({{ tag.productCount }})</span>
                  <i class="fas fa-edit ms-2 edit-icon" @click.stop="handleEditTag(tag)" title="編輯標籤名稱"></i>
                  <i class="fas fa-times ms-2 delete-icon" @click.stop="confirmDelete(tag)" title="刪除標籤"></i>
                </span>
              </div>
            </div>
          </div>

          <div class="col-md-6">
            <div class="fw-bold text-muted mb-2 small" style="font-size: 0.9rem;">
              <i class="fas fa-eye-slash me-1"></i>未上架 / 閒置標籤 ({{ inactiveTags.length }})
            </div>
            <div class="tag-scroll-box shadow-sm bg-light">
              <div class="d-flex flex-wrap gap-2 align-items-center">
                <div v-if="inactiveTags.length === 0" class="text-muted small">尚無閒置標籤</div>
                <span v-for="tag in inactiveTags" :key="tag.categoryId" 
                      class="badge-chip shadow-sm tag-hidden-status" 
                      :class="{'active-tag-chip': activeTagId === tag.categoryId}"
                      @click="handleTagClick(tag)"
                      style="cursor: pointer; user-select: none;"
                      title="因當前無進行中活動或未綁定商品，前台已自動隱藏此標籤">
                  <i class="fas fa-eye-slash me-1 text-muted" title="前台隱藏中"></i>
                  {{ tag.categoryName }}
                  <span v-if="tag.productCount > 0" class="tag-count" title="關聯商品數">({{ tag.productCount }})</span>
                  <i class="fas fa-edit ms-2 edit-icon" @click.stop="handleEditTag(tag)" title="編輯標籤名稱"></i>
                  <i v-if="tag.categoryId !== 3" class="fas fa-times ms-2 delete-icon" @click.stop="confirmDelete(tag)" title="刪除標籤"></i>
                </span>
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>
  </div>
</template>

<style scoped>
.quick-add-section { max-width: 100%; }

/* ✨ 修改：配合暖色調，將虛線淡化，讓它在白底卡片中更自然優雅 */
.border-top { border-top: 1.5px dashed #f4ede4 !important; }

.badge-chip { display: inline-flex; align-items: center; background-color: rgba(216, 27, 96, 0.08); color: #d81b60; border: 1px solid rgba(216, 27, 96, 0.2); padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; transition: all 0.2s ease; }
.badge-chip:hover { background-color: rgba(216, 27, 96, 0.15); transform: translateY(-1px); }
.tag-count { margin-left: 4px; font-size: 0.75rem; opacity: 0.7; }
.delete-icon { cursor: pointer; opacity: 0.5; transition: all 0.2s; padding: 2px; font-size: 0.9rem; }
.delete-icon:hover { opacity: 1; color: #c2185b; transform: scale(1.2); }

/* 選中高亮標籤的專屬高質感樣式 */
.active-tag-chip {
  background-color: #d81b60 !important;
  color: white !important;
  border-color: #d81b60 !important;
}
.active-tag-chip .delete-icon, .active-tag-chip .edit-icon {
  color: white !important;
  opacity: 0.8;
}
/* 小筆編輯圖示的滑鼠懸停動畫樣式 */
.edit-icon { cursor: pointer; opacity: 0.5; transition: all 0.2s; padding: 2px; font-size: 0.85rem; }
.edit-icon:hover { opacity: 1; color: #0d6efd; transform: scale(1.2); }

/* ✨ 新增：溫慢系一體化系統卡片樣式 (完美復刻圖2風格) */
.tag-system-unified-card {
    background-color: #ffffff;
    border-radius: 16px;                     /* 溫潤大圓角 */
    padding: 24px 30px;                      /* 舒適具呼吸感的內襯空間 */
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04); /* 業界標準的極輕量高質感陰影 */
    border-left: 6px solid #ff9f43;          /* 核心特徵：橘黃色加粗裝飾邊框 */
    transition: all 0.3s ease;
    margin-bottom: 20px;
}
/* ✨ 新增：改造 A - 暖橘系實心活力按鈕樣式 (顏色與卡片左側粗邊框完全一致) */
.btn-warm-solid {
    background-color: #ff9f43;                   /* 活力暖橘實色底色 */
    color: #ffffff;                              /* 純白文字欄位 */
    border: 1.5px solid #ff9f43;
    border-radius: 8px;                          /* 保持與左側輸入框一致的精緻圓角 */
    padding: 10px 20px;
    transition: all 0.2s ease-in-out;            /* 讓滑鼠動態回饋更柔和 */
}

/* 懸停時的動態回饋 (稍微加深橘色，並帶有微幅浮起與溫慢系微陰影) */
.btn-warm-solid:hover:not(:disabled) {
    background-color: #ee8a2e;                   /* 優雅加深的暖橘色 */
    border-color: #ee8a2e;
    transform: translateY(-1px);                 /* 微妙的精緻向上浮動 */
    box-shadow: 0 4px 12px rgba(255, 159, 67, 0.2); /* 呼應橘色系的高質感輕陰影 */
}

/* 按鈕正在建立中、被禁用時的優雅防呆樣式 */
.btn-warm-solid:disabled {
    background-color: #f8f9fa;
    color: #6c757d;
    border-color: #dee2e6;
    opacity: 0.6;
    cursor: not-allowed;
    box-shadow: none;
    transform: none;
}

/* ✨ 新增：後台專屬 - 標示前台隱藏狀態的低飽和度膠囊樣式 */
.tag-hidden-status {
    opacity: 0.55 !important;             /* 降低透明度，視覺上呈現隱身感 */
    background-color: #f1f3f5 !important; /* 換成稍微暗淡的灰色基底 */
    border-style: dashed !important;      /* 外框改為虛線，強調其處於非正式外顯狀態 */
    border-color: #ced4da !important;
}

/* 懸停時稍微亮起，保持良好的操作回饋 */
.tag-hidden-status:hover {
    opacity: 0.85 !important;
    cursor: pointer;
}

/* ✨ 修改：防爆版捲動收納盒樣式 */
.tag-scroll-box {
  max-height: 180px;                 /* 限制最大高度，防止標籤海撐爆畫面 */
  overflow-y: auto;                  /* 超過高度自動顯示垂直捲軸 */
  padding: 16px;                     /* 舒適的內部留白 */
  border: 1px solid #e9ecef;         /* 淡雅的收納盒邊框 */
  border-radius: 12px;               /* 圓滑邊角呼應整體 UI */
  background-color: #ffffff;
}

/* 優化捲軸的視覺體驗 (Webkit) */
.tag-scroll-box::-webkit-scrollbar {
  width: 6px;
}
.tag-scroll-box::-webkit-scrollbar-track {
  background: transparent;
}
.tag-scroll-box::-webkit-scrollbar-thumb {
  background-color: #ced4da;
  border-radius: 10px;
}
.tag-scroll-box::-webkit-scrollbar-thumb:hover {
  background-color: #adb5bd;
}
</style>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import '@/assets/css/BeautyAdmin.css'

const items = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const keyword = ref('')
const statusFilter = ref('')

const emptyForm = () => ({
  itemName: '',
  itemDescription: '',
  durationSlots: 1,
  isActive: true,
  prices: [
    { petSize: '小型', itemPrice: 0, isActive: true },
    { petSize: '中型', itemPrice: 0, isActive: true },
    { petSize: '大型', itemPrice: 0, isActive: true },
  ],
})

const form = reactive(emptyForm())

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !keyword.value || item.itemName?.includes(keyword.value)
    const matchStatus =
      statusFilter.value === '' || String(item.isActive) === statusFilter.value
    return matchKeyword && matchStatus
  })
})

const loadItems = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin/beauty/items')
    items.value = res.data || []
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '美容項目載入失敗', 'error')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  editingId.value = null
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = item => {
  resetForm()
  editingId.value = item.beautyId
  form.itemName = item.itemName
  form.itemDescription = item.itemDescription
  form.durationSlots = item.durationSlots
  form.isActive = item.isActive

  const priceMap = new Map((item.prices || []).map(price => [price.petSize, price]))
  form.prices = form.prices.map(price => ({
    petSize: price.petSize,
    itemPrice: priceMap.get(price.petSize)?.itemPrice ?? 0,
    isActive: priceMap.get(price.petSize)?.isActive ?? true,
  }))

  dialogVisible.value = true
}

const saveItem = async () => {
  try {
    if (editingId.value) {
      await request.put(`/api/admin/beauty/items/${editingId.value}`, form)
      Swal.fire('成功', '美容項目已更新', 'success')
    } else {
      await request.post('/api/admin/beauty/items', form)
      Swal.fire('成功', '美容項目已新增', 'success')
    }
    dialogVisible.value = false
    loadItems()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '儲存失敗', 'error')
  }
}

const toggleStatus = async item => {
  const nextStatus = !item.isActive
  try {
    await request.put(`/api/admin/beauty/items/${item.beautyId}/status`, {
      isActive: nextStatus,
    })
    item.isActive = nextStatus
    Swal.fire('成功', '狀態已更新', 'success')
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '狀態切換失敗', 'error')
  }
}

const priceOf = (item, petSize) => {
  const price = item.prices?.find(row => row.petSize === petSize)
  return price ? `$${Number(price.itemPrice).toLocaleString()}` : '-'
}

onMounted(loadItems)
</script>

<template>
  <div class="beauty-admin-page">
    <div class="beauty-card">
      <div class="beauty-toolbar">
        <div>
          <h3 class="beauty-title">美容項目管理</h3>
          <div class="beauty-subtitle">維護服務項目、所需時段與不同體型價格</div>
        </div>
        <div class="beauty-filter">
          <el-input v-model="keyword" clearable placeholder="搜尋項目名稱" style="width: 220px" />
          <el-select v-model="statusFilter" placeholder="狀態" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="啟用" value="true" />
            <el-option label="停用" value="false" />
          </el-select>
          <el-button type="primary" @click="openAddDialog">新增美容項目</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="filteredItems" stripe>
        <el-table-column prop="beautyId" label="ID" width="80" />
        <el-table-column prop="itemName" label="項目名稱" min-width="150" />
        <el-table-column prop="itemDescription" label="說明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="durationSlots" label="時段數" width="90" align="center" />
        <el-table-column label="小型" width="110" align="center">
          <template #default="{ row }">{{ priceOf(row, '小型') }}</template>
        </el-table-column>
        <el-table-column label="中型" width="110" align="center">
          <template #default="{ row }">{{ priceOf(row, '中型') }}</template>
        </el-table-column>
        <el-table-column label="大型" width="110" align="center">
          <template #default="{ row }">{{ priceOf(row, '大型') }}</template>
        </el-table-column>
        <el-table-column label="狀態" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? '啟用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center">
          <template #default="{ row }">
            <div class="beauty-actions">
              <el-button size="small" @click="openEditDialog(row)">修改</el-button>
              <el-button size="small" :type="row.isActive ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.isActive ? '停用' : '啟用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '修改美容項目' : '新增美容項目'" width="720px">
      <el-form label-position="top">
        <div class="beauty-form-grid">
          <el-form-item label="項目名稱">
            <el-input v-model="form.itemName" />
          </el-form-item>
          <el-form-item label="所需 30 分鐘時段數">
            <el-input-number v-model="form.durationSlots" :min="1" style="width: 100%" />
          </el-form-item>
          <el-form-item class="full" label="項目說明">
            <el-input v-model="form.itemDescription" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="項目狀態">
            <el-switch v-model="form.isActive" active-text="啟用" inactive-text="停用" />
          </el-form-item>
          <div class="full price-grid">
            <div v-for="price in form.prices" :key="price.petSize" class="price-box">
              <div class="price-box-title">{{ price.petSize }}價格</div>
              <el-input-number v-model="price.itemPrice" :min="0" style="width: 100%" />
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

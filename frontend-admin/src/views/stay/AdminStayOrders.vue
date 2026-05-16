<script setup>
import request from '@/utils/request.js'
import { ref } from 'vue'

const orders = ref([])
const isLoading = ref(false)

// 搜尋相關
const searchType = ref('name')
const searchKeyword = ref('')

const editingId = ref(null)
const editingStatus = ref('')

const startEdit = (order) => {
  editingId.value = order.stayId
  editingStatus.value = order.stayStatus
}

const cancelEdit = () => {
  editingId.value = null
  editingStatus.value = ''
}

const confirmEdit = async () => {
  await updateStatus(editingId.value, editingStatus.value)
  editingId.value = null
  editingStatus.value = ''
}

// 拿所有訂單
const fetchAllOrders = async () => {
  isLoading.value = true
  try {
    const res = await request.get('/api/stay/all')
    orders.value = res.data
  } catch (e) {
    console.error('訂單載入失敗', e)
  } finally {
    isLoading.value = false
  }
}

// 搜尋
const search = async () => {
  if (!searchKeyword.value.trim()) {
    fetchAllOrders()
    return
  }
  isLoading.value = true
  try {
    let res
    if (searchType.value === 'name') {
      res = await request.get('/api/stay/search/name', {
        params: { name: searchKeyword.value },
      })
    } else if (searchType.value === 'id') {
      res = await request.get('/api/stay/search/id', {
        params: { stayId: searchKeyword.value },
      })
    } else {
      res = await request.get('/api/stay/search/phone', {
        params: { phone: searchKeyword.value },
      })
    }
    orders.value = res.data
  } catch (e) {
    console.error('搜尋失敗', e)
  } finally {
    isLoading.value = false
  }
}

// 修改訂單狀態
const updateStatus = async (stayId, status) => {
  try {
    await request.patch(`/api/stay/${stayId}/status`, null, {
      params: { status },
    })
    await fetchAllOrders()
  } catch (e) {
    alert('修改失敗')
  }
}

// 取消訂單
const cancelOrder = async (stayId) => {
  if (!confirm('確定要取消此訂單嗎？')) return
  await updateStatus(stayId, 'CANCELLED')
}

// 狀態中文對照
const statusLabel = (status) => {
  const map = {
    active: '已成立',
    CONFIRMED: '已確認',
    CHECKED_IN: '已入住',
    CANCELLED: '已取消',
    CHECKED_OUT: '已退房',
  }
  return map[status] ?? status
}

const resetSearch = () => {
  searchKeyword.value = ''
  fetchAllOrders()
}

fetchAllOrders()
</script>

<template>
  <div class="admin-stay-orders">
    <h2 class="page-title">住宿訂單管理</h2>

    <!-- 搜尋列 -->
    <div class="search-bar">
      <select v-model="searchType" class="search-select">
        <option value="name">會員姓名</option>
        <option value="id">訂單編號</option>
        <option value="phone">手機末三碼</option>
      </select>
      <input
        v-model="searchKeyword"
        class="search-input"
        placeholder="輸入搜尋關鍵字..."
        @keyup.enter="search"
      />
      <button class="btn-search" @click="search">搜尋</button>
      <button class="btn-reset" @click="resetSearch">重置</button>
    </div>

    <!-- 訂單數量 -->
    <p class="order-count">共 {{ orders.length }} 筆訂單</p>

    <!-- 載入中 -->
    <div v-if="isLoading" class="loading-tip">載入中...</div>

    <!-- 表格 -->
    <div v-else class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>訂單編號</th>
            <th>會員姓名</th>
            <th>電話</th>
            <th>房型</th>
            <th>房號</th>
            <th>寵物</th>
            <th>入住</th>
            <th>退房</th>
            <th>天數</th>
            <th>總金額</th>
            <th>付款狀態</th>
            <th>訂單狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.stayId">
            <td># {{ order.stayId }}</td>
            <td>{{ order.memberName }}</td>
            <td>{{ order.memberPhone }}</td>
            <td>{{ order.roomTypeName }}</td>
            <td>{{ order.roomNo }}</td>
            <td>{{ order.petName }}</td>
            <td>{{ order.stayStartDate }}</td>
            <td>{{ order.stayEndDate }}</td>
            <td>{{ order.stayDay }} 晚</td>
            <td>NT$ {{ order.sumPrice?.toLocaleString() }}</td>
            <td>
              <span
                :class="[
                  'badge',
                  order.paymentStatus === 'SUCCESS' ? 'badge-success' : 'badge-pending',
                ]"
              >
                {{ order.paymentStatus === 'SUCCESS' ? '已付款' : '未付款' }}
              </span>
            </td>
            <!-- 訂單狀態欄位 -->
            <td>
              <!-- 非編輯模式：顯示文字 -->
              <span v-if="editingId !== order.stayId" class="badge badge-status">
                {{ statusLabel(order.stayStatus) }}
              </span>

              <!-- 編輯模式：顯示 select -->
              <select v-else v-model="editingStatus" class="status-select">
                <option value="active">已成立</option>
                <option value="CONFIRMED">已確認</option>
                <option value="CHECKED_IN">已入住</option>
                <option value="CHECKED_OUT">已退房</option>
                <option value="CANCELLED">已取消</option>
              </select>
            </td>
            <td class="action-cell">
              <template v-if="editingId !== order.stayId">
                <button class="btn-edit" @click="startEdit(order)">編輯</button>
                <button
                  class="btn-cancel"
                  v-if="order.stayStatus !== 'CANCELLED'"
                  @click="cancelOrder(order.stayId)"
                >
                  取消
                </button>
              </template>

              <template v-else>
                <button class="btn-confirm-edit" @click="confirmEdit">確認</button>
                <button class="btn-cancel-edit" @click="cancelEdit">取消編輯</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  
</template>

<style scoped>
.admin-stay-orders {
  padding: 24px;
}
.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
}
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.search-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}
.search-input {
  flex: 1;
  max-width: 300px;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}
.btn-search {
  padding: 8px 20px;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}
.btn-reset {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #555;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}
.order-count {
  font-size: 0.9rem;
  color: #888;
  margin-bottom: 12px;
}
.loading-tip {
  text-align: center;
  padding: 60px;
  color: #999;
}
.table-wrap {
  overflow-x: auto;
}
.order-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}
.order-table th {
  background: #f9fafb;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 2px solid #e5e7eb;
  font-weight: 600;
  color: #555;
  white-space: nowrap;
}
.order-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}
.order-table tr:hover td {
  background: #fafafa;
}
.badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 600;
}
.badge-success {
  background: #dcfce7;
  color: #166534;
}
.badge-pending {
  background: #fef9c3;
  color: #854d0e;
}
.badge-status {
  background: #e0e7ff;
  color: #3730a3;
}
.action-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}
.status-select {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-cancel {
  padding: 4px 12px;
  background: none;
  border: 1px solid #fca5a5;
  color: #ef4444;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-cancel:hover {
  background: #fef2f2;
}

.btn-edit {
  padding: 4px 12px;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-confirm-edit {
  padding: 4px 12px;
  background: #16a34a;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-cancel-edit {
  padding: 4px 12px;
  background: #f3f4f6;
  color: #555;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}
</style>

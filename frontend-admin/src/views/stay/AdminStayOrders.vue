<script setup>
import request from '@/utils/request.js'
import { ref, computed } from 'vue'

const isTodayActive = ref(false)
const orders = ref([])
const isLoading = ref(false)
const paymentFilter = ref('')

const selectedOrderPets = ref(null)

// 搜尋相關
const searchType = ref('name')
const searchKeyword = ref('')

const editingId = ref(null)
const editingStatus = ref('')

// 在原本的 ref 區塊加入：
const refundConfirmId = ref(null) // 待退款的 stayId
const refundResult = ref(null) // { success: boolean, message: string }

// 替換原本的 refundOrder
const refundOrder = (stayId) => {
  refundConfirmId.value = stayId // 開啟確認Modal
}

const confirmRefund = async () => {
  const stayId = refundConfirmId.value
  refundConfirmId.value = null // 關閉確認Modal

  try {
    await request.post(`/api/stay/${stayId}/refund`)
    await fetchAllOrders()
    refundResult.value = { success: true, message: '退款已成功處理！' }
  } catch (e) {
    refundResult.value = {
      success: false,
      message: '退款失敗：' + (e.response?.data?.message || '請稍後重試'),
    }
  }
}

// 套用付款狀態篩選
const filteredOrders = computed(() => {
  if (!paymentFilter.value) return orders.value
  return orders.value.filter((o) => o.paymentStatus === paymentFilter.value)
})
// 抓寵物Modal
const hasPets = (order) => {
  if (!order.stayRemark) return false
  try {
    const remark = JSON.parse(order.stayRemark)
    return remark.pets && remark.pets.length > 1
  } catch {
    return false
  }
}

const statusBadgeClass = (status) => {
  const map = {
    active: 'badge-active',
    CONFIRMED: 'badge-confirmed',
    CHECKED_IN: 'badge-checkin',
    CHECKED_OUT: 'badge-checkout',
    CANCELLED: 'badge-cancelled',
    REFUNDED: 'badge-refunded',
  }
  return map[status] ?? 'badge-active'
}

const openPetDetail = (order) => {
  if (!order.stayRemark) return
  try {
    const remark = JSON.parse(order.stayRemark)
    selectedOrderPets.value = {
      stayId: order.stayId,
      pets: remark.pets || [],
      customerNote: remark.customerNote || '',
    }
  } catch {
    return
  }
}

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
    orders.value = res.data.sort((a, b) => b.stayId - a.stayId)
  } catch (e) {
    console.error('訂單載入失敗', e)
  } finally {
    isLoading.value = false
  }
}

// 搜尋
const search = async () => {
  // today 不需要關鍵字，單獨處理
  if (searchType.value === 'today') {
    isLoading.value = true
    try {
      const today = new Date().toISOString().split('T')[0]
      const res = await request.get('/api/stay/search/checkin', {
        params: { date: today },
      })
      orders.value = res.data.sort((a, b) => b.stayId - a.stayId)
    } catch (e) {
      console.error('搜尋失敗', e)
    } finally {
      isLoading.value = false
    }
    return
  }

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
    orders.value = res.data.sort((a, b) => b.stayId - a.stayId)
  } catch (e) {
    console.error('搜尋失敗', e)
  } finally {
    isLoading.value = false
  }
}
// 今日搜尋
const searchToday = async () => {
  isTodayActive.value = true
  try {
    const today = new Date().toISOString().split('T')[0]
    const res = await request.get('/api/stay/search/checkin', {
      params: { date: today },
    })
    orders.value = res.data.sort((a, b) => b.stayId - a.stayId)
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
    REFUNDED: '已退款',
  }
  return map[status] ?? status
}

const resetSearch = () => {
  isTodayActive.value = false
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

      <div style="flex: 1"></div>

      <button class="btn-reset" @click="resetSearch">重置</button>
      <button :class="['btn-today', { 'btn-today-active': isTodayActive }]" @click="searchToday">
        今日入住
      </button>
      <select v-model="paymentFilter" class="search-select">
        <option value="">全部</option>
        <option value="SUCCESS">已付款</option>
        <option value="PENDING">未付款</option>
        <option value="REFUNDED">已退款</option>
      </select>
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
          <tr v-for="order in filteredOrders" :key="order.stayId">
            <td># {{ order.stayId }}</td>
            <td>{{ order.memberName }}</td>
            <td>{{ order.memberPhone }}</td>
            <td>{{ order.roomTypeName }}</td>
            <td>{{ order.roomNo }}</td>
            <td>
              <span
                class="pet-link"
                data-bs-toggle="modal"
                data-bs-target="#petDetailModal"
                @click="openPetDetail(order)"
              >
                {{ order.petName }}
                <span v-if="hasPets(order)" class="pet-more">+同行</span>
              </span>
            </td>
            <td>{{ order.stayStartDate }}</td>
            <td>{{ order.stayEndDate }}</td>
            <td>{{ order.stayDay }} 晚</td>
            <td>NT$ {{ order.sumPrice?.toLocaleString() }}</td>
            <td>
              <span
                :class="[
                  'badge',
                  order.paymentStatus === 'REFUNDED'
                    ? 'badge-refunded'
                    : order.paymentStatus === 'SUCCESS'
                      ? 'badge-success'
                      : 'badge-pending',
                ]"
              >
                {{
                  order.paymentStatus === 'REFUNDED'
                    ? '已退款'
                    : order.paymentStatus === 'SUCCESS'
                      ? '已付款'
                      : '未付款'
                }}
              </span>
            </td>
            <!-- 訂單狀態欄位 -->
            <td>
              <!-- 非編輯模式：顯示文字 -->
              <span
                v-if="editingId !== order.stayId"
                :class="['badge', statusBadgeClass(order.stayStatus)]"
              >
                {{ statusLabel(order.stayStatus) }}
              </span>

              <!-- 編輯模式：顯示 select -->
              <select v-else v-model="editingStatus" class="status-select">
                <option value="CONFIRMED">已確認</option>
                <option value="CHECKED_IN">已入住</option>
                <option value="CHECKED_OUT">已退房</option>
              </select>
            </td>
            <td class="action-cell">
              <template v-if="editingId !== order.stayId">
                <!-- 只有已付款才顯示編輯 -->
                <button
                  class="btn-edit"
                  v-if="order.paymentStatus === 'SUCCESS'"
                  @click="startEdit(order)"
                >
                  編輯
                </button>

                <button
                  class="btn-refund"
                  v-if="order.paymentStatus === 'SUCCESS' && order.stayStatus !== 'CANCELLED'"
                  @click="refundOrder(order.stayId)"
                >
                  退款
                </button>

                <button
                  class="btn-cancel"
                  v-if="order.stayStatus !== 'CANCELLED' && order.paymentStatus !== 'PENDING'"
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
  <!-- 寵物Modal -->
  <div class="modal fade" id="petDetailModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content" style="border-radius: 12px; padding: 24px">
        <div
          style="
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
          "
        >
          <h5 style="margin: 0; font-weight: 700">
            訂單 #{{ selectedOrderPets?.stayId }} 入住寵物
          </h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>

        <div v-if="selectedOrderPets">
          <table class="order-table">
            <thead>
              <tr>
                <th>角色</th>
                <th>寵物名稱</th>
                <th>種類</th>
                <th>品種</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(pet, index) in selectedOrderPets.pets" :key="pet.petId">
                <td>
                  <span class="badge" :class="index === 0 ? 'badge-success' : 'badge-pending'">
                    {{ index === 0 ? '主要' : '同行' }}
                  </span>
                </td>
                <td>{{ pet.petName }}</td>
                <td>{{ pet.species }}</td>
                <td>{{ pet.breed }}</td>
              </tr>
            </tbody>
          </table>

          <div
            v-if="selectedOrderPets.customerNote"
            style="
              margin-top: 16px;
              padding: 12px;
              background: #f9fafb;
              border-radius: 8px;
              font-size: 0.9rem;
            "
          >
            <strong>備註：</strong>{{ selectedOrderPets.customerNote }}
          </div>
        </div>
      </div>
    </div>
  </div>
  <!-- 退款確認 Modal -->
  <div
    class="modal fade"
    id="refundConfirmModal"
    tabindex="-1"
    :class="{ show: refundConfirmId !== null }"
    :style="{
      display: refundConfirmId !== null ? 'flex' : 'none',
      alignItems: 'center',
      backgroundColor: 'rgba(0,0,0,0.5)',
    }"
  >
    <div class="modal-dialog modal-dialog-centered" style="margin: auto">
      <div class="modal-content text-center" style="border-radius: 16px; padding: 36px 28px">
        <div style="margin-bottom: 20px">
          <div class="modal-icon modal-icon-warning">!</div>
        </div>
        <h5 style="font-weight: 700; font-size: 1.3rem; margin-bottom: 10px">確定要退款嗎？</h5>
        <p style="color: #666; font-size: 0.95rem; margin-bottom: 28px">
          退款後無法復原，訂單將同步取消
        </p>
        <div style="display: flex; gap: 12px; justify-content: center">
          <button class="btn-modal-confirm" @click="confirmRefund">是的，退款</button>
          <button class="btn-modal-cancel" @click="refundConfirmId = null">取消</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 退款結果 Modal -->
  <div
    class="modal fade"
    id="refundResultModal"
    tabindex="-1"
    :class="{ show: refundResult !== null }"
    :style="{
      display: refundResult !== null ? 'flex' : 'none',
      alignItems: 'center',
      backgroundColor: 'rgba(0,0,0,0.5)',
    }"
  >
    <div class="modal-dialog modal-dialog-centered" style="margin: auto">
      <div class="modal-content text-center" style="border-radius: 16px; padding: 36px 28px">
        <div style="margin-bottom: 20px">
          <div
            :class="[
              'modal-icon',
              refundResult?.success ? 'modal-icon-success' : 'modal-icon-danger',
            ]"
          >
            {{ refundResult?.success ? '✓' : '✕' }}
          </div>
        </div>
        <h5 style="font-weight: 700; font-size: 1.3rem; margin-bottom: 10px">
          {{ refundResult?.success ? '退款成功' : '退款失敗' }}
        </h5>
        <p style="color: #666; font-size: 0.95rem; margin-bottom: 28px">
          {{ refundResult?.message }}
        </p>
        <div style="display: flex; justify-content: center">
          <button class="btn-modal-confirm" @click="refundResult = null">確認</button>
        </div>
      </div>
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
.badge-refunded {
  background: #f3e8ff;
  color: #6b21a8;
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
.btn-today {
  padding: 8px 16px;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  white-space: nowrap;
}

.btn-today:hover {
  background: #d97706;
}
.pet-link {
  cursor: pointer;
  color: #16a34a;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.pet-link:hover {
  color: #15803d;
}

.pet-more {
  font-size: 0.72rem;
  background: #dcfce7;
  color: #166534;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 600;
}
.btn-refund {
  padding: 4px 12px;
  background: none;
  border: 1px solid #f97316;
  color: #f97316;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-refund:hover {
  background: #fff7ed;
}

.badge-active {
  background: #fef9c3;
  color: #854d0e;
}
.badge-confirmed {
  background: #dcfce7;
  color: #166534;
}
.badge-checkin {
  background: #dbeafe;
  color: #1e40af;
}
.badge-checkout {
  background: #f3e8ff;
  color: #6b21a8;
}
.badge-cancelled {
  background: #fee2e2;
  color: #991b1b;
}
.btn-today-active {
  background: #92400e;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
}
/* Modal 圖示 */
.modal-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  font-size: 2rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}
.modal-icon-warning {
  border: 3px solid #f59e0b;
  color: #f59e0b;
}
.modal-icon-success {
  border: 3px solid #16a34a;
  color: #16a34a;
}
.modal-icon-danger {
  border: 3px solid #ef4444;
  color: #ef4444;
}

/* Modal 按鈕 */
.btn-modal-confirm {
  padding: 10px 28px;
  background: #6d6aff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-modal-confirm:hover {
  background: #4f46e5;
}
.btn-modal-cancel {
  padding: 10px 28px;
  background: #9ca3af;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-modal-cancel:hover {
  background: #6b7280;
}
.order-table tr:hover td .pet-link {
  color: #16a34a;
}

.order-table tr:hover td .pet-link:hover {
  color: #15803d;
}
</style>

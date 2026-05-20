<template>
  <div class="admin-container">
    <header class="dashboard-header">
      <div class="header-content">
        <h2 class="title">
          <i class="bi bi-bag-check-fill"></i>
          <span>訂單管理系統</span>
        </h2>
        <div class="search-box">
          <i class="bi bi-search"></i>
          <input
            v-model="searchQuery"
            @keyup.enter="fetchOrders"
            type="text"
            placeholder="搜尋收件人姓名..."
          />
          <button @click="fetchOrders" class="search-btn">檢索</button>
        </div>
      </div>
    </header>

    <Transition name="fade">
      <div v-if="errorMessage" class="error-banner">
        <i class="bi bi-exclamation-triangle-fill"></i>
        <span>{{ errorMessage }}</span>
        <button class="close-btn" @click="errorMessage = ''">✕</button>
      </div>
    </Transition>

    <div class="stats-row">
      <div class="stat-mini orange-solid">
        <div class="mini-content">
          <span class="mini-label">訂單總量</span>
          <span class="mini-value">{{ orders?.length || 0 }} 筆</span>
        </div>
        <i class="bi bi-collection-fill"></i>
      </div>
      <div class="stat-mini status-working">
        <div class="mini-content">
          <span class="mini-label">處理中</span>
          <span class="mini-value"
            >{{ (orders || []).filter((o) => o.orderStatus === '處理中').length }} 筆</span
          >
        </div>
        <i class="bi bi-clock-history"></i>
      </div>
      <div class="stat-mini status-done">
        <div class="mini-content">
          <span class="mini-label">已完成</span>
          <span class="mini-value"
            >{{ (orders || []).filter((o) => o.orderStatus === '已完成').length }} 筆</span
          >
        </div>
        <i class="bi bi-check2-circle"></i>
      </div>
    </div>

    <div class="main-card shadow-lg">
      <div class="table-responsive">
        <table class="custom-table">
          <thead>
            <tr>
              <th>訂單編號</th>
              <th>姓名</th>
              <th>聯絡電話</th>
              <th>訂單成立時間</th>
              <th>使用點數</th>
              <th>剩餘點數</th>
              <th>實付金額</th>
              <th>訂單狀態</th>
              <th>付款方式</th>
              <th>編輯操作</th>
            </tr>
          </thead>
          <tbody>
            <TransitionGroup name="list">
              <tr
                v-for="order in paginatedOrders"
                :key="order.orderId"
                :class="{ 'is-editing': order.isEditing }"
              >
                <td>
                  <span class="order-id" @click="openDetail(order.orderId)">
                    #{{ order.orderId }}
                  </span>
                </td>
                <td>
                  <div class="client-info">
                    <span class="name">{{ order.orderName }}</span>
                    <span class="member-id">ID: {{ order.memberId }}</span>
                  </div>
                </td>
                <td>{{ order.orderPhone }}</td>
                <td>{{ formatDate(order.orderDate) }}</td>
                <td>
                  <span :class="['point-use', order.usedPoint > 0 ? 'has-used' : '']">
                    {{ order.usedPoint > 0 ? '-' + order.usedPoint : 0 }} P
                  </span>
                </td>
                <td>
                  <span class="point-remaining">{{ order.remainingPoint }} P</span>
                </td>
                <td>
                  <span class="price-tag">${{ order.orderTotal }}</span>
                </td>

                <td>
                  <div v-if="order.isEditing" class="edit-select">
                    <select v-model="order.orderStatus">
                      <option value="處理中">處理中</option>
                      <option value="已取消">已取消</option>
                      <option value="已完成">已完成</option>
                    </select>
                  </div>
                  <span v-else :class="['status-pill', getStatusType(order.orderStatus)]">
                    {{ order.orderStatus }}
                  </span>
                </td>
                <td>
                  <div v-if="order.isEditing" class="edit-select">
                    <select v-model="order.orderPayment">
                      <option value="金融卡">金融卡</option>
                      <option value="信用卡">信用卡</option>
                      <option value="LinePay">LinePay</option>
                    </select>
                  </div>
                  <span v-else class="payment-text">{{ order.orderPayment }}</span>
                </td>
                <td>
                  <div class="action-btns">
                    <template v-if="!order.isEditing">
                      <button class="btn-icon edit" @click="order.isEditing = true">
                        修改 <i class="bi bi-pencil-square"></i>
                      </button>
                      <button class="btn-icon delete" @click="deleteOrder(order.orderId)">
                        刪除 <i class="bi bi-trash3"></i>
                      </button>
                    </template>
                    <template v-else>
                      <button class="btn-icon save" @click="saveOrder(order)">
                        儲存 <i class="bi bi-check-lg"></i>
                      </button>
                      <button
                        class="btn-icon cancel"
                        @click="order.isEditing = false"
                        style="background: #eee; color: #666"
                      >
                        取消 <i class="bi bi-x-lg"></i>
                      </button>
                    </template>
                  </div>
                </td>
              </tr>
            </TransitionGroup>
          </tbody>
        </table>
        <div v-if="!orders.length && !errorMessage" class="empty-state">目前沒有符合條件的訂單</div>
      </div>

      <div class="pagination-bar" v-if="orders.length > 0">
        <div class="page-info">
          共 {{ orders.length }} 筆訂單，第 {{ currentPage }} / {{ totalPages }} 頁
        </div>
        <div class="page-controls">
          <button class="p-btn" :disabled="currentPage === 1" @click="currentPage = 1">«</button>
          <button class="p-btn" :disabled="currentPage === 1" @click="currentPage--">上一頁</button>

          <div class="page-numbers">
            <button
              v-for="p in totalPages"
              :key="p"
              class="num-btn"
              :class="{ active: currentPage === p }"
              @click="currentPage = p"
              v-show="Math.abs(p - currentPage) < 3"
            >
              {{ p }}
            </button>
          </div>

          <button class="p-btn" :disabled="currentPage === totalPages" @click="currentPage++">
            下一頁
          </button>
          <button
            class="p-btn"
            :disabled="currentPage === totalPages"
            @click="currentPage = totalPages"
          >
            »
          </button>
        </div>
      </div>
    </div>

    <Transition name="zoom">
      <div v-if="selectedOrderId" class="glass-overlay" @click.self="selectedOrderId = null">
        <div class="glass-modal large">
          <div class="modal-header">
            <h3><i class="bi bi-file-earmark-text me-2"></i>訂單詳細明細</h3>
            <button class="close-circle" @click="selectedOrderId = null">✕</button>
          </div>
          <div class="modal-body px-0">
            <OrderDetailAdminView :orderId="selectedOrderId" />
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import OrderDetailAdminView from './OrderDetailAdminView.vue'
import { useEmployeeStore } from '@/stores/employee'

// 建立的明細彈窗組件              --------------------->活動需要
// ✨ 修改：已將獨立的明細組件與功能停用，為遵守不破壞原則，將其註解保留
// import OrderDiscountModal from '@/components/OrderDiscountModal.vue'

const employeeStore = useEmployeeStore()

const orders = ref([])
const searchQuery = ref('')
const errorMessage = ref('')
const selectedOrderId = ref(null)

//  建立 ref 與開啟彈窗的方法       --------------------->活動需要
// ✨ 修改：清理不再使用的明細點擊事件邏輯，為遵守不破壞原則，將其註解保留
// const discountModalRef = ref(null)
// const viewOrderDiscountDetails = (orderId) => {
//     discountModalRef.value.openModal(orderId)
// }

// --- 分頁邏輯變數 ---
const currentPage = ref(1)
const itemsPerPage = 10

// 計算總頁數
const totalPages = computed(() => {
  return Math.ceil(orders.value.length / itemsPerPage) || 1
})

// 計算當前頁面要顯示的數據
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return orders.value.slice(start, end)
})

// 當搜尋時，自動回到第一頁
watch(searchQuery, () => {
  currentPage.value = 1
})

// 取得所有訂單
const fetchOrders = async () => {
  errorMessage.value = ''
  try {
    const url = searchQuery.value
      ? `/api/order/all?search=${encodeURIComponent(searchQuery.value)}`
      : `/api/order/all`

    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${employeeStore.token}` },
    })

    if (!res.ok) throw new Error(`伺服器連線失敗 (${res.status})`)

    const data = await res.json()
    orders.value = Array.isArray(data) ? data.map((o) => ({ ...o, isEditing: false })) : []
    currentPage.value = 1 // 重新讀取數據後回到第一頁
  } catch (err) {
    errorMessage.value = err.message
  }
}

const openDetail = (id) => {
  selectedOrderId.value = id
}

// 儲存修改
const saveOrder = async (order) => {
  if (!confirm(`確定要儲存訂單 #${order.orderId} 的修改嗎？`)) return

  try {
    const response = await fetch(`/api/order/update/${order.orderId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${employeeStore.token}`,
      },
      body: JSON.stringify({
        orderStatus: order.orderStatus,
        orderPayment: order.orderPayment,
      }),
    })

    if (!response.ok) throw new Error('儲存失敗，請稍後再試')

    order.isEditing = false
    alert('訂單更新成功！')
    fetchOrders()
  } catch (err) {
    errorMessage.value = err.message
    alert('錯誤: ' + err.message)
  }
}

// 刪除訂單
const deleteOrder = async (id) => {
  if (!confirm(`確定要永久刪除訂單 #${id} 嗎？此操作將無法從清單中撤銷。`)) return

  try {
    const response = await fetch(`/api/order/delete/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${employeeStore.token}`,
      },
    })

    if (!response.ok) throw new Error('伺服器處理失敗')

    orders.value = orders.value.filter((o) => o.orderId !== id)
    alert('訂單已刪除成功。')
  } catch (err) {
    console.error(err)
    alert('刪除失敗：' + err.message)
  }
}

const formatDate = (s) => (s ? new Date(s).toLocaleString('zh-TW', { hour12: false }) : '-')

const getStatusType = (status) => {
  const map = { 已完成: 'success', 已取消: 'danger', 處理中: 'warning' }
  return map[status] || 'warning'
}

onMounted(fetchOrders)
</script>

<style scoped src="@/assets/css/OrderAdmin.css"></style>

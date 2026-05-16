<template>
  <div class="product-order-container">
    <!-- 載入狀態 -->
    <div v-if="isLoading" class="text-center py-5">
      <div class="spinner-border text-warning" role="status"></div>
      <p class="mt-2 text-muted">訂單載入中...</p>
    </div>

    <!-- 空狀態 -->
    <div v-else-if="orders.length === 0" class="empty-state text-center py-5">
      <div class="mb-3">
        <i class="fas fa-box-open fa-4x text-light-orange"></i>
      </div>
      <p class="text-muted fw-bold">目前沒有購買紀錄</p>
      <router-link to="/" class="btn btn-warning rounded-pill px-4 shadow-sm">
        前往商城逛逛
      </router-link>
    </div>

    <!-- 訂單表格 -->
    <div v-else class="table-responsive">
      <table class="table align-middle custom-table">
        <thead class="table-light">
          <tr>
            <th style="width: 14%" class="ps-4">訂單編號</th>
            <th style="width: 14%" class="text-center">成立時間</th>
            <th style="width: 14%">總額</th>
            <th style="width: 14%" class="text-center">狀態</th>
            <th style="width: 14%">付款方式</th>
            <th style="width: 14%" class="text-center">訂單操作</th>
            <th style="width: 14%" class="text-center">訂單明細</th>
          </tr>
        </thead>
        <tbody>
          <!-- 改成使用 paginatedOrders -->
          <tr v-for="order in paginatedOrders" :key="order.orderId">
            <td class="ps-4">
              <strong class="text-dark">#{{ order.orderId }}</strong>
            </td>
            <td class="text-center text-secondary">{{ formatDateTime(order.orderDate) }}</td>
            <td>
              <span class="price-text">${{ formatPrice(order.orderTotal) }}</span>
            </td>
            <td class="text-center">
              <span :class="['badge', getStatusLabel(order.orderStatus).class]">
                {{ getStatusLabel(order.orderStatus).text }}
              </span>
            </td>
            <td>
              <span class="badge bg-light text-dark border fw-normal">
                {{ order.orderPayment }}
              </span>
            </td>

            <!-- 取消訂單邏輯 -->
            <td class="text-center">
              <div class="d-flex flex-column gap-2 align-items-center">
                <button
                  v-if="order.orderStatus === '處理中' || order.orderStatus === '1'"
                  @click="handleComplete(order.orderId)"
                  class="btn btn-sm btn-success rounded-pill px-3"
                >
                  確認收貨
                </button>

                <button
                  v-if="order.orderStatus !== '已取消' && order.orderStatus !== '已完成'"
                  @click="handleCancel(order.orderId)"
                  :disabled="!isWithinThreeDays(order.orderDate)"
                  :class="[
                    'btn btn-sm rounded-pill px-3',
                    isWithinThreeDays(order.orderDate) ? 'btn-outline-danger' : 'btn-readonly',
                  ]"
                >
                  {{ isWithinThreeDays(order.orderDate) ? '取消訂單' : '不可取消' }}
                </button>
                <span v-else-if="order.orderStatus === '已取消'" class="text-muted small">已取消</span>
                <span v-else-if="order.orderStatus === '已完成'" class="text-success small fw-bold">訂單已結案</span>
              </div>
            </td>

            <td class="text-center">
              <button @click="openModal(order.orderId)" class="btn btn-detail-action btn-sm px-4">
                查看內容
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分頁控制 -->
      <div class="pagination-controls text-center mt-3">
        <button
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage === 1"
          class="btn btn-sm btn-outline-secondary mx-1"
        >
          上一頁
        </button>
        <span class="mx-2">第 {{ currentPage }} / {{ totalPages }} 頁</span>
        <button
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage === totalPages"
          class="btn btn-sm btn-outline-secondary mx-1"
        >
          下一頁
        </button>
      </div>
    </div>

    <!-- 訂單明細 Modal -->
    <div class="modal fade" id="orderDetailModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content shadow-lg border-0" v-if="selectedOrderDetail">
          <!-- Header -->
          <div class="modal-header custom-header-orange text-white py-3 px-4">
            <h5 class="modal-title fw-bold">
              <i class="fas fa-file-invoice me-2"></i>訂單詳情 #{{ selectedOrderDetail.orderId }}
            </h5>
            <button
              type="button"
              class="btn-close btn-close-white"
              data-bs-dismiss="modal"
              aria-label="Close"
            ></button>
          </div>

          <div class="modal-body p-4">
            <!-- 收件資訊 -->
            <div class="info-card rounded-3 p-3 mb-4 bg-light border-start border-warning border-4">
              <div class="row">
                <div class="col-md-6 mb-2 mb-md-0">
                  <small class="text-muted d-block">收件人</small>
                  <span class="fw-bold text-dark">{{ selectedOrderDetail.orderName }}</span>
                </div>
                <div class="col-md-6">
                  <small class="text-muted d-block">配送地址</small>
                  <span class="fw-bold text-dark">{{ selectedOrderDetail.orderAddress }}</span>
                </div>
              </div>
            </div>

            <!-- 商品清單 -->
            <div class="table-responsive">
              <table class="table table-borderless item-table align-middle">
                <thead class="bg-faint-gray">
                  <tr>
                    <th class="py-3">商品</th>
                    <th class="py-3 text-center">單價</th>
                    <th class="py-3 text-center">數量</th>
                    <th class="py-3 text-center">折扣</th>
                    <th class="py-3 text-end">小計</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="item in selectedOrderDetail.items"
                    :key="item.productName"
                    class="border-bottom"
                  >
                    <td class="py-3 fw-bold text-dark">{{ item.productName }}</td>
                    <td class="py-3 text-center text-secondary">
                      ${{ formatPrice(item.productPrice) }}
                    </td>
                    <td class="py-3 text-center">
                      <span
                        class="badge rounded-pill bg-secondary bg-opacity-10 text-secondary px-3"
                      >
                        {{ item.quantity }}
                      </span>
                    </td>
                    <td class="py-3 text-center text-success">- ${{ item.discount }}</td>
                    <td class="py-3 text-end fw-bold text-dark">
                      ${{ formatPrice(item.subtotal) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- 結算 -->
            <div class="d-flex justify-content-end mt-4">
              <div class="total-box p-3 rounded-3 text-end">
                <span class="text-muted me-3">實付總金額</span>
                <h3 class="text-danger fw-bold d-inline-block mb-0">
                  ${{ formatPrice(selectedOrderDetail.orderTotal) }}
                </h3>
              </div>
            </div>
          </div>

          <div class="modal-footer border-0 pb-4">
            <button
              type="button"
              class="btn btn-outline-secondary rounded-pill px-4"
              data-bs-dismiss="modal"
            >
              關閉
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/axios'
import { Modal } from 'bootstrap'

// ✨ 新增/修改：引入 Pinia Store 供後續更新會員紅利點數
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()

// 狀態宣告
const orders = ref([])
const selectedOrderDetail = ref(null)
const isLoading = ref(true)
const route = useRoute()
let detailModal = null
const currentPage = ref(1)
const pageSize = 10




//換頁效果
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return orders.value.slice(start, end)
})

const totalPages = computed(() => Math.ceil(orders.value.length / pageSize))

function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// 將 checkAndOpenOrder 獨立成一個方法
const checkAndOpenOrder = async () => {
  const openOrderId = route.query.openOrderId;
  if (openOrderId && orders.value.length > 0) {
    const targetId = parseInt(openOrderId);
    // 1. 找出該筆訂單在陣列中的 Index
    const index = orders.value.findIndex(o => o.orderId === targetId);
    if (index !== -1) {
      // 2. 精準計算這筆訂單在哪一頁 (無條件捨去 + 1)
      const targetPage = Math.floor(index / pageSize) + 1;
      
      // 3. 把背景切換到正確的頁碼
      goToPage(targetPage);
      
      // 4. 等待畫面渲染後，彈出明細視窗
      await nextTick();
      await openModal(targetId);
      
      // 5. 將網址上的參數抹除，避免使用者按 F5 重新整理時又一直打開
      router.replace({ query: {} });
    }
  }
}



// 核心功能：抓取歷史訂單  移除路徑 includes 判斷，改由 watch 觸發點來控制，確保穩定


const fetchOrders = async () => {
  console.log('開始抓取訂單，目前路徑:', route.path)
  isLoading.value = true

  try {
    
    const response = await axios.get('/orders/historyorders')
    if (response.data) {
      orders.value = response.data
      console.log('資料抓取成功:', response.data.length, '筆紀錄')
    }
  } catch (err) {
    console.error('抓取歷史訂單失敗:', err)
  } finally {
    
    setTimeout(async () => {
      isLoading.value = false
      // ✨ 新增/修改：當資料載入完畢後，執行檢查是否需要自動跳轉與開窗
      await checkAndOpenOrder()
    }, 150)
  }
}

//監聽路由變化 當使用者在標籤間切換時，這個 watch 會確保 fetchOrders 被執行
watch(
  () => route.path,
  (newPath) => {
    // 只有當進入此組件的路徑時才觸發 (例如 /orderhistory 或 /orderhistory/products)
    // 這裡我們簡化判斷，只要路徑包含 orderhistory 就抓取
    if (newPath.includes('orderhistory')) {
      fetchOrders()
    }
  },
  { immediate: true }, // immediate: true 確保 F5 重新整理時也會跑
)

//生命週期掛載
onMounted(async () => {
  // 初始化 Bootstrap Modal
  const modalElement = document.getElementById('orderDetailModal')
  if (modalElement) {
    detailModal = new Modal(modalElement, { focus: false })
  }

  // watch因為某些原因沒跑，這裡補跑一次
  if (orders.value.length === 0 && isLoading.value === true) {
    await fetchOrders()
  }
})

//開啟訂單詳情 Modal
const openModal = async (orderId) => {
  try {
    const response = await axios.get(`/orders/detail/${orderId}`)
    selectedOrderDetail.value = response.data

    //等待 DOM 更新後顯示 Modal
    await nextTick()
    if (detailModal) {
      detailModal.show()
    }
  } catch (err) {
    console.error('抓取訂單明細失敗:', err)
  }
}

const isWithinThreeDays = (orderDate) => {
  if (!orderDate) return false
  const now = new Date()
  const orderTime = new Date(orderDate)
  const diff = now.getTime() - orderTime.getTime()
  const threeDaysInMs = 3 * 24 * 60 * 60 * 1000
  return diff <= threeDaysInMs
}

const handleCancel = async (orderId) => {
  if (!confirm('確定要取消這筆訂單嗎？此動作無法復原。')) return

  try {
    // 呼叫後端 API (路徑需與 Controller 對應)
    const response = await axios.post(`/orders/cancel/${orderId}`)

    if (response.status === 200) {
      // 成功後，手動更新前端 orders 狀態，不需重新抓取整份列表
      const order = orders.value.find((o) => o.orderId === orderId)
      if (order) {
        order.orderStatus = '已取消'
      }
     // 若有退回點數，觸發 store 更新全站點數
      await userStore.fetchUser()
      alert('訂單已成功取消！(若有使用紅利已全數退回)')
    }
  } catch (err) {
    console.error('取消訂單失敗:', err)
    // 處理後端回傳的錯誤訊息(超過3天)
    const errorMsg = err.response?.data || '取消失敗，請稍後再試'
    alert(errorMsg)
  }
}

// 確認收貨邏輯          活動新增：當訂單狀態為「已完成」時，會員會自動獲得紅利點數，這裡的 API 會處理點數發放的邏輯
const handleComplete = async (orderId) => {
  if (!confirm('收到商品了嗎？確認收貨後，系統將自動發放紅利點數！')) return

  try {
    const response = await axios.post(`/orders/complete/${orderId}`)
    if (response.status === 200) {
      const order = orders.value.find((o) => o.orderId === orderId)
      if (order) {
        order.orderStatus = '已完成'
      }
      // 紅利發放後，立即觸發 store 更新全站的會員資訊與點數餘額
      await userStore.fetchUser()
      alert('成功確認收貨！紅利點數已發放至您的帳戶。')
    }
  } catch (err) {
    console.error('確認收貨失敗:', err)
    const errorMsg = err.response?.data || '確認失敗，請稍後再試'
    alert(errorMsg)
  }
}

//工具函數：格式化
const formatPrice = (price) => (price ? new Intl.NumberFormat('zh-TW').format(price) : '0')
const formatDateTime = (str) => (str ? str.replace('T', ' ').substring(0, 19) : '')
const getStatusLabel = (s) => {
  const map = {
    處理中: { class: 'bg-warning text-dark', text: '處理中' },
    已完成: { class: 'bg-success', text: '已完成' },
    已取消: { class: 'bg-secondary-subtle text-muted', text: '已取消' },
  }
  return map[s] || { class: 'bg-secondary', text: s || '處理中' }
}
</script>

<style scoped>
@import '../assets/css/ProductOrder.css';
</style>

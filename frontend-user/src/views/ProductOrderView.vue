<template>
  <div class="product-order-container">
    <div v-if="isLoading" class="text-center py-5">
      <div class="spinner-border text-warning" role="status"></div>
      <p class="mt-2 text-muted">訂單載入中...</p>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state text-center py-5">
      <i class="fas fa-box-open fa-3x text-muted mb-3"></i>
      <p class="text-muted">目前沒有購買紀錄</p>
      <router-link to="/" class="btn btn-warning">前往商城逛逛</router-link>
    </div>

    <div v-else class="table-responsive">
      <table class="table table-hover align-middle custom-table">
        <thead class="table-light">
          <tr>
            <th>訂單編號</th>
            <th>日期</th>
            <th>總額</th>
            <th>狀態</th>
            <th>付款方式</th>
            <th class="text-end">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.orderId">
            <td>
              <strong>#{{ order.orderId }}</strong>
            </td>
            <td>{{ formatDateTime(order.orderDate) }}</td>
            <td>
              <span class="price-text text-danger fw-bold"
                >${{ formatPrice(order.orderTotal) }}</span
              >
            </td>
            <td>
              <span :class="['badge', getStatusLabel(order.orderStatus).class]">
                {{ getStatusLabel(order.orderStatus).text }}
              </span>
            </td>
            <td>{{ order.orderPayment }}</td>
            <td class="text-end">
              <button
                @click="openModal(order.orderId)"
                class="btn btn-outline-warning btn-sm px-4 fw-bold"
              >
                查看內容
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="modal fade" id="orderDetailModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-lg">
        <div class="modal-content" v-if="selectedOrderDetail">
          <div class="modal-header bg-warning text-white">
            <h5 class="modal-title">訂單詳情 #{{ selectedOrderDetail.orderId }}</h5>
            <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Close"
            ></button>
          </div>
          <div class="modal-body">
            <div class="row mb-3">
              <div class="col-md-6">
                <p><strong>收件人：</strong> {{ selectedOrderDetail.orderName }}</p>
                <p><strong>配送地址：</strong> {{ selectedOrderDetail.orderAddress }}</p>
              </div>
            </div>
            <table class="table border">
              <thead class="table-light">
                <tr>
                  <th>商品</th>
                  <th>單價</th>
                  <th>數量</th>
                  <th>小計</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedOrderDetail.items" :key="item.productName">
                  <td>{{ item.productName }}</td>
                  <td>${{ item.productPrice }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>${{ item.subtotal }}</td>
                </tr>
              </tbody>
            </table>
            <div class="text-end mt-3">
              <h4 class="text-danger">總計: ${{ formatPrice(selectedOrderDetail.orderTotal) }}</h4>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/axios'
import { Modal } from 'bootstrap'

const orders = ref([])
const selectedOrderDetail = ref(null)
const isLoading = ref(true)
let detailModal = null

onMounted(async () => {
  await fetchOrders()
  const modalElement = document.getElementById('orderDetailModal')
  if (modalElement) {
    detailModal = new Modal(modalElement, { focus: false })
  }
})

const fetchOrders = async () => {
  isLoading.value = true
  try {
    // 確保路徑對應後端 /api/orders/historyorders
    const response = await axios.get('/orders/historyorders')
    orders.value = response.data
  } catch (err) {
    console.error('抓取失敗:', err)
  } finally {
    isLoading.value = false
  }
}

const openModal = async (orderId) => {
  try {
    const response = await axios.get(`/orders/detail/${orderId}`)
    selectedOrderDetail.value = response.data
    if (detailModal) detailModal.show()
  } catch (err) {
    console.error('抓取訂單明細失敗:', err)
    alert('無法讀取明細')
  }
}

const formatPrice = (price) => (price ? new Intl.NumberFormat('zh-TW').format(price) : '0')
const formatDateTime = (str) => (str ? str.replace('T', ' ').substring(0, 19) : '')
const getStatusLabel = (s) => {
  const map = {
    處理中: { class: 'bg-warning text-dark', text: '處理中' },
    已完成: { class: 'bg-success', text: '已完成' },
    已取消: { class: 'bg-danger', text: '已取消' },
  }
  return map[s] || { class: 'bg-secondary', text: s || '處理中' }
}
</script>

<style scoped>
.custom-table {
  border-collapse: separate;
  border-spacing: 0 10px;
}
.price-text {
  font-size: 1.1rem;
}
.empty-state {
  min-height: 300px;
}
</style>

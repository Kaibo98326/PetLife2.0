<template>
  <div class="product-order-container">
    <!-- 如果訂單為空 -->
    <div v-if="orders.length === 0" class="empty-state text-center py-5">
      <div class="empty-icon mb-3">
        <i class="fas fa-shopping-cart fa-3x text-muted opacity-25"></i>
      </div>
      <p class="fs-5 text-muted">目前還沒有任何訂單紀錄喔！</p>
      <!-- 要來改導回頁面的路徑 -->
      <router-link to="/shop/index" class="btn btn-warning px-4 py-2 mt-2 shadow-sm fw-bold">
        前往購物
      </router-link>
    </div>

    <!-- 訂單資訊 -->
    <div v-else class="table-responsive">
      <table class="table table-hover align-middle custom-table">
        <thead class="table-light">
          <tr>
            <th style="width: 10%">訂單編號</th>
            <th style="width: 25%">下單時間</th>
            <th style="width: 15%">訂單金額</th>
            <th style="width: 15%">交易狀態</th>
            <th style="width: 15%">付款方式</th>
            <th style="width: 20%" class="text-end">訂單明細</th>
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
              <span
                :class="[
                  'badge',
                  order.orderStatus === '已完成' ? 'bg-success' : 'bg-warning text-dark',
                ]"
              >
                {{ order.orderStatus }}
              </span>
            </td>
            <td>{{ order.orderPayment }}</td>
            <td class="text-end">
              <!-- 查看訂單明細 -->
              <button
                @click="viewDetail(order.orderId)"
                class="btn btn-outline-warning btn-sm px-4 fw-bold shadow-sm"
              >
                查看內容
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const orders = ref([])

// 抓取後端資料
onMounted(async () => {
  try {
    const response = await axios.get('/orders')
    orders.value = response.data
  } catch (error) {
    console.error('抓取訂單失敗:', error)
  }
})

// 格式化價格
const formatPrice = (price) => {
  return new Intl.NumberFormat('zh-TW').format(price)
}

// 格式化日期
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-TW', { hour12: false })
}

// 前往明細頁面
const viewDetail = (id) => {
  router.push(`/orderDetail/${id}`)
}
</script>

<style scoped>
.custom-table {
  border-collapse: separate;
  border-spacing: 0 10px;
}

.custom-table thead th {
  border: none;
  background-color: #f8f9fa;
  color: #666;
  font-weight: 600;
  padding: 15px;
}

.custom-table tbody tr {
  transition: transform 0.2s;
}

.custom-table tbody tr:hover {
  transform: scale(1.01);
  background-color: #fffdf9;
}

.price-text {
  font-size: 1.1rem;
}

.empty-state {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>

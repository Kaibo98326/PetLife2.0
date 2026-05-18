<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'

const router = useRouter()
const isLoading = ref(false)
const orders = ref([])
const currentPage = ref(1)
const pageSize = 10

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return orders.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.max(1, Math.ceil(orders.value.length / pageSize)))

const loadOrders = async () => {
  isLoading.value = true
  try {
    const res = await axios.get('/beauty/appointments/my')
    orders.value = res.data || []
    currentPage.value = 1
  } catch (err) {
    console.log(err)
    Swal.fire('讀取失敗', err.response?.data?.message || '美容預約紀錄讀取失敗', 'error')
  } finally {
    isLoading.value = false
  }
}

const goToPage = page => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

const goDetail = appointmentId => {
  router.push({
    name: 'prettyOrderDetail',
    params: { appointmentId },
  })
}

const formatMoney = value => `$${Number(value || 0).toLocaleString()}`
const formatMinutes = totalSlots => `${Number(totalSlots || 0) * 30} 分鐘`
const formatStartTime = slotName => {
  if (!slotName) return ''
  return String(slotName).split('-')[0].trim()
}

const formatItems = details => {
  if (!details || details.length === 0) return '-'
  return details.map(detail => detail.itemNameSnapshot).filter(Boolean).join('、')
}

const statusClass = status => {
  const map = {
    待確認: 'status-pending',
    已確認: 'status-confirmed',
    已完成: 'status-done',
    已取消: 'status-cancelled',
    未到: 'status-no-show',
  }

  return map[status] || 'status-default'
}

onMounted(loadOrders)
</script>

<template>
  <div class="pretty-order-container">
    <div v-if="isLoading" class="text-center py-5">
      <div class="spinner-border text-warning" role="status"></div>
      <p class="mt-2 text-muted">美容預約紀錄讀取中...</p>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state text-center py-5">
      <div class="mb-3">
        <i class="fas fa-cut fa-4x text-light-orange"></i>
      </div>
      <p class="text-muted fw-bold">目前沒有美容預約紀錄</p>
      <router-link to="/beauty-booking" class="btn btn-warning rounded-pill px-4 shadow-sm">
        前往預約美容
      </router-link>
    </div>

    <div v-else>
      <div class="table-responsive">
        <table class="table align-middle custom-table">
          <thead class="table-light">
            <tr>
              <th class="ps-4">預約編號</th>
              <th>預約日期</th>
              <th>寵物</th>
              <th>美容師</th>
              <th>美容項目</th>
              <th class="text-center">時長</th>
              <th class="text-end">金額</th>
              <th class="text-center">狀態</th>
              <th class="text-center">詳情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in paginatedOrders" :key="order.appointmentId">
              <td class="ps-4">
                <strong>#{{ order.appointmentId }}</strong>
              </td>
              <td>
                <div class="fw-bold">{{ order.appointDate || '-' }}</div>
                <small class="text-muted">{{ formatStartTime(order.startSlotName) || `時段 ${order.startSlotId}` }}</small>
              </td>
              <td>{{ order.petName || '-' }}</td>
              <td>{{ order.groomerName || '-' }}</td>
              <td class="item-summary">{{ formatItems(order.details) }}</td>
              <td class="text-center">{{ formatMinutes(order.totalSlots) }}</td>
              <td class="text-end price-text">{{ formatMoney(order.totalAmount) }}</td>
              <td class="text-center">
                <span :class="['status-badge', statusClass(order.appointmentStatus)]">
                  {{ order.appointmentStatus || '-' }}
                </span>
              </td>
              <td class="text-center">
                <button class="btn btn-detail-action btn-sm px-4" @click="goDetail(order.appointmentId)">
                  查看詳情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-controls text-center mt-3">
        <button
          class="btn btn-sm btn-outline-secondary mx-1"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
        >
          上一頁
        </button>
        <span class="mx-2">第 {{ currentPage }} / {{ totalPages }} 頁</span>
        <button
          class="btn btn-sm btn-outline-secondary mx-1"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          下一頁
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pretty-order-container {
  width: 100%;
}

.custom-table {
  border-collapse: separate;
  border-spacing: 0 10px;
}

.custom-table thead th {
  color: #6b5a50;
  font-weight: 700;
  white-space: nowrap;
}

.custom-table tbody tr {
  background: #fffaf4;
  box-shadow: 0 4px 14px rgba(82, 60, 42, 0.08);
}

.custom-table tbody td {
  padding-top: 16px;
  padding-bottom: 16px;
  border: 0;
  color: #4f4037;
  vertical-align: middle;
}

.item-summary {
  max-width: 220px;
  color: #6d5f57;
}

.price-text {
  color: #d2691e;
  font-weight: 800;
}

.status-badge {
  display: inline-flex;
  min-width: 72px;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 14px;
}

.status-pending {
  background: #fff3cd;
  color: #8a5b00;
}

.status-confirmed {
  background: #dff3e4;
  color: #257145;
}

.status-done {
  background: #e7f0ff;
  color: #275b9f;
}

.status-cancelled,
.status-no-show {
  background: #ececec;
  color: #6d6d6d;
}

.status-default {
  background: #f4eee8;
  color: #6b5a50;
}

.btn-detail-action {
  border-radius: 999px;
  border: 1px solid #f1b457;
  background: #fff;
  color: #d78021;
  font-weight: 700;
}

.btn-detail-action:hover {
  background: #f39c12;
  color: #fff;
}

.text-light-orange {
  color: #f4bf7a;
}
</style>

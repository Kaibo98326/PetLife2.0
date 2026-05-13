<script setup>
import axios from '@/axios.js'
import { useUserStore } from '@/stores/user'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const isLoading = ref(true)
const orders = ref([])

const fetchOrders = async () => {
  try {
    const memberId = userStore.memberId
    const res = await axios.get(`/stay/member/${memberId}`)
    orders.value = res.data
  } catch (e) {
    console.error('訂單載入失敗', e)
  } finally {
    isLoading.value = false
  }
}

// 狀態中文對照
const statusLabel = (status) => {
  const map = {
    active: '待確認',
    CONFIRMED: '已確認',
    CHECKED_IN: '已入住',
    CANCELLED: '已取消',
  }
  return map[status] ?? status
}

// 狀態顏色
const statusClass = (status) => {
  const map = {
    active: 'badge-pending',
    CONFIRMED: 'badge-confirmed',
    CHECKED_IN: 'badge-checkin',
    CANCELLED: 'badge-cancelled',
  }
  return map[status] ?? ''
}

const cancelOrder = async (stayId) => {
  if (!confirm('確定要取消此訂單嗎？')) return
  try {
    await axios.patch(`/stay/${stayId}/cancel`)
    await fetchOrders()
  } catch (e) {
    alert('取消失敗：' + (e.response?.data?.message || '請稍後重試'))
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="stay-orders">
    <h3 class="page-title">寵物住宿訂單</h3>

    <div v-if="isLoading" class="loading-tip">載入中...</div>

    <div v-else-if="orders.length === 0" class="empty-tip">目前沒有住宿訂單</div>

    <div v-else class="order-list">
      <div v-for="order in orders" :key="order.stayId" class="order-card">
        <!-- 上方：房型 + 狀態 -->
        <div class="order-header">
          <span class="room-type">{{ order.roomTypeName }}</span>
          <span :class="['badge', statusClass(order.stayStatus)]">
            {{ statusLabel(order.stayStatus) }}
          </span>
        </div>

        <!-- 中間：訂單資訊 -->
        <div class="order-body">
          <div class="info-row">
            <span class="info-label">房號</span>
            <span class="info-val">{{ order.roomNo }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">寵物</span>
            <span class="info-val">{{ order.petName }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">入住</span>
            <span class="info-val">{{ order.stayStartDate }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">退房</span>
            <span class="info-val">{{ order.stayEndDate }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">天數</span>
            <span class="info-val">{{ order.stayDay }} 晚</span>
          </div>
          <div class="info-row">
            <span class="info-label">總金額</span>
            <span class="info-val price">NT$ {{ order.sumPrice?.toLocaleString() }}</span>
          </div>
          <div class="info-row" v-if="order.stayRemark">
            <span class="info-label">備註</span>
            <span class="info-val remark">{{ order.stayRemark }}</span>
          </div>
        </div>

        <!-- 下方：取消按鈕 -->
        <div class="order-footer">
          <button
            class="btn-cancel"
            v-if="order.stayStatus !== 'CANCELLED' && order.stayStatus !== 'CHECKED_IN'"
            @click="cancelOrder(order.stayId)"
          >
            取消訂單
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stay-orders {
  --brown: #6b4c2a;
  --gold: #c9933a;
  padding: 24px;
  font-family: 'Noto Serif TC', serif;
}
.page-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--brown);
  margin-bottom: 24px;
}
.loading-tip,
.empty-tip {
  text-align: center;
  color: #999;
  padding: 60px 0;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.order-card {
  background: white;
  border: 1px solid #ecdfd0;
  border-radius: 12px;
  overflow: hidden;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fdf6ee;
  border-bottom: 1px solid #ecdfd0;
}
.room-type {
  font-weight: 700;
  color: var(--brown);
  font-size: 1rem;
}
.badge {
  font-size: 0.8rem;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 600;
}
.badge-pending {
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
.badge-cancelled {
  background: #f3f4f6;
  color: #6b7280;
}
.order-body {
  padding: 16px 20px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5ede0;
  font-size: 0.9rem;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  color: #999;
}
.info-val {
  font-weight: 600;
  color: var(--brown);
}
.info-val.price {
  color: var(--gold);
  font-size: 1rem;
}
.info-val.remark {
  font-size: 0.85rem;
  color: #888;
  font-weight: 400;
  max-width: 60%;
  text-align: right;
}
.order-footer {
  padding: 12px 20px;
  border-top: 1px solid #ecdfd0;
  text-align: right;
}
.btn-cancel {
  background: none;
  border: 1px solid #fca5a5;
  color: #ef4444;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-cancel:hover {
  background: #fef2f2;
}
</style>

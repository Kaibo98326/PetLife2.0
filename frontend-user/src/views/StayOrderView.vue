<script setup>
import axios from '@/axios.js'
import { useUserStore } from '@/stores/user'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const isLoading = ref(true)
const orders = ref([])
const pendingCancelId = ref(null)
const detailOrder = ref(null)

const fetchOrders = async () => {
  try {
    const memberId = userStore.memberId
    const res = await axios.get(`/stay/member/${memberId}`)
    // 訂單編號越大排越上面
    orders.value = res.data.sort((a, b) => b.stayId - a.stayId)
  } catch (e) {
    console.error('訂單載入失敗', e)
  } finally {
    isLoading.value = false
  }
}
const openDetail = (order) => {
  detailOrder.value = {
    ...order,
    parsedRemark: order.stayRemark ? JSON.parse(order.stayRemark) : null,
  }
}

// 分類
// ✅ 已付款：付款成功 且 沒有被取消
const paidOrders = computed(() =>
  orders.value.filter((o) => o.paymentStatus === 'SUCCESS' && o.stayStatus !== 'CANCELLED'),
)

// ✅ 未付款：付款狀態是 PENDING 且 沒有被取消
const pendingOrders = computed(() =>
  orders.value.filter((o) => o.paymentStatus === 'PENDING' && o.stayStatus !== 'CANCELLED'),
)

// ✅ 已取消：只看 stayStatus
const cancelledOrders = computed(() => orders.value.filter((o) => o.stayStatus === 'CANCELLED'))

// 已入住
const checkedInOrders = computed(() => orders.value.filter((o) => o.stayStatus === 'CHECKED_IN'))

// 已退房
const checkedOutOrders = computed(() => orders.value.filter((o) => o.stayStatus === 'CHECKED_OUT'))

// 目前選的 Tab
const activeTab = ref('paid')

// 狀態中文對照
const statusLabel = (status) => {
  const map = {
    active: '已成立(待付款)',
    CONFIRMED: '已確認',
    CHECKED_IN: '已入住',
    CANCELLED: '已取消',
  }
  return map[status] ?? status
}
const confirmCancel = async () => {
  try {
    const token = localStorage.getItem('jwtToken')
    await axios.patch(`/stay/${pendingCancelId.value}/cancel`, null, {
      headers: { Authorization: `Bearer ${token}` },
    })
    await fetchOrders()
  } catch (e) {
    alert('取消失敗：' + (e.response?.data?.message || '請稍後重試'))
  }
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

const cancelOrder = async (stayId, paymentStatus) => {
  if (paymentStatus === 'SUCCESS') {
    alert('已付款訂單無法自行取消，請聯繫客服')
    return
  }
  if (!confirm('確定要取消此訂單嗎？')) return
  try {
    const token = localStorage.getItem('jwtToken')
    await axios.patch(`/stay/${stayId}/cancel`, null, {
      headers: { Authorization: `Bearer ${token}` },
    })
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

    <div v-else>
      <!-- Tab 切換 -->
      <div class="tab-bar">
        <button
          :class="['tab-btn', { active: activeTab === 'checkedIn' }]"
          @click="activeTab = 'checkedIn'"
        >
          已入住（{{ checkedInOrders.length }}）
        </button>

        <button :class="['tab-btn', { active: activeTab === 'paid' }]" @click="activeTab = 'paid'">
          已付款（{{ paidOrders.length }}）
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'pending' }]"
          @click="activeTab = 'pending'"
        >
          未付款（{{ pendingOrders.length }}）
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'checkedOut' }]"
          @click="activeTab = 'checkedOut'"
        >
          已退房（{{ checkedOutOrders.length }}）
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'cancelled' }]"
          @click="activeTab = 'cancelled'"
        >
          已取消（{{ cancelledOrders.length }}）
        </button>
      </div>

      <!-- 訂單列表 -->
      <div class="order-list">
        <div
          v-for="order in activeTab === 'paid'
            ? paidOrders
            : activeTab === 'pending'
              ? pendingOrders
              : activeTab === 'checkedIn'
                ? checkedInOrders
                : activeTab === 'checkedOut'
                  ? checkedOutOrders
                  : cancelledOrders"
          :key="order.stayId"
          class="order-card"
        >
          <div class="order-header">
            <span class="room-type">{{ order.roomTypeName }}</span>
            <span :class="['badge', statusClass(order.stayStatus)]">
              {{ statusLabel(order.stayStatus) }}
            </span>
          </div>

          <div class="order-body">
            <div class="info-row">
              <span class="info-label">訂單編號</span>
              <span class="info-val"># {{ order.stayId }}</span>
            </div>
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
          </div>

          <div class="order-footer">
            <button
              class="btn-detail"
              data-bs-toggle="modal"
              data-bs-target="#detailModal"
              @click="openDetail(order)"
            >
              訂單明細
            </button>
            <button
              class="btn-cancel"
              v-if="order.stayStatus !== 'CHECKED_IN'"
              :disabled="order.stayStatus === 'CANCELLED' || order.paymentStatus === 'SUCCESS'"
              data-bs-toggle="modal"
              data-bs-target="#cancelModal"
              @click="pendingCancelId = order.stayId"
            >
              {{
                order.stayStatus === 'CANCELLED'
                  ? '已取消'
                  : order.paymentStatus === 'SUCCESS'
                    ? '已付款不可取消'
                    : '取消訂單'
              }}
            </button>
          </div>
        </div>

        <!-- 空狀態 -->
        <div
          class="empty-tip"
          v-if="
            (activeTab === 'paid' && paidOrders.length === 0) ||
            (activeTab === 'pending' && pendingOrders.length === 0) ||
            (activeTab === 'checkedIn' && checkedInOrders.length === 0) ||
            (activeTab === 'checkedOut' && checkedOutOrders.length === 0) ||
            (activeTab === 'cancelled' && cancelledOrders.length === 0)
          "
        >
          此分類沒有訂單
        </div>
      </div>
    </div>
  </div>

  <!-- Modal HTML -->
  <div class="modal fade" id="cancelModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content custom-modal">
        <!-- 圖示 -->
        <div class="modal-icon-wrap">
          <div class="modal-icon">!</div>
        </div>

        <!-- 標題 -->
        <h5 class="custom-modal-title">確定要取消訂單嗎？</h5>

        <!-- 說明文字 -->
        <p class="custom-modal-desc">取消後無法復原，如需協助請聯繫客服</p>

        <!-- 按鈕 -->
        <div class="custom-modal-footer">
          <button class="btn-modal-confirm" data-bs-dismiss="modal" @click="confirmCancel">
            是的，取消訂單
          </button>
          <button class="btn-modal-cancel" data-bs-dismiss="modal">返回</button>
        </div>
      </div>
    </div>
  </div>
  <div class="modal fade" id="detailModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content custom-modal">
        <div class="modal-header-row">
          <h5 class="custom-modal-title">訂單明細</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>

        <div v-if="detailOrder">
          <!-- 會員資料 -->
          <div class="detail-section">
            <h6 class="detail-section-title">會員資料</h6>
            <div class="detail-row">
              <span class="detail-label">姓名</span>
              <span class="detail-val">{{ detailOrder.memberName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">電話</span>
              <span class="detail-val">{{ detailOrder.memberPhone }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Email</span>
              <span class="detail-val">{{ detailOrder.memberEmail }}</span>
            </div>
          </div>

          <!-- 訂單資訊 -->
          <div class="detail-section">
            <h6 class="detail-section-title">訂單資訊</h6>
            <div class="detail-row">
              <span class="detail-label">訂單編號</span>
              <span class="detail-val"># {{ detailOrder.stayId }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">訂單成立時間</span>
              <span class="detail-val">{{
                detailOrder.createdAt?.replace('T', ' ').slice(0, 16)
              }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">付款時間</span>
              <span class="detail-val">
                {{
                  detailOrder.paidAt
                    ? detailOrder.paidAt.replace('T', ' ').slice(0, 16)
                    : '尚未付款'
                }}
              </span>
            </div>
          </div>
          <!-- 入住寵物詳細資料 -->
          <div class="detail-section" v-if="detailOrder.parsedRemark?.pets?.length">
            <h6 class="detail-section-title">入住寵物</h6>
            <div
              v-for="(pet, index) in detailOrder.parsedRemark.pets"
              :key="pet.petId"
              class="detail-row"
            >
              <span class="detail-label">
                <span class="pet-badge" v-if="index === 0">主要</span>
                <span class="pet-badge extra" v-else>同行</span>
                {{ pet.petName }}
              </span>
              <span class="detail-val">{{ pet.species }} · {{ pet.breed }}</span>
            </div>
          </div>
          <!-- 客人備註 -->
          <div class="detail-section" v-if="detailOrder.parsedRemark?.customerNote">
            <h6 class="detail-section-title">備註</h6>
            <p class="detail-note">{{ detailOrder.parsedRemark.customerNote }}</p>
          </div>
        </div>

        <div class="custom-modal-footer">
          <button class="btn-modal-cancel" data-bs-dismiss="modal">關閉</button>
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

.tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 2px solid #ecdfd0;
  padding-bottom: 0;
  flex-wrap: wrap;
}
.tab-btn {
  background: none;
  border: none;
  padding: 10px 20px;
  font-size: 0.95rem;
  color: #999;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-family: inherit;
  transition: all 0.2s;
}
.tab-btn.active {
  color: var(--brown);
  font-weight: 700;
  border-bottom-color: var(--brown);
}
.tab-btn:hover {
  color: var(--brown);
}

.btn-cancel:disabled {
  border-color: #d1d5db;
  color: #9ca3af;
  cursor: not-allowed;
  background: none;
}

.custom-modal {
  border: none;
  border-radius: 16px;
  padding: 36px 24px 28px;
  text-align: center;
}

.modal-icon-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.modal-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid #f0a500;
  color: #f0a500;
  font-size: 2rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.custom-modal-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 10px;
}

.custom-modal-desc {
  font-size: 0.9rem;
  color: #7f8c8d;
  margin-bottom: 28px;
}

.custom-modal-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-modal-confirm {
  background: #7c6ef5;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-modal-confirm:hover {
  background: #6a5ce0;
}

.btn-modal-cancel {
  background: #9ca3af;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-modal-cancel:hover {
  background: #6b7280;
}

.btn-detail {
  background: none;
  border: 1px solid #c9933a;
  color: #c9933a;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-detail:hover {
  background: #fdf6ee;
}

.modal-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 20px;
  border-bottom: 1px solid #f5ede0;
  padding-bottom: 16px;
}

.detail-section:last-of-type {
  border-bottom: none;
}

.detail-section-title {
  font-size: 0.85rem;
  color: #9ca3af;
  font-weight: 600;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 0.9rem;
  border-bottom: 1px solid #fdf6ee;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-val {
  font-weight: 600;
  color: #6b4c2a;
}

.detail-val.price {
  color: #c9933a;
}

.detail-note {
  font-size: 0.9rem;
  color: #6b4c2a;
  line-height: 1.6;
}

.pet-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fef9c3;
  color: #854d0e;
}

.pet-badge.extra {
  background: #dcfce7;
  color: #166534;
}
</style>

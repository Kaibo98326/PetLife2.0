<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'

const route = useRoute()
const router = useRouter()
const appointment = ref(null)
const loading = ref(false)

const details = computed(() => appointment.value?.details || [])
const totalAmount = computed(() => `$${Number(appointment.value?.totalAmount || 0).toLocaleString()}`)
const totalMinutes = computed(() => Number(appointment.value?.totalSlots || 0) * 30)
const isOrderHistoryDetail = computed(() => route.name === 'prettyOrderDetail')
const backButtonText = computed(() => (isOrderHistoryDetail.value ? '返回美容訂單' : '返回美容項目'))

const goBack = () => {
  router.push(isOrderHistoryDetail.value ? '/orderhistory/prettyorders' : '/beauty-booking')
}

const goBeautyOrders = () => {
  router.push({ name: 'prettyorders' })
}

const loadAppointment = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/beauty/appointments/${route.params.appointmentId}`)
    appointment.value = res.data
  } catch (err) {
    console.log(err)
    Swal.fire('讀取失敗', err.response?.data?.message || '美容預約詳情讀取失敗', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadAppointment)
</script>

<template>
  <main class="detail-page container py-4">
    <div class="detail-heading">
      <div>
        <h2>預約明細</h2>
        <p>訂單狀態</p>
      </div>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-warning" role="status"></div>
    </div>

    <section v-else-if="appointment" class="detail-card">
      <div class="detail-status">
        <span>預約編號 #{{ appointment.appointmentId }}</span>
        <strong>{{ appointment.appointmentStatus }}</strong>
      </div>

      <div class="detail-grid">
        <div>
          <span>寵物</span>
          <strong>{{ appointment.petName || '-' }}</strong>
        </div>
        <div>
          <span>美容師</span>
          <strong>{{ appointment.groomerName || '-' }}</strong>
        </div>
        <div>
          <span>日期</span>
          <strong>{{ appointment.appointDate }}</strong>
        </div>
        <div>
          <span>開始時段</span>
          <strong>{{ appointment.startSlotName || appointment.startSlotId }}</strong>
        </div>
        <div>
          <span>總時長</span>
          <strong>{{ totalMinutes }} 分鐘</strong>
        </div>
        <div>
          <span>總金額</span>
          <strong>{{ totalAmount }}</strong>
        </div>
      </div>

      <h3>美容項目</h3>
      <div class="detail-lines">
        <div v-for="line in details" :key="line.detailId" class="detail-line">
          <div>
            <strong>{{ line.itemNameSnapshot }}</strong>
            <span>{{ Number(line.durationSlotsSnapshot || 0) * 30 }} 分鐘</span>
          </div>
          <strong>${{ Number(line.itemPriceSnapshot || 0).toLocaleString() }}</strong>
        </div>
      </div>

      <div class="detail-actions">
        <button class="btn btn-outline-secondary" @click="goBack">
          {{ backButtonText }}
        </button>
        <button v-if="!isOrderHistoryDetail" class="btn btn-warning" @click="goBeautyOrders">
          查看美容訂單
        </button>
      </div>

      <div v-if="appointment.contactNote" class="detail-note">
        <span>備註</span>
        <p>{{ appointment.contactNote }}</p>
      </div>

      <div v-if="appointment.cancelReason" class="detail-note">
        <span>取消原因</span>
        <p>{{ appointment.cancelReason }}</p>
      </div>

      <div class="phone-note">
        若需要調整預約內容，請聯繫店家協助處理。
      </div>
    </section>
  </main>
</template>

<style scoped>
.detail-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  color: #4f4037;
}

.detail-heading h2 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.detail-heading p {
  margin: 0;
  color: #776b64;
}

.detail-card {
  padding: 24px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(82, 60, 42, 0.08);
}

.detail-status {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee3da;
  color: #5a4a42;
}

.detail-status strong {
  color: #d78021;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin: 20px 0;
}

.detail-grid div {
  padding: 14px;
  border-radius: 8px;
  background: #f8f2ec;
}

.detail-grid span,
.detail-note span {
  display: block;
  margin-bottom: 6px;
  color: #83746b;
  font-size: 14px;
}

.detail-card h3 {
  margin: 24px 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: #4f4037;
}

.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0e7df;
}

.detail-line span {
  display: block;
  color: #7c6d64;
  font-size: 14px;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f0e7df;
}

.detail-actions .btn {
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.detail-actions .btn-outline-secondary:hover,
.detail-actions .btn-outline-secondary:focus-visible {
  background: #6b5a50;
  border-color: #6b5a50;
  color: #fff;
}

.detail-actions .btn-warning:hover,
.detail-actions .btn-warning:focus-visible {
  background: #d78021;
  border-color: #d78021;
  color: #fff;
}

.detail-actions .btn:hover,
.detail-actions .btn:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(82, 60, 42, 0.16);
}

.detail-actions .btn:active {
  transform: translateY(0);
  box-shadow: none;
}

.detail-note {
  margin-top: 20px;
  padding: 14px;
  border-radius: 8px;
  background: #fffaf4;
}

.detail-note p {
  margin: 0;
}

.phone-note {
  margin-top: 20px;
  padding: 14px;
  border-radius: 8px;
  background: #fff3df;
  color: #7b5421;
  font-weight: 700;
}

@media (max-width: 768px) {
  .detail-heading,
  .detail-status,
  .detail-actions {
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

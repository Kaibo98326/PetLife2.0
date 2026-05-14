<script setup>
import axios from '@/axios.js'
import { useUserStore } from '@/stores/user'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ======== 房型名稱對照 ========
const roomTypeNames = {
  1: '大型犬豪華房',
  2: '小型犬溫馨房',
  3: '貓咪蜜月套房',
  4: '貓咪輕旅房',
  5: '貓狗同樂房',
}

// ======== 狀態 ========
const editPhone = ref('')
const customerNote = ref('')
const assignedRoomNo = ref('')
const selectedPaymentMethod = ref('LINE_PAY')
const isSubmitting = ref(false)
const showPaymentModal = ref(false)

// ======== 從 query 拿日期跟寵物 ========
const checkIn = route.query.checkIn
const checkOut = route.query.checkOut
const petIds = route.query.pets ? route.query.pets.split(',').map(Number) : []

// ======== 房間資料 ========
const roomType = ref(null)
const memberInfo = ref(null)
const pets = ref([])
const availableRooms = ref([])

// ======== 拿房型資料 ========
const fetchRoomType = async () => {
  try {
    const res = await axios.get(`/stay/roomtype/${route.params.roomTypeId}`)
    roomType.value = res.data
  } catch (e) {
    console.error('房型載入失敗', e)
  }
}

// ======== 拿會員資料 ========
const fetchMemberInfo = async () => {
  try {
    const token = localStorage.getItem('jwtToken')
    const res = await axios.get('/member/me', {
      headers: { Authorization: `Bearer ${token}` },
    })
    memberInfo.value = res.data
    editPhone.value = res.data.phone || ''
  } catch (e) {
    console.error('會員資料載入失敗', e)
  }
}

// ======== 拿寵物資料 ========
const fetchPets = async () => {
  try {
    const res = await axios.get(`/pets/member/${userStore.memberId}`)
    pets.value = res.data.filter((p) => petIds.includes(p.petId))
  } catch (e) {
    console.error('寵物資料載入失敗', e)
  }
}

// ======== 自動分配房間 ========
const assignRoom = async () => {
  try {
    const roomTypeId = parseInt(route.params.roomTypeId)
    const res = await axios.get('/stay/available', {
      params: {
        roomTypeId: roomTypeId,
        startDate: checkIn,
        endDate: checkOut,
      },
    })

    if (res.data.availableCount > 0) {
      const roomPrefix = {
        1: 'A',
        2: 'B',
        3: 'C',
        4: 'D',
        5: 'E',
      }

      const prefix = roomPrefix[roomTypeId]
      assignedRoomNo.value = `${prefix}101`
    } else {
      alert('此日期該房型已滿房，請重新選擇')
      router.back()
    }
  } catch (e) {
    console.error('房間查詢失敗', e)
  }
}

// ======== 計算住幾晚 ========
const stayNights = computed(() => {
  if (!checkIn || !checkOut) return 0
  const a = new Date(checkIn)
  const b = new Date(checkOut)
  return Math.round((b - a) / (1000 * 60 * 60 * 24))
})

// ======== 計算總價 ========
const totalPrice = computed(() => {
  if (!roomType.value || stayNights.value === 0) return 0
  return roomType.value.roomPrice * stayNights.value
})

// ======== 主要寵物（第一隻） ========
const mainPet = computed(() => {
  return pets.value.length > 0 ? pets.value[0] : null
})

// ======== 附加寵物（第二隻以上） ========
const additionalPets = computed(() => {
  return pets.value.length > 1 ? pets.value.slice(1) : []
})

// ======== 生成備註（包含第二隻以上的寵物） ========
const generateRemark = () => {
  let remark = customerNote.value

  if (additionalPets.value.length > 0) {
    const petInfoText = additionalPets.value
      .map((pet, index) => `寵物 ${index + 2}：${pet.petName}（${pet.species}・${pet.breed}）`)
      .join('\n')

    if (remark) {
      remark += '\n\n--- 同行寵物 ---\n' + petInfoText
    } else {
      remark = '--- 同行寵物 ---\n' + petInfoText
    }
  }

  return remark
}

// ======== 送出預約 ========
const submitBooking = async () => {
  if (!editPhone.value.trim()) {
    alert('請輸入聯絡電話')
    return
  }

  // 顯示支付方式選擇 Modal
  showPaymentModal.value = true
}

// ======== 確認支付方式後送出 ========
const confirmPayment = async () => {
  isSubmitting.value = true
  showPaymentModal.value = false

  try {
    const bookingData = {
      petId: mainPet.value?.petId,
      stayRoomTypeId: parseInt(route.params.roomTypeId),
      stayStartDate: checkIn,
      stayEndDate: checkOut,
      petCount: pets.value.length,
      customerNote: customerNote.value,
      extraPetIds: additionalPets.value.map((p) => p.petId),
      roomNo: assignedRoomNo.value,
      memberPhone: editPhone.value,
      paymentMethod: selectedPaymentMethod.value,
    }

    const res = await axios.post('/stay', bookingData) // ✅ 改成 /stay

    if (selectedPaymentMethod.value === 'LINE_PAY') {
      if (res.data.paymentUrl) {
        const currentHost = window.location.origin
        const redirectUrl = `${currentHost}/stay/booking-success`
        const paymentUrlWithRedirect = `${res.data.paymentUrl}&redirectUrl=${encodeURIComponent(redirectUrl)}`

        window.location.href = paymentUrlWithRedirect
      } else {
        router.push({
          path: '/stay/booking-success',
          query: { stayId: res.data.stayId },
        })
      }
    } else {
      router.push({
        path: '/stay/booking-success',
        query: { stayId: res.data.stayId },
      })
    }
  } catch (e) {
    console.error('預約失敗', e)
    alert('預約失敗：' + (e.response?.data?.message || '請稍後重試'))
    isSubmitting.value = false
  }
}

// ======== 生命週期 ========
onMounted(() => {
  fetchRoomType()
  fetchMemberInfo()
  fetchPets()
  assignRoom()
})
</script>

<template>
  <div class="booking-wrap">
    <p class="loading-tip" v-if="!memberInfo || !roomType">載入中...</p>

    <div v-else>
      <!-- 頁首 -->
      <div class="booking-header">
        <h2 class="booking-title">確認預約資料</h2>
        <p class="booking-sub">請確認以下資料後送出預約</p>
      </div>

      <!-- 訂單摘要 -->
      <div class="summary-card">
        <div class="summary-row">
          <span class="summary-label">房型</span>
          <span class="summary-val">{{ roomTypeNames[route.params.roomTypeId] }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">入住</span>
          <span class="summary-val">{{ checkIn }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">退房</span>
          <span class="summary-val">{{ checkOut }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">住宿天數</span>
          <span class="summary-val">{{ stayNights }} 晚</span>
        </div>
        <div class="summary-row total">
          <span class="summary-label">總金額</span>
          <span class="summary-val price">NT$ {{ totalPrice.toLocaleString() }}</span>
        </div>
      </div>

      <!-- 寵物資料 -->
      <div class="section">
        <h3 class="section-title">入住寵物</h3>

        <!-- 主要寵物 -->
        <div v-if="mainPet" class="pet-card main">
          <div class="pet-header">
            <span class="pet-badge primary">主要寵物</span>
            <span class="pet-name">{{ mainPet.petName }}</span>
          </div>
          <div class="pet-details">
            <span>品種：{{ mainPet.breed }}</span>
            <span>類型：{{ mainPet.species }}</span>
            <span>年齡：{{ mainPet.age }} 歲</span>
            <span>體重：{{ mainPet.weight }} kg</span>
          </div>
        </div>

        <!-- 附加寵物提示 -->
        <div v-if="additionalPets.length > 0" class="additional-pets-hint">
          <p>
            <strong>{{ additionalPets.length }} 隻同行寵物</strong>
            的資料將顯示在訂單明細的備註欄
          </p>
        </div>
      </div>

      <!-- 聯絡資料 -->
      <div class="section">
        <h3 class="section-title">聯絡資料</h3>
        <div class="form-group">
          <label>姓名</label>
          <input type="text" :value="memberInfo.memberName" disabled class="form-control" />
        </div>
        <div class="form-group">
          <label>電話</label>
          <input type="tel" v-model="editPhone" placeholder="請輸入電話" class="form-control" />
        </div>
      </div>

      <!-- 備註 -->
      <div class="section">
        <h3 class="section-title">特殊需求備註</h3>
        <textarea
          v-model="customerNote"
          placeholder="有什麼需要告訴我們的嗎？例如：寵物怕生、需要特殊照護..."
          rows="4"
          class="form-control"
        ></textarea>
        <p v-if="additionalPets.length > 0" class="remark-hint">
          💡 第二隻以上的寵物資訊會自動附加至此欄位
        </p>
      </div>

      <!-- 提示文字 -->
      <div class="notice-box">
        <h4>📱 住宿期間追蹤</h4>
        <p>
          毛孩辦理入住後，可在
          <strong>會員中心 → 訂單紀錄 → 寵物住宿</strong> 中查看毛孩房間的即時畫面。
        </p>
      </div>

      <!-- 送出按鈕 -->
      <div class="submit-wrap">
        <button class="btn-back" @click="$router.back()">上一步</button>
        <button class="btn-submit" @click="submitBooking" :disabled="isSubmitting">
          {{ isSubmitting ? '處理中...' : `確認送出預約 NT$ ${totalPrice.toLocaleString()}` }}
        </button>
      </div>
    </div>
  </div>

  <!-- 支付方式選擇 Modal -->
  <div v-if="showPaymentModal" class="modal-overlay">
    <div class="payment-modal">
      <div class="modal-header">
        <h3>選擇支付方式</h3>
        <button class="close-btn" @click="showPaymentModal = false">✕</button>
      </div>

      <div class="modal-body">
        <div class="payment-amount">
          <span>總金額</span>
          <span class="price">NT$ {{ totalPrice.toLocaleString() }}</span>
        </div>

        <div class="payment-methods">
          <label class="payment-option">
            <input type="radio" v-model="selectedPaymentMethod" value="LINE_PAY" />
            <span class="option-content">
              <span class="option-name">LINE PAY</span>
              <span class="option-desc">有任何問題 請先來電詢問 切勿急於下單</span>
            </span>
          </label>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="showPaymentModal = false">取消</button>
        <button class="btn-confirm" @click="confirmPayment" :disabled="isSubmitting">
          {{ isSubmitting ? '處理中...' : '確認支付' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.booking-wrap {
  max-width: 780px;
  margin: 48px auto;
  padding: 0 20px 100px;
  font-family: 'Noto Serif TC', serif;
}

.loading-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

/* ====== 頁首 ====== */
.booking-header {
  text-align: center;
  margin-bottom: 32px;
}

.booking-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #6b4c2a;
  margin: 0 0 8px;
}

.booking-sub {
  font-size: 0.95rem;
  color: #999;
  margin: 0;
}

/* ====== 訂單摘要 ====== */
.summary-card {
  background: linear-gradient(135deg, #f5e6d0 0%, #fdf6ee 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 32px;
  border: 1px solid #ecdfd0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
}

.summary-row.total {
  border-bottom: none;
  padding-top: 16px;
  border-top: 2px solid rgba(0, 0, 0, 0.1);
}

.summary-label {
  color: #999;
  font-size: 0.9rem;
}

.summary-val {
  font-weight: 600;
  color: #6b4c2a;
}

.summary-val.price {
  font-size: 1.4rem;
  color: #c9933a;
}

/* ====== 分區 ====== */
.section {
  background: white;
  border: 1px solid #ecdfd0;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #6b4c2a;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f9f6f0;
}

/* ====== 表單 ====== */
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  color: #6b4c2a;
  margin-bottom: 6px;
}

.form-control {
  width: 100%;
  padding: 12px;
  border: 1px solid #ecdfd0;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  transition: all 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #c9933a;
  box-shadow: 0 0 0 3px rgba(201, 147, 58, 0.1);
}

.form-control:disabled {
  background: #f9f6f0;
  color: #999;
  cursor: not-allowed;
}

/* ====== 房間資訊 ====== */
.room-info {
  background: #f9f6f0;
  border-radius: 8px;
  padding: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.info-label {
  color: #999;
  font-size: 0.9rem;
}

.room-no {
  font-size: 1.4rem;
  font-weight: 700;
  color: #6b4c2a;
  background: white;
  padding: 6px 16px;
  border-radius: 20px;
  border: 2px solid #c9933a;
}

.info-hint {
  margin: 0;
  font-size: 0.85rem;
  color: #666;
}

/* ====== 寵物卡片 ====== */
.pet-card {
  background: #f9f6f0;
  border: 1px solid #ecdfd0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.pet-card.main {
  border: 2px solid #c9933a;
  background: #fffbf5;
}

.pet-photo-wrapper {
  display: none;
}

.pet-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pet-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  background: #e0e0e0;
  color: #666;
}

.pet-badge.primary {
  background: #c9933a;
  color: white;
}

.pet-name {
  font-weight: 700;
  color: #6b4c2a;
  font-size: 1.2rem;
  text-align: center;
}

.pet-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  font-size: 0.85rem;
  color: #666;
  text-align: center;
}

.additional-pets-hint {
  background: #fff9f2;
  border: 1px dashed #c9933a;
  border-radius: 6px;
  padding: 12px;
  margin-top: 12px;
}

.additional-pets-hint p {
  margin: 0;
  font-size: 0.9rem;
  color: #d9860e;
}

.remark-hint {
  margin-top: 8px;
  font-size: 0.85rem;
  color: #d9860e;
}

/* ====== 提示框 ====== */
.notice-box {
  background: #e3f2fd;
  border-left: 4px solid #2196f3;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 24px;
}

.notice-box h4 {
  margin: 0 0 8px;
  color: #1565c0;
  font-size: 0.95rem;
}

.notice-box p {
  margin: 0;
  color: #424242;
  font-size: 0.9rem;
  line-height: 1.5;
}

/* ====== 按鈕 ====== */
.submit-wrap {
  display: flex;
  gap: 12px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 16px 20px;
  border-top: 1px solid #ecdfd0;
  z-index: 100;
  max-width: 100%;
}

@media (max-width: 780px) {
  .submit-wrap {
    max-width: calc(100% - 40px);
    left: 20px;
    right: 20px;
    width: auto;
  }
}

.btn-back,
.btn-submit {
  flex: 1;
  padding: 14px 20px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}

.btn-back {
  background: #e0e0e0;
  color: #666;
}

.btn-back:hover {
  background: #d0d0d0;
}

.btn-submit {
  background: #6b4c2a;
  color: white;
}

.btn-submit:hover:not(:disabled) {
  background: #c9933a;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 76, 42, 0.2);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ====== Modal ====== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.payment-modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 450px;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease-out;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #ecdfd0;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #6b4c2a;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 24px 20px;
}

.payment-amount {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f9f6f0;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  font-size: 0.95rem;
}

.payment-amount .price {
  font-size: 1.3rem;
  font-weight: 700;
  color: #c9933a;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-option {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #ecdfd0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.payment-option:hover {
  border-color: #c9933a;
  background: #f9f6f0;
}

.payment-option input {
  width: 20px;
  height: 20px;
  margin-right: 12px;
  cursor: pointer;
  accent-color: #c9933a;
}

.option-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-name {
  font-weight: 600;
  color: #6b4c2a;
}

.option-desc {
  font-size: 0.8rem;
  color: #999;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #ecdfd0;
  background: #f9f6f0;
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}

.btn-cancel {
  background: white;
  color: #666;
  border: 1px solid #ecdfd0;
}

.btn-cancel:hover {
  background: #f5f5f5;
}

.btn-confirm {
  background: #6b4c2a;
  color: white;
}

.btn-confirm:hover:not(:disabled) {
  background: #c9933a;
  transform: translateY(-2px);
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ====== RWD ====== */
@media (max-width: 600px) {
  .booking-wrap {
    margin: 24px auto;
    padding: 0 16px 120px;
  }

  .booking-title {
    font-size: 1.4rem;
  }

  .summary-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .pet-details {
    grid-template-columns: 1fr;
  }

  .payment-modal {
    width: 95%;
    max-width: none;
  }
}
</style>

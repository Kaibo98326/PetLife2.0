<script setup>
import axios from '@/axios.js'
import { useUserStore } from '@/stores/user'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const userStore = useUserStore()

// 寵物資料
const userPets = ref([])
const selectedPets = ref([])

// Modal 相關
let warningModal = null
const modalTitle = ref('')
const modalMessage = ref('')
const showWarningModal = ref(false)
const currentWarningAction = ref(null)

const route = useRoute()
const router = useRouter()

// 房型資料
const roomType = ref(null)
// 行事曆資料（後端回傳的每日空房陣列）
const calendarDays = ref([])
// 目前顯示的年月
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1) // getMonth() 從 0 開始，+1 才是真實月份
// 使用者選的入住/退房日期
const checkIn = ref(null)
const checkOut = ref(null)
const loading = ref(true)

// 月份名稱對照
const monthNames = [
  '一月',
  '二月',
  '三月',
  '四月',
  '五月',
  '六月',
  '七月',
  '八月',
  '九月',
  '十月',
  '十一月',
  '十二月',
]

// 拿房型資料（顯示名稱、價格用）
const fetchRoomType = async () => {
  try {
    const res = await axios.get(`/stay/roomtype/${route.params.roomTypeId}`)
    roomType.value = res.data
  } catch (e) {
    console.error('房型載入失敗', e)
  }
}

// 拿行事曆資料
const fetchCalendar = async () => {
  loading.value = true
  try {
    const res = await axios.get('/stay/calendar', {
      params: {
        roomTypeId: route.params.roomTypeId,
        year: currentYear.value,
        month: currentMonth.value,
      },
    })
    calendarDays.value = res.data
  } catch (e) {
    console.error('行事曆載入失敗', e)
  } finally {
    loading.value = false
  }
}

// 上個月
const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  // 切換月份後重新拉資料，並清空已選日期
  checkIn.value = null
  checkOut.value = null
  fetchCalendar()
  selectedPets.value = []
}

// 下個月
const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  checkIn.value = null
  checkOut.value = null
  fetchCalendar()
  selectedPets.value = []
}

// 這個月第一天是星期幾（0=日, 1=一 ... 6=六）
// 用來在月曆前面補空白格
const firstDayOfWeek = computed(() => {
  return new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
})

// 把後端回傳的陣列轉成 Map，方便用日期字串快速查詢
// key: '2025-06-01'，value: { availableCount, isAvailable }
const calendarMap = computed(() => {
  const map = {}
  calendarDays.value.forEach((d) => {
    map[d.date] = d
  })
  return map
})

// 組成 yyyy-MM-dd 格式的日期字串
const formatDate = (year, month, day) => {
  const m = String(month).padStart(2, '0')
  const d = String(day).padStart(2, '0')
  return `${year}-${m}-${d}`
}

// 這個月有幾天
const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value, 0).getDate()
})

// 判斷某天是否在選取區間內（入住和退房之間，顯示淡藍底色）
const isInRange = (dateStr) => {
  if (!checkIn.value || !checkOut.value) return false
  return dateStr > checkIn.value && dateStr < checkOut.value
}

// 點擊日期的邏輯
const selectDate = (dateStr, dayData) => {
  if (!dayData || !dayData.isAvailable) return
  if (dateStr < today()) return

  if (!checkIn.value) {
    checkIn.value = dateStr
    checkOut.value = null
  } else if (!checkOut.value && dateStr > checkIn.value) {
    checkOut.value = dateStr

    // ✨ 修改：選完日期後自動 scroll 到寵物區
    nextTick(() => {
      const petSection = document.querySelector('.pet-section')
      if (petSection) {
        petSection.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    })
  } else {
    checkIn.value = dateStr
    checkOut.value = null
  }
}

// 今天的日期字串
const today = () => {
  const t = new Date()
  return formatDate(t.getFullYear(), t.getMonth() + 1, t.getDate())
}

// 計算住幾晚
const stayNights = computed(() => {
  if (!checkIn.value || !checkOut.value) return 0
  const a = new Date(checkIn.value)
  const b = new Date(checkOut.value)
  return Math.round((b - a) / (1000 * 60 * 60 * 24))
})

// 計算總價
const totalPrice = computed(() => {
  if (!roomType.value || stayNights.value === 0) return 0
  return roomType.value.roomPrice * stayNights.value
})

// 房型對應規則
const roomTypePolicies = {
  // 狗狗專用房
  1: { type: 'dog', label: '狗狗專用' },
  2: { type: 'dog', label: '狗狗專用' },
  // 貓貓專用房
  3: { type: 'cat', label: '貓貓專用' },
  4: { type: 'cat', label: '貓貓專用' },
  // 混合房
  5: { type: 'mixed', label: '貓狗混和' },
}

// 獲取已選寵物的類型
const getSelectedPetTypes = () => {
  const types = new Set()
  selectedPets.value.forEach((petId) => {
    const pet = userPets.value.find((p) => p.petId === petId)
    if (pet) {
      types.add(pet.species) // species 直接是「狗」或「貓」
    }
  })
  return types
}

// 顯示警告 Modal
const showPetWarning = (title, message, actionType) => {
  modalTitle.value = title
  modalMessage.value = message
  currentWarningAction.value = actionType
  showWarningModal.value = true
}

// 處理 Modal 的三個選項
const handleContinue = () => {
  showWarningModal.value = false
  goToBooking()
}

const handleRethink = () => {
  showWarningModal.value = false
}

const handleChangeRoom = () => {
  showWarningModal.value = false

  if (currentWarningAction.value?.includes('mixed')) {
    // 前往混合房日曆頁
    router.push(`/stay/5/`)
  } else {
    // 返回選擇房型頁面
    router.push('/stay')
  }
}

// 檢查寵物類型是否符合房型
const validatePetTypeForRoom = () => {
  const currentRoomTypeId = parseInt(route.params.roomTypeId)
  const roomPolicy = roomTypePolicies[currentRoomTypeId]

  // 房型 5 是混合房，不需要檢查
  if (currentRoomTypeId === 5) return true

  const selectedTypes = getSelectedPetTypes()

  // 狗狗專用房 (1, 2)
  if (roomPolicy.type === 'dog') {
    if (selectedTypes.has('貓') && selectedTypes.size === 1) {
      // 只選貓貓
      showPetWarning(
        '目前房型為設計為給狗狗專用',
        '此房型專為狗狗設計，您選擇了貓貓。確定要繼續嗎？',
        'dog-only-cat',
      )
      return false
    } else if (selectedTypes.has('狗') && selectedTypes.has('貓')) {
      // 同時選狗和貓
      showPetWarning(
        '建議選擇貓狗混和房型',
        '您同時選擇了貓貓和狗狗，我們建議選擇貓狗混和房型會更適合喔！',
        'dog-only-mixed',
      )
      return false
    }
  }

  // 貓貓專用房 (3, 4)
  if (roomPolicy.type === 'cat') {
    if (selectedTypes.has('狗') && selectedTypes.size === 1) {
      // 只選狗狗
      showPetWarning(
        '目前房型為設計為給貓貓專用',
        '此房型專為貓貓設計，您選擇了狗狗。確定要繼續嗎？',
        'cat-only-dog',
      )
      return false
    } else if (selectedTypes.has('狗') && selectedTypes.has('貓')) {
      // 同時選狗和貓
      showPetWarning(
        '建議選擇貓狗混和房型',
        '您同時選擇了貓貓和狗狗，我們建議選擇貓狗混和房型會更適合喔！',
        'cat-only-mixed',
      )
      return false
    }
  }

  return true
}

// 前往預約表單（帶入住退房日期）
const goToBooking = () => {
  // 檢查是否有選擇寵物
  if (selectedPets.value.length === 0) {
    showPetWarning('請選擇寵物', '請選擇至少一隻寵物才能繼續預約', 'no-pet')
    return
  }

  if (!checkIn.value || !checkOut.value) return

  // 檢查寵物類型是否符合房型
  if (!validatePetTypeForRoom()) {
    return // 不符合，顯示警告，停止
  }

  router.push({
    path: `/stay/${route.params.roomTypeId}/booking`,
    query: {
      checkIn: checkIn.value,
      checkOut: checkOut.value,
      pets: selectedPets.value.join(','),
    },
  })
}

// 拿用戶的寵物列表
const fetchUserPets = async () => {
  try {
    const res = await axios.get(`/pets/member/${userStore.memberId}`)
    userPets.value = res.data
  } catch (e) {
    console.error('寵物載入失敗', e)
  }
}

// 切換寵物選取
const togglePet = (petId) => {
  const index = selectedPets.value.indexOf(petId)
  if (index > -1) {
    selectedPets.value.splice(index, 1)
  } else {
    selectedPets.value.push(petId)
  }
}

// 判斷寵物是否被選中
const isPetSelected = (petId) => {
  return selectedPets.value.includes(petId)
}

onMounted(() => {
  fetchRoomType()
  fetchCalendar()
  fetchUserPets()
  console.log(userStore.user)
})
</script>

<template>
  <div class="calendar-wrap">
    <!-- 頁首：房型名稱 + 價格 -->
    <div class="cal-header">
      <div class="cal-room-info" v-if="roomType">
        <h2 class="cal-room-name">{{ roomType.roomName }}</h2>
        <span class="cal-room-price">NT$ {{ roomType.roomPrice.toLocaleString() }} / 晚</span>
      </div>
    </div>

    <!-- 月份切換 -->
    <div class="month-nav">
      <button class="nav-btn" @click="prevMonth">&#8249;</button>
      <span class="month-label">{{ currentYear }} 年 {{ monthNames[currentMonth - 1] }}</span>
      <button class="nav-btn" @click="nextMonth">&#8250;</button>
    </div>

    <!-- 星期標題列 -->
    <div class="weekdays">
      <span v-for="d in ['日', '一', '二', '三', '四', '五', '六']" :key="d">{{ d }}</span>
    </div>

    <!-- 月曆格子 -->
    <div class="days-grid" v-if="!loading">
      <!-- 補齊第一週前面的空白格（例如這個月1號是週三，前面要補3格） -->
      <div v-for="n in firstDayOfWeek" :key="'blank-' + n" class="day-cell blank"></div>

      <!-- 每一天的格子 -->
      <div
        v-for="day in daysInMonth"
        :key="day"
        class="day-cell"
        :class="{
          // 沒有空房 → 灰色不可點
          unavailable: !calendarMap[formatDate(currentYear, currentMonth, day)]?.isAvailable,
          // 今天以前 → 灰色不可點
          past: formatDate(currentYear, currentMonth, day) < today(),
          // 選取區間內 → 淡藍底色
          'in-range': isInRange(formatDate(currentYear, currentMonth, day)),
          // 入住日 → 深色標記
          'check-in': formatDate(currentYear, currentMonth, day) === checkIn,
          // 退房日 → 深色標記
          'check-out': formatDate(currentYear, currentMonth, day) === checkOut,
        }"
        @click="
          selectDate(
            formatDate(currentYear, currentMonth, day),
            calendarMap[formatDate(currentYear, currentMonth, day)],
          )
        "
      >
        <span class="day-num">{{ day }}</span>
        <!-- 顯示剩餘間數，沒資料或已滿顯示「已滿」 -->
        <span class="day-avail">
          {{
            calendarMap[formatDate(currentYear, currentMonth, day)]?.isAvailable
              ? `剩 ${calendarMap[formatDate(currentYear, currentMonth, day)].availableCount} 間`
              : '已滿'
          }}
        </span>
      </div>
    </div>

    <!-- 載入中 -->
    <div class="loading-tip" v-else>載入中...</div>

    <!-- 圖例說明 -->
    <div class="legend">
      <span class="legend-item"><span class="dot green"></span>有空房</span>
      <span class="legend-item"><span class="dot gray"></span>已滿 / 不可選</span>
      <span class="legend-item"><span class="dot blue"></span>選取區間</span>
    </div>

    <!-- 選取結果 + 前往預約 -->
    <div class="booking-bar" v-if="checkIn">
      <div class="booking-info">
        <div class="booking-dates">
          <div class="date-block">
            <span class="date-label">入住</span>
            <span class="date-val">{{ checkIn }}</span>
          </div>
          <span class="date-arrow">→</span>
          <div class="date-block">
            <span class="date-label">退房</span>
            <span class="date-val">{{ checkOut ?? '請選擇' }}</span>
          </div>
        </div>
        <div class="booking-calc" v-if="stayNights > 0">
          <span>{{ stayNights }} 晚</span>
          <span class="total-price">NT$ {{ totalPrice.toLocaleString() }}</span>
        </div>
      </div>
      <!-- 入住和退房都選了才能點 -->
      <button
        class="btn-next"
        :disabled="!checkIn || !checkOut || selectedPets.length === 0"
        @click="goToBooking"
      >
        前往預約 ››
      </button>
    </div>
  </div>

  <!-- 寵物選擇區域（只有選好日期才顯示） -->
  <div class="pet-section" v-if="checkIn && checkOut">
    <div class="section-divider"></div>

    <h3 class="section-title">選擇寵物</h3>

    <div class="container-fluid">
      <div class="row justify-content-center" v-if="userPets.length > 0">
        <div v-for="pet in userPets" :key="pet.petId" class="col-auto mb-3">
          <div
            class="pet-card"
            :class="{ selected: isPetSelected(pet.petId) }"
            @click="togglePet(pet.petId)"
          >
            <!-- 寵物照片 -->
            <div class="pet-photo-wrapper">
              <img v-if="pet.petPhoto" :src="pet.petPhoto" :alt="pet.petName" class="pet-photo" />
              <div v-else class="pet-photo-placeholder">🐾</div>
            </div>

            <!-- 勾選符號 -->
            <div class="pet-check">✓</div>

            <!-- 寵物資訊 -->
            <div class="pet-info">
              <span class="pet-name">{{ pet.petName }}</span>
              <span class="pet-type">{{ pet.species }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="row" v-else>
        <div class="col-12 text-center">
          <p class="no-pets">沒有寵物資料</p>
        </div>
      </div>
    </div>

    <div class="text-center mt-3" v-if="selectedPets.length > 0">
      <span class="pet-selected-count"> 已選擇 {{ selectedPets.length }} 隻寵物 </span>
    </div>
  </div>

  <!-- 自訂樣式 Modal -->
  <div v-if="showWarningModal" class="custom-modal-overlay">
    <div class="custom-modal">
      <!-- 警告圖示 -->
      <div class="modal-icon">
        <svg viewBox="0 0 24 24" width="60" height="60">
          <circle cx="12" cy="12" r="11" fill="none" stroke="currentColor" stroke-width="2" />
          <line x1="12" y1="8" x2="12" y2="12" stroke="currentColor" stroke-width="2" />
          <circle cx="12" cy="16" r="1" fill="currentColor" />
        </svg>
      </div>

      <!-- 標題 -->
      <h3 class="modal-title">{{ modalTitle }}</h3>

      <!-- 訊息 -->
      <p class="modal-message">{{ modalMessage }}</p>

      <!-- 按鈕區 -->
      <div class="modal-buttons">
        <button class="btn-cancel" @click="handleRethink">取消</button>
        <button
          class="btn-change"
          @click="handleChangeRoom"
          v-if="currentWarningAction !== 'no-pet'"
        >
          {{ currentWarningAction?.includes('mixed') ? '去混和房型' : '返回選擇房型' }}
        </button>
        <button class="btn-confirm" @click="handleContinue">
          {{ currentWarningAction === 'no-pet' ? '我知道了' : '是的，繼續' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calendar-wrap {
  --brown: #6b4c2a;
  --gold: #c9933a;
  --cream: #fdf6ee;
  --light: #fff9f2;
  max-width: 780px;
  margin: 48px auto;
  padding: 0 20px 120px;
  font-family: 'Noto Serif TC', serif;
}

/* 頁首 */
.cal-header {
  text-align: center;
  margin-bottom: 32px;
}
.cal-room-name {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--brown);
}
.cal-room-price {
  font-size: 1rem;
  color: var(--gold);
  margin-top: 4px;
  display: block;
}

/* 月份切換 */
.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 20px;
}
.month-label {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--brown);
  min-width: 160px;
  text-align: center;
}
.nav-btn {
  background: none;
  border: 1px solid #d4b896;
  color: var(--brown);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 1.4rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.nav-btn:hover {
  background: #f5e6d0;
}

/* 星期標題 */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 0.85rem;
  color: #999;
  margin-bottom: 8px;
  padding: 0 4px;
}

/* 月曆格子 */
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}
.day-cell {
  aspect-ratio: 1;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: white;
  border: 1px solid #ecdfd0;
  transition:
    background 0.15s,
    border-color 0.15s;
  padding: 4px;
}
.day-cell:hover:not(.unavailable):not(.past):not(.blank) {
  background: #f5e6d0;
  border-color: var(--gold);
}
.day-cell.blank {
  background: transparent;
  border: none;
  cursor: default;
}
.day-cell.unavailable,
.day-cell.past {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
  border-color: #eee;
}
.day-cell.in-range {
  background: #dbeafe;
  border-color: #93c5fd;
}
.day-cell.check-in,
.day-cell.check-out {
  background: var(--brown);
  color: white;
  border-color: var(--brown);
}
.day-cell.check-in .day-avail,
.day-cell.check-out .day-avail {
  color: #f5e6d0;
}
.day-num {
  font-size: 0.95rem;
  font-weight: 600;
}
.day-avail {
  font-size: 0.65rem;
  color: #3a8c5c;
  margin-top: 2px;
}
.day-cell.unavailable .day-avail,
.day-cell.past .day-avail {
  color: #ccc;
}

/* 載入 */
.loading-tip {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

/* 圖例 */
.legend {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 16px;
  font-size: 0.8rem;
  color: #888;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot.green {
  background: #3a8c5c;
}
.dot.gray {
  background: #ccc;
}
.dot.blue {
  background: #93c5fd;
}

/* 底部預約 bar */
.booking-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid #ecdfd0;
  padding: 16px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  z-index: 100;
}
.booking-info {
  display: flex;
  align-items: center;
  gap: 32px;
}
.booking-dates {
  display: flex;
  align-items: center;
  gap: 12px;
}
.date-block {
  display: flex;
  flex-direction: column;
}
.date-label {
  font-size: 0.75rem;
  color: #999;
}
.date-val {
  font-size: 1rem;
  font-weight: 700;
  color: var(--brown);
}
.date-arrow {
  color: #ccc;
  font-size: 1.2rem;
}
.booking-calc {
  display: flex;
  flex-direction: column;
  font-size: 0.85rem;
  color: #888;
}
.total-price {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--brown);
}
.btn-next {
  background: var(--brown);
  color: white;
  border: none;
  padding: 14px 40px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}
.btn-next:hover {
  background: var(--gold);
}
.btn-next:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.pet-section {
  margin-top: 32px;
}

.section-divider {
  height: 1px;
  background: #ecdfd0;
  margin: 24px 0;
}

.section-title {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--brown);
  margin-bottom: 24px;
  text-align: center;
}

.pet-card {
  border: 2px solid #ecdfd0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  background: white;
  width: 140px;
  text-align: center;
}

.pet-card:hover {
  border-color: var(--gold);
  background: #fdf6ee;
  transform: translateY(-2px);
}

.pet-card.selected {
  border-color: var(--brown);
  background: var(--light);
  box-shadow: 0 4px 12px rgba(107, 76, 42, 0.15);
}

/* 寵物照片 */
.pet-photo-wrapper {
  width: 100%;
  height: 100px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f6f0;
  border-radius: 8px;
  overflow: hidden;
}

.pet-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pet-photo-placeholder {
  font-size: 2.5rem;
  color: #d4b896;
}

/* 勾選符號 */
.pet-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: var(--brown);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: bold;
  opacity: 0;
  transition: opacity 0.2s;
}

.pet-card.selected .pet-check {
  opacity: 1;
}

/* 寵物資訊 */
.pet-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pet-name {
  display: block;
  font-weight: 700;
  color: var(--brown);
  font-size: 0.95rem;
}

.pet-type {
  display: block;
  font-size: 0.8rem;
  color: #999;
}

.no-pets {
  color: #999;
  padding: 20px;
  margin: 0;
}

.pet-selected-count {
  font-size: 0.95rem;
  color: #3a8c5c;
  font-weight: 600;
}

/* 警告按鈕樣式 */
/* 自訂 Modal 樣式 */
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.custom-modal {
  background: white;
  border-radius: 16px;
  padding: 40px 32px;
  max-width: 380px;
  width: 90%;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  color: #f4a76f;
}

.modal-icon svg {
  width: 100%;
  height: 100%;
}

.modal-title {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--brown);
  margin: 0 0 12px;
  font-family: 'Noto Serif TC', serif;
}

.modal-message {
  font-size: 0.95rem;
  color: #666;
  margin: 0 0 28px;
  line-height: 1.6;
  font-family: 'Noto Serif TC', serif;
}

.modal-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.modal-buttons button {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: 'Noto Serif TC', serif;
  flex: 1;
  min-width: 100px;
}

/* 取消按鈕 */
.btn-cancel {
  background: #e0e0e0;
  color: #666;
}

.btn-cancel:hover {
  background: #d0d0d0;
}

/* 返回選擇房型按鈕 */
.btn-change {
  background: #a0a0a0;
  color: white;
  flex: 0 1 auto;
  min-width: 120px;
}

.btn-change:hover {
  background: #909090;
}

/* 確認按鈕 */
.btn-confirm {
  background: #7366d9;
  color: white;
}

.btn-confirm:hover {
  background: #6355c8;
}

/* 當只有兩個按鈕時 */
@media (max-width: 480px) {
  .modal-buttons {
    flex-direction: column;
  }

  .modal-buttons button {
    width: 100%;
  }
}
</style>

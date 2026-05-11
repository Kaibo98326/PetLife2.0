<script setup>
import axios from '@/axios.js'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

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
  // 沒有空房的日期不能點
  if (!dayData || !dayData.isAvailable) return
  // 今天以前的日期不能點
  if (dateStr < today()) return

  if (!checkIn.value) {
    // 第一次點 → 設為入住日
    checkIn.value = dateStr
    checkOut.value = null
  } else if (!checkOut.value && dateStr > checkIn.value) {
    // 第二次點，且比入住日晚 → 設為退房日
    checkOut.value = dateStr
  } else {
    // 其他情況（點了比入住日早、或已選完要重選）→ 重新選入住日
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

// 前往預約表單（帶入住退房日期）
const goToBooking = () => {
  if (!checkIn.value || !checkOut.value) return
  router.push({
    path: `/stay/${route.params.roomTypeId}/booking`,
    query: {
      checkIn: checkIn.value,
      checkOut: checkOut.value,
    },
  })
}

onMounted(() => {
  fetchRoomType()
  fetchCalendar()
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
      <button class="btn-next" :disabled="!checkIn || !checkOut" @click="goToBooking">
        前往預約 ››
      </button>
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
</style>

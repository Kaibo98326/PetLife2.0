<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const items = ref([])
const pets = ref([])
const groomers = ref([])
const slots = ref([])
const loading = ref(false)
const groomerLoading = ref(false)
const slotLoading = ref(false)
const submitting = ref(false)

const selectedBeautyIds = ref([])
const selectedPetId = ref('')
const selectedGroomerId = ref('')
const appointDate = ref('')
const selectedSlotId = ref('')
const contactNote = ref('')
const calendarMonth = ref(new Date())

const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六']

const toDateInputValue = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseDateValue = value => {
  if (!value) return null
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const addDays = (date, amount) => {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  return next
}

const startOfMonth = date => new Date(date.getFullYear(), date.getMonth(), 1)

const today = toDateInputValue(new Date())

const calendarMonthLabel = computed(() => {
  const value = calendarMonth.value
  return `${value.getFullYear()} 年 ${value.getMonth() + 1} 月`
})

const selectedDateLabel = computed(() => {
  const value = parseDateValue(appointDate.value)
  if (!value) return '尚未選擇日期'
  return `${value.getMonth() + 1} 月 ${value.getDate()} 日 週${weekdayLabels[value.getDay()]}`
})

const quickDateOptions = computed(() => {
  const base = parseDateValue(today)
  return Array.from({ length: 7 }, (_, index) => {
    const date = addDays(base, index)
    const label = index === 0 ? '今天' : index === 1 ? '明天' : `週${weekdayLabels[date.getDay()]}`
    return {
      label,
      dateText: `${date.getMonth() + 1}/${date.getDate()}`,
      value: toDateInputValue(date),
    }
  })
})

const calendarWeeks = computed(() => {
  const firstDay = startOfMonth(calendarMonth.value)
  const start = addDays(firstDay, -firstDay.getDay())
  const days = Array.from({ length: 42 }, (_, index) => {
    const date = addDays(start, index)
    const value = toDateInputValue(date)
    return {
      value,
      day: date.getDate(),
      isCurrentMonth: date.getMonth() === calendarMonth.value.getMonth(),
      isToday: value === today,
      isSelected: value === appointDate.value,
      isPast: value < today,
    }
  })

  return Array.from({ length: 6 }, (_, index) => days.slice(index * 7, index * 7 + 7))
})

const selectAppointmentDate = value => {
  if (value < today) return
  appointDate.value = value
}

const changeCalendarMonth = amount => {
  const next = new Date(calendarMonth.value)
  next.setMonth(next.getMonth() + amount, 1)
  calendarMonth.value = next
}

const showCurrentMonth = () => {
  calendarMonth.value = startOfMonth(new Date())
  appointDate.value = today
}

const selectedItems = computed(() => {
  const ids = new Set(selectedBeautyIds.value.map(Number))
  return items.value.filter(item => ids.has(item.beautyId))
})

const selectedPet = computed(() => {
  return pets.value.find(pet => Number(pet.petId) === Number(selectedPetId.value)) || null
})

const selectedPetSize = computed(() => {
  const weight = Number(selectedPet.value?.weight)
  if (!selectedPet.value || selectedPet.value.weight === null || selectedPet.value.weight === undefined || Number.isNaN(weight)) {
    return ''
  }
  if (weight <= 10) return '小型'
  if (weight <= 20) return '中型'
  return '大型'
})

const selectedGroomer = computed(() => {
  return groomers.value.find(groomer => Number(groomer.groomerId) === Number(selectedGroomerId.value)) || null
})

const selectedSlot = computed(() => {
  return slots.value.find(slot => Number(slot.slotId) === Number(selectedSlotId.value)) || null
})

const totalMinutes = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + Number(item.durationSlots || 0) * 30, 0)
})

const selectedItemPrice = item => {
  if (!selectedPetSize.value) return null
  const price = item.prices?.find(row => row.petSize === selectedPetSize.value)
  return price?.itemPrice ?? null
}

const estimatedTotalAmount = computed(() => {
  if (!selectedPetSize.value || selectedItems.value.length === 0) return null

  const prices = selectedItems.value.map(item => selectedItemPrice(item))
  if (prices.some(price => price === null)) return null

  return prices.reduce((sum, price) => sum + Number(price || 0), 0)
})

const canLoadGroomers = computed(() => selectedBeautyIds.value.length > 0 && appointDate.value)
const canLoadSlots = computed(() => selectedGroomerId.value && canLoadGroomers.value)
const canSubmit = computed(() => {
  return selectedPetId.value && selectedGroomerId.value && appointDate.value && selectedSlotId.value
      && selectedBeautyIds.value.length > 0
})

const IMG_BASE = 'http://localhost:8082'

const beautyImagePositionMap = {
  '膠原蛋白酵素養護': 'center 30%',
}

const formatMoney = value => `$${Number(value || 0).toLocaleString()}`

const normalizeImageUrl = imageUrl => {
  if (!imageUrl) return `${IMG_BASE}/images/beauty/default.jpg`
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  return imageUrl.startsWith('/') ? `${IMG_BASE}${imageUrl}` : `${IMG_BASE}/${imageUrl}`
}

const beautyImageUrl = item => normalizeImageUrl(item.imageUrl)
const beautyImagePosition = item => beautyImagePositionMap[item.itemName?.trim()] || 'center center'

const itemPriceText = item => {
  const prices = item.prices || []
  if (prices.length === 0) return '依寵物資料計價'

  const amounts = prices.map(price => Number(price.itemPrice || 0))
  const min = Math.min(...amounts)
  const max = Math.max(...amounts)
  return min === max ? formatMoney(min) : `${formatMoney(min)} - ${formatMoney(max)}`
}

const beautyParam = () => selectedBeautyIds.value.join(',')

const loadItems = async () => {
  const res = await axios.get('/beauty/items')
  items.value = res.data || []

  const initialBeautyId = Number(route.query.beautyId)
  if (initialBeautyId && items.value.some(item => item.beautyId === initialBeautyId)) {
    selectedBeautyIds.value = [initialBeautyId]
  }
}

const loadPets = async () => {
  if (!userStore.token) return
  const res = await axios.get(`/pets/member/${userStore.memberId}`)
  pets.value = res.data || []
}

const loadBaseData = async () => {
  if (!userStore.token) {
    await Swal.fire('請先登入', '登入會員後才能建立美容預約', 'warning')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    await Promise.all([loadItems(), loadPets()])
  } catch (err) {
    console.log(err)
    Swal.fire('讀取失敗', err.response?.data?.message || '預約資料讀取失敗', 'error')
  } finally {
    loading.value = false
  }
}

const loadGroomers = async () => {
  selectedGroomerId.value = ''
  selectedSlotId.value = ''
  groomers.value = []
  slots.value = []

  if (!canLoadGroomers.value) return

  groomerLoading.value = true
  try {
    const res = await axios.get('/beauty/groomers', {
      params: {
        beautyIds: beautyParam(),
        date: appointDate.value,
      },
    })
    groomers.value = res.data || []
  } catch (err) {
    console.log(err)
    Swal.fire('讀取失敗', err.response?.data?.message || '美容師資料讀取失敗', 'error')
  } finally {
    groomerLoading.value = false
  }
}

const loadSlots = async () => {
  selectedSlotId.value = ''
  slots.value = []

  if (!canLoadSlots.value) return

  slotLoading.value = true
  try {
    const res = await axios.get('/beauty/available-slots', {
      params: {
        groomerId: selectedGroomerId.value,
        date: appointDate.value,
        beautyIds: beautyParam(),
      },
    })
    slots.value = res.data || []
  } catch (err) {
    console.log(err)
    Swal.fire('讀取失敗', err.response?.data?.message || '可預約時段讀取失敗', 'error')
  } finally {
    slotLoading.value = false
  }
}

const cancelBooking = async () => {
  const result = await Swal.fire({
    title: '取消本次預約填寫？',
    text: '目前填寫的內容不會保留',
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '確定取消',
    cancelButtonText: '繼續填寫',
    confirmButtonColor: '#d33',
  })

  if (result.isConfirmed) {
    router.push('/beauty-booking')
  }
}

const submitBooking = async () => {
  if (!canSubmit.value) {
    Swal.fire('資料未完成', '請完成寵物、服務、美容師、日期與時段選擇', 'info')
    return
  }

  const result = await Swal.fire({
    title: '確認送出預約？',
    text: '送出後系統會依寵物重量計算體型與價格',
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '送出預約',
    cancelButtonText: '再檢查一下',
    confirmButtonColor: '#e67e22',
  })

  if (!result.isConfirmed) return

  submitting.value = true
  try {
    const res = await axios.post('/beauty/appointments', {
      petId: Number(selectedPetId.value),
      groomerId: Number(selectedGroomerId.value),
      appointDate: appointDate.value,
      startSlotId: Number(selectedSlotId.value),
      beautyIds: selectedBeautyIds.value.map(Number),
      contactNote: contactNote.value,
    })

    await Swal.fire('預約送出成功', '已建立美容預約明細', 'success')
    router.push({
      name: 'BeautyAppointmentDetail',
      params: { appointmentId: res.data.appointmentId },
    })
  } catch (err) {
    console.log(err)
    Swal.fire('預約失敗', err.response?.data?.message || '預約建立失敗，請稍後再試', 'error')
  } finally {
    submitting.value = false
  }
}

watch([selectedBeautyIds, appointDate], loadGroomers, { deep: true })
watch(selectedGroomerId, loadSlots)
watch(appointDate, value => {
  const selected = parseDateValue(value)
  if (selected) calendarMonth.value = startOfMonth(selected)
})

onMounted(loadBaseData)
</script>

<template>
  <main class="booking-page container py-4">
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-warning" role="status"></div>
    </div>

    <div v-else class="row g-4">
      <section class="col-lg-8">
        <div class="booking-panel">
          <h3>1. 選擇寵物</h3>
          <select v-model="selectedPetId" class="form-select">
            <option value="" disabled>請選擇寵物</option>
            <option v-for="pet in pets" :key="pet.petId" :value="pet.petId">
              {{ pet.petName }} / {{ pet.weight ?? '未填體重' }} kg
            </option>
          </select>
          <p class="form-hint">預約價格會由後端依寵物體重自動計算，前台不需手動選擇體型。</p>

          <h3>2. 選擇美容項目</h3>
          <div class="item-check-list">
            <label v-for="item in items" :key="item.beautyId" class="item-check">
              <input v-model="selectedBeautyIds" type="checkbox" :value="item.beautyId" />
              <img
                class="item-check-image"
                :src="beautyImageUrl(item)"
                :alt="item.itemName"
                :style="{ objectPosition: beautyImagePosition(item) }"
              />
              <span>
                <strong>{{ item.itemName }}</strong>
                <small>{{ Number(item.durationSlots || 0) * 30 }} 分鐘 / {{ itemPriceText(item) }}</small>
              </span>
            </label>
          </div>

          <h3>3. 選擇日期</h3>
          <div class="booking-date-picker">
            <div class="date-picker-summary">
              <span>預約日期</span>
              <strong>{{ selectedDateLabel }}</strong>
            </div>

            <div class="quick-date-list" aria-label="快速選擇預約日期">
              <button
                v-for="option in quickDateOptions"
                :key="option.value"
                type="button"
                class="quick-date-btn"
                :class="{ active: appointDate === option.value }"
                @click="selectAppointmentDate(option.value)"
              >
                <span>{{ option.label }}</span>
                <strong>{{ option.dateText }}</strong>
              </button>
            </div>

            <div class="calendar-box">
              <div class="calendar-toolbar">
                <button type="button" class="calendar-nav-btn" aria-label="上一個月" @click="changeCalendarMonth(-1)">
                  ‹
                </button>
                <strong>{{ calendarMonthLabel }}</strong>
                <div class="calendar-toolbar-actions">
                  <button type="button" class="calendar-today-btn" @click="showCurrentMonth">今天</button>
                  <button type="button" class="calendar-nav-btn" aria-label="下一個月" @click="changeCalendarMonth(1)">
                    ›
                  </button>
                </div>
              </div>

              <div class="calendar-weekdays">
                <span v-for="label in weekdayLabels" :key="label">{{ label }}</span>
              </div>

              <div class="calendar-grid">
                <template v-for="(week, weekIndex) in calendarWeeks" :key="weekIndex">
                  <button
                    v-for="day in week"
                    :key="day.value"
                    type="button"
                    class="calendar-day-btn"
                    :class="{
                      muted: !day.isCurrentMonth,
                      today: day.isToday,
                      active: day.isSelected,
                      disabled: day.isPast,
                    }"
                    :disabled="day.isPast"
                    @click="selectAppointmentDate(day.value)"
                  >
                    <span>{{ day.day }}</span>
                  </button>
                </template>
              </div>
            </div>
          </div>

          <h3>4. 選擇美容師</h3>
          <div v-if="groomerLoading" class="form-hint">美容師讀取中...</div>
          <select v-else v-model="selectedGroomerId" class="form-select" :disabled="!canLoadGroomers">
            <option value="" disabled>{{ canLoadGroomers ? '請選擇美容師' : '請先選擇項目與日期' }}</option>
            <option v-for="groomer in groomers" :key="groomer.groomerId" :value="groomer.groomerId">
              {{ groomer.displayName || `美容師 ${groomer.groomerId}` }}
            </option>
          </select>

          <h3>5. 選擇可預約時段</h3>
          <div v-if="slotLoading" class="form-hint">時段讀取中...</div>
          <div v-else-if="!canLoadSlots" class="empty-slots">請先完成項目、日期與美容師選擇</div>
          <div v-else-if="slots.length === 0" class="empty-slots">目前沒有可預約時段</div>
          <div v-else class="slot-grid">
            <button
              v-for="slot in slots"
              :key="slot.slotId"
              type="button"
              class="slot-btn"
              :class="{ active: Number(selectedSlotId) === slot.slotId }"
              @click="selectedSlotId = slot.slotId"
            >
              {{ slot.startTime }}
            </button>
          </div>

          <h3>6. 備註</h3>
          <textarea
            v-model="contactNote"
            rows="3"
            class="form-control"
            placeholder="可填寫寵物狀況或希望店家注意的事項"
          ></textarea>
        </div>
      </section>

      <aside class="col-lg-4 booking-summary-col">
        <div class="booking-summary">
          <h3>預約摘要</h3>

          <div class="summary-section">
            <div class="summary-field">
              <span>寵物</span>
              <strong>{{ selectedPet?.petName || '尚未選擇' }}</strong>
            </div>
            <div class="summary-field">
              <span>體型</span>
              <strong>{{ selectedPetSize || '依體重判斷' }}</strong>
            </div>
            <div class="summary-field">
              <span>日期</span>
              <strong>{{ appointDate || '尚未選擇' }}</strong>
            </div>
            <div class="summary-field">
              <span>美容師</span>
              <strong>{{ selectedGroomer?.displayName || (selectedGroomerId ? `美容師 ${selectedGroomerId}` : '尚未選擇') }}</strong>
            </div>
            <div class="summary-field">
              <span>預約時段</span>
              <strong>
                <template v-if="selectedSlot">
                  {{ selectedSlot.startTime }}
                </template>
                <template v-else>尚未選擇</template>
              </strong>
            </div>
          </div>

          <div class="summary-section">
            <div class="summary-section-title">美容項目</div>
            <div v-if="selectedItems.length === 0" class="summary-empty">尚未選擇美容項目</div>
            <div v-for="item in selectedItems" :key="item.beautyId" class="summary-line">
              <span>{{ item.itemName }}</span>
              <small>
                {{ Number(item.durationSlots || 0) * 30 }} 分鐘
                <template v-if="selectedItemPrice(item) !== null">
                  / {{ formatMoney(selectedItemPrice(item)) }}
                </template>
              </small>
            </div>
          </div>

          <div class="summary-total">
            <span>預估服務時間</span>
            <strong>{{ totalMinutes || 0 }} 分鐘</strong>
          </div>
          <div class="summary-total">
            <span>預估金額</span>
            <strong>{{ estimatedTotalAmount === null ? '待確認' : formatMoney(estimatedTotalAmount) }}</strong>
          </div>
          <div class="summary-note">實際金額由系統依寵物重量與服務價格快照計算。</div>
          <div class="summary-actions">
            <button class="btn btn-warning w-100" :disabled="submitting" @click="submitBooking">
              {{ submitting ? '送出中...' : '送出預約' }}
            </button>
            <button class="btn btn-outline-secondary w-100" @click="cancelBooking">
              取消
            </button>
          </div>
        </div>
      </aside>
    </div>
  </main>
</template>

<style scoped>
.booking-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  color: #4f4037;
}

.booking-heading h2 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.booking-heading p {
  margin: 0;
  color: #776b64;
}

.booking-panel,
.booking-summary {
  padding: 22px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(82, 60, 42, 0.08);
}

.booking-panel h3,
.booking-summary h3 {
  margin: 22px 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: #4f4037;
}

.booking-panel h3:first-child,
.booking-summary h3:first-child {
  margin-top: 0;
}

.form-hint {
  margin-top: 8px;
  color: #8a7b72;
  font-size: 14px;
}

.item-check-list {
  display: grid;
  gap: 10px;
}

.item-check {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border: 1px solid #eee3da;
  border-radius: 8px;
  cursor: pointer;
}

.item-check-image {
  width: 72px;
  height: 56px;
  flex: 0 0 72px;
  object-fit: cover;
  border-radius: 8px;
  background: #f8f2ec;
}

.item-check small {
  display: block;
  color: #7c6d64;
}

.booking-date-picker {
  display: grid;
  gap: 14px;
}

.date-picker-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fffaf4;
  color: #806f65;
}

.date-picker-summary strong {
  color: #4f4037;
  font-size: 18px;
  text-align: right;
}

.quick-date-list {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.quick-date-btn,
.calendar-day-btn,
.calendar-nav-btn,
.calendar-today-btn {
  border: 0;
  background: transparent;
  font: inherit;
}

.quick-date-btn {
  display: grid;
  gap: 4px;
  min-height: 72px;
  padding: 10px 6px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  color: #806f65;
}

.quick-date-btn strong {
  color: #4f4037;
  font-size: 18px;
}

.quick-date-btn:hover,
.calendar-day-btn:not(:disabled):hover {
  border-color: #e8a94f;
  background: #fff7e8;
}

.quick-date-btn.active,
.calendar-day-btn.active {
  border-color: #e8a94f;
  background: #fff0d3;
  color: #4f4037;
  box-shadow: 0 0 0 3px rgba(232, 169, 79, 0.18);
}

.calendar-box {
  padding: 16px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fffdf9;
}

.calendar-toolbar,
.calendar-toolbar-actions {
  display: flex;
  align-items: center;
}

.calendar-toolbar {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: #4f4037;
}

.calendar-toolbar-actions {
  gap: 8px;
}

.calendar-nav-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  color: #5a4a42;
  font-size: 24px;
  line-height: 1;
}

.calendar-today-btn {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  color: #806f65;
}

.calendar-weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.calendar-weekdays {
  margin-bottom: 8px;
  color: #9a8b82;
  font-size: 13px;
  text-align: center;
}

.calendar-day-btn {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 44px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #fff;
  color: #4f4037;
}

.calendar-day-btn.muted {
  color: #b9ada6;
  background: #fbf7f2;
}

.calendar-day-btn.today::after {
  position: absolute;
  bottom: 6px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #e8a94f;
  content: "";
}

.calendar-day-btn.disabled {
  color: #cfc6bf;
  background: #f7f1ec;
  cursor: not-allowed;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 10px;
}

.slot-btn {
  min-height: 64px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fffaf4;
  color: #5a4a42;
}

.slot-btn.active {
  border-color: #e8a94f;
  background: #fff0d3;
  font-weight: 700;
}

.slot-btn small {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}

.empty-slots {
  padding: 18px;
  border-radius: 8px;
  background: #f8f2ec;
  color: #7d6e65;
  text-align: center;
}

.booking-summary {
  width: 100%;
  z-index: 20;
  max-height: calc(100vh - 170px);
  overflow-y: auto;
}

.booking-summary-col {
  align-self: flex-start;
}

@media (min-width: 992px) {
  .booking-summary {
    position: fixed;
    top: 150px;
    right: 52px;
    width: min(420px, max(320px, calc((100vw - 104px) / 3)));
  }
}

.summary-section {
  padding: 12px 0;
  border-bottom: 1px solid #f0e7df;
}

.summary-section:first-of-type {
  padding-top: 0;
}

.summary-section-title {
  margin-bottom: 8px;
  color: #4f4037;
  font-weight: 700;
}

.summary-field {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  color: #806f65;
}

.summary-field strong {
  max-width: 62%;
  color: #4f4037;
  text-align: right;
  word-break: break-word;
}

.summary-empty {
  padding: 8px 0;
  color: #8a7b72;
  font-size: 14px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0e7df;
}

.summary-line:last-child {
  border-bottom: 0;
}

.summary-line span {
  color: #4f4037;
  font-weight: 600;
}

.summary-line small {
  color: #806f65;
  text-align: right;
}

.summary-total {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eadfd6;
}

.summary-note {
  margin: 14px 0;
  color: #806f65;
  font-size: 14px;
}

.summary-actions {
  display: grid;
  gap: 10px;
}

@media (max-width: 768px) {
  .booking-heading {
    flex-direction: column;
  }

  .date-picker-summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .date-picker-summary strong {
    text-align: left;
  }

  .quick-date-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .calendar-box {
    padding: 12px;
  }

  .calendar-day-btn {
    min-height: 40px;
  }

  .booking-summary {
    position: static;
    width: 100%;
    max-height: none;
    overflow: visible;
  }
}
</style>

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

const today = new Date().toISOString().slice(0, 10)

const selectedItems = computed(() => {
  const ids = new Set(selectedBeautyIds.value.map(Number))
  return items.value.filter(item => ids.has(item.beautyId))
})

const totalMinutes = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + Number(item.durationSlots || 0) * 30, 0)
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

onMounted(loadBaseData)
</script>

<template>
  <main class="booking-page container py-4">
    <div class="booking-heading">
      <div>
        <h2>填寫美容預約</h2>
        <p>選擇寵物、服務、美容師與日期後，系統會顯示真正可預約時段。</p>
      </div>
      <button class="btn btn-outline-secondary" @click="router.push('/beauty-booking')">
        返回項目
      </button>
    </div>

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
          <input v-model="appointDate" type="date" class="form-control" :min="today" />

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
              {{ slot.slotName }}
              <small>{{ slot.startTime }} - {{ slot.endTime }}</small>
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

      <aside class="col-lg-4">
        <div class="booking-summary">
          <h3>預約摘要</h3>
          <div v-if="selectedItems.length === 0" class="text-muted">尚未選擇美容項目</div>
          <div v-for="item in selectedItems" :key="item.beautyId" class="summary-line">
            <span>{{ item.itemName }}</span>
            <small>{{ Number(item.durationSlots || 0) * 30 }} 分鐘</small>
          </div>
          <div class="summary-total">
            <span>預估服務時間</span>
            <strong>{{ totalMinutes || 0 }} 分鐘</strong>
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
  position: sticky;
  top: 120px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0e7df;
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

  .booking-summary {
    position: static;
  }
}
</style>

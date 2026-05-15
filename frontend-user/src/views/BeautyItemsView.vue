<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/axios.js'
import Swal from 'sweetalert2'

const router = useRouter()
const items = ref([])
const loading = ref(false)

const activeItems = computed(() => items.value.filter(item => item.isActive !== false))

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

const priceText = item => {
  const prices = item.prices || []
  if (prices.length === 0) return '依寵物資料計價'

  const amounts = prices.map(price => Number(price.itemPrice || 0))
  const min = Math.min(...amounts)
  const max = Math.max(...amounts)
  return min === max ? formatMoney(min) : `${formatMoney(min)} - ${formatMoney(max)}`
}

const durationText = item => {
  const minutes = Number(item.durationSlots || 0) * 30
  return minutes > 0 ? `${minutes} 分鐘` : '依現場評估'
}

const loadItems = async () => {
  loading.value = true
  try {
    const res = await axios.get('/beauty/items')
    items.value = res.data || []
  } catch (err) {
    console.log(err)
    Swal.fire('讀取失敗', err.response?.data?.message || '美容項目讀取失敗', 'error')
  } finally {
    loading.value = false
  }
}

const reserveItem = item => {
  router.push({
    name: 'BeautyBooking',
    query: { beautyId: item.beautyId },
  })
}

onMounted(loadItems)
</script>

<template>
  <main class="beauty-page container py-4">
    <div class="beauty-heading">
      <div>
        <h2>服務項目</h2>
      </div>
      <button class="btn btn-outline-secondary" @click="loadItems">
        <i class="fas fa-rotate-right me-1"></i>重新整理
      </button>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-warning" role="status"></div>
    </div>

    <div v-else-if="activeItems.length === 0" class="text-center py-5 text-muted">
      目前沒有可預約的美容項目
    </div>

    <div v-else class="row g-3">
      <div v-for="item in activeItems" :key="item.beautyId" class="col-md-6 col-xl-4">
        <article class="beauty-item-card">
          <img
            class="beauty-item-image"
            :src="beautyImageUrl(item)"
            :alt="item.itemName"
            :style="{ objectPosition: beautyImagePosition(item) }"
          />
          <div class="beauty-item-body">
            <h3>{{ item.itemName }}</h3>
            <p>{{ item.itemDescription || '專業美容師依照寵物狀況提供服務。' }}</p>
            <div class="beauty-meta">
              <span><i class="far fa-clock me-1"></i>{{ durationText(item) }}</span>
              <span><i class="fas fa-dollar-sign me-1"></i>{{ priceText(item) }}</span>
            </div>
          </div>
          <button class="btn btn-warning w-100" @click="reserveItem(item)">
            立即預約
          </button>
        </article>
      </div>
    </div>

    <aside class="booking-note">
      <strong>預約提醒</strong>
      <span>價格與體型由系統依寵物重量計算，實際可預約時段會在預約頁選完寵物、美容師與日期後顯示。</span>
    </aside>
  </main>
</template>

<style scoped>
.beauty-page {
  color: #4f4037;
}

.beauty-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.beauty-heading h2 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.beauty-heading p {
  margin: 0;
  color: #776b64;
}

.beauty-item-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(82, 60, 42, 0.08);
}

.beauty-item-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: 8px;
  background: #f8f2ec;
}

.beauty-item-body {
  flex: 1;
}

.beauty-item-body h3 {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
}

.beauty-item-body p {
  min-height: 48px;
  margin: 0 0 14px;
  color: #6f625c;
  line-height: 1.6;
}

.beauty-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.beauty-meta span {
  padding: 6px 10px;
  border-radius: 8px;
  background: #f8f2ec;
  font-size: 14px;
}

.booking-note {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: min(320px, calc(100vw - 48px));
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #eadfd6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(82, 60, 42, 0.16);
  color: #5a4a42;
  font-size: 14px;
}

@media (max-width: 768px) {
  .beauty-heading {
    flex-direction: column;
  }

  .booking-note {
    position: static;
    width: 100%;
    margin-top: 20px;
  }
}
</style>

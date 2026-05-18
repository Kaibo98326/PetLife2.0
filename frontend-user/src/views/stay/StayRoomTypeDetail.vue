<script setup>
import axios from '@/axios.js'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { Modal } from 'bootstrap'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
let loginModal = null

const route = useRoute()
console.log('params:', route.params)
const router = useRouter()

const roomType = ref(null)
const loading = ref(true)

// 住宿流程
const flowSteps = [
  {
    icon: '📅',
    title: '選擇住宿時間',
    desc: '選擇入住與退房日期，系統顯示可預約房型與數量',
  },
  {
    icon: '💳',
    title: '確認付款方式',
    desc: '提供線上刷卡與現金預約兩種方式',
  },
  {
    icon: '✅',
    title: '預約成立',
    desc: '完成確認後收到預約確認信或簡訊，內含預約編號與住宿資訊',
  },
  {
    icon: '📞',
    title: '專員聯繫確認',
    desc: '選擇現金預約者，專員將透過簡訊或電話再次確認預約細節',
  },
  {
    icon: '🐾',
    title: '準時送毛孩來',
    desc: '入住當天請攜帶狂犬病疫苗證明等健康文件辦理入住',
  },
]
// 住宿公約
const rules = [
  {
    icon: '🕒',
    title: '入住與退房時間',
    desc: '入住時間：下午 3:00 後。退房時間：中午 12:00 前。逾時將加收費用。',
  },
  {
    icon: '💉',
    title: '健康證明必備',
    desc: '所有毛孩需提供一年內有效之狂犬病疫苗及必要疫苗證明，無法提供者恕不接受入住。',
  },
  {
    icon: '🍖',
    title: '飲食與個人用品',
    desc: '建議自備毛孩慣用飼料、零食、玩具及睡墊，以減少適應新環境的不安。',
  },
  {
    icon: '🐕',
    title: '行為管理責任',
    desc: '請確保毛孩無攻擊性或過度吠叫行為，若造成困擾或傷害，飼主需負全責。',
  },
  {
    icon: '🌿',
    title: '環境維護',
    desc: '外出散步請繫牽繩並清理排泄物。若毛孩造成設施損壞，飼主需照價賠償。',
  },
  {
    icon: '⚠️',
    title: '責任歸屬',
    desc: '非因旅館疏失所致之財物損失或意外傷害，旅館不負賠償責任，請飼主知悉。',
  },
]

const fetchRoomType = async () => {
  try {
    const res = await axios.get(`/stay/roomtype/${route.params.roomTypeId}`)
    roomType.value = res.data
  } catch (e) {
    console.error('載入失敗', e)
  } finally {
    loading.value = false
  }
}

const envImages = computed(() => {
  if (!roomType.value) return []

  const start = (roomType.value.roomTypeId - 1) * 4 + 1

  return [0, 1, 2, 3].map((i) => `/src/assets/images/stay_room_env${start + i}.jpg`)
})

// 回首頁
const goToStayList = () => {
  router.push({ name: 'StayRoomTypeList' })
}

//日歷邏輯
// goToCalendar 改成用 memberId 判斷，不需要再呼叫 initFromLocalStorage
const goToCalendar = () => {
  if (!userStore.memberId) {
    // 第一次進來才 new Modal，之後重複用同一個實例
    if (!loginModal) {
      loginModal = new Modal(document.getElementById('loginModal'))
    }
    loginModal.show()
    return
  }
  router.push(`/stay/${route.params.roomTypeId}/calendar`)
}

// 跳轉登入
const goToLogin = () => {
  loginModal.hide()
  setTimeout(() => {
    router.push('/login')
  }, 300)
}

onMounted(() => {
  userStore.initFromLocalStorage() // 頁面載入時就還原登入狀態
  fetchRoomType()
})
</script>
<template>
  <div class="detail-wrap" v-if="!loading && roomType">
    <!-- Banner -->
    <section
      class="banner"
      :style="{
        backgroundImage: `url('/src/assets/images/stay_room${roomType.roomTypeId}.jpg')`,
      }"
    >
      <div class="banner-overlay"></div>
      <div class="banner-text">
        <p class="banner-sub">PetLife 寵物旅館</p>
        <h1 class="banner-title">{{ roomType.roomName }}</h1>
      </div>
    </section>

    <!--  房型介紹與價格 -->
    <section class="intro-section">
      <div class="intro-grid">
        <div class="intro-desc">
          <h2 class="section-title">房型介紹</h2>
          <p class="desc-text">{{ roomType.roomDescription }}</p>
        </div>
        <div class="intro-price-box">
          <div class="price-item">
            <span class="price-label">每晚價格</span>
            <span class="price-value">NT$ {{ roomType.roomPrice.toLocaleString() }}</span>
          </div>
          <div class="price-item">
            <span class="price-label">容納寵物數</span>
            <span class="price-value">{{ roomType.capacity }} 隻</span>
          </div>
          <div class="price-item">
            <span class="price-label">房間數量</span>
            <span class="price-value available">{{ roomType.availableCount }} 間</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 環境圖片 -->
    <section class="env-section">
      <h2 class="section-title center">房型環境</h2>
      <div class="env-grid">
        <div v-for="(img, idx) in envImages" :key="idx" class="env-img-wrap">
          <img :src="img" :alt="`環境圖片 ${idx + 1}`" class="env-img" />
        </div>
      </div>
    </section>

    <!-- 預約流程 -->
    <section class="flow-section">
      <h2 class="section-title center">預約流程</h2>
      <div class="flow-steps">
        <div class="flow-step" v-for="(step, i) in flowSteps" :key="i">
          <div class="step-icon">{{ step.icon }}</div>
          <div class="step-num">0{{ i + 1 }}</div>
          <div class="step-title">{{ step.title }}</div>
          <div class="step-desc">{{ step.desc }}</div>
          <div class="step-arrow" v-if="i < flowSteps.length - 1">›</div>
        </div>
      </div>
    </section>

    <!-- 住宿公約 -->
    <section class="rule-section">
      <h2 class="section-title center">住宿公約</h2>
      <div class="rule-grid">
        <div class="rule-item" v-for="(rule, i) in rules" :key="i">
          <span class="rule-icon">{{ rule.icon }}</span>
          <div>
            <div class="rule-title">{{ rule.title }}</div>
            <div class="rule-desc">{{ rule.desc }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Google 地圖 -->
    <section class="map-section">
      <h2 class="section-title center">旅館位置</h2>
      <div class="map-wrap">
        <iframe
          src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d14465.763122798746!2d121.21150374412534!3d24.985134168280037!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x34682183e7b783c3%3A0xf0ebfba2069b6158!2z6IGW5b635Z-6552j5a246Zmi!5e0!3m2!1szh-TW!2stw!4v1778244093302!5m2!1szh-TW!2stw"
          width="100%"
          height="400"
          style="border: 0"
          allowfullscreen=""
          loading="lazy"
          referrerpolicy="no-referrer-when-downgrade"
        >
        </iframe>
      </div>
    </section>

    <!-- 立即預約按鈕 -->
    <section class="cta-section">
      <button class="btn-back" @click="goToStayList">返回房型列表 ‹‹</button>
      <button class="btn-book" @click="goToCalendar">立即預約 ››</button>
    </section>

    <div class="modal fade" id="loginModal" tabindex="-1" data-bs-backdrop="static">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content text-center p-4">
          <div class="modal-body">
            <div class="mb-3" style="font-size: 3rem">🔒</div>
            <h5 class="fw-bold mb-2">尚未登入</h5>
            <p class="text-muted">需要登入才能預約，請問要前往登入嗎？</p>
          </div>
          <div class="modal-footer justify-content-center border-0 pt-0">
            <button type="button" class="btn btn-outline-secondary px-4" @click="loginModal.hide()">
              留在此頁
            </button>
            <button
              type="button"
              class="btn btn-warning px-4 text-white fw-bold"
              @click="goToLogin"
            >
              登入去
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="loading" class="loading-wrap">載入中...</div>
  <div v-else class="loading-wrap">找不到此房型</div>
</template>
<style scoped>
/* ── 全域變數 ── */
.detail-wrap {
  --brown: #6b4c2a;
  --cream: #fdf6ee;
  --gold: #c9933a;
  --text: #2d1f0e;
  --light: #fff9f2;
  font-family: 'Noto Serif TC', 'Georgia', serif;
  background: var(--cream);
  color: var(--text);
}

/* ── Banner ── */
.banner {
  position: relative;
  height: 480px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-end;
}
.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.65) 0%, rgba(0, 0, 0, 0.1) 60%);
}
.banner-text {
  position: relative;
  z-index: 1;
  padding: 40px 60px;
  color: white;
}
.banner-sub {
  font-size: 0.9rem;
  letter-spacing: 0.15em;
  margin-bottom: 8px;
  opacity: 0.85;
}
.banner-title {
  font-size: 3rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

/* ── 通用標題 ── */
.section-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--brown);
  margin-bottom: 28px;
  position: relative;
}
.section-title.center {
  text-align: center;
}
.section-title::after {
  content: '';
  display: block;
  width: 40px;
  height: 3px;
  background: var(--gold);
  margin-top: 8px;
}
.section-title.center::after {
  margin: 8px auto 0;
}

/* ── 介紹區 ── */
.intro-section {
  padding: 64px 60px;
  background: var(--light);
}
.intro-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 48px;
  max-width: 1100px;
  margin: 0 auto;
}
.desc-text {
  font-size: 1.05rem;
  line-height: 2;
  color: #4a3520;
}
.intro-price-box {
  background: white;
  border: 1px solid #e8d9c5;
  border-radius: 12px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-self: start;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}
.price-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.price-label {
  font-size: 0.8rem;
  color: #999;
  letter-spacing: 0.05em;
}
.price-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--brown);
}
.price-value.available {
  color: #3a8c5c;
}

/* ── 環境圖片 ── */
.env-section {
  padding: 64px 60px;
  background: var(--cream);
  max-width: 1200px;
  margin: 0 auto;
}
.env-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.env-img-wrap {
  border-radius: 10px;
  overflow: hidden;
  aspect-ratio: 4/3;
}
.env-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.env-img:hover {
  transform: scale(1.04);
}

/* ── 預約流程 ── */
.flow-section {
  padding: 64px 60px;
  background: var(--brown);
}
.flow-section .section-title {
  color: #f5e6d0;
}
.flow-section .section-title::after {
  background: var(--gold);
}
.flow-steps {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 0;
  flex-wrap: wrap;
  max-width: 1100px;
  margin: 0 auto;
  position: relative;
}
.flow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 160px;
  position: relative;
}
.step-icon {
  font-size: 2rem;
  margin-bottom: 12px;
}
.step-num {
  font-size: 0.75rem;
  color: var(--gold);
  letter-spacing: 0.1em;
  margin-bottom: 6px;
}
.step-title {
  font-size: 1rem;
  font-weight: 700;
  color: #f5e6d0;
  margin-bottom: 8px;
}
.step-desc {
  font-size: 0.8rem;
  color: #c9b89e;
  line-height: 1.6;
}
.step-arrow {
  position: absolute;
  right: -18px;
  top: 28px;
  font-size: 2rem;
  color: var(--gold);
}

/* ── 住宿公約 ── */
.rule-section {
  padding: 64px 60px;
  background: var(--light);
  max-width: 1200px;
  margin: 0 auto;
}
.rule-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
.rule-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  background: white;
  border-radius: 10px;
  padding: 20px 24px;
  border: 1px solid #e8d9c5;
}
.rule-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}
.rule-title {
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--brown);
}
.rule-desc {
  font-size: 0.875rem;
  color: #6b5c49;
  line-height: 1.6;
}

/* ── 地圖 ── */
.map-section {
  padding: 64px 60px;
  background: var(--cream);
  max-width: 1200px;
  margin: 0 auto;
}
.map-wrap {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

/* ── CTA ── */
.cta-section {
  padding: 80px 60px;
  background: var(--cream);
  display: flex;
  justify-content: center;
}
.btn-book {
  background: var(--brown);
  color: white;
  border: none;
  padding: 18px 64px;
  font-size: 1.1rem;
  font-weight: 700;
  border-radius: 50px;
  cursor: pointer;
  letter-spacing: 0.08em;
  transition:
    background 0.2s,
    transform 0.15s;
  font-family: inherit;
}
.btn-book:hover {
  background: var(--gold);
  transform: translateY(-2px);
}

.btn-back {
  background: transparent;
  color: var(--brown);
  border: 2px solid var(--brown);
  padding: 18px 48px;
  font-size: 1.1rem;
  font-weight: 700;
  border-radius: 50px;
  cursor: pointer;
  letter-spacing: 0.08em;
  transition:
    background 0.2s,
    transform 0.15s,
    color 0.2s;
  font-family: inherit;
  margin-right: 20px; /* 與右邊按鈕保持間距 */
}

.btn-back:hover {
  background: var(--brown);
  color: white;
  transform: translateY(-2px);
}

/* ── 載入 ── */
.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  font-size: 1.2rem;
  color: #999;
}
</style>

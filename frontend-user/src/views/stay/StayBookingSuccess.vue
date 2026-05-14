<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/axios.js'

const route = useRoute()
const router = useRouter()

const stayId = route.query.stayId
const stayInfo = ref(null)

const fetchStayInfo = async () => {
  try {
    const res = await axios.get(`/stay/${stayId}`)
    stayInfo.value = res.data
  } catch (e) {
    console.error('訂單載入失敗', e)
  }
}

onMounted(() => {
  if (stayId) fetchStayInfo()
})
</script>

<template>
  <div class="success-wrap">
    <div class="success-icon">🐾</div>
    <h2 class="success-title">預約成功！</h2>
    <p class="success-sub">感謝您的預約，我們期待與您的毛孩相見</p>

    <div class="info-card" v-if="stayInfo">
      <div class="info-row">
        <span class="info-label">訂單編號</span>
        <span class="info-val"># {{ stayInfo.stayId }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">房型</span>
        <span class="info-val">{{ stayInfo.roomTypeName }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">入住日期</span>
        <span class="info-val">{{ stayInfo.stayStartDate }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">退房日期</span>
        <span class="info-val">{{ stayInfo.stayEndDate }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">房號</span>
        <span class="info-val">{{ stayInfo.roomNo }}</span>
      </div>

      <div class="info-row">
        <span class="info-label">住宿天數</span>
        <span class="info-val">{{ stayInfo.stayDay }} 晚</span>
      </div>
      <div class="info-row total">
        <span class="info-label">總金額</span>
        <span class="info-val price">NT$ {{ stayInfo.sumPrice?.toLocaleString() }}</span>
      </div>
    </div>

    <div class="notice-box">
      📱 毛孩辦理入住後，可在
      <strong>會員中心 → 訂單紀錄 → 寵物住宿</strong>
      中查看毛孩房間的即時畫面。
    </div>

    <div class="btn-group">
      <button class="btn-order" @click="router.push('/orderhistory/stayorders')">
        查看我的訂單
      </button>
      <button class="btn-home" @click="router.push('/')">返回首頁</button>
    </div>
  </div>
</template>

<style scoped>
.success-wrap {
  --brown: #6b4c2a;
  --gold: #c9933a;
  max-width: 560px;
  margin: 60px auto;
  padding: 0 20px 60px;
  text-align: center;
  font-family: 'Noto Serif TC', serif;
}
.success-icon {
  font-size: 4rem;
  margin-bottom: 16px;
}
.success-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--brown);
  margin-bottom: 8px;
}
.success-sub {
  font-size: 1rem;
  color: #888;
  margin-bottom: 32px;
}
.info-card {
  background: #fdf6ee;
  border: 1px solid #ecdfd0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  text-align: left;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #ecdfd0;
  font-size: 0.95rem;
}
.info-row:last-child {
  border-bottom: none;
}
.info-row.total {
  margin-top: 8px;
  padding-top: 16px;
}
.info-label {
  color: #999;
}
.info-val {
  font-weight: 600;
  color: var(--brown);
}
.info-val.price {
  font-size: 1.2rem;
  color: var(--gold);
}
.notice-box {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
  padding: 16px;
  font-size: 0.9rem;
  color: #0369a1;
  margin-bottom: 32px;
  line-height: 1.6;
  text-align: left;
}
.btn-group {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.btn-order {
  background: var(--brown);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}
.btn-order:hover {
  background: var(--gold);
}
.btn-home {
  background: none;
  color: var(--brown);
  border: 2px solid #ecdfd0;
  padding: 14px 32px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-home:hover {
  border-color: var(--brown);
}
</style>

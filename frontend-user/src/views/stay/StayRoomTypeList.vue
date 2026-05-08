<script setup>
import axios from '@/axios.js'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vuetify/lib/composables/router.mjs'

const router = useRouter()
const roomTypes = ref([])
const loading = ref(true)
const selectedId = ref(null)

const fetchRoomType = async () => {
  try {
    const res = await axios.get('/stay/roomtype')
    roomTypes.value = res.data
  } catch (e) {
    console.error('載入失敗', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchRoomType()
})
</script>
<template>
  <div class="stay-panel-wrap">
    <div
      v-for="roomType in roomTypes"
      :key="roomType.roomTypeId"
      class="stay-panel"
      :class="{ active: selectedId === roomType.roomTypeId }"
      :style="{ backgroundImage: `url('/src/assets/images/stay_room${roomType.roomTypeId}.jpg')` }"
      @click="selectedId = roomType.roomTypeId"
    >
      <!-- 半透明遮罩 -->
      <div class="panel-overlay"></div>

      <div class="panel-content">
        <h2 class="panel-title">{{ roomType.roomName }}</h2>
        <button
          v-if="selectedId === roomType.roomTypeId"
          class="btn-go"
          @click="router.push(`/stay/${roomType.roomTypeId}`)"
        >
          前往訂房 ››
        </button>
      </div>
    </div>
  </div>
</template>
<style scoped>
.stay-panel-wrap {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

.stay-panel {
  flex: 1;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  transition: flex 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

/* 選中的區塊展開成 6 */
.stay-panel.active {
  flex: 6;
}

/* 半透明遮罩 */
.panel-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  transition: background 0.5s ease;
}

.stay-panel.active .panel-overlay {
  background: rgba(0, 0, 0, 0.15);
}

.panel-content {
  position: absolute;
  bottom: 40px;
  left: 24px;
  color: white;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.6);
  z-index: 1;
}

/* 未選中：文字直排 */
.panel-title {
  font-size: 1.2rem;
  font-weight: 700;
  writing-mode: vertical-rl;
  margin-bottom: 16px;
  transition: font-size 0.4s ease;
}

/* 選中：文字橫排，字變大 */
.stay-panel.active .panel-title {
  writing-mode: horizontal-tb;
  font-size: 2.2rem;
}

/* 前往訂房按鈕 */
.btn-go {
  display: block;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid white;
  color: white;
  padding: 10px 28px;
  border-radius: 30px;
  font-size: 1rem;
  cursor: pointer;
  backdrop-filter: blur(4px);
  transition: background 0.2s;
  opacity: 0;
  animation: fadeIn 0.4s ease 0.3s forwards;
}

.btn-go:hover {
  background: rgba(255, 255, 255, 0.35);
}

/* 按鈕淡入動畫 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

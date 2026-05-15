<template>
  <div class="admin-header">
    <div class="header-logo">
      PetLife 後臺管理系統
    </div>

    <div class="header-user-area">
      <!-- 客服聊聊入口 -->
      <router-link 
        to="/admin/chat-support" 
        class="header-chat-btn" 
        :class="{ 'has-waiting': waitingCount > 0 }"
        title="客服中心"
      >
        <el-badge :value="waitingCount" :max="99" :hidden="waitingCount === 0" class="chat-badge">
          <i class="fas fa-comment-dots"></i>
        </el-badge>
      </router-link>

      <div class="user-profile">
        <div class="user-avatar-circle">
          <img src="https://img.icons8.com/ios-filled/50/user-male-circle.png" alt="avatar" />
        </div>
        <div class="user-text-info">
          <span class="user-name">{{ employeeStore.empName }}</span>
          <span class="user-role">{{ employeeStore.role || '系統管理員' }}</span>
        </div>
      </div>
      <button class="logout-btn" @click="logout">登出</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useEmployeeStore } from '@/stores/employee';
import { useRouter } from 'vue-router';
import axios from 'axios';

const employeeStore = useEmployeeStore();
const router = useRouter();
const waitingCount = ref(0);
let pollTimer = null;

const API_BASE = 'http://localhost:8082/api/chat';

const fetchWaitingCount = async () => {
  try {
    const res = await axios.get(API_BASE + '/session/waiting');
    const sessions = res.data || [];
    // 只計算等待中的人數
    waitingCount.value = sessions.filter(s => s.status === 'waiting').length;
  } catch (e) {
    console.error('取得等待客服人數失敗', e);
  }
};

const logout = () => {
  employeeStore.logout();
  router.push('/');
};

onMounted(() => {
  fetchWaitingCount();
  // 每 10 秒檢查一次是否有新的等待連線
  pollTimer = setInterval(fetchWaitingCount, 10000);
});

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped>
.chat-badge :deep(.el-badge__content) {
  top: 2px;
  right: 2px;
  background-color: #ff4d4f; /* 更鮮豔的紅色 */
  border: 2px solid #fff;     /* 白色邊框更明顯 */
  font-weight: bold;
  height: 20px;
  min-width: 20px;
  line-height: 16px;
  padding: 0 6px;
}
</style>
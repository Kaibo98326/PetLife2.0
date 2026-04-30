<template>
  <div class="admin-header">
    <div class="header-logo">
      PetLife 後臺管理系統
    </div>

    <div class="header-user-area">
      <div class="user-profile">
        <div class="user-avatar-circle">
          <img src="https://img.icons8.com/ios-filled/50/user-male-circle.png" alt="avatar" />
        </div>
        <div class="user-text-info">
          <span class="user-name">{{ employeeStore.empName }}</span>
          <span class="user-role">{{ employeeStore.role || '系統管理員' }}</span>
        </div>
      </div>
      <el-button 
    type="info" 
    plain 
    class="btn-back-to-mall me-3"
    @click="goToMall"
  >
    回商城
  </el-button>
      <button class="logout-btn" @click="logout">登出</button>
    </div>
  </div>
</template>

<script setup>
import { useEmployeeStore } from '@/stores/employee';
import { useRouter } from 'vue-router';
import { ShoppingCart } from '@element-plus/icons-vue'

const employeeStore = useEmployeeStore();
const router = useRouter();

const logout = () => {
  employeeStore.logout();
  router.push('/');
};

// 跳轉商城邏輯
const goToMall = () => {
  window.location.href = 'http://localhost:5173/';
};
</script>
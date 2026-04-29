<template>
  <div class="dashboard-content">
    <el-card class="welcome-card">
      <div class="welcome-text">
        <h3>你好，{{ employeeStore.empName }}！ </h3>
        <p>歡迎回到 PetLife 管理系統。今天想處理什麼事務呢？</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>今日商城訂單</template>
          <div class="stat-number">12</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>美容預約</template>
          <div class="stat-number">5</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>旅館空房</template>
          <div class="stat-number">8</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>商品庫存警告</template>
          <div class="stat-number text-danger">3</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useEmployeeStore } from '@/stores/employee';
import { useRouter } from 'vue-router';


const employeeStore = useEmployeeStore();
const router = useRouter();
const openMenu = ref(null);
const toggleSubmenu = (menuName) => {
    openMenu.value = openMenu.value === menuName ? null : menuName;
};
const logout = () => {
    employeeStore.logout();
    router.push('/');
};
</script>

<style scoped>
.dashboard-content {
  padding: 10px;
}

.welcome-card {
  margin-bottom: 20px;
  border-left: 5px solid var(--primary-orange); /* 使用全域定義的橘色 */
}

.stat-row {
  margin-top: 20px;
}

.stat-card {
  text-align: center;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
}

.text-danger {
  color: #f56c6c;
}
</style>
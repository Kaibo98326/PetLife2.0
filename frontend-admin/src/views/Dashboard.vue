<template>
  <div class="dashboard-content">
    <el-card class="welcome-card">
      <div class="welcome-text">
        <h3>你好，{{ employeeStore.empName }}！ </h3>
        <p>歡迎回到 PetLife 管理系統。今天想處理什麼事務呢？</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <template #header>今日商城訂單</template>
          <div class="stat-number">{{ todayOrderCount }}</div>
        </el-card>
      </el-col>
      <!-- <el-col :span="6">
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
      </el-col> -->
      <el-col :span="12">
        <el-card 
          shadow="hover" 
          class="stat-card clickable-card" 
          @click="router.push({ path: '/admin/product', query: { lowStock: 'true' } })"
        >
          <template #header>商品庫存警告</template>
          <div :class="['stat-number', productStore.lowStockCount > 0 ? 'text-danger' : 'text-success']">
            {{ productStore.lowStockCount }}
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useEmployeeStore } from '@/stores/employee';
import { useProductStore } from '@/stores/product';
import request from '@/utils/request';

const router = useRouter();
const employeeStore = useEmployeeStore();
const productStore = useProductStore();
const todayOrderCount = ref(0);

const fetchTodayOrders = async () => {
  try {
    const res = await request.get('/api/order/all');
    const allOrders = res.data;
    
    // 取得今天日期的 YYYY-MM-DD
    const now = new Date();
    const todayStr = now.getFullYear() + '-' + 
                     String(now.getMonth() + 1).padStart(2, '0') + '-' + 
                     String(now.getDate()).padStart(2, '0');

    // 過濾出今日訂單
    const todayOrders = allOrders.filter(order => {
      return order.orderDate && order.orderDate.startsWith(todayStr);
    });

    todayOrderCount.value = todayOrders.length;
  } catch (error) {
    console.error('抓取今日訂單失敗:', error);
    todayOrderCount.value = 0;
  }
};

onMounted(() => {
  fetchTodayOrders();
});
</script>

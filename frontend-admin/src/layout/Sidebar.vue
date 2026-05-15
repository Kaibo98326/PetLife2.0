<template>
    <el-menu
    class="sidebar-menu"
    background-color="#2c3e50"
    text-color="#adb5bd"
    active-text-color="#ff7a00"
    :default-active="$route.path"
    unique-opened
    router
  >
    <el-menu-item index="/admin/dashboard">
      <span>首頁</span>
    </el-menu-item>

    <el-sub-menu index="member-group">
      <template #title>
        <el-icon>
          <User />
        </el-icon>
        <span>會員管理</span>
      </template>
      <el-menu-item index="/admin/member/list">現有會員</el-menu-item>
      <el-menu-item index="/admin/member/analysis">會員狀態分析</el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/admin/employee">
      <el-icon>
        <UserFilled />
      </el-icon>
      <span>員工管理</span>
    </el-menu-item>

    <el-menu-item index="/admin/pet">
      <el-icon>
        <MagicStick />
      </el-icon>
      <span>寵物管理</span>
    </el-menu-item>

    <el-sub-menu index="product-group">
      <template #title>
        <el-icon>
          <Box />
        </el-icon>
        <span>商品管理</span>
        <!-- 父目錄：顯示驚嘆號圖示 -->
        <i v-if="productStore.lowStockCount > 0" class="fas fa-exclamation-triangle parent-warning-icon"></i>
      </template>
      <el-menu-item index="/admin/category">商品類別管理</el-menu-item>
      <el-menu-item index="/admin/product" class="product-menu-item">
        <span>商品管理</span>
        <!-- 子目錄：顯示紅色小數字徽章 -->
        <el-badge 
          v-if="productStore.lowStockCount > 0" 
          :value="productStore.lowStockCount" 
          :max="99" 
          class="sub-stock-badge"
        />
      </el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/admin/order">
      <el-icon>
        <ShoppingCart />
      </el-icon>
      <span>訂單管理</span>
    </el-menu-item>


    <el-sub-menu index="beauty-group">
      <template #title>
        <el-icon>
          <Scissor />
        </el-icon>
        <span>寵物美容管理</span>
      </template>
      <el-menu-item index="/admin/beauty/items">美容項目管理</el-menu-item>
      <el-menu-item index="/admin/beauty/appointments">美容預約管理</el-menu-item>
      <el-menu-item index="/admin/beauty/groomers">美容師管理</el-menu-item>
      <el-menu-item index="/admin/beauty/schedules">班表管理</el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="hotel-group">
      <template #title>
        <el-icon>
          <House />
        </el-icon>
        <span>寵物旅館管理</span>
      </template>
      <el-menu-item index="/admin/hotel/room">房型管理</el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/admin/discount">
      <el-icon>
        <PriceTag />
      </el-icon>
      <span>優惠活動管理</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import {
  User,
  Box,
  Scissor,
  House,
  ShoppingCart,
  PriceTag,
  UserFilled,
  MagicStick,
  ChatLineSquare,
} from '@element-plus/icons-vue'
import { useProductStore } from '@/stores/product'

const productStore = useProductStore()
</script>

<style scoped>
/* 確保側邊欄是滿版的 */
.sidebar-menu {
  height: 100vh;
  border-right: none;
}

/* 父目錄驚嘆號圖示 */
.parent-warning-icon {
  color: #ff4d4f;
  font-size: 14px;
  margin-left: 8px;
  animation: pulse 2s infinite;
}

/* 子目錄徽章平行且垂直置中排列 */
.product-menu-item {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-start;
  gap: 8px;
}

:deep(.sub-stock-badge) {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

:deep(.sub-stock-badge .el-badge__content) {
  position: static;
  transform: scale(0.8);
  background-color: #ff4d4f;
  border: none;
  line-height: 18px; /* 確保圓圈高度一致 */
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}
</style>

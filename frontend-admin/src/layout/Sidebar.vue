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

    <el-menu-item index="/admin/member">
      <el-icon><User /></el-icon>
      <span>會員管理</span>
    </el-menu-item>

    <el-menu-item index="/admin/employee">
      <el-icon><UserFilled /></el-icon>
      <span>員工管理</span>
    </el-menu-item>

    <el-menu-item index="/admin/pet">
      <el-icon><MagicStick /></el-icon>
      <span>寵物管理</span>
    </el-menu-item>

    <el-sub-menu index="product-group">
      <template #title>
        <el-icon><Box /></el-icon>
        <span>商品管理</span>
      </template>
      <el-menu-item index="/admin/category">商品類別管理</el-menu-item>
      <el-menu-item index="/admin/product">商品管理</el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/admin/order">
      <el-icon><ShoppingCart /></el-icon>
      <span>訂單管理</span>
    </el-menu-item>

    <el-sub-menu index="beauty-group">
      <template #title>
        <el-icon><Scissor /></el-icon>
        <span>寵物美容管理</span>
      </template>
      <el-menu-item index="/admin/beauty/service">服務項目</el-menu-item>
      <el-menu-item index="/admin/beauty/order">預約訂單</el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="hotel-group">
      <template #title>
        <el-icon><House /></el-icon>
        <span>寵物旅館管理</span>
      </template>
      <el-menu-item index="/admin/hotel/room">房型管理</el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/admin/discount">
      <el-icon><PriceTag /></el-icon>
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
} from '@element-plus/icons-vue'
</script>

<style scoped>
/* 確保側邊欄是滿版的 */
.sidebar-menu {
  height: 100vh;
  border-right: none;
}
</style>

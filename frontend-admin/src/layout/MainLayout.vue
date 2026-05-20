<template>
  <div class="admin-view-wrapper">
    <Header />

    <div class="admin-bottom-container">
      <div class="sidebar-wrapper">
        <Sidebar />
      </div>
      
      <main class="admin-main-body">
        <div class="content-header">
          <div class="breadcrumb-container">
            <span class="root-path">首頁</span>
            <span class="separator"> > </span>
            <span class="current-path">{{ $route.name || '管理系統' }}</span>
          </div>
          <i v-if="$route.meta.icon" :class="[$route.meta.icon, 'me-2']"></i>{{ $route.name }}
        </div>

        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import { useProductStore } from '@/stores/product'

const productStore = useProductStore()

onMounted(() => {
  productStore.fetchLowStockCount()
})
</script>

<style scoped></style>

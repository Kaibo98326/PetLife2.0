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
          <h2 class="page-title">{{ $route.name }}</h2>
        </div>

        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>

<style scoped></style>

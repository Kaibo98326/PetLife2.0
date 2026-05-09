<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div id="app">
    <RouterView />
  </div>
</template>

<style>
/* 全域樣式：ShopPanel.css 由 UserLayout 引入，這裡只保留必要的 App 層級樣式 */
@import '@/assets/css/ShopPanel.css';

#app {
  font-family: 'Noto Sans TC', sans-serif;
  background-color: var(--bg-warm);
  color: var(--text-main);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>

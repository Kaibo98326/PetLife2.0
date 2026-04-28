<script setup>
import { RouterView } from 'vue-router';

//全域元件(之後可以拆成立檔案)

import Footer from './components/FooterView.vue';


</script>
<template>

     <div class="app d-flex flex-column min-vh-100">
    <!-- 主內容區域 -->
    <main class="flex-grow-1">
      <RouterView />
    </main>

    <!-- 全域 Footer -->
    <Footer class="mt-auto bg-light text-center p-3" />
  </div>
</template>
<style scoped>
@import './assets/css/ShopPanel.css';

#app {
    font-family: 'Noto Sans TC', sans-serif;
    background-color: var(--bg-warm);
    color: var(--text-main);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

main {
    flex: 1;
    /* 讓內容稱滿剩餘空間 */
}

</style>

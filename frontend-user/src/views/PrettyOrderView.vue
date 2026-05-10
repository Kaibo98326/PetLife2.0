<script setup>
import { ref, onMounted } from 'vue'

// 這是關鍵：把載入狀態設為 false，內容才會從 v-if="!isLoading" 顯現
const isLoading = ref(false)
const orders = ref([]) // 目前 API 沒串，給空陣列即可
</script>

<template>
  <div v-if="isLoading">載入中...</div>
  <div v-else>
    <h3 class="p-4">美容歷史訂單</h3>
    <!-- 這裡可以先放你原本的簡易文字或 HTML -->
  </div>
</template>
<style scoped></style>

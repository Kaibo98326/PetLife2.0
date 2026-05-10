<script setup>
import { ref, onMounted } from 'vue'

const isLoading = ref(false)
const orders = ref([])
</script>

<template>
  <div v-if="isLoading">載入中...</div>
  <div v-else>
    <h3 class="p-4">住宿歷史訂單</h3>
  </div>
</template>
<style scoped></style>

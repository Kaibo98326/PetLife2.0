<template>
  <div class="order-detail-container" v-if="order">
    <div class="detail-id-badge"><i class="bi bi-hash"></i> 訂單編號：{{ order.orderId }}</div>

    <div class="row g-4">
      <!-- 收件與付款資訊 -->
      <div class="col-md-5">
        <div class="info-section mb-4">
          <h5 class="section-title"><i class="bi bi-person-fill"></i> 收件資訊</h5>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">收件人</span
              ><span class="info-value fw-bold">{{ order.orderName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">聯絡電話</span
              ><span class="info-value">{{ order.orderPhone }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">收件地址</span
              ><span class="info-value">{{ order.orderAddress }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品明細清單 -->
      <div class="col-md-7">
        <div class="info-section h-100">
          <h5 class="section-title"><i class="bi bi-box-seam-fill"></i> 商品明細</h5>
          <table class="table detail-items-table">
            <thead>
              <tr>
                <th>商品</th>
                <th class="text-center">單價</th>
                <th class="text-center">數量</th>
                <th class="text-end">小計</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in details" :key="item.productId">
                <td>{{ item.productName }}</td>
                <td class="text-center">${{ item.productPrice }}</td>
                <td class="text-center">{{ item.quantity }}</td>
                <td class="text-end fw-bold">${{ item.subtotal }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
  <div v-else-if="loading" class="text-center py-5">
    <div class="spinner-border text-primary"></div>
    <p>載入中...</p>
<!--                                           因應活動新增 -->
<Transition name="zoom">
      <div v-if="selectedOrderId" class="glass-overlay" @click.self="selectedOrderId = null">
        <div class="glass-modal large">
          </div>
      </div>
    </Transition>

    <OrderDiscountModal ref="discountModalRef" />

  </div>
<!--                                            因應活動新增 -->

  <!-- </div>      原先的 -->
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

// 接收父組件傳過來的orderId
const props = defineProps({
  orderId: {
    type: [Number, String],
    required: true,
  },
})

const order = ref(null)
const details = ref([])
const loading = ref(false)

const fetchDetail = async () => {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await fetch(`/api/order/detail/${props.orderId}`)
    const data = await res.json()
    // 後端回傳格式為{ order: {...}, details: [...] }
    order.value = data.order
    details.value = data.details
  } catch (err) {
    console.error('明細抓取失敗', err)
  } finally {
    loading.value = false
  }
}

// 當props.orderId改變時重新抓取(防止同一個Modal切換不同訂單)
watch(() => props.orderId, fetchDetail)

onMounted(fetchDetail)
</script>

<style scoped src="@/assets/css/OrderDetailAdmin.css"></style>

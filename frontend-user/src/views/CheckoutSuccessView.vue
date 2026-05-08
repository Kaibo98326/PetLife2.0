<template>
  <div class="checkout-success">
    <div class="success-container">
      <div class="header-banner">
        <h2><i class="fas fa-paw"></i> 訂單成立！感謝您的支持</h2>
      </div>

      <div class="info-section">
        <div class="section-title">📦 配送資訊</div>
        <table class="info-table">
          <tbody>
            <tr>
              <td class="label">訂單編號</td>
              <td class="order-id">#{{ orderMain.orderId }}</td>
              <td class="label">成立時間</td>
              <td>{{ formatDateTime(orderMain.orderDate) }}</td>
            </tr>
            <tr>
              <td class="label">收件人</td>
              <td>{{ orderMain.orderName }}</td>
              <td class="label">配送地址</td>
              <td>{{ orderMain.orderAddress }}</td>
            </tr>
          </tbody>
        </table>

        <div class="section-title">🛒 購買清單</div>
        <table class="item-table">
          <thead>
            <tr>
              <th class="text-left">項目</th>
              <th class="text-center">單價</th>
              <th class="text-center">數量</th>
              <th class="text-center">折扣</th>
              <th class="text-right">小計</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in orderItems" :key="index">
              <td class="text-left">{{ item.productName }}</td>
              <td class="text-center">$ {{ item.productPrice || 0 }}</td>
              <td class="text-center">{{ item.quantity }}</td>
              <td class="text-center text-discount">
                {{ item.discount > 0 ? '-$ ' + item.discount : '$ 0' }}
              </td>
              <td class="text-right subtotal-cell">$ {{ item.subtotal }}</td>
            </tr>
          </tbody>
        </table>

        <div class="total-box">
          <span class="total-label">應付總額：</span>
          <span class="total-price">$ {{ orderMain.orderTotal }}</span>
        </div>
        <span class="warn">確認訂單狀態請前往會員中心查看。</span>

        <div class="actions">
          <router-link to="/" class="btn-orange">回到首頁</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import axios from '@/axios'

const orderMain = reactive({
  orderId: '',
  orderDate: '',
  orderName: '',
  orderAddress: '',
  orderTotal: 0,
})

const orderItems = reactive([])

// 時間格式化
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  const lastOrderId = sessionStorage.getItem('lastOrderId')

  if (lastOrderId) {
    try {
      const res = await axios.get(`/orders/detail/${lastOrderId}`)
      const data = res.data
      console.log('🔥 後端回傳資料：', data)

      orderMain.orderId = data.orderId
      orderMain.orderDate = data.orderDate
      orderMain.orderName = data.orderName || '未提供'
      orderMain.orderAddress = data.orderAddress
      orderMain.orderPrice = data.orderPrice
      orderMain.orderTotal = data.orderTotal

      orderItems.length = 0
      const items = data.items || []
      orderItems.push(...items)
    } catch (error) {
      console.error('抓取訂單失敗:', error)
    }
  }
})
</script>

<style scoped>
@import '../assets/css/CheckoutSuccess.css';
</style>

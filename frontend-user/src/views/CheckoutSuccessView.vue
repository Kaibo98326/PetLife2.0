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

       <div class="total-box" style="width: 50%; max-width: 500px; margin-left: auto;">
          <div class="summary-line" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <span class="total-label" style="font-size: 16px; color: #666;">商品總計：</span>
            <span class="total-price" style="font-size: 18px; color: #666;">$ {{ (orderMain.orderTotal + discountAmount).toLocaleString() }}</span>
          </div>
          
          <div v-if="discountAmount > 0" class="summary-line discount-line" style="display: flex; flex-direction: column; margin-bottom: 8px;">
            <div @click="isDiscountExpanded = !isDiscountExpanded" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; cursor: pointer;">
              <span class="total-label" style="font-size: 16px; color: #ff4d4f;">
                <i :class="isDiscountExpanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'" style="color: #ff4d4f; margin-right: 5px;"></i>活動折抵明細：
              </span>
              <span class="total-price" style="font-size: 18px; color: #ff4d4f;">- $ {{ discountAmount.toLocaleString() }}</span>
            </div>
            <template v-if="isDiscountExpanded">
              <div v-for="(ad, index) in appliedDiscounts" :key="index" style="font-size: 0.9em; margin-bottom: 3px; display: flex; justify-content: space-between; align-items: center; padding-left: 20px;">
                <span class="label" style="color: #888; background: none; font-weight: normal; padding: 0;">{{ ad.name }}</span>
                <span style="color: #ff4d4f;">- $ {{ ad.amount ? ad.amount.toLocaleString() : '0' }}</span>
              </div>
            </template>
          </div>
          
          <div v-if="orderMain.usedPoint > 0" class="summary-line discount-line" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <span class="total-label" style="font-size: 16px; color: #ff4d4f;">紅利折抵：</span>
            <span class="total-price" style="font-size: 18px; color: #ff4d4f;">- $ {{ orderMain.usedPoint.toLocaleString() }}</span>
          </div>

          <div class="summary-line final-total-line" style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #eee; padding-top: 10px; margin-top: 10px;">
            <span class="total-label" style="font-size: 16px; font-weight: bold;">應付總額：</span>
            <span class="total-price" style="font-size: 20px; font-weight: bold; color: #e67e22;">$ {{ orderMain.orderTotal.toLocaleString() }}</span>
          </div>
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
import { reactive, onMounted, ref } from 'vue'
import axios from '@/axios'

const orderMain = reactive({
  orderId: '',
  orderDate: '',
  orderName: '',
  orderAddress: '',
  orderTotal: 0,
  usedPoint: 0, // 用來接收紅利點數
})

const orderItems = reactive([])
const discountAmount = ref(0)
const appliedDiscounts = ref([])

// ✨ 修改：新增控制折扣明細展開/收合狀態的響應式布林值變數
const isDiscountExpanded = ref(false)

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
      orderMain.usedPoint = data.usedPoint || 0 //把後端傳來的紅利點數存起來


      orderItems.length = 0
      const items = data.items || []
      orderItems.push(...items)

      // 活動修改：改從 OrderDiscount 表讀取已儲存的折扣明細，不再重新計算
      // 原做法呼叫 /cart/calculate，但 order detail 沒有 categoryId，導致分類型活動算不出折扣
     try {
        // 2026-05-14 修正：移除多餘的 /api，避免與 axios 的 baseURL 疊加造成 403 錯誤
        const discountRes = await axios.get(`/order-discounts/order/${lastOrderId}/summary`)
        const summaryList = discountRes.data || []
        if (summaryList.length > 0) {
          // 計算總折扣金額
          const totalDiscount = summaryList.reduce((sum, item) => sum + (item.amount || 0), 0)
          discountAmount.value = totalDiscount
          appliedDiscounts.value = summaryList.map(item => ({
            name: item.name,
            amount: item.amount
          }))
        }
      } catch (discountError) {
        console.error('結帳成功頁面讀取折扣明細失敗:', discountError)
      }

    } catch (error) {
      console.error('抓取訂單失敗:', error)
    }
  }
})
</script>

<style scoped>
@import '../assets/css/CheckoutSuccess.css';
</style>
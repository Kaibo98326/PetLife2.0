<script setup>
import { onMounted, ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import axios from '@/axios'
import Swal from 'sweetalert2'

const userStore = useUserStore()
const cartItems = ref([])
const isProcessing = ref(false)

// 表單資料綁定
const orderForm = ref({
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  paymentMethod: 'LinePay', // 預設支付方式
  orderNotes: '',
})

//取得會員資訊並預填表單
const fetchMemberInfo = async () => {
  const mId = userStore.memberId
  if (!mId) return
  try {
    const res = await axios.get(`/member/${mId}`)
    const member = res.data
    orderForm.value.receiverName = member.memberName || ''
    orderForm.value.receiverPhone = member.phone || ''
    orderForm.value.shippingAddress = member.address || ''
  } catch (error) {
    console.error('獲取會員資訊失敗:', error)
  }
}

// 取得購物車資料
const fetchCart = async () => {
  const mId = userStore.memberId
  if (!mId) return
  try {
    const res = await axios.get(`/cart/${mId}`)
    cartItems.value = res.data
    if (res.data && res.data.length > 0 && res.data[0].cartId) {
      userStore.cartId = res.data[0].cartId
      console.log('✅ cartId 已更新:', userStore.cartId)
    }
  } catch (error) {
    console.error('獲取購物車失敗:', error)
  }
}

//計算總額
const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

//送出訂單並處理跳轉
const submitOrder = async () => {
  if (
    !orderForm.value.receiverName ||
    !orderForm.value.receiverPhone ||
    !orderForm.value.shippingAddress
  ) {
    Swal.fire('提示', '請填寫完整的收件資訊', 'warning')
    return
  }

  // 三種支付方式都跳出確認框
  const result = await Swal.fire({
    title: '確認下單',
    text: `您選擇了 ${orderForm.value.paymentMethod}，是否要送出訂單？`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '確定',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return

  isProcessing.value = true
  try {
    const orderData = {
      memberId: userStore.memberId,
      orderName: orderForm.value.receiverName,
      orderPhone: orderForm.value.receiverPhone,
      orderAddress: orderForm.value.shippingAddress,
      orderPayment: orderForm.value.paymentMethod,
      orderNote: orderForm.value.orderNotes,
      usedPoint: 0,
      remainingPoint: 0,
    }

    console.log('準備送出的 cartId:', userStore.cartId)
    const res = await axios.post('/orders/checkout', orderData, {
      params: { cartId: userStore.cartId },
    })

    if (res.data.order?.orderId) {
      sessionStorage.setItem('lastOrderId', res.data.order.orderId)
      console.log('訂單 ID 已存入 sessionStorage:', res.data.order.orderId)
    }

    if (res.data.form) {
      const div = document.createElement('div')
      div.innerHTML = res.data.form
      document.body.appendChild(div)
      const form = div.querySelector('form')
      if (form) form.submit()
    } else {
      const backupForm = document.getElementById('ecpayForm')
      if (backupForm) backupForm.submit()
    }
  } catch (error) {
    console.error('下單失敗詳細資訊:', error.response?.data)
    Swal.fire('錯誤', '訂單處理失敗，請檢查後端 Console', 'error')
  } finally {
    isProcessing.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchCart(), fetchMemberInfo()])
})
</script>

<template>
  <div class="checkout-page">
    <div class="checkout-card">
      <h2 class="main-title"><i class="fa-regular fa-clipboard"></i> 填寫結帳資訊</h2>

      <div class="order-details-section">
        <table class="styled-table">
          <thead>
            <tr>
              <th class="text-start">商品明細</th>
              <th class="text-center">數量</th>
              <th class="text-end">小計</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in cartItems" :key="item.itemId">
              <td class="product-name">{{ item.productName }}</td>
              <td class="product-qty">{{ item.quantity }}</td>
              <td class="product-subtotal">$ {{ item.subtotal.toLocaleString() }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="form-section">
        <h4 class="section-title"><i class="fa-solid fa-truck"></i> 寄送資訊</h4>
        <div class="form-group">
          <label>收件人姓名</label>
          <input
            v-model="orderForm.receiverName"
            type="text"
            placeholder="請輸入姓名"
            class="custom-input"
          />
        </div>
        <div class="form-group">
          <label>連絡電話</label>
          <input
            v-model="orderForm.receiverPhone"
            type="text"
            placeholder="請輸入電話"
            class="custom-input"
          />
        </div>
        <div class="form-group">
          <label>寄送地址</label>
          <input
            v-model="orderForm.shippingAddress"
            type="text"
            placeholder="請輸入完整地址"
            class="custom-input"
          />
        </div>

        <h4 class="section-title mt-4"><i class="fa-solid fa-credit-card"></i> 付款方式</h4>
        <div class="payment-methods">
          <label class="radio-item">
            <input type="radio" v-model="orderForm.paymentMethod" value="LinePay" />
            <span class="radio-label">LinePay</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="orderForm.paymentMethod" value="信用卡" />
            <span class="radio-label">信用卡</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="orderForm.paymentMethod" value="金融卡" />
            <span class="radio-label">金融卡</span>
          </label>
        </div>

        <div class="form-group mt-3">
          <label>訂單備註</label>
          <textarea
            v-model="orderForm.orderNotes"
            placeholder="有什麼想告訴我們的？"
            class="custom-textarea"
          ></textarea>
        </div>
      </div>

      <div class="checkout-footer">
        <div class="total-amount-box">
          <span class="label">應付總額：</span>
          <span class="amount">$ {{ totalAmount.toLocaleString() }}</span>
        </div>

        <div class="button-group">
          <router-link to="/cart" class="btn-cancel">返回修改購物車</router-link>
          <button class="btn-confirm" @click="submitOrder" :disabled="isProcessing">
            {{ isProcessing ? '處理中...' : '確認下單' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '../assets/css/CheckoutView.css';
</style>

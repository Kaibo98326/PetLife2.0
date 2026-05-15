<script setup>
import { onMounted, ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import axios from '@/axios'
import Swal from 'sweetalert2'

const userStore = useUserStore()
const cartItems = ref([])
const isProcessing = ref(false)

//  因應活動新增  用來裝後端算好的折扣與最終金額              --->活動新增
const discountAmount = ref(0)
const finalAmount = ref(0)
const appliedDiscounts = ref([])
const isDiscountExpanded = ref(false)
// 5/14更新：新增接收後端原始總額與明細清單的變數
const backendOriginalTotal = ref(0) //   5/14更新

// ✨ 新增/修改：從購物車讀取使用者決定使用的紅利點數 (CheckoutView)
const usedBonusPoints = ref(parseInt(sessionStorage.getItem('usedBonusPoints')) || 0)

// ✨ 新增/修改：計算真正的應付總額 (活動折抵後，再扣紅利)
const actualFinalAmount = computed(() => {
  return Math.max(0, finalAmount.value - usedBonusPoints.value)
})

// ✨ 新增/修改：計算這筆訂單完成後可獲得的紅利 (實付總額 * 0.01，無條件捨去)
const estimatedEarnPoints = computed(() => {
  return Math.floor(actualFinalAmount.value * 0.01)
})
 
//活動折扣邏輯                                              --->活動新增
const calculateDiscount = async () => {
  try {
    const requestData = {
      cartItems: cartItems.value.map(item => ({
       itemId: item.itemId, //   5/14更新：補上 itemId 供後端對應
        productId: item.productId,
        categoryId: item.categoryId, 
        price: item.productPrice,
        quantity: item.quantity
      }))
    }
    const res = await axios.post('/cart/calculate', requestData)
    discountAmount.value = res.data.discountAmount || 0
    finalAmount.value = res.data.finalAmount || 0
    //   5/14更新：同步後端傳回的原始總額與明細
    backendOriginalTotal.value = res.data.originalTotal || totalAmount.value //   5/14更新
    appliedDiscounts.value = res.data.appliedDiscounts || []
  } catch (error) {
    console.error('結帳折扣計算失敗', error)
  }
}

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

    // 資料抓完後，呼叫後端算折扣   ---->活動新增
    await calculateDiscount()

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
    text: `您選擇了 ${orderForm.value.paymentMethod}付款，是否要送出訂單？`,
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
      // ✨ 新增/修改：將前台計算的紅利數值帶給後端 (後端將以此進行原子化扣除)
      usedPoint: usedBonusPoints.value,
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

    // 2026-05-14 17:44 修改：優化綠界跳轉邏輯，強制設定 target 為 _self 解決「開新分頁」問題，並確保指令唯一
    if (res.data.form) {
      const div = document.createElement('div')
      // 使用 innerHTML 插入時，HTML 內的 <script> 不會自動執行，這能精確控制由我們手動觸發一次提交
      div.innerHTML = res.data.form
      document.body.appendChild(div)
      const form = div.querySelector('form')
      if (form) {
        // 強制指定在當前視窗跳轉，避免瀏覽器因非同步延遲將其判定為彈出視窗
        form.setAttribute('target', '_self')
        form.submit()
      }
    } else {
      const backupForm = document.getElementById('ecpayForm')
      if (backupForm) {
        backupForm.setAttribute('target', '_self')
        backupForm.submit()
      }
    }
    // 2026-05-14 17:44 結束修改
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
        <div class="total-amount-box" style="display: flex; flex-direction: column; align-items: flex-end;">
          
          <div class="checkout-summary-row">
            <span class="checkout-row-label">商品總額：</span>
            <span class="checkout-row-amount">$ {{ (backendOriginalTotal || totalAmount).toLocaleString() }}</span>
          </div>

          <div v-if="discountAmount > 0" class="checkout-summary-row checkout-discount-clickable" @click="isDiscountExpanded = !isDiscountExpanded">
            <span class="checkout-row-label checkout-discount-label">
              <i :class="isDiscountExpanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'" class="checkout-chevron-icon"></i>
              活動折抵明細：
            </span>
            <span class="checkout-row-amount checkout-discount-amount">- $ {{ discountAmount.toLocaleString() }}</span>
          </div>

          <div v-if="isDiscountExpanded && appliedDiscounts.length > 0" class="checkout-detail-box">
            <div v-for="(ad, index) in appliedDiscounts" :key="index" class="checkout-detail-line">
              <span class="checkout-detail-name">　　{{ ad.name }}</span>
              <span class="checkout-detail-amount">- $ {{ ad.amount.toLocaleString() }}</span>
            </div>
          </div>

          <div v-if="usedBonusPoints > 0" class="checkout-summary-row">
            <span class="checkout-row-label" style="color: #666;">紅利點數折抵：</span>
            <span class="checkout-row-amount" style="color: #ff4d4f;">- $ {{ usedBonusPoints.toLocaleString() }}</span>
          </div>

          <div class="checkout-summary-row checkout-final-row">
            <span class="checkout-row-label checkout-final-label">應付總額：</span>
            <span class="checkout-row-amount checkout-final-amount">$ {{ actualFinalAmount.toLocaleString() }}</span>
          </div>

          <div style="text-align: right; margin-top: 5px; color: #e67e22; font-size: 0.95em; font-weight: 700;">
            <i class="fas fa-coins me-1"></i> 此單預計獲得紅利：{{ estimatedEarnPoints }} 點
          </div>

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
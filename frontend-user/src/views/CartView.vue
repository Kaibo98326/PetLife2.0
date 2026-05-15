<script setup>
import { onMounted, ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router' //
import axios from '@/axios'
import Swal from 'sweetalert2'

const userStore = useUserStore()
const router = useRouter()
const cartItems = ref([])

  //  因應活動新增  用來裝後端算好的折扣與最終金額              --->活動新增
const discountAmount = ref(0)
const finalAmount = ref(0)
const backendOriginalTotal = ref(0) 
const appliedDiscounts = ref([])    
const isDiscountExpanded = ref(false)

// ✨ 新增/修改：紅利點數狀態管理 (CartView)
const isBonusEnabled = ref(false)
const useBonusPoints = ref(0)

// ✨ 新增/修改：計算可用紅利最大值 (不能超過自己擁有的點數，也不能超過折抵後的總額)
const maxUsablePoints = computed(() => {
  const points = userStore.user?.bonusPoints || 0
  return Math.min(points, finalAmount.value)
})

// ✨ 新增/修改：切換紅利開關時，自動帶入最大可折抵金額或歸零
const toggleBonus = () => {
  if (isBonusEnabled.value) {
    useBonusPoints.value = maxUsablePoints.value
  } else {
    useBonusPoints.value = 0
  }
}

// ✨ 新增/修改：防呆驗證輸入框，防止手動輸入負數或超過上限
const validateBonus = () => {
  if (!isBonusEnabled.value) return
  let val = parseInt(useBonusPoints.value) || 0
  if (val < 0) val = 0
  if (val > maxUsablePoints.value) val = maxUsablePoints.value
  useBonusPoints.value = val
}

// 取得購物車資料
const fetchCart = async () => {
  const mId = userStore.memberId
  // console.log('讀取到的會員ID:', mId)
  if (!mId) {
    console.warn('請確認是否已登入')
    return
  }
  try {
    const res = await axios.get(`/cart/${mId}`)
    // console.log('購物車 API 回傳結果:', res.data)
    cartItems.value = res.data
  //  資料抓完後，立刻呼叫計算折扣的 API                   --->活動新增
    await calculateDiscount() 
  } catch (error) {
    console.error('獲取購物車失敗', error)
  }
}

// 計算總計金額
const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

// 後端 API 計算最新折扣                 --->活動新增
const calculateDiscount = async () => {
  if (cartItems.value.length === 0) {
    discountAmount.value = 0
    finalAmount.value = 0
    return
  }

  //防呆：先把應付金額預設為原價，避免 API 失敗時變成 0
  finalAmount.value = totalAmount.value

  try {
    // 將前端的 cartItems 轉成後端需要的 DTO 格式
    // ⚠️ 注意：請確保你的 res.data 裡面有 productId 和 categoryId，如果沒有，後端分類折扣會算不出來！
    const requestData = {
      cartItems: cartItems.value.map(item => ({
        productId: item.productId,
        categoryId: item.categoryId, 
        price: item.productPrice,
        quantity: item.quantity
      }))
    }
    console.log("傳給後端的購物車資料：", requestData);
    const res = await axios.post('/cart/calculate', requestData)
    
   // 將後端算好的金額存進變數
    discountAmount.value = res.data.discountAmount || 0
    finalAmount.value = res.data.finalAmount || 0
    //   5/14更新：同步後端傳回的原始總額與明細清單
    backendOriginalTotal.value = res.data.originalTotal || totalAmount.value //   5/14更新  
    appliedDiscounts.value = res.data.appliedDiscounts || []
    
    // ✨ 新增/修改：當活動折扣重算導致總額變更時，重新驗證紅利是否超過上限
    validateBonus()

    // 更新原本的 cartItems 的標籤 (對應 itemId)
    if (res.data.cartItems) {
      cartItems.value = cartItems.value.map(item => {
        const found = res.data.cartItems.find(i => i.itemId == item.itemId)
        if (found) {
          return {
            ...item,
            appliedDiscountText: found.appliedDiscountText,
            reminderText: found.reminderText
          }
        }
        return item
      })
    }
  } catch (error) {
    console.error('計算折扣失敗:', error)
  }
}

// 修改數量
const changeQty = async (item, delta) => {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    deleteItem(item.itemId)
    return
  }
  try {
    await axios.put(`/cart/update/${item.itemId}`, null, {
      params: { quantity: newQty },
    })
    await fetchCart()  //  觸發自動算前----->活動新增
//    fetchCart()  --->原本的
  } catch {
    Swal.fire('錯誤', '更新數量失敗', 'error')
  }
}

// 刪除商品
const deleteItem = async (itemId) => {
  // 詢問是否確定刪除
  const result = await Swal.fire({
    title: '確定要移除這項商品嗎？',
    text: '移除後需重新加入購物車喔！',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#fd7e14',
    confirmButtonText: '確定',
    cancelButtonText: '取消',
  })

  if (result.isConfirmed) {
    try {
      await axios.delete(`/cart/item/${itemId}`)

      // 告知刪除成功
      await Swal.fire({
        title: '已刪除！',
        text: '商品已從購物車中移除。',
        icon: 'success',
        confirmButtonColor: '#fd7e14',
        timer: 1500,
      })

      fetchCart()
    } catch (error) {
      console.error('刪除失敗:', error)
      Swal.fire('錯誤', '刪除失敗，請稍後再試', 'error')
    }
  }
}

// 結帳按鈕
const goToCheckout = () => {
  if (cartItems.value.length === 0) {
    Swal.fire('提示', '購物車是空的，無法結帳喔！', 'warning')
    return
  }
  // ✨ 新增/修改：前往結帳前，將決定的紅利點數存入 sessionStorage
  sessionStorage.setItem('usedBonusPoints', useBonusPoints.value || 0)
  router.push('/checkout')
}

onMounted(() => {
  fetchCart()
})
</script>

<template>
  <div class="cart-page-container">
    <div class="cart-card">
      <h2 class="cart-title">🛒 我的購物車</h2>

      <table class="cart-table">
        <thead>
          <tr>
            <th>商品名稱</th>
            <th>單價</th>
            <th>數量</th>
            <th>小計</th>
            <th>操作</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="item in cartItems" :key="item.itemId">
            <td style="text-align: left">
              <div>{{ item.productName }}</div>
              <div v-if="item.reminderText" class="cart-reminder-text" v-html="item.reminderText"></div>
              <div v-if="item.appliedDiscountText" class="cart-discount-badge">{{ item.appliedDiscountText }}</div>
            </td>
            <td class="price-text">$ {{ item.productPrice }}</td>
            <td>
              <button class="btn-qty" @click="changeQty(item, -1)">-</button>
              <span class="qty-display">{{ item.quantity }}</span>
              <button class="btn-qty" @click="changeQty(item, 1)">+</button>
            </td>
            <td class="price-text">$ {{ item.subtotal }}</td>
            <td>
              <button class="btn-del" @click="deleteItem(item.itemId)">
                <i class="fas fa-trash-can"></i> 刪除
              </button>
            </td>
          </tr>

          <tr v-if="cartItems.length === 0">
            <td colspan="5" class="empty-msg">💡 目前購物車是空的喔！</td>
          </tr>
        </tbody>
      </table>

      <div class="total-section-enhanced" v-if="cartItems.length > 0">
        <div class="cart-summary-container">
          <div class="cart-summary-line" style="font-size: 0.9em; color: #666;">
            <span class="cart-summary-label">商品總計：</span>
            <span>$ {{ totalAmount.toLocaleString() }}</span>
          </div>
          
          <div v-if="discountAmount > 0" class="cart-discount-row" @click="isDiscountExpanded = !isDiscountExpanded">
            <span class="cart-discount-label">
              <i :class="isDiscountExpanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'" class="cart-chevron-icon"></i>
              活動折抵明細：
            </span>
            <span class="cart-discount-value">- $ {{ discountAmount.toLocaleString() }}</span>
          </div>
          
          <div v-if="isDiscountExpanded && appliedDiscounts.length > 0" class="cart-detail-box">
            <div v-for="(ad, index) in appliedDiscounts" :key="index" class="cart-detail-line">
              <span class="cart-detail-name">{{ ad.detailText }}</span>
              <span class="cart-detail-amount">- $ {{ ad.amount.toLocaleString() }}</span>
            </div>
          </div>

          <div class="cart-bonus-row" style="margin-top: 10px; font-size: 0.9em; border-top: 1px dashed #eee; padding-top: 10px;">
            <label style="cursor: pointer; display: flex; align-items: center; justify-content: space-between; width: 100%;">
              <div style="color: #666;">
                <input type="checkbox" v-model="isBonusEnabled" @change="toggleBonus" />
                <span style="margin-left: 8px;">使用紅利點數折抵 (目前擁有: {{ userStore.user?.bonusPoints || 0 }} 點)</span>
              </div>
              <div :style="{ opacity: isBonusEnabled ? 1 : 0.4, color: isBonusEnabled ? '#000' : '#999' }">
                - $ 
                <input 
                  type="number" 
                  v-model.number="useBonusPoints" 
                  @input="validateBonus" 
                  :disabled="!isBonusEnabled" 
                  style="width: 60px; text-align: right; border: 1px solid #ddd; border-radius: 4px; padding: 2px 4px; outline: none;" 
                />
              </div>
            </label>
          </div>
          
          <div class="cart-final-line" style="margin-top: 10px;">
            <span class="cart-final-label">總計金額：</span>
            <span class="cart-final-value">$ {{ Math.max(0, finalAmount - useBonusPoints).toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <router-link to="/" class="btn-orange no-underline">
          <i class="fa-solid fa-house me-1"></i>返回首頁
        </router-link>

        <button v-if="cartItems.length > 0" class="btn-orange" @click="goToCheckout">
          <i class="fa-solid fa-credit-card me-1"></i>前往結帳
        </button>
      </div>

    </div>
  </div>
</template>

<style scoped>
@import '../assets/css/CartView.css';
</style>
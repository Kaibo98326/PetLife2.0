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
// --- 活動新增開始 ---
const discountAmount = ref(0)
const finalAmount = ref(0)
const backendOriginalTotal = ref(0) // 紀錄後端傳回的原價總計，確保對齊
const appliedDiscounts = ref([])    // 存活動折抵明細清單
const isDiscountExpanded = ref(false) // 控制明細收合狀態
// --- 活動新增結束 ---


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
    // --- 活動新增 ---
    appliedDiscounts.value = []
    return
  }

  //防呆：先把應付金額預設為原價，避免 API 失敗時變成 0
  finalAmount.value = totalAmount.value

  try {
    const requestData = {
      cartItems: cartItems.value.map(item => ({
        itemId: item.itemId, // 新增：供後端回傳後對應
        productId: item.productId,
        categoryId: item.categoryId, 
        price: item.productPrice,
        quantity: item.quantity
      }))
    }

    const res = await axios.post('/cart/calculate', requestData)
    
    // --- 活動新增：更新資料 ---
    discountAmount.value = res.data.discountAmount
    finalAmount.value = res.data.finalAmount
    backendOriginalTotal.value = res.data.originalTotal || totalAmount.value
    appliedDiscounts.value = res.data.appliedDiscounts || []

    cartItems.value = cartItems.value.map(item => {
      const match = res.data.cartItems.find(i => i.itemId === item.itemId)
      return {
        ...item,
        appliedDiscountText: match?.appliedDiscountText || '', 
        reminderText: match?.reminderText || ''               
      }
    })
    // --- 活動新增結束 ---
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
            <!-- <td style="text-align: left">{{ item.productName }}</td>              原先的            
            <td class="price-text">$ {{ item.productPrice }}</td> -->

            
                                                     <!--                    活動新增              -->
            <td style="text-align: left">
              {{ item.productName }}
              <div v-if="item.reminderText" class="cart-reminder-text">
                <i class="fas fa-circle-exclamation"></i> {{ item.reminderText }}
              </div>
            </td>
            <td class="price-text">
              $ {{ item.productPrice.toLocaleString() }}
              <span v-if="item.appliedDiscountText" class="cart-discount-badge">
                ({{ item.appliedDiscountText }})
              </span>
            </td>




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

          <!-- 空購物車顯示 -->
          <tr v-if="cartItems.length === 0">
            <td colspan="5" class="empty-msg">💡 目前購物車是空的喔！</td>
          </tr>
        </tbody>
      </table>

      <!-- 總計區塊                                 我先註解保留，下面測試活動算帳
      <div class="total-section" v-if="cartItems.length > 0">
        總計金額：<span class="price-text">$ {{ totalAmount }}</span>
      </div>

       按鈕區塊 
      <div class="action-buttons">
        <router-link to="/" class="btn-orange no-underline">
          <i class="fa-solid fa-house me-1"></i>返回首頁
        </router-link>

        <button v-if="cartItems.length > 0" class="btn-orange" @click="goToCheckout">
          <i class="fa-solid fa-credit-card me-1"></i>前往結帳
        </button>
      </div> -->

      <!-- 因應活動新增 -->

<!-- // 總計區塊                                  -->
   <div class="total-section-enhanced" v-if="cartItems.length > 0">
        
        <div class="cart-summary-line">
          <span class="cart-summary-label">商品總計：</span>
          <span class="cart-summary-value">$ {{ (backendOriginalTotal || totalAmount).toLocaleString() }}</span>
        </div>
        
        <div v-if="discountAmount > 0" class="cart-discount-row" @click="isDiscountExpanded = !isDiscountExpanded">
          <span class="cart-discount-label">
            {{ isDiscountExpanded ? '▲' : '▼' }} 活動折抵明細：
          </span>
          <span class="cart-discount-value">- $ {{ discountAmount.toLocaleString() }}</span>
        </div>

        <div v-if="isDiscountExpanded && discountAmount > 0" class="cart-detail-box">
          <template v-if="appliedDiscounts && appliedDiscounts.length > 0">
            <div v-for="(disc, idx) in appliedDiscounts" :key="idx" class="cart-detail-line">
              <span class="cart-detail-name">{{ disc.detailText }}</span>
              <span class="cart-detail-amount">- $ {{ disc.amount.toLocaleString() }}</span>
            </div>
          </template>
        </div>

        <div class="cart-final-line">
          <span class="cart-final-label">總計金額：</span>
          <span class="cart-final-value">$ {{ finalAmount.toLocaleString() }}</span>
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

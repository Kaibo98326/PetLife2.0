<script setup>
import { ref, onMounted} from 'vue'
import { useUserStore } from '@/stores/user'
import axios from '@/axios'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

// 1. 統一資料定義
const cartItems = ref([])
const totalAmount = ref(0)
const currentDate = ref(new Date().toLocaleString())

const orderForm = ref({
  orderName: '',
  orderPhone: '',
  orderAddress: '',
  orderPayment: 'LinePay',
  orderNote: ''
})

onMounted(async () => {
    
  if (!userStore.memberId) {
    Swal.fire('請先登入', '', 'info')
    router.push('/login')
    return
  }

  try {
    // 獲取會員預設資訊 (注意路徑：不加開頭斜線與 api)
    const memberRes = await axios.get(`cart/member/info/${userStore.memberId}`)
    const member = memberRes.data
    orderForm.value.orderName = member.memberName
    orderForm.value.orderPhone = member.phone
    orderForm.value.orderAddress = member.address

    // 獲取購物車內容與總金額
    const cartRes = await axios.get(`cart/${userStore.memberId}`)
    
    // 根據後端回傳格式賦值
    // 如果後端直接回傳 List，就用 cartRes.data；如果有封裝，就用 cartRes.data.items
    cartItems.value = cartRes.data.items || cartRes.data 
    totalAmount.value = cartRes.data.totalAmount || 0
    
    // 如果 totalAmount 是計算出來的，也可以這樣寫：
    if(!totalAmount.value && cartItems.value.length > 0) {
       totalAmount.value = cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
    }

  } catch (error) {
    console.error("載入資料失敗:", error)
  }
})

// 確認下單函式
const submitOrder = async () => {
  const result = await Swal.fire({
    title: '確認要下單嗎？',
    text: `付款方式：${orderForm.value.orderPayment}`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#f39c12',
    cancelButtonColor: '#d33',
    confirmButtonText: '確定下單',
    cancelButtonText: '再檢查一下',
    reverseButtons: true
  });

  if (!result.isConfirmed) return;

  try {
    Swal.fire({
      title: '訂單處理中...',
      text: '正在聯繫金流伺服器，請勿關閉視窗',
      allowOutsideClick: false,
      didOpen: () => { Swal.showLoading(); }
    });

    // 呼叫 API
    // 路徑要確認跟 OrderController 一樣
    const res = await axios.post(`/orders/checkout?cartId=${userStore.cartId}`, {
      ...orderForm.value,
      memberId: userStore.memberId
    });

    // 拿到後端回傳的資料 (order跟form表單)
    const { order, form } = res.data;

    // 訂單編號暫存，結帳成功頁面可以用
    sessionStorage.setItem('lastOrderId', order.orderId);

    // 綠界跳轉
    const div = document.createElement('div');
    div.innerHTML = form; // 將後端產出的 <form id="ecpayForm"> 塞進去
    document.body.appendChild(div);
    
    // 綠界表單內含自動submit的script，如果沒有跑舊手動觸發
    document.getElementById("ecpayForm").submit();

  } catch (error) {
    console.error("下單失敗:", error);
    Swal.fire({
      icon: 'error',
      title: '下單失敗',
      text: '金流連線異常，請稍後再試',
      confirmButtonColor: '#f39c12'
    });
  }
};
</script>

<template>
  <div class="checkout-container py-5">
    <div class="success-container shadow-sm rounded-3 overflow-hidden">
      <!-- 標題 -->
      <div class="header-banner bg-orange text-white p-3 text-center">
        <h2><i class="fas fa-paw me-2"></i> 填寫配送資訊</h2>
      </div>

      <form @submit.prevent="submitOrder" class="p-4 bg-white">
        <!-- 配送資訊表格 -->
        <table class="table table-bordered align-middle info-grid">
        <tbody>

            <tr>
                <td class="label-bg text-center fw-bold" style="width: 15%;">收件人姓名</td>
                <td style="width: 35%;">
                    <input type="text" v-model="orderForm.orderName" class="form-control" required>
                </td>
                <td class="label-bg text-center fw-bold" style="width: 15%;">聯絡電話</td>
            <td style="width: 35%;">
                <input type="text" v-model="orderForm.orderPhone" class="form-control" required>
            </td>
        </tr>
        <tr>
            <td class="label-bg text-center fw-bold">配送地址</td>
            <td colspan="3">
                <input type="text" v-model="orderForm.orderAddress" class="form-control" required>
            </td>
        </tr>
        <tr>
            <td class="label-bg text-center fw-bold">付款方式</td>
            <td>
                <select v-model="orderForm.orderPayment" class="form-select">
                    <option value="visa金融卡">visa金融卡</option>
                    <option value="信用卡">信用卡</option>
                    <option value="LinePay">LinePay</option>
                </select>
            </td>
        </tr>
        <tr>
            <td class="label-bg text-center fw-bold">訂單備註</td>
            <td colspan="3">
                <textarea v-model="orderForm.orderNote" class="form-control" rows="2" placeholder="有什麼想告訴毛孩店員的嗎？"></textarea>
            </td>
        </tr>
    </tbody>
    </table>

        <!-- 訂單摘要 -->
        <div class="list-title mt-4 mb-3 fw-bold fs-5 border-bottom pb-2">
          <i class="far fa-file-alt me-2"></i> 訂單摘要
        </div>

        <div class="summary-box px-3">
          <div class="d-flex justify-content-between py-2">
            <span>會員名稱</span>
            <span>{{ userStore.memberName || '載入中...' }}</span>
          </div>

          <div class="d-flex justify-content-between py-2">
            <span>訂單日期</span>
            <span>{{ currentDate }}</span>
          </div>

               <!-- 本次購買商品清單 -->
         <div class="purchase-section border-top pt-3">
           <div class="fw-bold mb-2 text-secondary"><i class="fas fa-shopping-bag me-1"></i> 本次購買</div>
        
           <div class="cart-items-list mb-3">
             <div v-for="item in cartItems" :key="item.productId" 
                  class="d-flex justify-content-between align-items-center py-2 border-bottom-dashed">
               <div class="item-info">
                 <span class="fw-bold" style="font-size: 0.95rem;">{{ item.productName }}</span>
                 <small class="text-muted ms-2">x {{ item.quantity }}</small>
               </div>
               <span class="text-dark fw-medium">$ {{ item.price * item.quantity }}</span>
             </div>
           </div>
         </div>

         <hr class="my-3">
          
          <div class="total-row d-flex justify-content-between align-items-center mt-3">
            <span class="total-label fs-5 fw-bold">應付總額：</span>
            <span class="total-amount fs-4 text-danger fw-bold">$ {{ totalAmount }}</span>
          </div>
        </div>
        
        <div class="button-area d-flex justify-content-end mt-5 me-3 gap-3">
          <!-- 回首頁按鈕 -->
          <router-link to="/" class="btn btn-orange px-4 py-2 fs-5 text-white shadow-sm">返回商店</router-link>
          <!-- 結帳按鈕 -->
          <button type="submit" class="btn btn-orange px-4 py-2 text-white fs-5 shadow-sm">前往結帳</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.success-container {
  max-width: 900px;
  margin: 0 auto;
  border: 1px solid #eee;
}

.bg-orange {
  background-color: #f39c12;
}

.text-orange {
  color: #f39c12;
}

.btn-orange {
  background-color: #f39c12;
  border: none;
  transition: 0.3s;
}

.btn-orange:hover {
  background-color: #e67e22;
  transform: translateY(-2px);
}

.label-bg {
  background-color: #fafafa;
}

.info-grid td {
  padding: 12px;
}

.form-control:focus, .form-select:focus {
  border-color: #f39c12;
  box-shadow: 0 0 0 0.25rem rgba(243, 156, 18, 0.25);
}
</style>
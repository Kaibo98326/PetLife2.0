<script setup>
import { ref, onMounted, watch } from 'vue'
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
  // 1. 彈出確認視窗
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

  // 點擊取消或關閉視窗就 return 回結帳畫面
  if (!result.isConfirmed) {
    return;
  }

  // 2. 確定後執行下單邏輯與金流分流
  try {
    // Loading 過場
    Swal.fire({
      title: '訂單處理中...',
      text: '正在聯繫金流伺服器，請勿關閉視窗',
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });

    const payload = {
      ...orderForm.value,
      memberId: userStore.memberId,
    };

    // 先在後端建立基礎訂單資訊
    const res = await axios.post('order/create', payload);
    const orderId = res.data.orderId; // 假設後端回傳對象包含 orderId

    // 根據支付方式執行跳轉
    if (orderForm.value.orderPayment === 'LinePay') {
      // --- LinePay 處理 ---
      const lpRes = await axios.post(`payment/linepay/${orderId}`);
      if (lpRes.data.paymentUrl) {
        window.location.href = lpRes.data.paymentUrl;
      } else {
        throw new Error('無法取得 LinePay 跳轉連結');
      }
    } else {
      // --- 信用卡 / visa金融卡 (綠界 ECPay) 處理 ---
      const ecRes = await axios.post(`payment/ecpay/${orderId}`);
      
      // 綠界回傳 <form> 字串
      const div = document.createElement('div');
      div.style.display = 'none';
      div.innerHTML = ecRes.data; 
      document.body.appendChild(div);
      
      const form = div.querySelector('form');
      if (form) {
        form.submit(); // 自動提交表單，瀏覽器會跳轉到綠界頁面
      } else {
        throw new Error('無法產出綠界支付表單');
      }
    }

  } catch (error) {
    console.error("下單失敗:", error);
    // 發生錯誤時，覆蓋掉前面的 Loading 視窗
    Swal.fire({
      icon: 'error',
      title: '下單失敗',
      text: error.response?.data?.message || '下單過程中發生問題，請稍後再試',
      confirmButtonColor: '#f39c12'
    });
  }
};

//以下 串金流要用的

// 暫存卡片資訊 (僅用於前端驗證，實際金流由綠界/LinePay處理)
const cardTemp = ref({
  cardNumber: '',
  expiryDate: '',
  cvv: ''
})
// 監聽付款方式切換
watch(() => orderForm.value.orderPayment, (newVal) => {
  if (newVal === 'visa金融卡' || newVal === '信用卡') {
    openCardModal(newVal);
  }
});

// 打開卡片資訊彈窗
const openCardModal = async (type) => {
  const { value: formValues } = await Swal.fire({
    title: `<div style="color: #f39c12; font-size: 1.5rem; font-weight: bold;">
              <i class="fas fa-paw"></i> 填寫${type}資訊
            </div>`,
    html: `
      <div style="padding: 10px; font-family: 'Noto Sans TC', sans-serif;">
        <!-- 卡號區塊：4碼一格 -->
        <div style="margin-bottom: 15px;">
          <label style="display: block; text-align: left; margin-bottom: 5px; color: #666; font-weight: 500;">信用卡號</label>
          <div style="display: flex; gap: 8px;" id="card-number-group">
            <input id="card-1" class="swal2-input card-field" placeholder="0000" maxlength="4" style="width: 25%; margin: 0; text-align: center; padding: 10px 0;">
            <input id="card-2" class="swal2-input card-field" placeholder="0000" maxlength="4" style="width: 25%; margin: 0; text-align: center; padding: 10px 0;">
            <input id="card-3" class="swal2-input card-field" placeholder="0000" maxlength="4" style="width: 25%; margin: 0; text-align: center; padding: 10px 0;">
            <input id="card-4" class="swal2-input card-field" placeholder="0000" maxlength="4" style="width: 25%; margin: 0; text-align: center; padding: 10px 0;">
          </div>
        </div>

        <div style="display: flex; gap: 15px;">
          <div style="flex: 1;">
            <label style="display: block; text-align: left; margin-bottom: 5px; color: #666; font-weight: 500;">有效日期</label>
            <input id="swal-expiry" class="swal2-input" placeholder="MM/YY" maxlength="5" 
              style="width: 100%; margin: 0; border-radius: 8px; border: 1px solid #ddd; box-sizing: border-box; font-size: 1.1rem;">
          </div>
          <div style="flex: 1;">
            <label style="display: block; text-align: left; margin-bottom: 5px; color: #666; font-weight: 500;">安全碼</label>
            <input id="swal-cvv" class="swal2-input" placeholder="CVV" maxlength="3" 
              style="width: 100%; margin: 0; border-radius: 8px; border: 1px solid #ddd; box-sizing: border-box; font-size: 1.1rem;">
          </div>
        </div>
      </div>`,
    didOpen: () => {
      // 監聽卡號輸入
      const fields = ['card-1', 'card-2', 'card-3', 'card-4'];
      fields.forEach((id, index) => {
        const el = document.getElementById(id);
        el.addEventListener('input', (e) => {
          // 只允許數字
          e.target.value = e.target.value.replace(/\D/g, '');
          
          // 打完4碼跳下一格
          if (e.target.value.length === 4 && index < fields.length - 1) {
            document.getElementById(fields[index + 1]).focus();
          }
        });
        
        // Backspace，沒內容可以跳回前一格
        el.addEventListener('keydown', (e) => {
          if (e.key === 'Backspace' && e.target.value.length === 0 && index > 0) {
            document.getElementById(fields[index - 1]).focus();
          }
        });
      });
    },
    background: '#fff',
    borderRadius: '15px',
    confirmButtonColor: '#f39c12',
    confirmButtonText: '確認資訊',
    showCancelButton: true,
    cancelButtonText: '返回',
    preConfirm: () => {
      // 組合卡號
      const card = [
        document.getElementById('card-1').value,
        document.getElementById('card-2').value,
        document.getElementById('card-3').value,
        document.getElementById('card-4').value
      ].join('');
      
      const expiry = document.getElementById('swal-expiry').value;
      const cvv = document.getElementById('swal-cvv').value;

      if (!/^\d{16}$/.test(card)) return Swal.showValidationMessage('請輸入完整的 16 位卡號');
      if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry)) return Swal.showValidationMessage('效期格式需為 MM/YY');
      if (!/^\d{3}$/.test(cvv)) return Swal.showValidationMessage('CVV 需為 3 位數字');

      return { number: card, expiry: expiry, cvv: cvv };
    }
  });

  if (formValues) {
    cardTemp.value = formValues; 
    Swal.fire({ icon: 'success', title: '卡片資訊已讀取', showConfirmButton: false, timer: 1000 });
  } else {
    orderForm.value.orderPayment = '';
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
        
        <!-- 結帳按鈕 -->
        <div class="button-area text-center mt-5">
          <button type="submit" class="btn btn-orange px-5 py-2 text-white fs-5 shadow-sm">確認下單</button>
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
<script setup>
import { onMounted, ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import axios from '@/axios'
import Swal from 'sweetalert2'

const userStore = useUserStore()
const cartItems = ref([])

// 取得購物車資料
const fetchCart = async () => {
  if (!userStore.user?.memberId) return
  try {
    const res = await axios.get(`/api/cart/${userStore.user.memberId}`)
    cartItems.value = res.data
  } catch (error) {
    console.error('獲取購物車失敗', error)
  }
}

// 計算總計金額
const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

// 修改數量
const changeQty = async (item, delta) => {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    deleteItem(item.itemId)
    return
  }
  try {
    await axios.put(`/api/cart/update/${item.itemId}`, null, {
      params: { quantity: newQty }
    })
    fetchCart()
  } catch {
    Swal.fire('錯誤', '更新數量失敗', 'error')
  }
}

// 刪除商品
const deleteItem = async (itemId) => {
  const result = await Swal.fire({
    title: '確定要移除這項商品嗎？',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#fd7e14',
    confirmButtonText: '確定',
    cancelButtonText: '取消'
  })

  if (result.isConfirmed) {
    try {
      await axios.delete(`/api/cart/item/${itemId}`)
      fetchCart()
    } catch {
      Swal.fire('錯誤', '刪除失敗', 'error')
    }
  }
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
            <td style="text-align: left;">{{ item.productName }}</td>
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

          <!-- 空購物車顯示 -->
          <tr v-if="cartItems.length === 0">
            <td colspan="5" class="empty-msg">💡 目前購物車是空的喔！</td>
          </tr>
        </tbody>
      </table>

      <!-- 總計區塊 -->
      <div class="total-section" v-if="cartItems.length > 0">
        總計金額：<span class="price-text">$ {{ totalAmount }}</span>
      </div>

      <!-- 按鈕區塊 -->
      <div class="action-buttons">
        <router-link to="/" class="btn-orange no-underline">
          <i class="fa-solid fa-house me-1"></i>返回首頁
        </router-link>

        <button v-if="cartItems.length > 0" class="btn-orange">
          <i class="fa-solid fa-credit-card me-1"></i>前往結帳
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>

.cart-page-container {
  padding: 50px 0;
  background-color: #f8f9fa;
  min-height: 80vh;
}

.cart-card {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.cart-title {
  color: #333;
  text-align: center;
  margin-bottom: 25px;
  font-weight: bold;
}

.cart-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.cart-table th {
  background-color: #fdf2e9;
  color: #e67e22;
  padding: 12px;
  border-bottom: 2px solid #fadbd8;
}

.cart-table td {
  padding: 15px;
  text-align: center;
  border-bottom: 1px solid #eee;
}

.price-text {
  color: #e67e22;
  font-weight: bold;
  font-size: 1.1rem;
}

.btn-qty {
  background-color: #eee;
  border: none;
  padding: 5px 12px;
  border-radius: 5px;
  cursor: pointer;
  transition: 0.3s;
}

.btn-qty:hover {
  background-color: #ddd;
}

.qty-display {
  padding: 0 15px;
  font-weight: bold;
}

.btn-del {
  background-color: #fff0f0;
  color: #e74c3c;
  border: 1px solid #ffcccc;
  padding: 8px 15px;
  border-radius: 5px;
  cursor: pointer;
}

.btn-del:hover {
  background-color: #e74c3c;
  color: white;
}

.total-section {
  text-align: right;
  font-size: 1.3rem;
  font-weight: bold;
  margin-top: 20px;
  padding-top: 10px;
  border-top: 2px solid #eee;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 25px;
}

.btn-orange {
  background-color: #fd7e14;
  color: white;
  padding: 12px 25px;
  border-radius: 8px;
  border: none;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  text-decoration: none;
}

.btn-orange:hover {
  background-color: #e86c00;
  color: white;
}

.no-underline {
  text-decoration: none;
}

.empty-msg {
  padding: 50px !important;
  color: #999;
  font-size: 1.2rem;
}
</style>
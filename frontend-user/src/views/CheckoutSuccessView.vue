<template>
  <div class="checkout-success">
    <div class="success-container">
      <!-- 頂部橘色 Banner -->
      <div class="header-banner">
        <h2><i class="fas fa-paw"></i> 訂單成立！感謝您的支持</h2>
      </div>
      
      <div class="info-section">
        <div class="section-title">📦 配送資訊</div>
        <table class="info-table">
          <tr>
            <td class="label">訂單編號</td>
            <td class="order-id">#{{ orderMain.orderId }}</td>
            <td class="label">成立時間</td>
            <td>{{ orderMain.orderDate }}</td>
          </tr>
          <tr>
            <td class="label">收件人</td>
            <td>{{ orderMain.orderName }}</td>
            <td class="label">配送地址</td>
            <td>{{ orderMain.orderAddress }}</td>
          </tr>
        </table>

        <div class="section-title">🛒 購買清單</div>
        <table class="item-table">
          <thead>
            <tr>
              <th class="text-left">項目</th>
              <th>數量</th>
              <th class="text-right">小計</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in orderItems" :key="index">
              <td class="text-left">{{ item.productName }}</td>
              <td>{{ item.quantity }}</td>
              <td class="text-right subtotal-cell">$ {{ item.subtotal }}</td>
            </tr>
          </tbody>
        </table>

        <!-- 總額 box -->
        <div class="total-box">
          <span class="total-label">應付總額：</span>
          <span class="total-price">$ {{ orderMain.orderTotal }}</span>
        </div>

        <!-- 按鈕區 -->
        <div class="actions">
          <router-link to="/" class="btn-orange">回到首頁</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';

// 預設呈現用的真實資料格式
const orderMain = reactive({
  orderId: '',
  orderDate: '',
  orderName: '',
  orderAddress: '',
  orderTotal: 0
});

const orderItems = reactive([]);
</script>

<style scoped>
.checkout-success {
  background-color: #f4f4f4;
  min-height: 100vh;
  padding: 50px 20px;
  --pet-orange: #f39c12; /* 依照你原稿的橘色 */
}

.success-container {
  max-width: 850px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  overflow: hidden;
}

.header-banner {
  background-color: var(--pet-orange);
  color: #fff;
  padding: 25px;
  text-align: center;
}

.header-banner h2 {
  margin: 0;
  font-size: 24px;
  letter-spacing: 1px;
}

.info-section {
  padding: 30px 40px;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  margin: 25px 0 15px;
  color: #444;
  display: flex;
  align-items: center;
}

/* 配送資訊表格 */
.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table td {
  padding: 15px;
  border: 1px solid #eee;
  font-size: 15px;
}

.label {
  background-color: #fafafa;
  width: 120px;
  font-weight: bold;
  color: #666;
}

.order-id {
  color: var(--pet-orange);
  font-weight: bold;
}

/* 購買清單表格 */
.item-table {
  width: 100%;
  border-collapse: collapse;
}

.item-table th {
  background-color: #f8f8f8;
  padding: 12px;
  border-bottom: 2px solid #eee;
  color: #555;
}

.item-table td {
  padding: 15px 12px;
  border-bottom: 1px solid #eee;
  text-align: center;
}

.subtotal-cell {
  font-weight: bold;
}

.text-left { text-align: left !important; padding-left: 20px !important; }
.text-right { text-align: right !important; padding-right: 20px !important; }

/* 總額 Box */
.total-box {
  margin-top: 30px;
  text-align: right;
  padding: 20px;
  background-color: #fff9f0;
  border-radius: 5px;
}

.total-label {
  font-size: 18px;
  font-weight: bold;
}

.total-price {
  font-size: 26px;
  font-weight: bold;
  color: #d35400;
  margin-left: 10px;
}

/* 按鈕 */
.actions {
  text-align: center;
  margin-top: 40px;
}

.btn-orange {
  background-color: var(--pet-orange);
  color: white;
  padding: 12px 50px;
  border-radius: 30px;
  text-decoration: none;
  font-weight: bold;
  font-size: 16px;
  transition: 0.3s;
  display: inline-block;
}

.btn-orange:hover {
  background-color: #e67e22;
  box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}
</style>
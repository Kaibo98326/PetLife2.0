<template>
  <div class="admin-stay-orders">
    <h1>後台訂單管理</h1>

    <!-- 搜尋表單 -->
    <div class="search-form">
      <input type="text" v-model="searchParams.stayId" placeholder="訂單編號" />
      <input type="text" v-model="searchParams.memberName" placeholder="會員名稱" />
      <input type="text" v-model="searchParams.memberPhone" placeholder="會員電話" />
      <select v-model="searchParams.stayStatus">
        <option value="">所有狀態</option>
        <option value="PENDING_PAYMENT">待支付</option>
        <option value="CONFIRMED">已確認</option>
        <option value="CHECKED_IN">已入住</option>
        <option value="CHECKED_OUT">已退房</option>
        <option value="CANCELLED">已取消</option>
      </select>
      <input type="date" v-model="searchParams.startDate" />
      <input type="date" v-model="searchParams.endDate" />
      <button @click="fetchOrders">搜尋</button>
    </div>

    <!-- 訂單列表 -->
    <table class="order-table">
      <thead>
        <tr>
          <th>訂單編號</th>
          <th>會員名稱</th>
          <th>寵物名稱</th>
          <th>房型</th>
          <th>入住日期</th>
          <th>退房日期</th>
          <th>總價</th>
          <th>狀態</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.stayId">
          <td>{{ order.stayId }}</td>
          <td>{{ order.memberName }}</td>
          <td>{{ order.mainPetName }}</td>
          <td>{{ order.roomTypeName }}</td>
          <td>{{ order.stayStartDate }}</td>
          <td>{{ order.stayEndDate }}</td>
          <td>{{ order.sumPrice }}</td>
          <td>{{ order.stayStatus }}</td>
          <td>
            <button @click="viewOrderDetails(order.stayId)">詳情</button>
            <button
              @click="confirmUpdateStatus(order.stayId, 'CONFIRMED')"
              v-if="order.stayStatus === 'PENDING_PAYMENT'"
            >
              確認
            </button>
            <button
              @click="confirmUpdateStatus(order.stayId, 'CHECKED_IN')"
              v-if="order.stayStatus === 'CONFIRMED'"
            >
              入住
            </button>
            <button
              @click="confirmUpdateStatus(order.stayId, 'CHECKED_OUT')"
              v-if="order.stayStatus === 'CHECKED_IN'"
            >
              退房
            </button>
            <button
              @click="confirmCancelOrder(order.stayId)"
              v-if="order.stayStatus !== 'CANCELLED' && order.stayStatus !== 'CHECKED_OUT'"
            >
              取消
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 分頁 -->
    <div class="pagination">
      <button @click="changePage(currentPage - 1)" :disabled="currentPage === 0">上一頁</button>
      <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages - 1">
        下一頁
      </button>
    </div>

    <!-- 訂單詳情彈窗 -->
    <div v-if="showDetailsModal" class="modal">
      <div class="modal-content">
        <h2>訂單詳情</h2>
        <pre>{{ selectedOrderDetails }}</pre>
        <button @click="showDetailsModal = false">關閉</button>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AdminStayOrders',
  data() {
    return {
      orders: [],
      searchParams: {
        page: 0,
        size: 10,
        stayId: null,
        stayStatus: null,
        memberName: null,
        memberPhone: null,
        startDate: null,
        endDate: null,
      },
      currentPage: 0,
      totalPages: 0,
      showDetailsModal: false,
      selectedOrderDetails: null,
    }
  },
  created() {
    this.fetchOrders()
  },
  methods: {
    // 獲取訂單列表
    async fetchOrders() {
      try {
        const response = await request.get('/api/admin/stay', { params: this.searchParams })
        // 根據 Spring Data Page 結構解析
        const data = response.data || response
        this.orders = data.content || []
        this.currentPage = data.number || 0
        this.totalPages = data.totalPages || 0
      } catch (error) {
        console.error('獲取訂單失敗:', error)
        alert('獲取訂單失敗！')
      }
    },
    // 查看單筆詳情
    async viewOrderDetails(stayId) {
      try {
        const response = await request.get(`/api/admin/stay/${stayId}`)
        this.selectedOrderDetails = response.data || response
        this.showDetailsModal = true
      } catch (error) {
        console.error('獲取詳情失敗:', error)
      }
    },
    // 更新訂單狀態
    async confirmUpdateStatus(stayId, newStatus) {
      if (confirm(`確定要更新狀態為 ${newStatus} 嗎？`)) {
        try {
          await request.patch(`/api/admin/stay/${stayId}/status`, null, { params: { newStatus } })
          alert('狀態更新成功！')
          this.fetchOrders()
        } catch (error) {
          alert('更新失敗：' + (error.response?.data?.message || error.message))
        }
      }
    },
    // 取消訂單
    async confirmCancelOrder(stayId) {
      if (confirm('確定要取消此訂單嗎？')) {
        try {
          await request.patch(`/api/admin/stay/${stayId}/cancel`)
          alert('訂單已取消！')
          this.fetchOrders()
        } catch (error) {
          alert('取消失敗')
        }
      }
    },
    changePage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.searchParams.page = page
        this.fetchOrders()
      }
    },
  },
}
</script>

<style scoped>
.admin-stay-orders {
  padding: 20px;
}
.search-form {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.search-form input,
.search-form select,
.search-form button {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.order-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}
.order-table th,
.order-table td {
  border: 1px solid #eee;
  padding: 8px;
  text-align: left;
}
.order-table th {
  background-color: #f2f2f2;
}
.order-table button {
  margin-right: 5px;
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  background-color: #007bff;
  color: white;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}
.pagination button {
  padding: 8px 15px;
  border: 1px solid #007bff;
  border-radius: 4px;
  background-color: #007bff;
  color: white;
  cursor: pointer;
}
.pagination button:disabled {
  background-color: #ccc;
  border-color: #ccc;
  cursor: not-allowed;
}
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 80%;
  max-height: 80%;
  overflow-y: auto;
}
</style>

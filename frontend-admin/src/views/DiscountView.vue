<script setup>
import { onMounted, ref, computed, watch } from 'vue';
import { useDiscount } from '@/stores/useDiscount';
import DiscountFormModal from '@/components/DiscountFormModal.vue';
// 建立的明細彈窗組件
import OrderDiscountModal from '@/components/OrderDiscountModal.vue';
// 入員工 Store 來拿 Token
import { useEmployeeStore } from '@/stores/employee';
const employeeStore = useEmployeeStore();


import axios from 'axios';
import Swal from 'sweetalert2';

// 建立一個 ref 來綁定子組件實例
const discountModalRef = ref(null);

// 定義點擊函數，透過 ref 呼叫子組件內部的 openModal 方法
const viewOrderDiscountDetails = (orderId) => {
    discountModalRef.value.openModal(orderId);
};

const { 
    API_BASE_URL, discounts, discountTypesList, 
    searchActivityName, statusFilter, scopeFilter, typeFilter,
    fetchDiscounts, fetchDiscountTypes, deleteActivity, filteredDiscounts, getStatusBadge 
} = useDiscount();

const showAdvancedFilter = ref(false);
const formModalRef = ref(null);

const currentPage = ref(1);
const pageSize = 10;
const totalPages = computed(() => Math.ceil(filteredDiscounts.value.length / pageSize));
const paginatedDiscounts = computed(() => {
    const start = (currentPage.value - 1) * pageSize;
    return filteredDiscounts.value.slice(start, start + pageSize);
});

watch([searchActivityName, statusFilter, scopeFilter, typeFilter], () => { currentPage.value = 1; });


// ✨ 新增：明細變數與功能
const discountDetails = ref([]);

const viewDiscountDetails = async (discountId) => {
  try {
    // 呼叫後端寫好的 API (加上 headers 帶入 Token)
    const res = await axios.get(`http://localhost:8082/api/order-discounts/discount/${discountId}`, {
      headers: {
        Authorization: `Bearer ${employeeStore.token}`
      }
    });
    
    discountDetails.value = res.data;

    // 防呆：如果沒有人使用過這個活動
    if (discountDetails.value.length === 0) {
      Swal.fire('提示', '目前還沒有訂單使用此活動喔！', 'info');
      return;
    }

    // 組合要顯示在彈跳視窗內的 HTML 表格字串
    let tableHtml = `
      <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 14px;">
        <thead>
          <tr style="border-bottom: 2px solid #ddd;">
            <th style="padding: 8px;">訂單編號</th>
            <th style="padding: 8px;">商品編號</th>
            <th style="padding: 8px;">數量</th>
            <th style="padding: 8px;">折扣金額</th>
          </tr>
        </thead>
        <tbody>
    `;
    let totalDiscountAmount = 0;

    discountDetails.value.forEach(detail => {
      tableHtml += `
        <tr style="border-bottom: 1px solid #eee;">
          <td style="padding: 8px;">#${detail.orderId}</td>
          <td style="padding: 8px;">${detail.productId}</td>
          <td style="padding: 8px;">${detail.quantity}</td>
          <td style="padding: 8px; color: red;">- $${detail.discountAmount}</td>
        </tr>
      `;
      totalDiscountAmount += detail.discountAmount;
    });

    tableHtml += `
        </tbody>
      </table>
      <div style="text-align: right; margin-top: 15px; font-weight: bold;">
        累計折抵總額：<span style="color: red;">$${totalDiscountAmount}</span>
      </div>
    `;

    // 使用 SweetAlert2 顯示明細
    Swal.fire({
      title: '活動使用明細',
      html: tableHtml,
      width: '600px',
      confirmButtonColor: '#198754', // 使用你的綠色主題
      confirmButtonText: '關閉'
    });

  } catch (error) {
    console.error('獲取活動明細失敗:', error);
    Swal.fire('錯誤', '無法獲取明細資料，請檢查網路連線', 'error');
  }
};





onMounted(() => {
    fetchDiscounts();
    fetchDiscountTypes();
});
</script>

<template>
    <div class="container-fluid py-3">
        <div id="listView">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div class="d-flex gap-2 align-items-center">
                    <input type="text" class="form-control" v-model="searchActivityName" placeholder="🔍 搜尋活動名稱..." style="width: 250px;">
                    <div class="dropdown position-relative">
                        <button class="btn btn-outline-secondary dropdown-toggle position-relative" type="button" @click="showAdvancedFilter = !showAdvancedFilter">
                            ⚙️ 進階篩選
                            <span v-if="statusFilter !== 'all' || scopeFilter !== 'all' || typeFilter !== 'all'" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.7rem; padding: 0.35em 0.6em;">已套用</span>
                        </button>
                        
                        <div v-if="showAdvancedFilter" class="position-fixed top-0 start-0 w-100 h-100" style="z-index: 1040;" @click="showAdvancedFilter = false"></div>
                        <div v-if="showAdvancedFilter" class="dropdown-menu shadow-lg p-4 rounded-3 mt-2 d-block" style="min-width: 480px; position: absolute; z-index: 1050; top: 100%; left: 0;">
                            
                            <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                                <h6 class="mb-0 fw-bold">⚙️ 進階篩選條件</h6>
                                <button class="btn btn-sm btn-light text-muted border px-3" @click="statusFilter = 'all'; scopeFilter = 'all'; typeFilter = 'all'; searchActivityName = ''">清除重設</button>
                            </div>
                            
                            <div class="mb-4">
                                <div class="text-muted small mb-2">適用範圍 (Scope)</div>
                                <div class="d-flex flex-wrap gap-2 filter-group">
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: scopeFilter==='all'}" @click="scopeFilter='all'">全部</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: scopeFilter==='1'}" @click="scopeFilter='1'">📂 指定分類</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: scopeFilter==='2'}" @click="scopeFilter='2'">📦 指定單品</button>
                                </div>
                            </div>
                            <div class="mb-4">
                                <div class="text-muted small mb-2">折扣類型 (Type)</div>
                                <div class="d-flex flex-wrap gap-2 filter-group">
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: typeFilter==='all'}" @click="typeFilter='all'">全部</button>
                                    <button v-for="t in discountTypesList" :key="t.discountTypeId" class="btn btn-outline-secondary btn-sm" :class="{active: typeFilter === t.discountTypeId.toString()}" @click="typeFilter = t.discountTypeId.toString()">{{ t.discountTypeName }}</button>
                                </div>
                            </div>
                            <div class="mb-2">
                                <div class="text-muted small mb-2">活動狀態 (Status)</div>
                                <div class="d-flex flex-wrap gap-2 filter-group">
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: statusFilter==='all'}" @click="statusFilter='all'">全部</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: statusFilter==='not_started'}" @click="statusFilter='not_started'">🔵 尚未開始</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: statusFilter==='upcoming'}" @click="statusFilter='upcoming'">🟡 即將開始</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: statusFilter==='active'}" @click="statusFilter='active'">🟢 進行中</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: statusFilter==='expired'}" @click="statusFilter='expired'">⚪ 已結束</button>
                                    <button class="btn btn-outline-secondary btn-sm" :class="{active: statusFilter==='inactive'}" @click="statusFilter='inactive'">🔴 已停用</button>
                                    <button class="btn btn-outline-dark btn-sm" :class="{active: statusFilter==='deleted'}" @click="statusFilter='deleted'">🗑️ 已刪除 (隱藏)</button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <button class="btn btn-success fw-bold" @click="formModalRef.openAdd()">➕ 新增活動</button>
                </div>
                
                <div class="d-flex justify-content-end align-items-center gap-4 text-muted">
                    <div><span>總活動數</span> <strong class="text-dark">{{ discounts.filter(d => d.status !== 'deleted').length }}</strong></div>
                    <div><span>搜尋結果</span> <strong class="text-dark">{{ (searchActivityName || statusFilter !== 'all' || scopeFilter !== 'all' || typeFilter !== 'all') ? filteredDiscounts.length : 0 }}</strong></div>
                </div>
            </div>

            <div class="table-responsive bg-white rounded shadow-sm border p-3">
                <table class="table table-hover align-middle">
                    <thead class="table-light"><tr><th>編號</th><th>狀態</th><th>適用範圍</th><th>活動名稱</th><th>折扣類別</th><th>活動期間</th><th>操作</th></tr></thead>
                    <tbody>
                        <tr v-if="paginatedDiscounts.length === 0"><td colspan="7" class="text-center text-muted py-4">目前尚無活動資料</td></tr>
                        <tr v-for="(item, index) in paginatedDiscounts" :key="item.discountId">
                            <td class="fw-bold">{{ (currentPage - 1) * pageSize + index + 1 }}</td> 
                            <td><span class="badge rounded-pill px-3 py-2 shadow-sm" :class="getStatusBadge(item).class">{{ getStatusBadge(item).text }}</span></td>
                            <td>
                                <span v-if="item.scopeType === 1" class="badge bg-info-subtle text-info border border-info-subtle px-2">📂 分類清單</span>
                                <span v-else class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">📦 單品清單</span>
                            </td>
                            <td>{{ item.discountName }}</td>
                            <td>{{ item.discountType ? item.discountType.discountTypeName : '' }}</td>
                            <td>{{ item.startDate }} ~ {{ item.endDate }}</td>
                            <td>
                                <button class="btn btn-link text-primary text-decoration-none p-0 me-3 fw-bold" @click="formModalRef.openEdit(item)">查看/修改</button>
                                <button class="btn btn-link text-danger text-decoration-none p-0 fw-bold" @click="deleteActivity(item)">刪除</button>

                                <button class="btn btn-link text-info text-decoration-none p-0 fw-bold" @click="viewDiscountDetails(item.discountId)">明細</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div v-if="totalPages > 1" class="d-flex justify-content-center mt-4">
                    <nav>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item" :class="{ disabled: currentPage === 1 }"><button class="page-link" @click="currentPage--">上一頁</button></li>
                            <li v-for="page in totalPages" :key="page" class="page-item" :class="{ active: currentPage === page }"><button class="page-link" @click="currentPage = page">{{ page }}</button></li>
                            <li class="page-item" :class="{ disabled: currentPage === totalPages }"><button class="page-link" @click="currentPage++">下一頁</button></li>
                        </ul>
                    </nav>
                </div>
            </div>
        </div>

        <DiscountFormModal ref="formModalRef" :api-base-url="API_BASE_URL" :discount-types="discountTypesList" @saved="fetchDiscounts" />
    </div>
</template>

<style scoped>
.filter-group button.active { background-color: #6c757d; color: white; border-color: #6c757d; }
.filter-group button.btn-outline-dark.active { background-color: #212529; color: white; border-color: #212529; }
.page-link { cursor: pointer; }
.pagination .active .page-link { background-color: #198754; border-color: #198754; color: white; }
</style>
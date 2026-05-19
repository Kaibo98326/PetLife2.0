<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';
import * as bootstrap from 'bootstrap';

// 狀態變數
const currentOrderId = ref(null);
const rawData = ref([]);
const searchQuery = ref('');
const currentPage = ref(1);
const pageSize = 15;
const isLoading = ref(false);

// 1. 開啟 Modal 並獲取資料 (供父元件呼叫)
const openModal = async (orderId) => {
    currentOrderId.value = orderId;
    searchQuery.value = '';
    currentPage.value = 1;
    rawData.value = [];
    isLoading.value = true;

    try {
        // 呼叫我們先前寫好的 API
        const res = await axios.get(`http://localhost:8082/api/order-discounts/order/${orderId}`);
        rawData.value = res.data || [];
        
        // 顯示 Modal
        const el = document.getElementById('orderDiscountModal');
        const modal = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
        modal.show();
    } catch (error) {
        console.error('獲取訂單明細失敗:', error);
    } finally {
        isLoading.value = false;
    }
};

// 暴露方法給父元件使用
defineExpose({ openModal });

// 2. 資料分組 (Group by Product)
const groupedData = computed(() => {
    const groups = {};
    rawData.value.forEach(item => {
        if (!groups[item.productId]) {
            groups[item.productId] = {
                productId: item.productId,
                // 如果後端沒傳商品名稱，給予防呆預設值
                productName: item.productName || '未知商品', 
                totalQuantity: 0,
                totalDiscount: 0,
                expanded: false,
                details: []
            };
        }
        // 處理數量與累加金額
        groups[item.productId].totalQuantity = Math.max(groups[item.productId].totalQuantity, item.quantity || 1);
        groups[item.productId].totalDiscount += item.discountAmount || 0;
        groups[item.productId].details.push(item);
    });
    return Object.values(groups);
});

// 3. 搜尋過濾邏輯
const filteredData = computed(() => {
    if (!searchQuery.value) return groupedData.value;
    const q = searchQuery.value.toLowerCase();
    return groupedData.value.filter(g => 
        g.productName.toLowerCase().includes(q) || 
        g.productId.toString().includes(q)
    );
});

// 4. 分頁邏輯 (超過 15 筆啟動)
const totalPages = computed(() => Math.ceil(filteredData.value.length / pageSize));
const paginatedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize;
    return filteredData.value.slice(start, start + pageSize);
});

// 5. 總計金額計算
const totalOrderDiscount = computed(() => {
    return rawData.value.reduce((sum, item) => sum + (item.discountAmount || 0), 0);
});

// 展開/摺疊單一商品
const toggleRow = (group) => {
    group.expanded = !group.expanded;
};
</script>

<template>
    <div class="modal fade" id="orderDiscountModal" tabindex="-1" data-bs-backdrop="static">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <div class="modal-content border-0 shadow">
                
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">
                        <i class="fas fa-tags text-danger me-2"></i>訂單編號：#{{ currentOrderId }} 優惠套用明細
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <div class="modal-body p-4">
                    <div v-if="isLoading" class="text-center py-5 text-muted">
                        <div class="spinner-border spinner-border-sm me-2"></div> 資料讀取中...
                    </div>

                    <div v-else-if="rawData.length === 0" class="text-center py-5 bg-light rounded border">
                        <i class="fas fa-box-open fa-3x text-muted mb-3"></i>
                        <h5 class="text-muted fw-bold">本訂單無套用任何優惠活動</h5>
                    </div>

                    <div v-else>
                        <div class="mb-3">
                            <div class="input-group shadow-sm">
                                <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
                                <input type="text" class="form-control border-start-0 ps-0" 
                                       v-model="searchQuery" 
                                       placeholder="請輸入商品名稱或 ID 搜尋"
                                       @input="currentPage = 1">
                            </div>
                        </div>

                        <div class="table-responsive border rounded shadow-sm">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th style="width: 50%;">商品資訊</th>
                                        <th class="text-center">數量</th>
                                        <th class="text-end">總折抵金額</th>
                                        <th class="text-center" style="width: 50px;"></th>
                                    </tr>
                                </thead>
                                <tbody v-for="group in paginatedData" :key="group.productId">
                                    
                                    <tr @click="toggleRow(group)" style="cursor: pointer;">
                                        <td>
                                            <div class="fw-bold">{{ group.productName }}</div>
                                            <div class="text-muted small">#{{ group.productId }}</div>
                                        </td>
                                        <td class="text-center">{{ group.totalQuantity }}</td>
                                        <td class="text-end text-danger fw-bold">- ${{ group.totalDiscount.toLocaleString() }}</td>
                                        <td class="text-center text-muted">
                                            <i class="fas" :class="group.expanded ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
                                        </td>
                                    </tr>

                                    <tr v-if="group.expanded" class="bg-light">
                                        <td colspan="4" class="p-3">
                                            <div class="d-flex flex-column gap-2">
                                                <div v-for="(detail, idx) in group.details" :key="idx" 
                                                     class="d-flex justify-content-between align-items-center bg-white p-2 rounded border-start border-danger border-4 shadow-sm">
                                                    <div>
                                                        <span class="badge bg-danger-subtle text-danger border border-danger-subtle me-2">🏷️ 活動</span>
                                                        <span class="fw-bold text-dark">
                                                            {{ detail.discountName || detail.discountTypeName || '專屬折扣' }}
                                                        </span>
                                                        <span v-if="detail.calculationDesc" class="text-muted small ms-2">
                                                            ({{ detail.calculationDesc }})
                                                        </span>
                                                    </div>
                                                    <div class="text-danger fw-bold">
                                                        - ${{ detail.discountAmount.toLocaleString() }}
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>

                                </tbody>
                                <tbody v-if="paginatedData.length === 0">
                                    <tr><td colspan="4" class="text-center text-muted py-4">找不到符合的商品</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="modal-footer bg-light d-flex justify-content-between align-items-center" v-if="rawData.length > 0">
                    
                    <div>
                        <nav v-if="totalPages > 1">
                            <ul class="pagination pagination-sm mb-0 shadow-sm">
                                <li class="page-item" :class="{ disabled: currentPage === 1 }">
                                    <button class="page-link" @click="currentPage--">上一頁</button>
                                </li>
                                <li class="page-item disabled">
                                    <span class="page-link text-dark">{{ currentPage }} / {{ totalPages }}</span>
                                </li>
                                <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                                    <button class="page-link" @click="currentPage++">下一頁</button>
                                </li>
                            </ul>
                        </nav>
                    </div>

                    <div class="fs-5 fw-bold text-dark">
                        本單累計優惠：<span class="text-danger">- ${{ totalOrderDiscount.toLocaleString() }}</span>
                    </div>

                </div>

            </div>
        </div>
    </div>
</template>

<style scoped>
/* 增加層次感的簡單樣式 */
.table-hover tbody tr:hover { background-color: #fcfcfc; }
.bg-danger-subtle { background-color: #f8d7da !important; }
</style>
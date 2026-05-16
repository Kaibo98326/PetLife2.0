<script setup>
import { onMounted, ref, computed, watch } from 'vue';
import { useDiscount } from '@/stores/useDiscount';
import DiscountFormModal from '@/components/DiscountFormModal.vue';


import DiscountUsageDetailModal from '@/components/DiscountUsageDetailModal.vue';

// 引入剛建立的快速新增組件
import QuickCategoryAdd from '@/components/QuickCategoryAdd.vue';

// 引入自己封裝的 request，並移除了 axios
import request from '@/utils/request';
import Swal from 'sweetalert2';

// 使用 useDiscount Pinia Store 管理折扣相關狀態與邏輯
const { 
    discounts, discountTypesList, 
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

// ✨ 修改：標籤新增成功後的處理 (可選，這裡主要做提醒或預刷新)
const handleTagSuccess = () => {
    // 這裡不需要額外動作，因為 DiscountFormModal 開啟時會重新 fetchOptions
    // 但如果想更新搜尋列的某些資訊也可以寫在這裡
    console.log('新標籤已就緒');
};

// 明細變數與功能
const discountDetails = ref([]);

// 綁定新的明細組件 Ref
const usageDetailModalRef = ref(null);

// 將原本又長又難維護的 Swal 拼接徹底刪除，改為一鍵呼叫新組件
const viewDiscountDetails = (discountId) => {
    if (usageDetailModalRef.value) {
        usageDetailModalRef.value.openModal(discountId);
    }
};

onMounted(() => {
    fetchDiscounts();
    fetchDiscountTypes();
});
</script>

<template>
    <div class="container-fluid py-3">
        
        <QuickCategoryAdd @success="handleTagSuccess" />

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

        <DiscountFormModal ref="formModalRef" :discount-types="discountTypesList" @saved="fetchDiscounts" />
        
        <DiscountUsageDetailModal ref="usageDetailModalRef" />
    </div>
</template>


<style scoped>
.filter-group button.active { background-color: #6c757d; color: white; border-color: #6c757d; }
.filter-group button.btn-outline-dark.active { background-color: #212529; color: white; border-color: #212529; }
.page-link { cursor: pointer; }
.pagination .active .page-link { background-color: #198754; border-color: #198754; color: white; }
</style>
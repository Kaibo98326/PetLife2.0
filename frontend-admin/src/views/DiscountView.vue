<script setup>
import { onMounted, ref, computed, watch } from 'vue';
import { useDiscount } from '@/stores/useDiscount';
import DiscountFormModal from '@/components/DiscountFormModal.vue';
import DiscountUsageDetailModal from '@/components/DiscountUsageDetailModal.vue';
import QuickCategoryAdd from '@/components/QuickCategoryAdd.vue';
import request from '@/utils/request';
import Swal from 'sweetalert2';

// 使用 useDiscount Pinia Store 管理折扣相關狀態與邏輯
const { 
    discounts, discountTypesList, 
    searchActivityName, statusFilter, scopeFilter, typeFilter, selectedTagFilter,
    fetchDiscounts, fetchDiscountTypes, deleteActivity, filteredDiscounts, getStatusBadge 
} = useDiscount();

const showAdvancedFilter = ref(false);
const formModalRef = ref(null);

const currentPage = ref(1);
const pageSize = ref(10);
// ✨ 修改：總筆數改為動態連動「進階篩選後」的陣列長度！這樣點選篩選按鈕時，分頁總數才會跟著即時變動
const totalElements = computed(() => {
    return Array.isArray(filteredDiscounts.value) ? filteredDiscounts.value.length : 0;
});

// ✨ 修改：純前端分頁裁切。直接對過濾後的完整資料進行 .slice 裁切出當前頁面的 10 筆，確保分頁與篩選不衝突
const paginatedDiscounts = computed(() => {
    if (!Array.isArray(filteredDiscounts.value)) return [];
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredDiscounts.value.slice(start, start + pageSize.value);
});

// ✨ 修改：頁碼改變時的處理函數。純前端分頁不需要重新呼叫後端 API，直接重置當前頁碼即可
const handlePageChange = (val) => {
    currentPage.value = val;
};

watch([searchActivityName, statusFilter, scopeFilter, typeFilter, selectedTagFilter], () => { currentPage.value = 1; });

const handleTagSuccess = () => {
    console.log('新標籤已就緒');
};

const discountDetails = ref([]);
const usageDetailModalRef = ref(null);

const viewDiscountDetails = (discountId) => {
    if (usageDetailModalRef.value) {
        usageDetailModalRef.value.openModal(discountId);
    }
};

onMounted(() => {
    // 若 store 支援，這裡可改為 fetchDiscounts(currentPage.value, pageSize.value)
    fetchDiscounts();
    fetchDiscountTypes();
});
</script>

<template>
    <div class="container-fluid py-4 px-4">
        
        <QuickCategoryAdd @success="handleTagSuccess" @tag-select="(tagId) => selectedTagFilter = tagId" />

        <div id="listView">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div class="d-flex gap-2 align-items-center">
                    <input type="text" class="form-control" v-model="searchActivityName" placeholder="🔍 搜尋活動名稱..." style="width: 250px;">
                    <button class="btn btn-outline-secondary position-relative" type="button" @click="showAdvancedFilter = !showAdvancedFilter">
                        ⚙️ 進階篩選
                        <span v-if="statusFilter !== 'all' || scopeFilter !== 'all' || typeFilter !== 'all'" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.7rem; padding: 0.35em 0.6em;">已套用</span>
                    </button>
                    <button class="btn btn-success fw-bold" @click="formModalRef.openAdd()">➕ 新增活動</button>
                </div>
                
                <div class="d-flex justify-content-end align-items-center gap-4 text-muted">
                    <div><span>總活動數</span> <strong class="text-dark">{{ discounts.filter(d => d.status !== 'deleted').length }}</strong></div>
                    <div><span>搜尋結果</span> <strong class="text-dark">{{ (searchActivityName || statusFilter !== 'all' || scopeFilter !== 'all' || typeFilter !== 'all') ? filteredDiscounts.length : 0 }}</strong></div>
                </div>
            </div>

            <div v-show="showAdvancedFilter" class="bg-light border rounded-3 p-4 mb-4 shadow-sm transition-all">
                <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                    <h6 class="mb-0 fw-bold">⚙️ 進階篩選條件</h6>
                    <button class="btn btn-sm btn-white text-muted border px-3 bg-white shadow-sm" @click="statusFilter = 'all'; scopeFilter = 'all'; typeFilter = 'all'; searchActivityName = ''">清除重設</button>
                </div>
                
                <div class="row">
                    <div class="col-md-4 mb-3 mb-md-0">
                        <div class="text-muted small mb-2 fw-bold">適用範圍 (Scope)</div>
                        <div class="d-flex flex-wrap gap-2 filter-group">
                            <button class="btn btn-outline-secondary btn-sm" :class="{active: scopeFilter==='all'}" @click="scopeFilter='all'">全部</button>
                            <button class="btn btn-outline-secondary btn-sm" :class="{active: scopeFilter==='1'}" @click="scopeFilter='1'">📂 指定分類</button>
                            <button class="btn btn-outline-secondary btn-sm" :class="{active: scopeFilter==='2'}" @click="scopeFilter='2'">📦 指定單品</button>
                        </div>
                    </div>
                    
                    <div class="col-md-8 mb-3 mb-md-0">
                        <div class="text-muted small mb-2 fw-bold">折扣類型 (Type)</div>
                        <div class="d-flex flex-wrap gap-2 filter-group">
                            <button class="btn btn-outline-secondary btn-sm" :class="{active: typeFilter==='all'}" @click="typeFilter='all'">全部</button>
                            <button v-for="t in discountTypesList" :key="t.discountTypeId" class="btn btn-outline-secondary btn-sm" :class="{active: typeFilter === t.discountTypeId.toString()}" @click="typeFilter = t.discountTypeId.toString()">{{ t.discountTypeName }}</button>
                        </div>
                    </div>
                    
                    <div class="col-12 mt-md-3">
                        <div class="text-muted small mb-2 fw-bold">活動狀態 (Status)</div>
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

            <div class="card border-0 shadow-sm rounded-3 p-4 bg-white">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="py-3" style="width: 60px;">編號</th>
                                <th class="py-3 text-center text-nowrap">狀態</th>
                                <th class="py-3 text-center text-nowrap">適用範圍</th>
                                <th class="py-3" style="width: auto;">活動名稱</th>
                                <th class="py-3 text-center text-nowrap">折扣類別</th>
                                <th class="py-3" style="width: 240px;">活動期間</th>
                                <th class="py-3 text-center" style="width: 180px;">操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-if="paginatedDiscounts.length === 0"><td colspan="7" class="text-center text-muted py-5">目前尚無活動資料</td></tr>
                            <tr v-for="(item, index) in paginatedDiscounts" :key="item.discountId">
                                <td class="fw-bold py-3 text-muted">{{ (currentPage - 1) * pageSize + index + 1 }}</td> 
                                <td class="py-3 text-center"><span class="badge rounded-pill px-3 py-2 shadow-sm" :class="getStatusBadge(item).class">{{ getStatusBadge(item).text }}</span></td>
                                <td class="py-3 text-center">
                                    <span v-if="item.scopeType === 1" class="badge bg-info-subtle text-info border border-info-subtle px-2 py-1">📂 分類清單</span>
                                    <span v-else class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">📦 單品清單</span>
                                </td>
                                <td class="py-3 fw-bold text-dark">{{ item.discountName }}</td>
                                <td class="py-3 text-center text-muted">{{ item.discountType ? item.discountType.discountTypeName : '' }}</td>
                                <td class="py-3 text-muted small">{{ item.startDate }} ~ {{ item.endDate }}</td>
                                <td class="py-3 text-center">
                                    <div class="d-flex justify-content-center align-items-center gap-2 text-nowrap">
                                        <button class="btn btn-link text-primary text-decoration-none p-0 fw-bold" style="font-size: 0.9em;" @click="formModalRef.openEdit(item)">查看/修改</button>
                                        <span class="text-muted opacity-50">|</span>
                                        <button class="btn btn-link text-danger text-decoration-none p-0 fw-bold" style="font-size: 0.9em;" @click="deleteActivity(item)">刪除</button>
                                        <span class="text-muted opacity-50">|</span>
                                        <button class="btn btn-link text-info text-decoration-none p-0 fw-bold" style="font-size: 0.9em;" @click="viewDiscountDetails(item.discountId)">明細</button>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                
                <div class="d-flex justify-content-center mt-4 pt-3 border-top">
                    <el-pagination
                        v-model:current-page="currentPage"
                        :page-size="pageSize"
                        :total="totalElements"
                        background
                        layout="total, prev, pager, next, jumper"
                        @current-change="handlePageChange"
                    />
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
</style>
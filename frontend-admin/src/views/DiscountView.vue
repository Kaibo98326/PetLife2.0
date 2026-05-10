<script setup>
import { onMounted, ref } from 'vue';
import { useDiscount } from '@/composables/useDiscount';
import DiscountFormModal from '@/components/DiscountFormModal.vue';

// 引入大腦 (邏輯)
const { 
    API_BASE_URL, discounts, discountTypesList, 
    searchActivityName, statusFilter, scopeFilter, typeFilter,
    fetchDiscounts, fetchDiscountTypes, deleteActivity, filteredDiscounts, getStatusBadge 
} = useDiscount();

// 控制下拉選單
const showAdvancedFilter = ref(false);

// 引用子元件 (彈跳視窗)
const formModalRef = ref(null);

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
                            <span v-if="statusFilter !== 'all' || scopeFilter !== 'all' || typeFilter !== 'all'" 
                                  class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" 
                                  style="font-size: 0.7rem; padding: 0.35em 0.6em;">
                                已套用
                            </span>
                        </button>
                        
                        <div v-if="showAdvancedFilter" class="position-fixed top-0 start-0 w-100 h-100" style="z-index: 1040;" @click="showAdvancedFilter = false"></div>
                        
                        <div v-if="showAdvancedFilter" class="dropdown-menu shadow-lg p-4 rounded-3 mt-2 d-block" style="min-width: 480px; position: absolute; z-index: 1050; top: 100%; left: 0;">
                            <div class="border-bottom pb-2 mb-3"><h6 class="mb-0 fw-bold">⚙️ 進階篩選條件</h6></div>
                            
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
                                    <button v-for="t in discountTypesList" :key="t.discountTypeId"
                                            class="btn btn-outline-secondary btn-sm" :class="{active: typeFilter === t.discountTypeId.toString()}" @click="typeFilter = t.discountTypeId.toString()">
                                        {{ t.discountTypeName }}
                                    </button>
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
                                </div>
                            </div>
                            
                            <div class="d-flex justify-content-end border-top pt-3 mt-4">
                                <button class="btn btn-sm btn-light text-muted border px-3" @click="statusFilter = 'all'; scopeFilter = 'all'; typeFilter = 'all'">清除重設</button>
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
                    <thead class="table-light">
                        <tr><th>編號</th><th>狀態</th><th>活動名稱</th><th>折扣類別</th><th>活動期間</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                        <tr v-if="filteredDiscounts.length === 0"><td colspan="6" class="text-center text-muted py-4">目前尚無活動資料</td></tr>
                        
                        <tr v-for="item in filteredDiscounts" :key="item.discountId">
                            <td>{{ item.discountId }}</td>
                            <td>
                                <span class="badge rounded-pill" :class="getStatusBadge(item).class" style="font-size: 0.9rem; padding: 0.45em 0.85em;">
                                    {{ getStatusBadge(item).text }}
                                </span>
                            </td>
                            <td class="fw-bold">{{ item.discountName }}</td>
                            <td>{{ item.discountType?.discountTypeName }}</td>
                            <td>{{ item.startDate }} ~ {{ item.endDate }}</td>
                            <td>
                                <button class="btn btn-sm text-primary fw-bold bg-transparent border-0 px-2" @click="formModalRef.openEdit(item)">查看/修改</button>
                                <button class="btn btn-sm text-danger fw-bold bg-transparent border-0 px-2" @click="deleteActivity(item)">刪除</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <DiscountFormModal 
            ref="formModalRef" 
            :api-base-url="API_BASE_URL" 
            :discount-types="discountTypesList"
            @saved="fetchDiscounts" 
        />
    </div>
</template>

<style scoped>
.filter-group button.active { background-color: #6c757d; color: white; border-color: #6c757d; }
</style>
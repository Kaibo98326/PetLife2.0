<script setup>
import { ref, computed } from 'vue';
import * as bootstrap from 'bootstrap';
import request from '@/utils/request';
import Swal from 'sweetalert2';

// 狀態變數
const currentDiscountId = ref(null);
const rawData = ref([]);
const isLoading = ref(false);

// 1. 開啟 Modal 並獲取資料
const openModal = async (discountId) => {
    currentDiscountId.value = discountId;
    rawData.value = [];
    isLoading.value = true;

    try {
        // ✨ 呼叫我們剛剛在後端新建的專屬 JOIN API
        const res = await request.get(`/api/order-discounts/discount/${discountId}/details`);
        rawData.value = res.data || [];
        
        if (rawData.value.length === 0) {
            Swal.fire('提示', '目前還沒有訂單使用此活動喔！', 'info');
            return;
        }

        const el = document.getElementById('discountUsageDetailModal');
        const modal = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
        modal.show();
    } catch (error) {
        console.error('獲取活動明細失敗:', error);
        Swal.fire('錯誤', '無法獲取明細資料，請檢查網路連線', 'error');
    } finally {
        isLoading.value = false;
    }
};

defineExpose({ openModal });

// 2. 計算頂部 KPI 卡片指標
const totalDiscountAmount = computed(() => {
    return rawData.value.reduce((sum, item) => sum + (item.discountAmount || 0), 0);
});

const totalUniqueOrders = computed(() => {
    const orderIds = rawData.value.map(item => item.orderId);
    return new Set(orderIds).size; // 使用 Set 去除重複的訂單編號
});

// 時間格式化
const formatDateTime = (dateStr) => {
    if (!dateStr) return '';
    return dateStr.replace('T', ' ').substring(0, 16);
};
</script>

<template>
    <div class="modal fade" id="discountUsageDetailModal" tabindex="-1" data-bs-backdrop="static">
        <div class="modal-dialog modal-xl modal-dialog-scrollable">
            <div class="modal-content border-0 shadow-lg">
                <div class="modal-header bg-light">
                    <h5 class="modal-title fw-bold">
                        <i class="fas fa-chart-line text-success me-2"></i>活動使用成效明細
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>

                <div class="modal-body p-4 bg-light">
                    <div class="row mb-4">
                        <div class="col-md-6">
                            <div class="card border-0 shadow-sm text-center py-3">
                                <div class="text-muted mb-1 fw-bold">累計折抵總額</div>
                                <div class="text-danger fw-bold" style="font-size: 2rem;">
                                    ${{ totalDiscountAmount.toLocaleString() }}
                                </div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="card border-0 shadow-sm text-center py-3">
                                <div class="text-muted mb-1 fw-bold">受惠訂單數</div>
                                <div class="text-dark fw-bold" style="font-size: 2rem;">
                                    {{ totalUniqueOrders }} <span style="font-size: 1rem;">筆</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card border-0 shadow-sm">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th style="width: 20%;">日期/時間</th>
                                        <th style="width: 40%;" class="text-start">受惠商品 (圖+名)</th>
                                        <th class="text-center">數量</th>
                                        <th class="text-end">折扣金額</th>
                                        <th class="text-center" style="width: 15%;">訂單連結</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(item, index) in rawData" :key="index">
                                        <td class="text-muted small">{{ formatDateTime(item.orderDate) }}</td>
                                        <td>
                                            <div class="d-flex align-items-center gap-3">
                                               <img :src="item.productImage ? `http://localhost:8082/${item.productImage}` : 'https://placehold.co/50x50?text=No+Image'" alt="商品圖片" class="rounded border" style="width: 50px; height: 50px; object-fit: cover;">
                                                <div>
                                                    <div class="fw-bold">{{ item.productName }}</div>
                                                    <div class="text-muted small">#{{ item.productId }}</div>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="text-center">{{ item.quantity }}</td>
                                        <td class="text-end text-danger fw-bold">- ${{ item.discountAmount.toLocaleString() }}</td>
                                        <td class="text-center">
                                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2" style="font-size: 0.85rem; cursor: pointer;">
                                                #{{ item.orderId }}
                                            </span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-white">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">關閉</button>
                </div>
            </div>
        </div>
    </div>
</template>
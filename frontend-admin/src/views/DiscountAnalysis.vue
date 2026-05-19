<template>
    <div class="container-fluid py-4">
        <div class="bg-white rounded-3 shadow-sm border p-4 mb-4">
            <h4 class="fw-bold mb-4"><i class="fas fa-chart-pie me-2 text-primary"></i>活動成效分析儀表板</h4>

            <div class="row mb-4">
                <div class="col-md-5">
                    <label class="form-label text-muted small fw-bold">選擇要分析的活動</label>
                    <select class="form-select border-2" v-model="selectedDiscountId">
                        <option value="" disabled>-- 請選擇優惠活動 --</option>
                        <option v-for="opt in discountOptions" :key="opt.discountId" :value="opt.discountId">
                            {{ opt.discountName }}
                        </option>
                    </select>
                </div>
            </div>

            <div v-if="selectedDiscountId">
                <div class="row g-3 mb-4">
                    <div class="col-md-3">
                        <div class="bg-danger-subtle border rounded-3 p-3 h-100">
                            <div class="text-muted small">總折抵金額</div>
                            <div class="fs-3 fw-bold text-danger">
                                $ {{ totalDiscountAmount.toLocaleString() }}
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-primary-subtle border rounded-3 p-3 h-100">
                            <div class="text-muted small">總銷售件數</div>
                            <div class="fs-3 fw-bold text-primary">
                                {{ totalQuantity }}
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-success-subtle border rounded-3 p-3 h-100">
                            <div class="text-muted small">參與商品數</div>
                            <div class="fs-3 fw-bold text-success">
                                {{ totalProducts }}
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-warning-subtle border rounded-3 p-3 h-100">
                            <div class="text-muted small">平均每件折抵</div>
                            <div class="fs-3 fw-bold text-warning">
                                $ {{ avgDiscountPerItem.toLocaleString() }}
                            </div>
                        </div>
                    </div>
                </div>
                <ul class="nav nav-pills mb-4 gap-2">
                    <li class="nav-item">
                        <button class="nav-link fw-bold px-4 rounded-pill"
                            :class="{ 'active bg-warning text-dark': activeTab === 'amount', 'bg-light text-muted': activeTab !== 'amount' }"
                            @click="activeTab = 'amount'">
                            💰 預算補貼佔比
                        </button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link fw-bold px-4 rounded-pill"
                            :class="{ 'active bg-warning text-dark': activeTab === 'volume', 'bg-light text-muted': activeTab !== 'volume' }"
                            @click="activeTab = 'volume'">
                            📦 商品銷量佔比
                        </button>
                    </li>
                </ul>

                <div class="chart-wrapper bg-light rounded border p-3 d-flex justify-content-center align-items-center">
                    <div id="analysisChart" style="width: 100%; height: 500px;"></div>
                </div>
                <!-- 活動分析明細表 -->
                <div class="mt-4 bg-white border rounded-3 p-3 shadow-sm">
                    <h5 class="fw-bold mb-3">
                        <i class="fas fa-table me-2 text-primary"></i>
                        活動商品分析明細
                    </h5>
                    <div class="table-responsive">
                        <table class="table align-middle table-hover">
                            <thead class="table-light">
                                <tr>
                                    <th>商品名稱</th>
                                    <th class="text-center">銷售件數</th>
                                    <th class="text-end">總折抵金額</th>
                                    <th class="text-end">平均每件折抵</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="item in chartData" :key="item.productName">
                                    <td class="fw-medium">{{ item.productName }}</td>
                                    <td class="text-center">{{ item.quantity }}</td>
                                    <td class="text-end text-danger fw-bold">
                                        $ {{ Number(item.discountAmount || 0).toLocaleString() }}
                                    </td>
                                    <td class="text-end">
                                        $ {{ item.quantity > 0
                                            ? Math.round(item.discountAmount / item.quantity).toLocaleString() : 0 }}
                                    </td>
                                </tr>
                                <tr v-if="chartData.length === 0">
                                    <td colspan="4" class="text-center text-muted py-4">
                                        尚無分析資料
                                    </td>
                                </tr>
                            </tbody>
                            <tfoot v-if="chartData.length > 0" class="table-light fw-bold">
                                <tr>
                                    <td>總計</td>
                                    <td class="text-center">
                                        {{
                                            chartData.reduce((sum, item) =>
                                                sum + Number(item.quantity || 0), 0)
                                        }}
                                    </td>
                                    <td class="text-end text-danger">
                                        $ {{
                                            chartData
                                                .reduce((sum, item) =>
                                                    sum + Number(item.discountAmount || 0), 0)
                                                .toLocaleString()
                                        }}
                                    </td>
                                    <td>

                                    </td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            </div>

            <div v-else class="text-center text-muted py-5 bg-light rounded border border-dashed">
                <i class="fas fa-hand-pointer fs-1 mb-3 opacity-50"></i>
                <h5>請先從上方選擇一個活動來檢視分析圖表</h5>
            </div>

        </div>
    </div>
</template>

<script setup>
// ✨ 修改：引入 computed 與 onMounted 支援響應式動態數據計算與生命週期強制同步
import { ref, nextTick, watch, computed, onMounted } from 'vue';

import * as echarts from 'echarts';

// ✨ 新增：引入活動的 Composable 與 axios 封裝工具物件對接後端
import { useDiscount } from '@/stores/useDiscount';
import request from '@/utils/request';

// 狀態管理
const selectedDiscountId = ref('');
const activeTab = ref('amount'); // 'amount' (折抵佔比) 或 'volume' (銷量佔比)
const chartInstance = ref(null);

// ✨ 新增：實例化活動 Composable 模組
const discountStore = useDiscount();

// 模擬從後端撈取的活動列表 (請替換為您的 API 呼叫)
// ✨ 修改：將原本寫死的陣列移除，改為 computed 屬性，並精準加上 .value 讀取 Composable 的 Ref 陣列
const discountOptions = computed(() => {
    if (!discountStore.discounts.value || !Array.isArray(discountStore.discounts.value)) return [];
    // 過濾掉所有已被標記為已刪除 ('deleted') 的活動
    return discountStore.discounts.value.filter(opt => opt.status !== 'deleted');
});

// 模擬從後端撈回來的單一活動明細數據 (請替換為您的 API 呼叫)
// 這裡的結構對應您的需求：商品名稱、折抵金額、數量
// ✨ 修改：徹底拔除 mockDetailData 寫死的假資料，改用響應式變數 chartData 承接後端真實回傳值
const chartData = ref([]);

// 初始化與渲染圖表
const renderChart = async () => {
    // 確保 DOM 已經更新
    await nextTick();
    const chartDom = document.getElementById('analysisChart');
    if (!chartDom) return;

    // 如果圖表已存在，先銷毀避免重複渲染
    if (chartInstance.value) {
        chartInstance.value.dispose();
    }

    // 初始化 ECharts 實例
    chartInstance.value = echarts.init(chartDom);

    // 根據當前頁籤，決定要用哪個維度的數據
    const isAmountTab = activeTab.value === 'amount';
    // ✨ 修改：將圖表資料來源由模擬陣列切換為真實的 chartData.value
    const seriesData = chartData.value.map(item => ({
        name: item.productName,
        value: isAmountTab ? item.discountAmount : item.quantity
    }));

    // 設定圖表外觀 (圓餅圖/環狀圖)
    const option = {
        title: {
            text: isAmountTab ? '活動預算折抵佔比' : '活動商品銷量佔比',
            left: 'center',
            top: 20,
            textStyle: { color: '#333', fontWeight: 'bold', fontSize: 18 }
        },
        tooltip: {
            trigger: 'item',
            formatter: isAmountTab ? '{a} <br/>{b}: $ {c} ({d}%)' : '{a} <br/>{b}: {c} 件 ({d}%)'
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            top: 'middle'
        },
        series: [
            {
                name: isAmountTab ? '折抵金額' : '銷量件數',
                type: 'pie',
                radius: ['40%', '70%'], // 設定為環狀圖
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 10,
                    borderColor: '#fff',
                    borderWidth: 2
                },
                label: {
                    show: true,
                    formatter: '{b}\n{d}%',
                    fontWeight: 'bold'
                },
                data: seriesData
            }
        ]
    };

    chartInstance.value.setOption(option);
};

// 監聽選擇的活動或頁籤變化時，重新渲染圖表
// ✨ 修改：將監聽器改為 async 非同步，發揮註解原創功能，真正執行 axios 呼叫向後端取得銷售與折抵數據
watch([selectedDiscountId, activeTab], async () => {
    if (selectedDiscountId.value) {
        // 這裡可以加入 axios 呼叫，根據 selectedDiscountId 更新 mockDetailData
        try {
            // 呼叫我們在 DiscountController 建立好的真實報表統計 API
            const response = await request.get(`/api/order-discounts/discount/${selectedDiscountId.value}/details`);
            const grouped = {};
            (response.data || []).forEach(item => {
                const key = item.productName || '未知商品';
                if (!grouped[key]) {
                    grouped[key] = {
                        productName: key,
                        discountAmount: 0,
                        quantity: 0
                    };
                }
                grouped[key].discountAmount += Number(item.discountAmount || 0);
                grouped[key].quantity += Number(item.quantity || 0);
            });
            chartData.value = Object.values(grouped);
            renderChart();
        } catch (error) {
            console.error('取得活動報表數據失敗:', error);
            chartData.value = [];
            renderChart();
        }
    }
});

const totalDiscountAmount = computed(() => {
    return chartData.value.reduce(
        (sum, item) => sum + Number(item.discountAmount || 0),
        0
    );
});

const totalQuantity = computed(() => {
    return chartData.value.reduce(
        (sum, item) => sum + Number(item.quantity || 0),
        0
    );
});

const totalProducts = computed(() => {
    return chartData.value.length;
});

const avgDiscountPerItem = computed(() => {
    if (totalQuantity.value === 0) return 0;

    return Math.round(
        totalDiscountAmount.value / totalQuantity.value
    );
});

// 視窗縮放時自適應圖表大小
window.addEventListener('resize', () => {
    if (chartInstance.value) chartInstance.value.resize();
});

// ✨ 新增：在報表組件掛載時，命令 Composable 執行一次非同步載入，同步最新活動清單
onMounted(() => {
    discountStore.fetchDiscounts();
});
</script>

<style scoped>
.nav-pills .nav-link {
    transition: all 0.3s ease;
    border: 1px solid transparent;
}

.nav-pills .nav-link.active {
    box-shadow: 0 4px 10px rgba(255, 193, 7, 0.3);
    border-color: #ffc107;
}

.border-dashed {
    border-style: dashed !important;
}
</style>
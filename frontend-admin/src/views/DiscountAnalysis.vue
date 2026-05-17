<script setup>
import { ref, onMounted, nextTick, watch } from 'vue';
// ✨ 新增：引入 ECharts 核心
import * as echarts from 'echarts';

// 狀態管理
const selectedDiscountId = ref('');
const activeTab = ref('amount'); // 'amount' (折抵佔比) 或 'volume' (銷量佔比)
const chartInstance = ref(null);

// 模擬從後端撈取的活動列表 (請替換為您的 API 呼叫)
const discountOptions = ref([
    { id: 1, name: '貓貓寶貝買2送1' },
    { id: 2, name: '全館滿千折百' }
]);

// 模擬從後端撈回來的單一活動明細數據 (請替換為您的 API 呼叫)
// 這裡的結構對應您的需求：商品名稱、折抵金額、數量
const mockDetailData = [
    { productName: '貓咪主食罐-雞肉', discountAmount: 110, quantity: 4 },
    { productName: '寵物護爪膏', discountAmount: 50, quantity: 2 },
    { productName: '頂級豆腐砂', discountAmount: 200, quantity: 8 }
];

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
    const seriesData = mockDetailData.map(item => ({
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
watch([selectedDiscountId, activeTab], () => {
    if (selectedDiscountId.value) {
        // 這裡可以加入 axios 呼叫，根據 selectedDiscountId 更新 mockDetailData
        renderChart();
    }
});

// 視窗縮放時自適應圖表大小
window.addEventListener('resize', () => {
    if (chartInstance.value) chartInstance.value.resize();
});
</script>

<template>
    <div class="container-fluid py-4">
        <div class="bg-white rounded-3 shadow-sm border p-4 mb-4">
            <h4 class="fw-bold mb-4"><i class="fas fa-chart-pie me-2 text-primary"></i>活動成效分析儀表板</h4>
            
            <div class="row mb-4">
                <div class="col-md-5">
                    <label class="form-label text-muted small fw-bold">選擇要分析的活動</label>
                    <select class="form-select border-2" v-model="selectedDiscountId">
                        <option value="" disabled>-- 請選擇優惠活動 --</option>
                        <option v-for="opt in discountOptions" :key="opt.id" :value="opt.id">
                            {{ opt.name }}
                        </option>
                    </select>
                </div>
            </div>

            <div v-if="selectedDiscountId">
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
            </div>
            
            <div v-else class="text-center text-muted py-5 bg-light rounded border border-dashed">
                <i class="fas fa-hand-pointer fs-1 mb-3 opacity-50"></i>
                <h5>請先從上方選擇一個活動來檢視分析圖表</h5>
            </div>

        </div>
    </div>
</template>

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
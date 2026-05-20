<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import VChart from 'vue-echarts'
import * as echarts from 'echarts/core'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import * as bootstrap from 'bootstrap'

const selectedStatusTitle = ref('')
const selectedOrders = ref([])
const chartMode = ref('payment')

// 點擊按鈕切換圖表
const changeChartMode = (mode) => {
  chartMode.value = mode
  if (mode === 'payment') {
    loadPaymentChart()
  } else if (mode === 'status') {
    loadStatusChart()
  } else if (mode === 'orderTrend') {
    loadOrderTrendChart()
  }
}

// 處理圖表點擊
const handleChartClick = async (params) => {
  let searchType = ''
  let keyword = params.name

  if (chartMode.value === 'payment') {
    searchType = 'paymentMethod'
  } else if (chartMode.value === 'status') {
    searchType = 'orderStatus'
  }

  selectedStatusTitle.value = params.name

  // 如果點擊的是每月趨勢圖
  if (chartMode.value === 'orderTrend') {
    selectedStatusTitle.value = `${params.name} 份訂單`
    searchType = 'all'
    keyword = ''
  }

  try {
    const res = await request.get('/api/order/analysis/listbycondition', {
      params: { searchType, keyword },
    })
    selectedOrders.value = res.data || []
  } catch (err) {
    console.error('撈取條件訂單失敗:', err)
  }

  // 打開彈窗
  const modal = new bootstrap.Modal(document.getElementById('orderAnalysisModal'))
  modal.show()
}

// ── ECharts 元件註冊 ──────────────────────────────────────────────
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  PieChart,
  LineChart,
  BarChart,
  CanvasRenderer,
])

const chartOption = ref({})

// 結帳方式分析
const loadPaymentChart = async () => {
  try {
    const res = await request.get('/api/order/analysis/payment')
    const data = res.data || { creditCard: 0, linePay: 0, transfer: 0, cod: 0 }

    chartOption.value = {
      title: { text: '訂單結帳方式佔比分析', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c} 筆 ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [
        {
          name: '訂單數量',
          type: 'pie',
          radius: '65%',
          data: [
            { value: data.creditCard, name: '信用卡' },
            { value: data.linePay, name: 'LinePay' },
            { value: data.transfer, name: '金融卡' },
          ],
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' },
          },
        },
      ],
    }
  } catch (err) {
    console.error('加載結帳方式失敗:', err)
  }
}

// 一個月內訂單狀態分析
const loadStatusChart = async () => {
  try {
    const res = await request.get('/api/order/analysis/status')
    const data = res.data || { completed: 0, pending: 0, cancelled: 0 }

    chartOption.value = {
      title: { text: '近一個月內訂單狀態統計', left: 'center' },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: ['已完成', '處理中', '已取消'] },
      yAxis: { type: 'value' },
      series: [
        {
          name: '訂單筆數',
          type: 'bar',
          barWidth: '40%',
          data: [
            { value: data.completed, itemStyle: { color: '#2ecc71' } },
            { value: data.pending, itemStyle: { color: '#f1c40f' } },
            { value: data.cancelled, itemStyle: { color: '#e74c3c' } },
          ],
        },
      ],
    }
  } catch (err) {
    console.error('加載訂單狀態失敗:', err)
  }
}

// 每月訂單趨勢分析
const loadOrderTrendChart = async () => {
  try {
    const res = await request.get('/api/order/analysis/trends')
    const months = res.data.map((item) => item.month)
    const counts = res.data.map((item) => item.count)

    chartOption.value = {
      title: { text: '營業每月訂單趨勢走勢', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: months },
      yAxis: { type: 'value' },
      series: [
        {
          name: '訂單筆數',
          data: counts,
          type: 'line',
          smooth: true,
          itemStyle: { color: '#3498db' },
          areaStyle: { color: 'rgba(52, 152, 219, 0.2)' },
        },
      ],
    }
  } catch (err) {
    console.error('加載趨勢圖表失敗:', err)
  }
}

onMounted(() => {
  loadPaymentChart()
})
</script>

<template>
  <div class="p-4">
    <div class="card shadow-sm p-4">
      <h3 class="mb-4">訂單數據業務分析系統</h3>

      <div class="d-flex gap-2 mb-4">
        <button
          class="btn"
          :class="chartMode === 'payment' ? 'btn-warning' : 'btn-outline-warning'"
          @click="changeChartMode('payment')"
        >
          結帳方式分析
        </button>
        <button
          class="btn"
          :class="chartMode === 'status' ? 'btn-warning' : 'btn-outline-warning'"
          @click="changeChartMode('status')"
        >
          近一個月狀態
        </button>
        <button
          class="btn"
          :class="chartMode === 'orderTrend' ? 'btn-warning' : 'btn-outline-warning'"
          @click="changeChartMode('orderTrend')"
        >
          每月訂單趨勢
        </button>
      </div>

      <v-chart class="chart" :option="chartOption" autoresize @click="handleChartClick" />

      <div class="modal fade" id="orderAnalysisModal" tabindex="-1">
        <div class="modal-dialog modal-xl modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header bg-light">
              <h5 class="modal-title fw-bold">
                篩選條件：【{{ selectedStatusTitle }}】相關訂單清單
              </h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
              <div class="table-responsive">
                <table class="table table-hover align-middle">
                  <thead class="table-dark">
                    <tr>
                      <th>訂單編號</th>
                      <th>收件人姓名</th>
                      <th>訂單總金額</th>
                      <th>結帳方式</th>
                      <th>訂單狀態</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="selectedOrders.length === 0">
                      <td colspan="5" class="text-center text-muted py-4">
                        💡 目前沒有符合此條件的訂單記錄
                      </td>
                    </tr>
                    <tr v-for="o in selectedOrders" :key="o.orderId">
                      <td class="fw-bold text-secondary"># {{ o.orderId }}</td>
                      <td>{{ o.orderName || '未填寫' }}</td>
                      <td class="text-danger fw-bold">
                        $ {{ Number(o.orderTotal || 0).toLocaleString() }}
                      </td>
                      <td>
                        <span class="badge bg-secondary">{{ o.orderPayment }}</span>
                      </td>
                      <td>
                        <span
                          class="badge"
                          :class="{
                            'bg-success':
                              o.orderStatus === 'COMPLETED' || o.orderStatus === '已完成',
                            'bg-warning text-dark':
                              o.orderStatus === 'PENDING' || o.orderStatus === '處理中',
                            'bg-danger':
                              o.orderStatus === 'CANCELLED' || o.orderStatus === '已取消',
                          }"
                        >
                          {{ o.orderStatus }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">關閉</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chart {
  height: 550px;
  width: 100%;
}
.table th {
  font-weight: 500;
}
</style>

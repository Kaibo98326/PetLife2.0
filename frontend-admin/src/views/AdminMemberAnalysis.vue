<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import VChart from 'vue-echarts'
import * as echarts from 'echarts/core'
import { TitleComponent, TooltipComponent, LegendComponent ,GridComponent } from 'echarts/components'
import { PieChart ,LineChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import * as bootstrap from 'bootstrap'


const selectedStatusTitle = ref('')
const selectedMembers = ref([])
const chartMode = ref('status')


const statusMap = {
    啟用: 'active',
    停權: 'disable',
    刪除: 'delete'
}

const handleChartClick = async (params) => {

    let searchType = ''
    let keyword = ''

    if (chartMode.value === 'status') {
        searchType = 'accountStatus'
        keyword = statusMap[params.name]
    }

    if (chartMode.value === 'provider') {
        searchType = 'provider'
        keyword = params.name
    }
    if(chartMode.value === 'registerTrend'){
        selectedStatusTitle.value = `${params.name} 註冊會員`

        const res = await request.get('/api/admin/members/register-month',
            {
                params:{
                    month: params.name
                }
            }
        )
        selectedMembers.value = res.data || []

        const modal = new bootstrap.Modal(
            document.getElementById('statusMemberModal')
        )
        modal.show()
        return
    }

    selectedStatusTitle.value = params.name

    const res = await request.get('/api/admin/members', {
        params: {
            page: 0,
            size: 100,
            searchType,
            keyword
        }
    })

    selectedMembers.value = res.data.content || []

    const modal = new bootstrap.Modal(
        document.getElementById('statusMemberModal')
    )

    modal.show()
}

echarts.use([
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    PieChart,
    CanvasRenderer,
    GridComponent,
    LineChart
])

const chartOption = ref({})

const loadStatusChart = async () => {

    try {

        const res = await request.get(
            '/api/admin/members/analysis/status'
        )
        chartOption.value = {
            title: {
                text: '會員帳號狀態分析',
                left: 'center'
            },
            tooltip: {
                trigger: 'item'
            },
            legend: {
                orient: 'vertical',
                left: 'left'
            },
            series: [
                {
                    name: '會員數量',
                    type: 'pie',
                    radius: '65%',
                    data: [
                        {
                            value: res.data.active,
                            name: '啟用'
                        },
                        {
                            value: res.data.disable,
                            name: '停權'
                        },
                        {
                            value: res.data.delete,
                            name: '刪除'
                        }
                    ],
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0
                        }
                    }
                }
            ]
        }

    } catch (err) {

        console.log(err)
    }
}
const loadProviderChart = async () => {
    try {
        const res = await request.get('/api/admin/members/analysis/provider')

        chartOption.value = {
            title: {
                text: '會員登入來源分析',
                left: 'center'
            },
            tooltip: {
                trigger: 'item'
            },
            legend: {
                orient: 'vertical',
                left: 'left'
            },
            series: [
                {
                    name: '會員數量',
                    type: 'pie',
                    radius: '65%',
                    data: [
                        {
                            value: res.data.local,
                            name: '本地註冊'
                        },
                        {
                            value: res.data.google,
                            name: 'google'
                        }
                    ]
                }
            ]
        }

    } catch (err) {
        console.log(err);

    }
}
const loadRegisterTrendChart = async () => {

    try {
        const res = await request.get('/api/admin/members/analysis/register-trend')

        const months = res.data.map(item => item.month)

        const counts = res.data.map(item => item.count)

        chartOption.value = {
            title: {
                text: '會員每月註冊趨勢',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis'
            },
            xAxis: {
                type: 'category',
                data: months
            },
            yAxis: {
                type: 'value'
            },
            series: [
                {
                    data: counts,
                    type: 'line',
                    smooth: true
                }
            ]
        }
    }catch(err){
        console.log(err);
        
    }

}

onMounted(() => {
    loadStatusChart()
})
</script>

<template>
    <div class="p-4">
        <div class="card shadow-sm p-4">
            <h3 class="mb-4">會員分析圖表</h3>
            <div class="d-flex gap-2 mb-4">
                <button class="btn" :class="chartMode === 'status' ? 'btn-warning' : 'btn-outline-warning'"
                    @click="chartMode = 'status'; loadStatusChart()">帳號狀態分析</button>
                <button class="btn" :class="chartMode === 'provider' ? 'btn-warning' : 'btn-outline-warning'"
                    @click="chartMode = 'provider'; loadProviderChart()">登入來源分析</button>
                <button class="btn" :class="chartMode === 'registerTrend'? 'btn-warning':'btn-outline-warning'" @click="chartMode = 'registerTrend' ; loadRegisterTrendChart()" >每月註冊趨勢</button>
            </div>
            <v-chart class="chart" :option="chartOption" autoresize @click="handleChartClick" />
            <!-- 狀態會員清單 Modal -->
            <div class="modal fade" id="statusMemberModal" tabindex="-1">
                <div class="modal-dialog modal-lg modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                {{ selectedStatusTitle }} 會員清單
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <table class="table table-hover align-middle">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>姓名</th>
                                        <th>Email</th>
                                        <th>電話</th>
                                        <th>登入來源</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-if="selectedMembers.length === 0">
                                        <td colspan="5" class="text-center text-muted">
                                            目前沒有會員資料
                                        </td>
                                    </tr>
                                    <tr v-for="m in selectedMembers" :key="m.memberId">
                                        <td>{{ m.memberId }}</td>
                                        <td>{{ m.memberName }}</td>
                                        <td>{{ m.email }}</td>
                                        <td>{{ m.phone || '-' }}</td>
                                        <td>{{ m.provider || 'local' }}</td>
                                    </tr>
                                </tbody>
                            </table>
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
    height: 500px;
}
</style>
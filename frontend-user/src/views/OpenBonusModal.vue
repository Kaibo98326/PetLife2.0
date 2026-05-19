<script setup>
// ===== 新增區塊：導入紅利模組所需之 Vue API 與套件 =====
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import Swal from 'sweetalert2';
import axios from '@/axios'; 
import { Modal } from 'bootstrap'; 

const userStore = useUserStore()
const router = useRouter()

const bonusHistory = ref([])
let bonusModal = null
const activeBonusTab = ref('all')

// ✨ 修改：宣告一個暫存變數，用來記錄即將跳轉的目標訂單 ID
let targetOrderId = null

// ===== 新增區塊：元件掛載時自動初始化 Modal、加載數據，並在關閉時退回上一頁 =====
onMounted(async () => {
    try {
        // 1. 發送異步請求獲取紅利明細
        const res = await axios.get('/orders/bonus-history', {
            headers: { Authorization: `Bearer ${userStore.token}` }
        })
        bonusHistory.value = res.data
        
        // 2. 獲取 DOM 節點並初始化 Bootstrap Modal
        const modalElement = document.getElementById('bonusHistoryModal')
        if (modalElement) {
            bonusModal = new Modal(modalElement)
            
            // 3. 監聽 Modal 的完全隱藏事件：當關閉彈窗時自動退回上一頁
            modalElement.addEventListener('hidden.bs.modal', () => {
                // ✨ 修改：在彈窗完全隱藏後進行判斷——若有暫存的目標訂單 ID 則執行正向跳轉，否則才退回上一頁
                if (targetOrderId) {
                    router.push({ path: '/orderhistory', query: { openOrderId: targetOrderId } })
                } else {
                    router.back()
                }
            })
        }
        
        // 4. 初始化顯示狀態並開啟彈窗
        activeBonusTab.value = 'all' 
        bonusModal?.show()
    } catch (err) {
        console.error('取得明細失敗:', err)
        Swal.fire('錯誤', '無法取得紅利明細，請稍後再試', 'error').then(() => {
            router.back() // 若加載失敗，關閉錯誤提示後也同步退回上一頁
        })
    }
})
// =========================================================================

const filteredBonusHistory = computed(() => {
    if (activeBonusTab.value === 'earn') {
        return bonusHistory.value.filter(item => item.points > 0)
    } else if (activeBonusTab.value === 'spend') {
        return bonusHistory.value.filter(item => item.points < 0)
    }
    return bonusHistory.value
})

const goToOrder = (orderId) => {
    // ✨ 修改：點擊時不要直接執行 router.push，而是先將傳入的 orderId 賦值給暫存變數，隨後僅觸發隱藏動畫
    targetOrderId = orderId
    bonusModal?.hide()
}

const formatDateTime = (str) => (str ? str.replace('T', ' ').substring(0, 16) : '')
// ===================================================
</script>

<template>
    <div class="modal fade" id="bonusHistoryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg mx-auto">
            <div class="modal-content custom-modal-border">
                <div class="modal-header custom-modal-header d-flex flex-column align-items-center position-relative py-4">
                    <button type="button" class="btn-close position-absolute top-0 end-0 m-3" data-bs-dismiss="modal" aria-label="Close"></button>
                    <span class="total-points-label">當前總點數</span>
                    <div class="total-points-value">
                        {{ userStore.user?.bonusPoints || 0 }} <span class="unit">點</span>
                        <div class="accent-underline"></div>
                    </div>
                    <div class="text-secondary small mt-2 fw-medium">
                        * 溫馨提醒：消費獲得的點數將於訂單「已完成」後自動發放並計入餘額。
                    </div>
                </div>

                <div class="modal-body p-4">
                    <div class="tabs-container mb-4">
                        <ul class="nav nav-pills justify-content-center gap-2">
                            <li class="nav-item">
                                <a class="nav-link modern-pill" :class="{ active: activeBonusTab === 'all' }" href="#" @click.prevent="activeBonusTab = 'all'">全部明細</a>
                            </li>
                            <li class="nav-item">
                                <a class="nav-link modern-pill" :class="{ active: activeBonusTab === 'earn' }" href="#" @click.prevent="activeBonusTab = 'earn'">點數獲取</a>
                            </li>
                            <li class="nav-item">
                                <a class="nav-link modern-pill" :class="{ active: activeBonusTab === 'spend' }" href="#" @click.prevent="activeBonusTab = 'spend'">點數消耗</a>
                            </li>
                        </ul>
                        <div class="tab-divider"></div>
                    </div>

                    <Transition name="fade" mode="out-in">
                        <div class="table-card" v-if="filteredBonusHistory.length > 0" :key="activeBonusTab">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="modern-thead">
                                    <tr>
                                        <th style="width: 20%">日期</th>
                                        <th style="width: 30%" class="text-start">項目內容</th>
                                        <th style="width: 15%">變動</th>
                                        <th style="width: 15%">餘額</th>
                                        <th style="width: 20%">訂單狀態</th> 
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(item, index) in filteredBonusHistory" :key="index" class="modern-tr">
                                        <td class="text-muted small">{{ formatDateTime(item.date) }}</td>
                                        <td class="text-start">
                                            <span class="description-text">{{ item.description }}</span>
                                            <span class="order-badge-wrapper" v-if="item.orderId">
                                                <a href="#" @click.prevent="goToOrder(item.orderId)" class="order-tag">
                                                    #{{ item.orderId }}
                                                </a>
                                            </span>
                                        </td>
                                        <td :class="['fw-bold', item.points > 0 ? 'points-plus' : 'points-minus']">
                                            {{ item.points > 0 ? '+' : '' }}{{ item.points }} P
                                        </td>
                                        <td class="fw-medium text-dark">{{ item.balance }} P</td>
                                        <td>
                                            <span :class="['badge', 
                                                item.orderStatus === '已完成' ? 'bg-success' : 
                                                item.orderStatus === '聯絡中' ? 'bg-secondary' : 'bg-warning text-dark']">
                                                {{ item.orderStatus || '處理中' }}
                                            </span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div v-else class="p-5 text-center text-muted">
                            <i class="fas fa-history fa-3x mb-3 opacity-25"></i>
                            <p class="mb-0 fw-bold">目前尚無相關紀錄</p>
                        </div>
                    </Transition>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
@import '../assets/css/OpenBonusModal.css';
</style>
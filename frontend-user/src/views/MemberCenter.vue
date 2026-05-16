<script setup>

import { ref } from 'vue'; 
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import Swal from 'sweetalert2';
import logo from '@/assets/images/logo01.png';
import Cropper from "cropperjs";
import "cropperjs/dist/cropper.css";

import axios from '@/axios'; 
import { Modal } from 'bootstrap'; 

const userStore = useUserStore()
const router = useRouter()

const bonusHistory = ref([])
let bonusModal = null
const activeBonusTab = ref('all')

const openBonusModal = async () => {
    try {
        const res = await axios.get('/orders/bonus-history', {
            headers: { Authorization: `Bearer ${userStore.token}` }
        })
        bonusHistory.value = res.data
        
        if (!bonusModal) {
            const modalElement = document.getElementById('bonusHistoryModal')
            if (modalElement) {
                bonusModal = new Modal(modalElement)
            }
        }
        activeBonusTab.value = 'all' 
        bonusModal?.show()
    } catch (err) {
        console.error('取得明細失敗:', err)
        Swal.fire('錯誤', '無法取得紅利明細，請稍後再試', 'error')
    }
}

import { computed } from 'vue';
const filteredBonusHistory = computed(() => {
    if (activeBonusTab.value === 'earn') {
        return bonusHistory.value.filter(item => item.points > 0)
    } else if (activeBonusTab.value === 'spend') {
        return bonusHistory.value.filter(item => item.points < 0)
    }
    return bonusHistory.value
})

const goToOrder = (orderId) => {
    bonusModal?.hide()
    router.push({ path: '/orderhistory', query: { openOrderId: orderId } })
}

const formatDateTime = (str) => (str ? str.replace('T', ' ').substring(0, 16) : '')

const openAvatarModal = () => {
    Swal.fire({
        title: '更換大頭貼',
        html: `<input type="file" id="avatarInput" accept="image/*" /><div id="cropContainer" style="max-width:300px;margin-top:10px;"></div>`,
        showCancelButton: true,
        confirmButtonText: '確認更換',
        didOpen: () => {
            const input = document.getElementById('avatarInput');
            let cropper;
            input.addEventListener('change', (event) => {
                const file = event.target.files[0];
                if (file) {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        const img = document.createElement('img');
                        img.src = e.target.result;
                        img.style.maxWidth = '100%';
                        const container = document.getElementById('cropContainer');
                        container.innerHTML = '';
                        container.appendChild(img);
                        cropper = new Cropper(img, { aspectRatio: 1, viewMode: 1, dragMode: 'move', background: false, autoCropArea: 1 });
                        Swal.getPopup().cropper = cropper;
                    };
                    reader.readAsDataURL(file);
                }
            });
        },
        preConfirm: () => {
            const cropper = Swal.getPopup().cropper;
            if (cropper) {
                const canvas = cropper.getCroppedCanvas({ width: 200, height: 200 });
                return canvas.toDataURL("image/jpg");
            }
        }
    }).then((result) => { if (result.isConfirmed && result.value) uploadAvatar(result.value); });
};

const uploadAvatar = async (base64Image) => {
    const blob = await (await fetch(base64Image)).blob();
    const formData = new FormData();
    formData.append("file", blob, "avatar.jpg");
    try {
        const res = await fetch(`/api/member/${userStore.memberId}/avatar`, { method: "POST", body: formData });
        Swal.fire({ icon: "success", title: "大頭貼更新成功", confirmButtonText: "確定" }).then(async () => { await userStore.fetchUser() });
    } catch (err) { Swal.fire({ icon: "error", title: "更新失敗", text: err.message }); }
};

const handleLogout = () => {
    Swal.fire({ icon: 'warning', title: '確定要登出嗎？', text: '登出後需要重新登入才能使用會員功能', showCancelButton: true, confirmButtonText: '是的，登出', cancelButtonText: '取消' }).then((result) => {
        if (result.isConfirmed) { userStore.logout(); Swal.fire({ icon: 'success', title: '已登出', confirmButtonText: '回首頁' }).then(() => { router.push('/') }) }
    })
}
</script>

<template>
    <div class="member-center container-fluid mt-4">
        <header class="member-header d-flex align-items-center justify-content between p-3 bg-light">
            <router-link class="shop-logo">
                <img :src="logo" @click="router.push('/')" alt="">
            </router-link>
            <nav class="nav">
                <router-link to="/" class="nav-link">首頁</router-link>
                <router-link to="/products" class="nav-link">商品分類</router-link>
                <router-link to="/member/center" class="nav-link">會員中心</router-link>
            </nav>
        </header>

        <section class="member-info mb-4 d-flex align-items-center">
            <div class="avatar-wrapper me-4">
                <img :src="userStore.user?.userImage" alt="大頭貼" class="avatar-img">
                <button class="edit-btn" @click="openAvatarModal">✏️</button>
            </div>
            <div class="member-details">
                <h2>會員中心</h2>
                <p>會員編號：{{ userStore.user?.memberId }}</p>
                <p>會員名稱：{{ userStore.user?.memberName }}</p>
                <p class="mb-0">
                    目前紅利點數：<span class="text-danger fw-bold fs-5">{{ userStore.user?.bonusPoints || 0 }} 點</span>
                </p>
            </div>
        </section>

        <div class="menu-grid">
            <div class="menu-item" @click="router.push('/orderhistory')">📦 訂單紀錄</div>
            <div class="menu-item" @click="router.push('/member/center/profile')">👤 個人資料</div>
            <div class="menu-item" @click="router.push('/member/center/favorites')">❤️ 我的收藏</div>
            <div class="menu-item" @click="router.push('/member/center/pets')">🐕 寵物管理</div>
            <div class="menu-item" @click="openBonusModal">💎 紅利明細</div>
            <div class="menu-item" @click="handleLogout">🔒 登出</div>
        </div>

        <section class="member-subview mt-4">
            <router-view />
        </section>

        <section class="notice mt-4">
            <h3>寵物百貨 溫馨提醒</h3>
            <p>我們不會以任何名義向您索取或核對金融帳戶資訊。若遇可疑電話請撥打 165 反詐騙專線。</p>
        </section>

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
        <th style="width: 20%">訂單狀態</th> </tr>
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
                                                item.orderStatus === '已取消' ? 'bg-secondary' : 'bg-warning text-dark']">
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
    </div>
</template>
<style scoped>
@import '../assets/css/MemberCenter.css';
</style>
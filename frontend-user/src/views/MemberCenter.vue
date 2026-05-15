<script setup>
import { ref } from 'vue'; // 引入 ref
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import Swal from 'sweetalert2';
import logo from '@/assets/images/logo01.png';
import Cropper from "cropperjs";
import "cropperjs/dist/cropper.css";

import axios from '@/axios'; // 引入 axios 以呼叫明細 API
import { Modal } from 'bootstrap'; // 引入 Bootstrap Modal

const userStore = useUserStore()
const router = useRouter()

// 紅利明細的狀態與邏輯
const bonusHistory = ref([])
let bonusModal = null

const openBonusModal = async () => {
    try {
        // 呼叫後端聚合的明細 API
        const res = await axios.get('/orders/bonus-history')
        bonusHistory.value = res.data
        
        // 顯示 Modal
        if (!bonusModal) {
            const modalElement = document.getElementById('bonusHistoryModal')
            if (modalElement) {
                bonusModal = new Modal(modalElement)
            }
        }
        bonusModal?.show()
    } catch (err) {
        console.error('取得明細失敗:', err)
        Swal.fire('錯誤', '無法取得紅利明細，請稍後再試', 'error')
    }
}

// 日期格式化工具
const formatDateTime = (str) => (str ? str.replace('T', ' ').substring(0, 16) : '')

const openAvatarModal = () => {
    Swal.fire({
        title: '更換大頭貼',
        html: `
      <input type="file" id="avatarInput" accept="image/*" />
      <div id="cropContainer" style="max-width:300px;margin-top:10px;"></div>
    `,
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

                        cropper = new Cropper(img, {
                            aspectRatio: 1,
                            viewMode: 1,
                            dragMode: 'move',
                            background: false,
                            autoCropArea: 1,
                        });
                        Swal.getPopup().cropper = cropper;
                    };
                    reader.readAsDataURL(file);
                }
            });
        },
        preConfirm: () => {
            const cropper = Swal.getPopup().cropper;
            if (cropper) {
                const canvas = cropper.getCroppedCanvas({
                    width: 200,
                    height: 200,
                });
                const dataUrl = canvas.toDataURL("image/jpg");

                // 顯示圓形預覽
                const preview = document.createElement("div");
                preview.className = "avatar-preview";
                const img = document.createElement("img");
                img.src = dataUrl;
                preview.appendChild(img);
                Swal.getPopup().appendChild(preview);

                return dataUrl;
            }
        }
    }).then((result) => {
        if (result.isConfirmed && result.value) {
            uploadAvatar(result.value);
        }
    });
};
const uploadAvatar = async (base64Image) => {
    const blob = await (await fetch(base64Image)).blob();
    const formData = new FormData();
    formData.append("file", blob, "avatar.jpg");

    try {
        const res = await fetch(`/api/member/${userStore.memberId}/avatar`, {
            method: "POST",
            body: formData,
        });
        const data = await res.json();
        Swal.fire({
            icon: "success",
            title: "大頭貼更新成功",
            confirmButtonText: "確定"
        }).then(async () => {
           await userStore.fetchUser()
            
        });
    } catch (err) {
        Swal.fire({
            icon: "error",
            title: "更新失敗",
            text: err.message
        });
    }
};

const handleLogout = () => {
    // SweetAlert 登出確認
    Swal.fire({
        icon: 'warning',
        title: '確定要登出嗎？',
        text: '登出後需要重新登入才能使用會員功能',
        showCancelButton: true,
        confirmButtonText: '是的，登出',
        cancelButtonText: '取消'
    }).then((result) => {
        if (result.isConfirmed) {
            userStore.logout()
            Swal.fire({
                icon: 'success',
                title: '已登出',
                text: '期待您再次回來！',
                confirmButtonText: '回首頁'
            }).then(() => {
                router.push('/')
            })
        }
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
        <!-- Member Info -->
        <section class="member-info mb-4 d-flex align-items-center">
            <!--大頭貼(左方)-->
            <div class="avatar-wrapper me-4">
                <img :src="userStore.user?.userImage" alt="大頭貼" class="avatar-img">
                <button class="edit-btn" @click="openAvatarModal">✏️</button>
            </div>
            <!--會員資訊(右方)-->
            <div class="member-details">
                <h2>會員中心</h2>
                <p>會員編號：{{ userStore.user?.memberId }}</p>
                <p>會員名稱：{{ userStore.user?.memberName }}</p>
                <p class="d-flex align-items-center gap-2 mb-0">
                    目前紅利點數：<span class="text-danger fw-bold fs-5">{{ userStore.user?.bonusPoints || 0 }} 點</span>
                    <button class="btn btn-sm btn-outline-warning rounded-pill px-3 ms-2" @click="openBonusModal">查看明細</button>
                </p>
            </div>
        </section>

        <!-- Menu Grid -->
        <div class="menu-grid">
            <div class="menu-item" @click="router.push('/orderhistory')">📦 訂單紀錄</div>
            <div class="menu-item" @click="router.push('/member/center/profile')">👤 個人資料</div>
            <div class="menu-item" @click="router.push('/member/center/favorites')">❤️ 我的收藏</div>
            <div class="menu-item" @click="router.push('/member/center/pets')">🐕 寵物管理</div>
            <div class="menu-item" @click="openBonusModal">💎 紅利點數</div>
            <div class="menu-item" @click="handleLogout">🔒 登出</div>
        </div>

        <!-- 子路由顯示區 -->
        <section class="member-subview mt-4">
            <router-view />
        </section>

        <!-- Notice -->
        <section class="notice mt-4">
            <h3>寵物百貨 溫馨提醒</h3>
            <p>
                我們不會以任何名義向您索取或核對金融帳戶、信用卡資訊，
                也不會要求您操作 ATM 或網路銀行匯款。
                若遇可疑電話請勿理會，並撥打 165 反詐騙專線查證。
            </p>
        </section>

        <!-- 紅利點數明細 Modal -->
        <div class="modal fade" id="bonusHistoryModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header bg-light">
                        <h5 class="modal-title fw-bold" style="color: #e67e22;">
                            <i class="fas fa-coins me-2"></i>紅利點數明細
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-0">
                        <ul class="list-group list-group-flush" v-if="bonusHistory.length > 0">
                            <li class="list-group-item d-flex justify-content-between align-items-center p-3" v-for="(item, index) in bonusHistory" :key="index">
                                <div>
                                    <div class="fw-bold text-dark">{{ item.description }}</div>
                                    <small class="text-muted">{{ formatDateTime(item.date) }}</small>
                                </div>
                                <span :class="item.points > 0 ? 'text-success fw-bold fs-5' : 'text-danger fw-bold fs-5'">
                                    {{ item.points > 0 ? '+' : '' }}{{ item.points }}
                                </span>
                            </li>
                        </ul>
                        <div v-else class="p-5 text-center text-muted">
                            <i class="fas fa-box-open fa-3x mb-3 text-light"></i>
                            <p class="mb-0 fw-bold">目前尚無紅利紀錄</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</template>
<style scoped>
@import '../assets/css/MemberCenter.css';
</style>
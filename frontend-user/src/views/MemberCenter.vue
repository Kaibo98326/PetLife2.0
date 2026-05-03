<script setup>
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import Swal from 'sweetalert2';
import logo from '@/assets/images/logo01.png';
import Cropper from "cropperjs";
import "cropperjs/dist/cropper.css";



const userStore = useUserStore()
const router = useRouter()

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
                <p>目前紅利點數：{{ userStore.user?.currentPoints }}</p>
            </div>
        </section>

        <!-- Menu Grid -->
        <div class="menu-grid">
            <div class="menu-item" @click="router.push('/member/center/orders')">📦 訂單紀錄</div>
            <div class="menu-item" @click="router.push('/member/center/profile')">👤 個人資料</div>
            <div class="menu-item" @click="router.push('/member/center/address')">🏠 收件資訊</div>
            <div class="menu-item" @click="router.push('/member/center/favorites')">❤️ 我的收藏</div>
            <div class="menu-item" @click="router.push(`/pets/list/${userStore.memberId}`)">🐕 寵物管理</div>
            <div class="menu-item" @click="router.push('/member/points')">💎 紅利點數</div>
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
    </div>
</template>
<style scoped>
@import '../assets/css/MemberCenter.css';
</style>
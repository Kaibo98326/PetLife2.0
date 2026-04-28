<script setup>
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import Swal from 'sweetalert2';
import logo from '@/assets/images/logo01.png'

const userStore = useUserStore()
const router = useRouter()

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
        <section class="member-info mb-4">
            <h2>會員中心</h2>
            <p>會員編號：{{ userStore.memberId }}</p>
            <p>會員名稱：{{ userStore.memberName }}</p>
            <p>目前紅利點數：{{ userStore.currentPoints }}</p>
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
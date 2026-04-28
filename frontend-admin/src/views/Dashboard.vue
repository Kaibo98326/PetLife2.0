<script setup>
import { useEmployeeStore } from '@/stores/employee';
import { useRouter } from 'vue-router';

const employeeStore = useEmployeeStore();
const router = useRouter();

const logout = () => {
    employeeStore.logout();
    router.push('/');
}

</script>
<template>
    <div class="dashboard">
        <!-- 頂部欄 -->
        <div class="header">
            <div class="header-left">
                <div class="admin-title">PetLife 後臺管理系統</div>
            </div>
            <div class="header-right">
                <div class="user-info">
                    <div class="user-avatar">👤</div>
                    <div class="user-details">
                        <div class="user-name">{{ employeeStore.empName }}</div>
                        <div class="user-role">{{ employeeStore.role }}</div>
                    </div>
                </div>
                <button class="logout-btn" @click="logout">登出</button>
            </div>
        </div>

        <!-- 主容器 -->
        <div class="main-container">
            <!-- 側邊欄 -->
            <aside class="sidebar">
                <div class="sidebar-header">功能選單</div>
                <ul class="menu-list">
                    <li><router-link to="/admin/member" class="menu-link">會員管理</router-link></li>
                    <li><router-link to="/admin/employee" class="menu-link">員工管理</router-link></li>
                    <li><router-link to="/admin/pet" class="menu-link">寵物管理</router-link></li>
                    <li><router-link to="/admin/product" class="menu-link">商品管理</router-link></li>
                    <li><router-link to="/admin/order" class="menu-link">訂單管理</router-link></li>
                    <li><router-link to="/admin/grooming" class="menu-link">美容管理</router-link></li>
                    <li><router-link to="/admin/hotel" class="menu-link">寵物旅館管理</router-link></li>
                    <li><router-link to="/admin/promotion" class="menu-link">優惠活動管理</router-link></li>
                </ul>
            </aside>

            <!-- 內容區域 -->
            <div class="content-area">
                <div class="content-header">
                    <div>
                        <div class="content-title">{{ $route.name }}</div>
                        <div class="content-breadcrumb">首頁 > <span>{{ $route.name }}</span></div>
                    </div>
                </div>
                <div class="content-body">
                    <router-view /> <!-- 顯示子頁面 -->
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>
@import '@/assets/css/Dashboard.css';
</style>
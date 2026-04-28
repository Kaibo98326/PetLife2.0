<script setup>
import { ref } from 'vue';
import { useEmployeeStore } from '@/stores/employee';
import { useRouter } from 'vue-router';

const employeeStore = useEmployeeStore();
const router = useRouter();

const openMenu = ref(null);

const toggleSubmenu = (menuName) => {
    openMenu.value = openMenu.value === menuName ? null : menuName;
};

const logout = () => {
    employeeStore.logout();
    router.push('/');
};
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
                    <!-- 單層選單 -->
                    <li class="menu-item">
                        <router-link to="/admin/member" class="menu-link">
                            <img width="24" height="24"
                                src="https://img.icons8.com/material-rounded/24/user-female-circle.png" />
                            會員管理
                        </router-link>
                    </li>

                    <!-- 商品管理 -->
                    <li class="menu-item has-submenu" :class="{ open: openMenu === 'product' }">
                        <a href="javascript:void(0);" class="menu-link" @click="toggleSubmenu('product')">
                            <img width="24" height="24" src="https://img.icons8.com/ios-glyphs/30/open-box.png" />
                            <span>商品管理</span>
                            <span class="arrow">{{ openMenu === 'product' ? '▼' : '▶' }}</span>
                        </a>
                        <ul class="submenu">
                            <li><router-link to="/admin/category" class="menu-link">&emsp;商品類別管理</router-link></li>
                            <li><router-link to="/admin/product" class="menu-link">&emsp;商品管理</router-link></li>
                        </ul>
                    </li>

                    <!-- 美容管理 -->
                    <li class="menu-item has-submenu" :class="{ open: openMenu === 'grooming' }">
                        <a href="javascript:void(0);" class="menu-link" @click="toggleSubmenu('grooming')">
                            <img width="24" height="24" src="https://img.icons8.com/ios-filled/50/scissors.png" />
                            <span>美容管理</span>
                            <span class="arrow">{{ openMenu === 'grooming' ? '▼' : '▶' }}</span>
                        </a>
                        <ul class="submenu">
                            <li><router-link to="/admin/groomingItem" class="menu-link">&emsp;美容項目管理</router-link></li>
                            <li><router-link to="/admin/groomingRecord" class="menu-link">&emsp;美容預約管理</router-link>
                            </li>
                        </ul>
                    </li>

                    <!-- 寵物旅館管理 -->
                    <li class="menu-item has-submenu" :class="{ open: openMenu === 'hotel' }">
                        <a href="javascript:void(0);" class="menu-link" @click="toggleSubmenu('hotel')">
                            <img width="24" height="24" src="https://img.icons8.com/ios-glyphs/30/bed.png" />
                            <span>寵物旅館管理</span>
                            <span class="arrow">{{ openMenu === 'hotel' ? '▼' : '▶' }}</span>
                        </a>
                        <ul class="submenu">
                            <li><router-link to="/admin/hotel_rooms" class="menu-link">&emsp;旅館房間管理</router-link></li>
                            <li><router-link to="/admin/hotel_types" class="menu-link">&emsp;旅館房型管理</router-link></li>
                            <li><router-link to="/admin/hotel_orders" class="menu-link">&emsp;旅館訂單管理</router-link></li>
                        </ul>
                    </li>

                    <!-- 單層選單 -->
                    <li class="menu-item">
                        <router-link to="/admin/order" class="menu-link">
                            <img width="24" height="24"
                                src="https://img.icons8.com/material-rounded/24/shopping-cart.png" />
                            訂單管理
                        </router-link>
                    </li>
                    <li class="menu-item">
                        <router-link to="/admin/promotion" class="menu-link">
                            <img width="24" height="24" src="https://img.icons8.com/ios-glyphs/30/discount--v1.png" />
                            優惠活動管理
                        </router-link>
                    </li>
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
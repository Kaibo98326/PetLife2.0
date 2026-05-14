<template>
  <div class="admin-header">
    <div class="header-logo">
      PetLife 後臺管理系統
    </div>

    <div class="header-user-area">
      <div class="user-profile">
        <div class="user-avatar-circle">
          <img src="https://img.icons8.com/ios-filled/50/user-male-circle.png" alt="avatar" />
        </div>
        <div class="user-text-info">
          <span class="user-name">{{ employeeStore.empName }}</span>
          <span class="user-role">{{ displayRole}}</span>
        </div>
      </div>
      <button class="logout-btn" @click="logout">登出</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useEmployeeStore } from '@/stores/employee';
import { useRouter } from 'vue-router';
import Swal from 'sweetalert2';

const employeeStore = useEmployeeStore();
const router = useRouter();

const displayRole = computed(() => {
  const roles = employeeStore.roles || []

  if (roles.includes('superuser')) {
    return '後台管理員'
  }

  if (roles.includes('staff')) {
    return '一般員工'
  }

  if (roles.includes('groomer')) {
    return '美容師'
  }

  return '未設定角色'
})

const logout = () => {
  Swal.fire({
    icon: 'warning',
    title: '確定要登出嗎？',
    text: '登出後需要重新登入才能使用後台系統功能',
    showCancelButton: true,
    confirmButtonText: '是的，登出',
    cancelButtonText: '取消'
  }).then((result) => {
        if(!result.isConfirmed) return;

        employeeStore.logout();
        Swal.fire({
                icon: 'success',
                title: '已登出',
                text: '期待您再次回來！',
                confirmButtonText: '回登入'
            }).then(() =>{
              router.push('/')
            })
    })
  
};
</script>
<script setup>
import { ref } from 'vue'
import Swal from 'sweetalert2'
import { useEmployeeStore } from '@/stores/employee'
import { useRouter } from 'vue-router'

const employeeStore = useEmployeeStore();
const router = useRouter();

const username = ref('');
const password = ref('');

const login = async () => {
    try {
        const res = await fetch('/api/employee/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username.value, password: password.value })
        })

        if (!res.ok) {
            const errMsg = await res.text()
            Swal.fire({ icon: 'error', title: '登入失敗', text: errMsg || '帳號或密碼錯誤' })
            return
        }

        // 後端回傳 JSON { token: "..." }
        const data = await res.json()
        employeeStore.login(data.token)

        Swal.fire({ icon: 'success', title: '登入成功', text: '歡迎回來！' })
            .then(() => {
                router.push('/admin/dashboard') // ✅ 建議加斜線，避免路由錯誤
            })
    } catch (err) {
        Swal.fire({ icon: 'error', title: '錯誤', text: err.message })
    }
}
</script>

<template>
    <div class="login-page">
        <div class="wrapper">
            <h1>員工登入</h1>
            <form @submit.prevent="login">
                <div class="input-box">
                    <input type="text" v-model="username" placeholder="帳號" required />
                    <i class="fas fa-user"></i>
                </div>
                <div class="input-box">
                    <input type="password" v-model="password" placeholder="密碼" required />
                    <i class="fas fa-lock"></i>
                </div>
                <div class="remember-forgot">
                    <label><input type="checkbox" /> 記住我</label>
                </div>
                <button type="submit" class="btn">登入</button>
            </form>
        </div>
    </div>
</template>

<style scoped src="@/assets/css/LoginEmp.css"></style>

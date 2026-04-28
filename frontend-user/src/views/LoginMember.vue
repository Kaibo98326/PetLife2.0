<script setup>
import { reactive, ref } from 'vue'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const useStore = useUserStore()

const isRegisterActive = ref(false)

const registerForm = reactive({
    memberName: '',
    phone: '',
    email: '',
    password: '',
    address: ''
})

const loginForm = reactive({
    email: '',
    password: ''
})

const errorMsg = ref('')
const errors = reactive({})

const placeholders = {
    memberName: '姓名',
    phone: '電話',
    email: '電子郵件',
    password: '密碼',
    address: '地址'
}

const registerFields = [
    { name: 'memberName', type: 'text' },
    { name: 'phone', type: 'text' },
    { name: 'email', type: 'email' },
    { name: 'password', type: 'password' },
    { name: 'address', type: 'text' }
]

const validateField = (name) => {
    if (!registerForm[name].trim()) {
        errors[name] = true
        placeholders[name] = `請輸入${placeholders[name]}`
    } else {
        errors[name] = false
    }
}

const handleRegister = async () => {
    try {
        await axios.post('/api/member/register', registerForm)
        Swal.fire({
            icon: 'success',
            title: '註冊成功！',
            text: '請使用帳號登入系統',
            confirmButtonText: '前往登入'
        }).then(() => {
            // SweetAlert 按下確認後導向登入頁面
            router.push('/login')
        })
    } catch (err) {
        Swal.fire({
            icon: 'error',
            title: '註冊失敗',
            text: '請稍後再試'
        })
    }
}

const handleLogin = async () => {
    try {
    const res = await axios.post('/api/member/login', loginForm)
    const token = res.data
    useStore.login(token)

    Swal.fire({
      icon: 'success',
      title: '登入成功！',
      text: '歡迎回到商城',
      confirmButtonText: '回首頁'
    }).then(() => {
      router.push('/')   // SweetAlert 按下確認後導向首頁
    })
  } catch (err) {
    Swal.fire({
      icon: 'error',
      title: '登入失敗',
      text: '帳號或密碼錯誤'
    })
  }
}



const goShop = () => {
    window.location.href = '/shop/index'
}
</script>
<template>
    <div class="container" :class="{ active: isRegisterActive }">
        <!-- 註冊表單 -->
        <div class="form-container sign-up">
            <form @submit.prevent="handleRegister">
                <h1>🐾建立帳號🐾</h1>
                <span>或使用電子郵件註冊</span>
                <div class="input-wrapper" v-for="field in registerFields" :key="field.name">
                    <input :type="field.type" v-model="registerForm[field.name]" :placeholder="placeholders[field.name]"
                        :class="{ 'input-error': errors[field.name] }" @blur="validateField(field.name)" />
                </div>
                <button type="submit">註冊</button>
            </form>
        </div>

        <!-- 登入表單 -->
        <div class="form-container sign-in">
            <form @submit.prevent="handleLogin">
                <h1>🐕登入</h1>
                <span>或使用電子郵件登入</span>
                <input type="email" v-model="loginForm.email" placeholder="電子郵件" required />
                <input type="password" v-model="loginForm.password" placeholder="密碼" required />
                <p v-if="errorMsg" style="color:red; font-size:13px;">{{ errorMsg }}</p>
                <a href="#">忘記密碼？</a>
                <button type="submit">登入</button>
                <button type="button" @click="goShop">回到賣場</button>
            </form>
        </div>

        <!-- 切換區塊 -->
        <div class="toggle-container">
            <div class="toggle">
                <div class="toggle-panel toggle-left">
                    <h1>歡迎回來！</h1>
                    <p>請輸入帳號以使用所有功能</p>
                    <button class="hidden" @click="isRegisterActive = false">登入</button>
                </div>
                <div class="toggle-panel toggle-right">
                    <h1>哈囉，新朋友！</h1>
                    <p>立即註冊以使用所有功能</p>
                    <button class="hidden" @click="isRegisterActive = true">註冊</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>
@import '../assets/css/LoginMember.css';
</style>
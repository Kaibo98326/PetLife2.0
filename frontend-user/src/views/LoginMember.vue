<script setup>
import { reactive, ref, onMounted } from 'vue'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { jwtDecode } from 'jwt-decode'
import * as bootstrap from 'bootstrap'





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

  const value = registerForm[name]?.trim()

  //空白檢查
  if (!value) {
    errors[name] = true

    Swal.fire({
      icon: 'warning',
      title: '欄位未填寫',
      text: `請輸入${placeholders[name]}`
    })
    return false
  }
  //Email 檢查
  if (name === 'email') {
    const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z][A-Za-z0-9.-]*\.[A-Za-z]{2,}$/

    if (!emailRegex.test(value)) {
      errors.email = true
      Swal.fire({
        icon: 'warning',
        title: 'Email 格式錯誤',
        text: '請輸入完整Eamil，例如 test@example.com'
      })

      return false
    }
  }

  if (name === 'address') {
    const addressRegex =
      /^[\u4e00-\u9fa5A-Za-z0-9號樓層段巷弄街路縣市區鎮鄉里村\-之\s]{5,100}$/

    if (!addressRegex.test(value)) {
      errors.address = true

      Swal.fire({
        icon: 'warning',
        title: '地址格式錯誤',
        text: '請輸入完整地址，至少包含縣市、區、街道等資訊'
      })
      return false
    }
  }

  errors[name] = false
  return true





}

const handleRegister = async () => {

  for (const field of registerFields) {
    const valid = validateField(field.name)

    if (!valid) {
      return
    }

  }
  try {
    await axios.post('/api/member/register', registerForm)

    Swal.fire({
      icon: 'success',
      title: '註冊成功！',
      text: '請使用帳號登入系統',
      confirmButtonText: '前往登入'
    }).then(() => {
      isRegisterActive.value = false
    })

  } catch (err) {
    const msg = err.response?.data || '請稍後再試'

    Swal.fire({
      icon: 'error',
      title: '註冊失敗',
      text: msg
    })
  }
}

const handleLogin = async () => {
  try {

    const res = await axios.post('/api/member/login', loginForm)

    const token = res.data.token

    useStore.login(token)

    // 🔥 存 user 資料
    useStore.user = res.data.user

    Swal.fire({
      icon: 'success',
      title: '登入成功！',
      text: '歡迎回到商城',
      confirmButtonText: '回首頁'
    }).then(() => {
      router.push('/')
    })

  } catch (err) {
    console.log(err.response?.status)
    console.log(err.response?.data)
    Swal.fire({
      icon: 'error',
      title: '登入失敗',
      text: err.response?.data || '帳號或密碼錯誤'
    })

  }
}

// Google 登入 URL
const googleAuthUrl =
  "https://accounts.google.com/o/oauth2/v2/auth" +
  "?client_id=70410001365-t87v8nd57cf2s3qa1mipfpftqsppr3hd.apps.googleusercontent.com" +
  "&redirect_uri=http://localhost:5173/login" +
  "&response_type=code" +
  "&scope=openid%20profile%20email" +
  "&access_type=offline" +
  "&prompt=consent";



async function handleGoogleLogin(code) {
  try {
    const res = await fetch(
      "http://localhost:8082/callback/google?code=" + code
    );

    if (!res.ok) {
      throw new Error(await res.text());
    }

    const data = await res.json();
    const token = data.token;

    useStore.login(token);
    useStore.user = data.user;


    const decoded = jwtDecode(token);


    if (decoded.mustSetPassword) {
      router.push("/member/center/profile");
    } else {

      router.push("/");
    }
  } catch (err) {
    console.error("Google login failed:", err);
    Swal.fire({
      icon: 'error',
      title: '登入失敗',
      text: err.response?.data || '無法登入，請聯繫管理員'
    })
  }
}

const forgotEmail = ref('')

const openForgotPasswordModal = () => {
  forgotEmail.value = ''

  new bootstrap.Modal(document.getElementById('forgotPasswordModal')).show()
}

const submitForgotPassword = async () => {
  if (!forgotEmail.value.trim()) {
    Swal.fire('提醒', '請輸入電子郵件', 'warning')

    return
  }

  try {
    await axios.post('/api/member/forgot-password', {
      email: forgotEmail.value
    })

    bootstrap.Modal.getInstance(document.getElementById('forgotPasswordModal')).hide()

    Swal.fire(
      '已寄出',
      '請到信箱查看重設密碼連結',
      'success'
    )

  } catch (err) {
    Swal.fire(
      '錯誤',
      err.response?.data || '請稍後在試',
      'error'

    ).then(() => {
      router.push('/login')
    })
  }
}


// 🔎 在頁面載入時檢查 URL 是否有 code
onMounted(() => {
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");

  if (code) {
    handleGoogleLogin(code);

    // 清掉 URL
    window.history.replaceState({}, document.title, "/login");
  }
});

const fillMemberDemo = () => {
  loginForm.email = 'demo@test.com'
  loginForm.password = '123456'
}

const fillAdminDemo = () => {
  loginForm.email = 'petlifetest123@gmail.com'
  loginForm.password = '123'
}
const fillRegisterDemo = () => {
  registerForm.memberName = '測試會員'
  registerForm.phone = '0912345678'
  registerForm.email = 'demo@test.com'
  registerForm.password = '123456'
  registerForm.address = '台北市信義區松仁路100號'
}

const goShop = () => {

  window.location.href = '/'
}
</script>
<template>
  <div class="login-page">
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
          <div class="register-btn-group">
            <button type="button" class="demo-btn" @click="fillRegisterDemo">一鍵輸入</button>
            <button type="submit" class="demo-btn">註冊</button>
          </div>
        </form>
      </div>

      <!-- 登入表單 -->
      <div class="form-container sign-in">
        <form @submit.prevent="handleLogin">
          <h1>🐕登入</h1>
          <span>或使用電子郵件登入</span>
          <div class="social-icons">
            <a :href="googleAuthUrl"><i class="bx bxl-google"></i></a>
          </div>
          <input type="email" v-model="loginForm.email" placeholder="電子郵件" required />
          <input type="password" v-model="loginForm.password" placeholder="密碼" required />
          <p v-if="errorMsg" style="color:red; font-size:13px;">{{ errorMsg }}</p>
          <a href="#" @click.prevent="openForgotPasswordModal">忘記密碼？</a>
          <div class="login-btn-group">
            <button type="button" class="demo-side-btn" @click="fillMemberDemo">Demo</button>
            <button type="submit" class="main-login-btn"> 登入</button>
            <button type="button" class="demo-side-btn" @click="fillAdminDemo">會員Demo</button>
          </div>
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
    <teleport to='body'>
      <div class="modal fade" id="forgotPasswordModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered modal-lg ">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">忘記密碼</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form @submit.prevent="submitForgotPassword">
              <div class="modal-body">
                <p class="text-muted">
                  請輸入註冊時使用的電子郵件，我們會寄送重設密碼連結。
                </p>
                <input v-model="forgotEmail" type="email" class="form-control" placeholder="請輸入電子郵件" />
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                <button type="submit" class="btn btn-primary">寄送連結</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>
<style scoped>
@import '../assets/css/LoginMember.css';
</style>
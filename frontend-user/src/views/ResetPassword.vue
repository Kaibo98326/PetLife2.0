<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'


const route = useRoute()
const router = useRouter()

const token = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

onMounted(() => {
  token.value = route.query.token || ''

  if (!token.value) {
    Swal.fire('錯誤', '重設連結無效', 'error')
    router.push('/login')
  }
})

const submitResetPassword = async () => {
  if (!newPassword.value || !confirmPassword.value) {
    Swal.fire('提醒', '請輸入新密碼與確認密碼', 'warning')
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    Swal.fire('提醒', '兩次輸入的密碼不一致', 'warning')
    return
  }

  try {
    await axios.post('/api/member/reset-password', {
      token: token.value,
      newPassword: newPassword.value
    })

    Swal.fire('成功', '密碼已重設，請重新登入', 'success')
      .then(() => {
        router.push('/login')
      })

  } catch (err) {
    Swal.fire(
      '錯誤',
      err.response?.data || '重設密碼失敗',
      'error'
    )
  }
}

</script>
<template>
  <div class="reset-page">
    <div class="reset-card">
      <h2>重設密碼</h2>

      <input
        v-model="newPassword"
        type="password"
        placeholder="請輸入新密碼"
      />

      <input
        v-model="confirmPassword"
        type="password"
        placeholder="請再次輸入新密碼"
      />

      <button @click="submitResetPassword">
        確認重設密碼
      </button>
    </div>
  </div>
</template>
<style scoped>
.reset-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(to right, #FFE5B4, #FFB347, #FF7F50);
}

.reset-card {
  width: 420px;
  background: white;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 5px 15px rgba(0,0,0,.25);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.reset-card h2 {
  text-align: center;
  color: #ff7f50;
}

.reset-card input {
  height: 48px;
  border-radius: 10px;
  border: 1px solid #ddd;
  padding: 0 14px;
}

.reset-card button {
  height: 48px;
  border: none;
  border-radius: 10px;
  background: #ff9968;
  color: white;
  font-weight: bold;
}
</style>
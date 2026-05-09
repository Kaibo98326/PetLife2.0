<script setup>
import { reactive, ref } from 'vue'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const useStore = useUserStore()

const form = reactive({
  newPassword: '',
  confirmPassword: ''
})

const errorMsg = ref('')

const handleSetPassword = async () => {
  if (form.newPassword !== form.confirmPassword) {
    errorMsg.value = '兩次輸入的密碼不一致'
    return
  }

  try {
    // 從 store 取出 memberId
    const decoded = jwtDecode(useStore.token)

    await axios.post('/api/member/set-password', {
      memberId,
      newPassword: form.newPassword
    }, {
      headers: {
        Authorization: `Bearer ${useStore.token}`
      }
    })

    Swal.fire({
      icon: 'success',
      title: '密碼設定成功！',
      text: '您現在可以使用帳號密碼登入',
      confirmButtonText: '回首頁'
    }).then(() => {
      router.push('/')
    })
  } catch (err) {
    Swal.fire({
      icon: 'error',
      title: '設定失敗',
      text: '請稍後再試'
    })
    console.log(err)
  }
}
</script>

<template>
  <div class="set-password-container">
    <h1>🔒 設定密碼</h1>
    <form @submit.prevent="handleSetPassword">
      <input type="password" v-model="form.newPassword" placeholder="新密碼" required />
      <input type="password" v-model="form.confirmPassword" placeholder="確認密碼" required />
      <p v-if="errorMsg" style="color:red; font-size:13px;">{{ errorMsg }}</p>
      <button type="submit">設定密碼</button>
    </form>
  </div>
</template>

<style scoped>
.set-password-container {
  max-width: 400px;
  margin: 50px auto;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  text-align: center;
}
.set-password-container input {
  display: block;
  width: 100%;
  margin: 10px 0;
  padding: 10px;
}
.set-password-container button {
  margin-top: 15px;
  padding: 10px;
  width: 100%;
}
</style>

<script setup>
import { useUserStore } from '@/stores/user';
import { ref, onMounted } from 'vue';
import Swal from 'sweetalert2';




const userStore = useUserStore()

// 表單資料
const memberForm = ref({
    memberId: '',
    memberName: '',
    email: '',
    phone: '',
    address: '',
    registerTime: '',
    lastLogin: ''
})
/*=========
   密碼區
 ==========*/
const oldPassword = ref('')
const newPassword = ref('')
const showNewPassword = ref(false)
const verifyResult = ref('')
/*=========
   防呆檢查
 ==========*/
const emailCheckResult = ref('')
const phoneCheckResult = ref('')

/* ==============
    原始值（比對用）
   ==============*/
const originalPhone = ref('')
const originalEmail = ref('')

// 載入初始值
onMounted(async () => {
    try {
        const res = await fetch(`/api/member/${userStore.memberId}`, {
            headers: {
                'Authorization': `Bearer ${userStore.token}`
            }
        })

        if (!res.ok) throw new Error('載入會員失敗')

        const data = await res.json()
        memberForm.value = {
            memberId: data.memberId,
            memberName: data.memberName,
            email: data.email,
            phone: data.phone,
            address: data.address,
            registerTime: new Date(data.registerTime).toLocaleString(),
            lastLogin: new Date(data.lastLogin).toLocaleString()
        }
        originalEmail.value = data.email
        originalPhone.value = data.phone

    } catch (err) {
        Swal.fire({ icon: 'error', title: '錯誤', text: err.message })
        console.log('memberId:', userStore.memberId);
    }
})

// 檢查 Email
const checkEmail = async () => {
    const res = await fetch(`/api/member/checkEmail?email=${memberForm.value.email}`, {
        headers: {
            'Authorization': `Bearer ${userStore.token}`
        }
    })
    const data = await res.json()
    emailCheckResult.value = data.available ? '可使用 ✔' : '已被使用 ✘'
}

// 檢查 Phone
const checkPhone = async () => {
    const res = await fetch(`/api/member/checkPhone?phone=${memberForm.value.phone}`, {
        headers: {
            'Authorization': `Bearer ${userStore.token}`
        }
    })
    const data = await res.json()
    phoneCheckResult.value = data.available ? '可使用 ✔' : '已被使用 ✘'
}

// 舊密碼驗證
const verifyOldPassword = async () => {
    try {
        const res = await fetch('/api/member/verifyPassword', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${userStore.token}`
            },
            body: JSON.stringify({
                memberId: userStore.memberId,
                oldPassword: oldPassword.value
            })
        })
        const data = await res.json()
        if (data.valid) {
            verifyResult.value = '舊密碼正確'
            showNewPassword.value = true   // ✅ 驗證成功才顯示新密碼框
        } else {
            verifyResult.value = '舊密碼錯誤'
            showNewPassword.value = false  // ✅ 驗證失敗就隱藏
        }
    } catch (err) {
        console.error(err)
        verifyResult.value = '驗證失敗'
        showNewPassword.value = false
    }
}

// 儲存修改
const saveProfile = async () => {
    // 如果 email 有變動 → 必須檢查通過
    if (memberForm.value.email !== originalEmail.value && emailCheckResult.value !== '可使用 ✔') {
        Swal.fire({ icon: 'warning', title: '請先驗證', text: '請先檢查電子郵件是否可用' })
        return
    }

    // 如果 phone 有變動 → 必須檢查通過
    if (memberForm.value.phone !== originalPhone.value && phoneCheckResult.value !== '可使用 ✔') {
        Swal.fire({ icon: 'warning', title: '請先驗證', text: '請先檢查電話是否可用' })
        return
    }

    try {
        const res = await fetch('/api/member/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${userStore.token}`
            },
            body: JSON.stringify({
                ...memberForm.value,
                password: showNewPassword.value ? newPassword.value : null
            })
        })

        if (!res.ok) {
            const errMsg = await res.text()
            Swal.fire({ icon: 'error', title: '失敗', text: errMsg || '會員資料更新失敗' })
            return
        }

        //重新同步 store
        Swal.fire({ icon: 'success', title: '成功', text: '會員資料已更新' })
        await userStore.fetchUser()

        // reset password state
        oldPassword.value = ''
        newPassword.value = ''
        showNewPassword.value = false
        verifyResult.value = ''


    } catch (err) {
        Swal.fire({ icon: 'error', title: '錯誤', text: err.message })
    }
}

</script>
<template>
    <div class="profile-view">
        <h2>修改會員資料</h2>
        <form @submit.prevent="saveProfile" class="form-group">

            <!-- 會員編號 -->
            <div class="mb-3">
                <label for="memberId">會員編號</label>
                <input type="text" id="memberId" v-model="memberForm.memberId" readonly class="form-control" />
            </div>

            <!-- 會員名稱 -->
            <div class="mb-3">
                <label for="memberName">會員名稱</label>
                <input type="text" id="memberName" v-model="memberForm.memberName" required class="form-control" />
            </div>

            <!-- Email -->
            <div class="mb-3">
                <label for="email">電子郵件</label>
                <div class="d-flex gap-2">
                    <input type="email" id="email" v-model="memberForm.email" required class="form-control" />
                    <button type="button" class="btn btn-secondary" @click="checkEmail">檢查</button>
                </div>
                <small>{{ emailCheckResult }}</small>
            </div>

            <!-- 電話 -->
            <div class="mb-3">
                <label for="phone">電話</label>
                <div class="d-flex gap-2">
                    <input type="text" id="phone" v-model="memberForm.phone" class="form-control" />
                    <button type="button" class="btn btn-secondary" @click="checkPhone">檢查</button>
                </div>
                <small>{{ phoneCheckResult }}</small>
            </div>

            <!-- 地址 -->
            <div class="mb-3">
                <label for="address">地址</label>
                <input type="text" id="address" v-model="memberForm.address" class="form-control" />
            </div>

            <!-- 創建時間 -->
            <div class="mb-3">
                <label for="registerTime">創建時間</label>
                <input type="text" id="registerTime" v-model="memberForm.registerTime" readonly class="form-control" />
            </div>

            <!-- 最後登入時間 -->
            <div class="mb-3">
                <label for="lastLogin">最後登入時間</label>
                <input type="text" id="lastLogin" v-model="memberForm.lastLogin" readonly class="form-control" />
            </div>

            <!-- 舊密碼驗證 -->
            <div class="mb-3">
                <label for="oldPassword">請輸入舊密碼</label>
                <input type="password" id="oldPassword" v-model="oldPassword" class="form-control" />
                <button type="button" class="btn btn-secondary mt-2" @click="verifyOldPassword">驗證</button>
                <div class="mt-2" style="color: red;">{{ verifyResult }}</div>
            </div>

            <!-- 新密碼 -->
            <div class="mb-3" v-if="showNewPassword">
                <label for="password">新密碼</label>
                <input type="password" id="password" v-model="newPassword" class="form-control" />
            </div>

            <!-- 儲存修改 -->
            <div class="d-flex gap-2">
                <button type="submit" class="btn btn-primary">儲存修改</button>
                <router-link to="/member/center" class="btn btn-secondary">返回會員中心</router-link>
            </div>
        </form>
    </div>
</template>
<style scoped>
.profile-view {
    max-width: 600px;
    margin: auto;
    font-family: 'Noto Sans TC', sans-serif;
}

h2 {
    margin-bottom: 20px;
    color: #faa23d;
}

.form-group label {
    font-weight: 500;
}

small {
    color: #46e631;
}

button.btn-secondary {
    white-space: nowrap;
}
</style>
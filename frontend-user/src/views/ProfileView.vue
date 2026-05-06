<script setup>
import { useUserStore } from '@/stores/user';
import { ref, onMounted } from 'vue';
import Swal from 'sweetalert2';
import { useRouter } from 'vue-router';
import { jwtDecode } from 'jwt-decode';

const router = useRouter()
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
// =====================
// Google 強制模式
// =====================
const isGoogleForceChange = ref(false);
/* ==============
    原始值（比對用）
   ==============*/
const originalPhone = ref('')
const originalEmail = ref('')

// 載入初始值
onMounted(async () => {
    try {
        const decoded = jwtDecode(userStore.token)
        isGoogleForceChange.value = decoded.mustSetPassword === true

        // ✅ Google 登入後已經有 user，先直接塞表單
        if (userStore.user) {
            const data = userStore.user

            memberForm.value = {
                memberId: data.memberId,
                memberName: data.memberName,
                email: data.email,
                phone: data.phone || '',
                address: data.address || '',
                registerTime: data.registerTime
                    ? new Date(data.registerTime).toLocaleString()
                    : '',
                lastLogin: data.lastLogin
                    ? new Date(data.lastLogin).toLocaleString()
                    : ''
            }

            originalEmail.value = data.email
            originalPhone.value = data.phone || ''
            return
        }

        // ✅ 沒有 user 才走原本 fetch
        const res = await fetch('/api/member/me', {
            headers: {
                Authorization: `Bearer ${userStore.token}`
            }
        })

        if (!res.ok) throw new Error('載入會員失敗')

        const data = await res.json()

        memberForm.value = {
            memberId: data.memberId,
            memberName: data.memberName,
            email: data.email,
            phone: data.phone || '',
            address: data.address || '',
            registerTime: data.registerTime
                ? new Date(data.registerTime).toLocaleString()
                : '',
            lastLogin: data.lastLogin
                ? new Date(data.lastLogin).toLocaleString()
                : ''
        }

        originalEmail.value = data.email
        originalPhone.value = data.phone || ''

    } catch (err) {
        Swal.fire({ icon: 'error', title: '錯誤', text: err.message })
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

    // =========================
    // 🔥 GOOGLE 強制改密碼模式
    // =========================
    if (isGoogleForceChange.value) {
        if (!newPassword.value) {
            Swal.fire({ icon: 'warning', title: '請輸入密碼' })
            return
        }

        try {
            const res = await fetch('/api/member/set-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${userStore.token}`
                },
                body: JSON.stringify({
                    memberId: memberForm.value.memberId,
                    newPassword: newPassword.value
                })
            })

            if (!res.ok) {
                const errMsg = await res.text()
                throw new Error(errMsg || '設定密碼失敗')
            }

            const newToken = await res.text()

            userStore.login(newToken)

            Swal.fire({
                icon: 'success',
                title: '設定完成',
                text: '歡迎使用系統'
            }).then(() => {
                router.push('/')
            })

        } catch (err) {
            console.error(err)
            Swal.fire({
                icon: 'error',
                title: '設定失敗',
                text: err.message
            })
        }

        return
    }


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
        <h2>{{ isGoogleForceChange ? '設定密碼' : '修改會員資料' }}</h2>

        <div v-if="isGoogleForceChange" style="color:red; font-weight:bold; margin-bottom: 15px;">
            ⚠️ Google 帳號首次登入，請先設定密碼
        </div>

        <form @submit.prevent="saveProfile" class="form-group">

            <!-- Google 強制改密碼模式 -->
            <div v-if="isGoogleForceChange">

                <div class="mb-3">
                    <label for="memberId">會員編號</label>
                    <input type="text" id="memberId" v-model="memberForm.memberId" readonly class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="memberName">會員名稱</label>
                    <input type="text" id="memberName" v-model="memberForm.memberName" readonly class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="email">電子郵件</label>
                    <input type="email" id="email" v-model="memberForm.email" readonly class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="password">請設定新密碼</label>
                    <input type="password" id="password" v-model="newPassword" required class="form-control"
                        placeholder="請輸入新密碼" />
                </div>

            </div>

            <!-- 一般會員修改模式 -->
            <div v-else>

                <div class="mb-3">
                    <label for="memberId">會員編號</label>
                    <input type="text" id="memberId" v-model="memberForm.memberId" readonly class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="memberName">會員名稱</label>
                    <input type="text" id="memberName" v-model="memberForm.memberName" required class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="email">電子郵件</label>

                    <div class="d-flex gap-2">
                        <input type="email" id="email" v-model="memberForm.email" required class="form-control" />

                        <button type="button" class="btn btn-secondary" @click="checkEmail">
                            檢查
                        </button>
                    </div>

                    <small>{{ emailCheckResult }}</small>
                </div>

                <div class="mb-3">
                    <label for="phone">電話</label>

                    <div class="d-flex gap-2">
                        <input type="text" id="phone" v-model="memberForm.phone" class="form-control" />

                        <button type="button" class="btn btn-secondary" @click="checkPhone">
                            檢查
                        </button>
                    </div>

                    <small>{{ phoneCheckResult }}</small>
                </div>

                <div class="mb-3">
                    <label for="address">地址</label>

                    <input type="text" id="address" v-model="memberForm.address" class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="registerTime">創建時間</label>

                    <input type="text" id="registerTime" v-model="memberForm.registerTime" readonly
                        class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="lastLogin">最後登入時間</label>

                    <input type="text" id="lastLogin" v-model="memberForm.lastLogin" readonly class="form-control" />
                </div>

                <div class="mb-3">
                    <label for="oldPassword">請輸入舊密碼</label>

                    <input type="password" id="oldPassword" v-model="oldPassword" class="form-control" />

                    <button type="button" class="btn btn-secondary mt-2" @click="verifyOldPassword">
                        驗證
                    </button>

                    <div class="mt-2" style="color: red;">
                        {{ verifyResult }}
                    </div>
                </div>

                <div class="mb-3" v-if="showNewPassword">
                    <label for="password">新密碼</label>

                    <input type="password" id="password" v-model="newPassword" class="form-control" />
                </div>

            </div>

            <!-- 按鈕 -->
            <div class="d-flex gap-2">

                <button type="submit" class="btn btn-primary">
                    {{ isGoogleForceChange ? '完成設定' : '儲存修改' }}
                </button>

                <router-link v-if="!isGoogleForceChange" to="/member/center" class="btn btn-secondary">
                    返回會員中心
                </router-link>

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
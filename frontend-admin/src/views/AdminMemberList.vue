<script setup>
import { ref, onMounted, computed } from 'vue';
import request from '@/utils/request';
import Swal from 'sweetalert2';
import * as bootstrap from 'bootstrap';


const members = ref([]);
const page = ref(0)
const size = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)

const searchType = ref('')
const keyword = ref('')
const selectKeyword = ref('')

const searchTypes = [
    { label: '不篩選', value: '' },
    { label: '會員姓名', value: 'memberName' },
    { label: '手機末三碼', value: 'phoneLast3' },
    { label: '電子郵件', value: 'email' },
    { label: '帳號狀態', value: 'accountStatus' },
    { label: '第三方登入來源', value: 'provider' }
]

const statusOptions = [
    { label: '啟用', value: 'active' },
    { label: '停權', value: 'disable' },
    { label: '刪除', value: 'delete' }
]

const providerOptions = [
    { label: '本地註冊', value: 'local' },
    { label: 'Google', value: 'google' }
]

const needSelectKeyword = computed(() => {
    return searchType.value === 'accountStatus' || searchType.value === 'provider'
});

const loadMembers = async () => {
    try {
        const params = {
            page: page.value,
            size: size.value
        }
        if (searchType.value) {
            const finalKeyword = needSelectKeyword.value ? selectKeyword.value : keyword.value

            if (!finalKeyword) {
                Swal.fire('提示', '請輸入或選擇查詢條件', 'info')
                return
            }
            params.searchType = searchType.value
            params.keyword = finalKeyword
        }
        const res = await request.get('/api/admin/members', { params })

        members.value = res.data.content || []
        page.value = Number(res.data.currentPage ?? res.data.number ?? 0)
        totalPages.value = Number(res.data.totalPages ?? 1)
        totalElements.value = Number(res.data.totalElements ?? 0)

    } catch (err) {
        console.log(err)
        Swal.fire('錯誤', '會員資料載入失敗', 'error')
    }
}
const handleKeywordInput = () => {
    //手機末三碼
    if (searchType.value === 'phoneLast3') {

        //只保留數字
        keyword.value = keyword.value.replace(/\D/g, '')
        //最多三個字
        keyword.value = keyword.value.slice(0, 3)
    }
}

const searchMembers = () => {
    page.value = 0
    loadMembers()
}

const resetSearch = () => {
    searchType.value = ''
    keyword.value = ''
    selectKeyword.value = ''
    page.value = 0
    loadMembers()
}
const prePage = () => {
    if (page.value <= 0) return
    page.value--
    loadMembers()
}
const nextPage = () => {
    if (page.value + 1 >= totalPages.value) return
    page.value++
    loadMembers()
}

const addForm = ref({
    memberName: '',
    email: '',
    password: '',
    phone: '',
    address: ''
});

const openAddModal = () => {
    addForm.value = {
        memberName: '',
        email: '',
        password: '',
        phone: '',
        address: ''
    }

    const modal = new bootstrap.Modal(
        document.getElementById('addMemberModal')
    );
    modal.show();
}

const submitAddMember = async () => {
    try {
        await request.post('/api/admin/members', addForm.value)

        Swal.fire(
            '成功',
            '新增會員成功',
            'success'
        )
        const modalEl = document.getElementById('addMemberModal')

        const modal = bootstrap.Modal.getInstance(modalEl)

        modal.hide()

        loadMembers()

    } catch (err) {
        console.log(err)
        Swal.fire(
            '失敗',
            err.response?.data || '新增失敗',
            'error'
        )
    }
}

onMounted(loadMembers)
</script>
<template>
    <div class="admin-member-wrapper p-4">
        <div class="admin-table-header-orange shadow-sm">
            <div class="d-flex justify-content-between align-items-center">
                <span class="fw-bold">會員管理 - 現有會員</span>
                <span class="badge bg-white text-orange-dark px-3 py-2">
                    共 {{ totalElements }} 筆會員
                </span>
            </div>
        </div>
        <div class="search-card shadow-sm p-3 mb-3">
            <div class="d-flex gap-2 justify-content-end align-items-center flex-wrap">
                <select v-model="searchType" class="form-select" style="width: 180px;">
                    <option v-for="type in searchTypes" :key="type.value" :value="type.value"> {{ type.label }}</option>
                </select>
                <input v-if="searchType && !needSelectKeyword" v-model="keyword" :type="text" class="form-control"
                    style="width: 250px;" :placeholder="searchType === 'phoneLast3' ? '請輸入手機末三碼' : '請輸入查詢關鍵字'"
                    :maxlength="searchType === 'phoneLast3' ? 3 : null" @input="handleKeywordInput"
                    @keyup.enter="searchMembers" />
                <select v-if="searchType === 'accountStatus'" v-model="selectKeyword" class="form-select"
                    style="width: 180px;">
                    <option value="" disabled>請選擇帳號狀態</option>
                    <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <select v-if="searchType === 'provider'" v-model="selectKeyword" class="form-select"
                    style="width: 180px;">
                    <option value="" disabled>請選擇登入來源</option>
                    <option v-for="opt in providerOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <button class="btn btn-orange-action" @click="searchMembers">搜尋</button>
                <button class="btn btn-orange-secondary" @click="resetSearch">重置</button>
                <button class="btn btn-success" @click="openAddModal">+新增會員</button>
            </div>
        </div>
        <div class="admin-table-container shadow-sm">
            <table class="table align-middle admin-fixed-talbe m-0">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>姓名</th>
                        <th>電話</th>
                        <th>Eamil</th>
                        <th>狀態</th>
                        <th>登入來源</th>
                        <th>註冊時間</th>
                        <th>最後登入</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="m in members" :key="m.memberId">
                        <td>{{ m.memberId }}</td>
                        <td class="fw-bold">{{ m.memberName }}</td>
                        <td>{{ m.phone || '-' }}</td>
                        <td>{{ m.email }}</td>
                        <td>
                            <span class="status-pill" :class="{
                                active: m.accountStatus === 'active',
                                disable: m.accountStatus === 'disable',
                                delete: m.accountStatus === 'delete'
                            }">{{ m.accountStatus }}</span>
                        </td>
                        <td>{{ m.provider || 'local' }}</td>
                        <td>{{ m.registerTime ? new Date(m.registerTime).toLocaleString() : '-' }}</td>
                        <td>{{ m.lastLogin ? new Date(m.lastLogin).toLocaleString() : '-' }}</td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary me-2">查看</button>
                            <button class="btn btn-sm btn-outline-warning">狀態</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="pagination-container mt-4 d-flex justify-content-center align-items-center gap-3">
            <button class="btn btn-outline-secondary" :disabled="page === 0" @click="prePage">上一頁</button>
            <span>第{{ Number(page) + 1 }} 頁 / 共 {{ Number(totalPages) }} 頁</span>
            <button class="btn btn-outline-secondary" :disabled="page + 1 >= totalPages" @click="nextPage">下一頁</button>
        </div>
        <!-- 新增會員 Modal -->
        <div class="modal fade" id="addMemberModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">新增會員</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <form @submit.prevent="submitAddMember">
                        <div class="modal-body">
                            <div class="mb-3">
                                <label>會員姓名</label>
                                <input v-model="addForm.memberName" type="text" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label>Email</label>
                                <input v-model="addForm.email" type="email" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label>密碼</label>
                                <input v-model="addForm.password" type="password" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label>電話</label>
                                <input v-model="addForm.phone" type="text" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label>地址</label>
                                <input v-model="addForm.address" type="text" class="form-control" />
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="submit" class="btn btn-success">新增會員</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>
.text-orange-dark {
    color: #E65100;
}

.admin-table-header-orange {
    background: linear-gradient(to right, #E65100, #FF9800);
    color: white;
    padding: 15px 25px;
    border-radius: 12px 12px 0 0;
}

.search-card {
    background: white;
    border-left: 2px solid #FFE0B2;
    border-right: 2px solid #FFE0B2;
}

.admin-table-container {
    background: white;
    border: 2px solid #FFE0B2;
    border-radius: 0 0 12px 12px;
}

.admin-fixed-table thead th {
    background-color: #FFF3E0;
    color: #BF360C;
    text-align: center;
    border-bottom: 2px solid #FFCC80;
    padding: 12px;
}

.admin-fixed-table tbody td {
    text-align: center;
    border-bottom: 1px solid #FFF3E0;
    padding: 10px;
}

.btn-orange-action {
    background-color: #FF9800;
    color: white;
    border: none;
    font-weight: bold;
}

.btn-orange-action:hover {
    background-color: #E65100;
    color: white;
}

.status-pill {
    padding: 4px 12px;
    border-radius: 999px;
    font-weight: bold;
    font-size: 13px;
}

.status-pill.active {
    background-color: #e8f5e9;
    color: #2e7d32;
}

.status-pill.suspended {
    background-color: #fff3e0;
    color: #ef6c00;
}

.status-pill.deleted {
    background-color: #ffebee;
    color: #c62828;
}
</style>
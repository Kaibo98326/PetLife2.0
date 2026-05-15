<script setup>
import { ref, onMounted, computed } from 'vue';
import request from '@/utils/request';
import Swal from 'sweetalert2';
import * as bootstrap from 'bootstrap';
import { useEmployeeStore } from '@/stores/employee';

const employeeStore = useEmployeeStore()

const isSuperUser = computed(() => {
    return employeeStore.roles?.includes('superuser')
})


const mode = ref('employee')

const employees = ref([])

const currentPage = ref(0)
const totalPages = ref(0)
const searchType = ref('')
const keyword = ref('')

const searchOptions = [
    { label: '員工姓名', value: 'empName' },
    { label: '員工ID', value: 'empId' },
    { label: '電話末三碼', value: 'phoneLast3' },
    { label: '狀態', value: 'status' }
]

const statusOptions = [
    { label: '啟用', value: 'active' },
    { label: '停權', value: 'disable' },
    { label: '刪除', value: 'delete' }
]


const loadEmployees = async (page = 0) => {

    try {
        const params = {
            page,
            size: 10
        }

        if (searchType.value && keyword.value) {
            params.searchType = searchType.value

            params.keyword = keyword.value
        }
        const res = await request.get('/api/admin/employees', { params })

        employees.value = res.data.content

        currentPage.value = res.data.number

        totalPages.value = res.data.totalPages

    } catch (err) {
        console.log(err);

    }
}

const searchEmployees = () => {
    loadEmployees(0)
}

const changeSearchType = () => {
    keyword.value = ''
}

const handleKeywordInput = () => {
    if (searchType.value === 'phoneLast3') {
        keyword.value = keyword.value.replace(/\D/g, '').slice(0, 3)
    }

    if (searchType.value === 'empId') {
        keyword.value = keyword.value.replace(/\D/g, '')
    }
}

const addForm = ref({
    username: '',
    passwordHash: '',
    empName: '',
    empPhone: '',
    empAddress: '',
    emergnecyContact: '',
    emergnecyPhone: ''
})

const fillRegisterDemo = () =>{
    addForm.value.username = 'marylin'
    addForm.value.passwordHash = '123'
    addForm.value.empName = '瑪莉琳'
    addForm.value.empPhone = '0912345678'
    addForm.value.empAddress = '台北市信義區松仁路123號'
    addForm.value.emergencyContact = '張小明'
    addForm.value.emergencyPhone = '0987654321'
}

const openAddModal = () => {
    addForm.value = {
        username: '',
        passwordHash: '',
        empName: '',
        empPhone: '',
        empAddress: '',
        emergencyContact: '',
        emergencyPhone: ''
    }

    const modal = new bootstrap.Modal(
        document.getElementById('addEmployeeModal')
    )

    modal.show()
}

const submitAddEmployee = async () => {
    try {
        await request.post('/api/admin/employees', addForm.value)

        Swal.fire(
            '成功',
            '新增員工成功',
            'success'
        )
        const modal = bootstrap.Modal.getInstance(
            document.getElementById('addEmployeeModal')
        )
        modal.hide()

        loadEmployees()
    } catch (err) {
        console.log(err);
        Swal.fire(
            '失敗',
            '新增失敗',
            'error'
        )

    }
}

const editForm = ref({
    empId: '',
    username: '',
    empName: '',
    newPassword: '',
    empPhone: '',
    empAddress: '',
    emergencyContact: '',
    emergencyPhone: '',
    status: ''
})

const openEditModal = (emp) => {
    editForm.value = { ...emp }

    const modal = new bootstrap.Modal(
        document.getElementById('editEmployeeModal')
    )

    modal.show()
}

const submitUpdateEmployee = async () => {
    try {
        const payload = {
            username: editForm.value.username,
            passwordHash: editForm.value.passwordHash,
            empName: editForm.value.empName,
            empPhone: editForm.value.empPhone,
            empAddress: editForm.value.empAddress,
            emergencyContact: editForm.value.emergencyContact,
            emergencyPhone: editForm.value.emergencyPhone,
            status: editForm.value.status
        }

        await request.put(`/api/admin/employees/${editForm.value.empId}`, payload)

        if (editForm.value.newPassword?.trim()) {
            await request.put(`/api/admin/employees/${editForm.value.empId}/password`, {
                newPassword: editForm.value.newPassword
            })
        }

        Swal.fire('成功', '員工資料已更新', 'success')

        const modal = bootstrap.Modal.getInstance(
            document.getElementById('editEmployeeModal')
        )

        modal.hide()
        loadEmployees()

    } catch (err) {
        console.log(err)
        Swal.fire('錯誤', '修改員工失敗', 'error')
    }
}
const toggleEmployeeStatus = async (emp) => {
    const newStatus = emp.status === 'active' ? 'disable' : 'active'

    try {
        await request.put(`/api/admin/employees/${emp.empId}/status`, {
            status: newStatus
        })

        emp.status = newStatus

    } catch (err) {
        console.log(err)
        Swal.fire('錯誤', '狀態切換失敗', 'error')
    }
}
const roles = ref([])

const roleForm = ref({
    roleId: null,
    roleName: '',
    description: ''
})

const isRoleEditing = ref(false)

const loadRoles = async () => {
    try {
        const res = await request.get('/api/admin/roles')
        roles.value = res.data
    } catch (err) {
        console.log(err);
        Swal.fire('錯誤', '角色資料載入失敗', 'error')
    }
}

const submitRole = async () => {

    try {
        if (isRoleEditing.value) {
            await request.put(`/api/admin/roles/${roleForm.value.roleId} `, roleForm.value)
            Swal.fire('成功', '角色已更新', 'success')
        } else {
            await request.post('/api/admin/roles', roleForm.value)
            Swal.fire('成功', '角色已新增', 'success')
        }
        resetRoleForm()
        loadRoles()
    } catch (err) {
        console.log(err);
        Swal.fire('錯誤', err.response?.data || '操作失敗', 'error')

    }

}

const editRole = (role) => {
    isRoleEditing.value = true;
    roleForm.value = { ...role }
}


const resetRoleForm = () => {
    isRoleEditing.value = false
    roleForm.value = {
        roleId: null,
        roleName: '',
        description: ''
    }
}

const deleteRole = async (role) => {
    const result = await Swal.fire({
        title: '確定刪除角色？',
        text: `角色：${role.roleName}`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '刪除',
        cancelButtonText: '取消'
    })

    if (!result.isConfirmed) return

    try {
        await request.delete(`/api/admin/roles/${role.roleId}`)
        Swal.fire('成功', '角色已刪除', 'success')
        loadRoles()

    } catch (err) {
        console.log(err);
        Swal.fire('錯誤', err.response?.data || '刪除失敗', 'error')
    }
}

const selectedEmployee = ref(null)

const selectedRoleIds = ref([])

const selectEmployeeRoles = async (emp) => {
    selectedEmployee.value = emp

    try {
        const res = await request.get(`/api/admin/employees/${emp.empId}/roles`)

        selectedRoleIds.value = res.data
    } catch (err) {
        console.log(err);

        Swal.fire('錯誤', '角色載入失敗', 'error')
    }
}

const saveEmployeeRoles = async () => {
    if (!selectedEmployee.value) {
        Swal.fire('提醒', '請先選擇員工', 'warning')

        return
    }

    try {
        await request.put(`/api/admin/employees/${selectedEmployee.value.empId}/roles`, {
            roleIds: selectedRoleIds.value
        })
        Swal.fire('成功', '角色更新成功', 'success')
        loadEmployees()
    } catch (err) {
        console.log(err);
        Swal.fire('錯誤', '角色更新失敗', 'error')
    }
}

onMounted(() => {
    loadEmployees()
    loadRoles()
})

</script>
<template>
    <div class="p-4">
        <div class="d-flex gap-2 mb-4">
            <button class="btn" :class="mode === 'employee' ? 'btn-warning' : 'btn-outline-warning'"
                @click="mode = 'employee'">員工清單</button>
            <button class="btn" :class="mode === 'role' ? 'btn-warning' : 'btn-outline-warning'"
                @click="mode = 'role'">角色管理</button>
            <button class="btn" :class="mode === 'employeeRole' ? 'btn-warning' : 'btn-outline-warning'"
                @click="mode = 'employeeRole'">員工角色綁定</button>
        </div>
        <!-- 員工清單 table-->
        <div v-if="mode === 'employee'">
            <div class="card shadow-sm p-3">
                <h4>員工清單</h4>
                <div class="d-flex gap-2 mb-3">
                    <select v-model="searchType" class="form-select w-auto" @change="changeSearchType">
                        <option value="">請選擇搜尋類型</option>
                        <option v-for="item in searchOptions" :key="item.value" :value="item.value">{{ item.label }}
                        </option>
                    </select>
                    <!--狀態-->
                    <select v-if="searchType === 'status'" v-model="keyword" class="form-select w-auto">
                        <option value="">請選擇狀態</option>
                        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}
                        </option>
                    </select>
                    <!--其他-->
                    <input v-else v-model="keyword" type="text" class="form-control w-auto" placeholder="請輸入搜尋內容"
                        @input="handleKeywordInput" />
                    <button class="btn btn-primary" @click="searchEmployees">搜尋</button>
                    <button v-if="isSuperUser" class="btn  btn-orange-action" @click="openAddModal"> + 新增員工</button>
                </div>
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>帳號</th>
                            <th>姓名</th>
                            <th>電話</th>
                            <th>角色</th>
                            <th>狀態</th>
                            <th>最後登入</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="emp in employees" :key="emp.empId">
                            <td>{{ emp.empId }}</td>
                            <td>{{ emp.username }}</td>
                            <td>{{ emp.empName }}</td>
                            <td>{{ emp.empPhone || '-' }}</td>
                            <td>
                                <span v-for="role in emp.roles" :key="role" class="badge bg-primary me-1">
                                    {{ role }}
                                </span>
                            </td>
                            <td>
                                <span class="badge"
                                    :class="emp.status === 'active' ? 'bg-success' : emp.status === 'disable' ? 'bg-warning text-dark' : 'bg-danger'">
                                    {{ emp.status }}
                                </span>
                            </td>
                            <td>
                                {{ emp.lastLoginAt || '尚未登入' }}
                            </td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary me-2"
                                    @click="openEditModal(emp)">詳情</button>
                                <template v-if="isSuperUser">
                                    <label class="switch">
                                        <input type="checkbox" :checked="emp.status === 'active'"
                                            @change="toggleEmployeeStatus(emp)">
                                        <span class="slider"></span>
                                    </label>
                                    <span class="ms-2 small">
                                        {{ emp.status === 'active' ? '啟用' : '停權' }}
                                    </span>
                                </template>
                                <span v-else class="text-muted small">
                                    僅可查看
                                </span>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div class="d-flex justify-content-center align-items-center gap-3 mt-4">
                    <button :disabled="currentPage === 0" @click="loadEmployees(currentPage - 1)"
                        class="btn btn-outline-secondary">上一頁</button>
                    <span>第{{ currentPage + 1 }}/{{ totalPages }}</span>
                    <button :disabled="currentPage + 1 >= totalPages" @click="loadEmployees(currentPage + 1)"
                        class="btn btn-outline-secondary">下一頁</button>
                </div>
            </div>
        </div>
        <div v-else-if="mode === 'role'">
            <!--角色清單/新增角色/修改角色 table-->
            <div class="card shadow-sm p-3">
                <h4 class="mb-3">角色管理</h4>
                <div v-if="isSuperUser" class="card p-3 mb-4 bg-light">
                    <h5>{{ isRoleEditing ? '修改角色' : '新增角色' }}</h5>
                    <div class="mb-3">
                        <label class="form-label">角色名稱</label>
                        <input v-model="roleForm.roleName" type="text" class="form-control"
                            placeholder="例如：superuser、groomer、staff" required />
                    </div>
                    <div class="mb-3">
                        <label class="form-label">角色描述</label>
                        <input v-model="roleForm.description" type="text" class="form-control" placeholder="請輸入角色說明" />
                    </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-orange-action" @click="submitRole">{{ isRoleEditing ? '儲存修改' : '新增角色'
                        }}</button>
                        <button v-if="isRoleEditing" class="btn btn-secondary" @click="resetRoleForm">取消修改</button>
                    </div>
                </div>
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>角色ID</th>
                            <th>角色名稱</th>
                            <th>描述</th>
                            <th v-if="isSuperUser">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-if="roles.length === 0">
                            <td :colspan="isSuperUser ? 4 : 3" class="text-center text-muted">
                                目前沒有角色資料
                            </td>
                        </tr>
                        <tr v-for="role in roles" :key="role.roleId">
                            <td>{{ role.roleId }}</td>
                            <td>
                                <span class="badge bg-primary">
                                    {{ role.roleName }}
                                </span>
                            </td>
                            <td>{{ role.description || '-' }}</td>
                            <td v-if="isSuperUser">
                                <button class="btn btn-sm btn-outline-primary me-2" @click="editRole(role)">修改</button>
                                <button class="btn btn-sm btn-outline-danger" @click="deleteRole(role)">刪除</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div v-else-if="mode === 'employeeRole'">
            <!--選員工+勾選角色 + 儲存鎖定 table-->
            <div class="row g-4">
                <!-- 左邊員工 -->
                <div class="col-md-5">
                    <div class="card shadow-sm p-3 h-100">
                        <h4 class="mb-3">員工列表</h4>
                        <div v-for="emp in employees" :key="emp.empId" class="border rounded p-3 mb-2 employee-card"
                            :class="{
                                'border-warning bg-warning-subtle':
                                    selectedEmployee?.empId === emp.empId
                            }" @click="selectEmployeeRoles(emp)">
                            <div class="fw-bold">{{ emp.empName }}</div>
                            <div class="small text-muted">{{ emp.username }}</div>
                            <div class="mt-2">
                                <span v-for="role in emp.roles" :key="role" class="badge bg-primary me-1">{{ role
                                    }}</span>
                            </div>
                        </div>
                    </div>
                </div>
                <!-- 右邊角色 -->
                <div class="col-md-7">
                    <div class="card shadow-sm p-3 h-100">
                        <h4 class="mb-3">員工角色綁定</h4>
                        <template v-if="selectedEmployee">
                            <div class="mb-3">
                                <div class="fw-bold fs-5">
                                    {{ selectedEmployee.empName }}
                                </div>
                                <div class="text-muted">
                                    {{ selectedEmployee.username }}
                                </div>
                            </div>
                            <div class="row">
                                <div v-for="role in roles" :key="role.roleId" class="col-md-6 mb-3">
                                    <div class="form-check border rounded p-3">
                                        <input class="form-check-input" type="checkbox" :value="role.roleId"
                                            v-model="selectedRoleIds" :disabled="!isSuperUser" />
                                        <label class="form-check-label ms-2">
                                            <div class="fw-bold">
                                                {{ role.roleName }}
                                            </div>
                                            <div class="small text-muted">
                                                {{ role.description }}
                                            </div>
                                        </label>
                                    </div>
                                </div>
                            </div>
                            <button v-if="isSuperUser" class="btn btn-warning mt-3"
                                @click="saveEmployeeRoles">儲存角色綁定</button>
                        </template>
                        <div v-else class="text-center text-muted py-5">請先選擇左側員工</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal fade" id="addEmployeeModal" tabindex="-1">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">新增員工</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <form @submit.prevent="submitAddEmployee">
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label">登入帳號</label>
                                <input v-model="addForm.username" type="text" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">密碼</label>
                                <input v-model="addForm.passwordHash" type="password" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工姓名</label>
                                <input v-model="addForm.empName" type="text" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工電話</label>
                                <input v-model="addForm.empPhone" type="text" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工地址</label>
                                <input v-model="addForm.empAddress" type="text" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">緊急聯絡人</label>
                                <input v-model="addForm.emergencyContact" type="text" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">緊急聯絡電話</label>
                                <input v-model="addForm.emergencyPhone" type="text" class="form-control" />
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" @click="fillRegisterDemo" class="btn btn-orange-action">一鍵輸入</button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="submit" class="btn btn-orange-action">新增員工</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <div class="modal fade" id="editEmployeeModal" tabindex="-1">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">員工詳細資料 / 修改</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <form @submit.prevent="submitUpdateEmployee">
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label">員工編號</label>
                                <input v-model="editForm.empId" type="text" class="form-control" readonly />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">登入帳號</label>
                                <input v-model="editForm.username" type="text" class="form-control" readonly />
                            </div>
                            <div class="mb-3" v-if="isSuperUser">
                                <label class="form-label">修改密碼</label>
                                <input v-model="editForm.newPassword" type="password" class="form-control"
                                    placeholder="不修改可留空" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工姓名</label>
                                <input v-model="editForm.empName" type="text" class="form-control" required
                                    :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工電話</label>
                                <input v-model="editForm.empPhone" type="text" class="form-control"
                                    :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工地址</label>
                                <input v-model="editForm.empAddress" type="text" class="form-control"
                                    :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">緊急聯絡人</label>
                                <input v-model="editForm.emergencyContact" type="text" class="form-control"
                                    :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">緊急聯絡電話</label>
                                <input v-model="editForm.emergencyPhone" type="text" class="form-control"
                                    :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">帳號狀態</label>
                                <select v-model="editForm.status" class="form-select" :disabled="!isSuperUser">
                                    <option value="active">啟用</option>
                                    <option value="disable">停權</option>
                                    <option value="delete">刪除</option>
                                </select>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button v-if="isSuperUser" type="submit" class="btn btn-warning">儲存修改 </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>
.switch {
    position: relative;
    display: inline-block;
    width: 48px;
    height: 26px;
    vertical-align: middle;
}

.switch input {
    display: none;
}

.slider {
    position: absolute;
    cursor: pointer;
    inset: 0;
    background-color: #fc0909;
    transition: 0.3s;
    border-radius: 999px;
}

.slider::before {
    position: absolute;
    content: "";
    height: 20px;
    width: 20px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    transition: 0.3s;
    border-radius: 50%;
}

.switch input:checked+.slider {
    background-color: #198754;
}

.switch input:checked+.slider::before {
    transform: translateX(22px);
}
.employee-card {
    cursor: pointer;
    transition: 0.2s;
}
.employee-card:hover {
    transform: translateY(-2px);
}
.btn-orange-action {
    background-color: #FF9800;
    color: white;
    border: none;
    font-weight: bold;
}
</style>
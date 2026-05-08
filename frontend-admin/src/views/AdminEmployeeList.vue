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

const loadEmployees = async () => {

    try {

        const res = await request.get('/api/admin/employees')

        employees.value = res.data

    } catch (err) {

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
onMounted(() => {
    loadEmployees()
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
                <button v-if="isSuperUser" class="btn btn-success" @click="openAddModal"> + 新增員工</button>
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
            </div>
        </div>
        <div v-else-if="mode === 'role'">
            <!--角色清單/新增角色/修改角色 table-->
        </div>
        <div v-else-if="mode === 'employeeRole'">
            <!--選員工+勾選角色 + 儲存鎖定 table-->
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
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="submit" class="btn btn-success">新增員工</button>
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
                                <input v-model="editForm.empName" type="text" class="form-control" required :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工電話</label>
                                <input v-model="editForm.empPhone" type="text" class="form-control" :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">員工地址</label>
                                <input v-model="editForm.empAddress" type="text" class="form-control" :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">緊急聯絡人</label>
                                <input v-model="editForm.emergencyContact" type="text" class="form-control" :disabled="!isSuperUser" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">緊急聯絡電話</label>
                                <input v-model="editForm.emergencyPhone" type="text" class="form-control" :disabled="!isSuperUser" />
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
</style>
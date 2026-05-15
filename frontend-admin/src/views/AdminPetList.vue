<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request';
import Swal from 'sweetalert2';
import * as bootstrap from 'bootstrap';



const pets = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const searchType = ref('')
const keyword = ref('')

const searchOptions = [
    { label: '寵物名稱', value: 'petName' },
    { label: '會員ID', value: 'memberId' },
    { label: '種類', value: 'species' },
    { label: '狀態', value: 'status' }
]

const loadPets = async (page = 0) => {
    try {
        const params = { page, size: 10 }

        if (searchType.value && keyword.value) {
            params.searchType = searchType.value
            params.keyword = keyword.value
        }

        const res = await request.get('/api/admin/pets', { params })

        pets.value = res.data.content

        currentPage.value = res.data.number

        totalPages.value = res.data.totalPages

    } catch (err) {
        console.log(err);
        Swal.fire('錯誤', '寵物資料載入失敗', 'error')
    }
}

const searchPets = () => {
    loadPets(0)
}

const changeSearchType = () => {
    keyword.value = ''
}

const statusOptions = [
    { label: '啟用', value: 'active' },
    { label: '刪除', value: 'delete' }
]
const addForm = ref({
    memberId: '',
    petName: '',
    species: '',
    breed: '',
    age: '',
    weight: '',
    medicalHistory: ''
})
const addFile = ref(null)
const addPreviewUrl = ref(null)

const fillAddDemo = () => {
    addForm.value = {
        memberId: 201,
        petName: '小白',
        species: '貓',
        breed: '布偶貓',
        age: 3,
        weight: 7.5,
        medicalHistory: '無重大疾病紀錄'
    }
}

const openAddModal = () => {
    addForm.value = {
        memberId: '',
        petName: '',
        species: '',
        breed: '',
        age: '',
        weight: '',
        medicalHistory: ''
    }

    addFile.value = null
    addPreviewUrl.value = null

    new bootstrap.Modal(
        document.getElementById('addPetModal')
    ).show()
}

const handleAddFileChange = (e) => {
    const file = e.target.files[0]
    if (!file) return

    addFile.value = file
    addPreviewUrl.value = URL.createObjectURL(file)
}

const submitAddPet = async () => {
    try {
        const formData = new FormData()

        formData.append('memberId', addForm.value.memberId)
        formData.append('petName', addForm.value.petName)
        formData.append('species', addForm.value.species)
        formData.append('breed', addForm.value.breed)
        formData.append('age', addForm.value.age)
        formData.append('weight', addForm.value.weight)
        formData.append('medicalHistory', addForm.value.medicalHistory)

        if (addFile.value) {
            formData.append('file', addFile.value)
        }

        await request.post('/api/admin/pets', formData)

        bootstrap.Modal.getInstance(
            document.getElementById('addPetModal')
        ).hide()

        Swal.fire('成功', '寵物新增成功', 'success')
        loadPets()

    } catch (err) {
        console.log(err)
        Swal.fire('錯誤', err.response?.data || '新增寵物失敗', 'error')
    }
}

const editForm = ref({
    petId: '',
    memberId: '',
    petName: '',
    species: '',
    breed: '',
    age: '',
    weight: '',
    medicalHistory: '',
    status: '',
    petPhoto: ''
})

const editFile = ref(null)
const editPreviewUrl = ref(null)

const openEditModal = (pet) => {
    editForm.value = { ...pet }

    editFile.value = null
    editPreviewUrl.value = pet.petPhoto
        ? `http://localhost:8082${pet.petPhoto}`
        : null

    new bootstrap.Modal(
        document.getElementById('editPetModal')
    ).show()
}

const handleEditFileChange = (e) => {
    const file = e.target.files[0]
    if (!file) return

    editFile.value = file
    editPreviewUrl.value = URL.createObjectURL(file)
}

const submitUpdatePet = async () => {
    try {
        const formData = new FormData()

        formData.append('memberId', editForm.value.memberId)
        formData.append('petName', editForm.value.petName)
        formData.append('species', editForm.value.species)
        formData.append('breed', editForm.value.breed)
        formData.append('age', editForm.value.age)
        formData.append('weight', editForm.value.weight)
        formData.append('medicalHistory', editForm.value.medicalHistory)
        formData.append('status', editForm.value.status)

        if (editFile.value) {
            formData.append('file', editFile.value)
        }

        await request.put(
            `/api/admin/pets/${editForm.value.petId}`,
            formData
        )

        bootstrap.Modal.getInstance(
            document.getElementById('editPetModal')
        ).hide()

        Swal.fire('成功', '寵物資料已更新', 'success')
        loadPets(currentPage.value)

    } catch (err) {
        console.log(err)
        Swal.fire('錯誤', typeof err.response?.data === 'string' ? err.response.data : '修改寵物失敗', 'error')
    }
}


onMounted(() => {
    loadPets()
})
</script>
<template>
    <div class="p-4">
        <div class="card shadow-sm p-3">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h4 class="mb-0">寵物管理</h4>
                <button class="btn btn-orange-action" @click="openAddModal"> + 新增寵物</button>
            </div>
            <div class="d-flex gap-2 mb-3">
                <select v-model="searchType" class="form-select w-auto" @change="changeSearchType">
                    <option value="">請選擇搜尋類型</option>
                    <option v-for="item in searchOptions" :key="item.value" :value="item.value">
                        {{ item.label }}
                    </option>
                </select>
                <select v-if="searchType === 'status'" v-model="keyword" class="form-select w-auto">
                    <option value="" disabled>請選擇狀態</option>
                    <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}
                    </option>
                </select>
                <input v-else v-model="keyword" type="text" class="form-control w-auto" placeholder="請輸入搜尋內容" />
                <button class="btn btn-orange-action" @click="searchPets">搜尋</button>
            </div>
            <table class="table table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>照片</th>
                        <th>寵物名稱</th>
                        <th>會員</th>
                        <th>種類</th>
                        <th>品種</th>
                        <th>年齡</th>
                        <th>體重</th>
                        <th>狀態</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="pets.length === 0">
                        <td colspan="10" class="text-center text-muted py-4">
                            目前沒有寵物資料
                        </td>
                    </tr>
                    <tr v-for="pet in pets" :key="pet.petId">
                        <td>{{ pet.petId }}</td>
                        <td>
                            <img :src="`http://localhost:8082${pet.petPhoto}`" class="pet-img" />
                        </td>
                        <td>{{ pet.petName }}</td>
                        <td>{{ pet.memberName }} (#{{ pet.memberId }})</td>
                        <td>{{ pet.species }}</td>
                        <td>{{ pet.breed || '-' }}</td>
                        <td>{{ pet.age ?? '-' }}</td>
                        <td>{{ pet.weight ?? '-' }} kg</td>
                        <td>
                            <span class="badge" :class="pet.status === 'active' ? 'bg-success' : 'bg-danger'">{{
                                pet.status }}</span>
                        </td>
                        <td>
                            <button @click="openEditModal(pet)" class="btn btn-sm btn-outline-primary">詳情</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            <div class="d-flex justify-content-center align-items-center gap-3 mt-4">
                <button class="btn btn-outline-secondary" :disabled="currentPage === 0"
                    @click="loadPets(currentPage - 1)">上一頁</button>
                <span>第 {{ currentPage + 1 }} / {{ totalPages }} 頁</span>
                <button class="btn btn-outline-secondary" :disabled="currentPage + 1 >= totalPages"
                    @click="loadPets(currentPage + 1)">下一頁</button>
            </div>
        </div>
        <div class="modal fade" id="addPetModal" tabindex="-1">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">新增寵物</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <form @submit.prevent="submitAddPet">
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label">會員 ID</label>
                                <input v-model="addForm.memberId" type="number" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">寵物照片</label>
                                <input type="file" accept="image/*" class="form-control"
                                    @change="handleAddFileChange" />
                                <div v-if="addPreviewUrl" class="mt-3">
                                    <img :src="addPreviewUrl" class="preview-img" />
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">寵物名稱</label>
                                <input v-model="addForm.petName" type="text" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">種類</label>
                                <select v-model="addForm.species" class="form-select" required>
                                    <option value="" disabled>請選擇種類</option>
                                    <option value="狗">狗</option>
                                    <option value="貓">貓</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">品種</label>
                                <input v-model="addForm.breed" type="text" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">年齡</label>
                                <input v-model="addForm.age" type="number" min="0" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">體重 kg</label>
                                <input v-model="addForm.weight" type="number" min="0" step="0.1" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">醫療紀錄</label>
                                <textarea v-model="addForm.medicalHistory" class="form-control" rows="4"></textarea>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" @click="fillAddDemo" class="btn btn-orange-action" >一鍵輸入</button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="submit" class="btn btn-orange-action"> 新增寵物</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <div class="modal fade" id="editPetModal" tabindex="-1">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">修改寵物資料</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <form @submit.prevent="submitUpdatePet">
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label">會員 ID</label>
                                <input v-model="editForm.memberId" type="number" class="form-control" readonly />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">寵物照片</label>
                                <input type="file" accept="image/*" class="form-control"
                                    @change="handleEditFileChange" />
                                <div v-if="editPreviewUrl" class="mt-3">
                                    <img :src="editPreviewUrl" class="preview-img" />
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">寵物名稱</label>
                                <input v-model="editForm.petName" type="text" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">種類</label>
                                <select v-model="editForm.species" class="form-select" required>
                                    <option value="狗">狗</option>
                                    <option value="貓">貓</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">品種</label>
                                <input v-model="editForm.breed" type="text" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">年齡</label>
                                <input v-model="editForm.age" type="number" min="0" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">體重 kg</label>
                                <input v-model="editForm.weight" type="number" min="0" step="0.1"
                                    class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">醫療紀錄</label>
                                <textarea v-model="editForm.medicalHistory" rows="4" class="form-control"></textarea>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">狀態</label>
                                <select v-model="editForm.status" class="form-select">
                                    <option value="active">啟用</option>
                                    <option value="delete">刪除</option>
                                </select>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="submit" class="btn btn-orange-action">儲存修改</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>
.pet-img {
    width: 70px;
    height: 70px;
    object-fit: cover;
    border-radius: 10px;
    border: 1px solid #ddd;
}

.preview-img {
    width: 180px;
    height: 180px;
    object-fit: cover;
    border-radius: 12px;
    border: 1px solid #ddd;
}
.btn-orange-action {
    background-color: #FF9800;
    color: white;
    border: none;
    font-weight: bold;
}
</style>
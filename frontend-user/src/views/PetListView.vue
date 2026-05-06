<script setup>
import { ref, onMounted ,reactive } from 'vue';
import axios from 'axios';
import Swal from 'sweetalert2';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import * as bootstrap from 'bootstrap';


const router = useRouter();

const userStore = useUserStore()

const pets = ref([])

const editPetForm = reactive({
    petId: '',
    memberId: '',
    petName: '',
    species: '',
    breed: '',
    age: '',
    weight: '',
    medicalHistory: '',
    petPhoto: ''
})

const editFile = ref(null)
const editPreviewUrl = ref(null)

const loadPets = async () => {
    try {
        const res = await axios.get(`/api/pets/member/${userStore.memberId}`)

        pets.value = res.data
    } catch (err) {

        console.log(err)

        Swal.fire({
            icon: 'error',
            title: '載入失敗',
            text: '無法取得寵物資料'
        })
    }
}
const openEditModal = (pet) => {
    editPetForm.petId = pet.petId
    editPetForm.memberId = pet.memberId
    editPetForm.petName = pet.petName
    editPetForm.species = pet.species
    editPetForm.breed = pet.breed
    editPetForm.age = pet.age
    editPetForm.weight = pet.weight
    editPetForm.medicalHistory = pet.medicalHistory
    editPetForm.petPhoto = pet.petPhoto

    editFile.value = null
    editPreviewUrl.value = pet.petPhoto
        ? `http://localhost:8082${pet.petPhoto}`
        : null

    const modalEl = document.getElementById('editPetModal')
    const modal = new bootstrap.Modal(modalEl)
    modal.show()
}
const handleEditFileChange = (e) => {
    const file = e.target.files[0]

    if (!file) return

    editFile.value = file
    editPreviewUrl.value = URL.createObjectURL(file)
}
const updatePet = async () => {
    try {
        const formData = new FormData()

        formData.append('memberId', editPetForm.memberId)
        formData.append('petName', editPetForm.petName)
        formData.append('species', editPetForm.species)
        formData.append('breed', editPetForm.breed)
        formData.append('age', editPetForm.age)
        formData.append('weight', editPetForm.weight)
        formData.append('medicalHistory', editPetForm.medicalHistory)

        if (editFile.value) {
            formData.append('file', editFile.value)
        }

        await axios.put(`/api/pets/${editPetForm.petId}`, formData)

        const modalEl = document.getElementById('editPetModal')
        const modal = bootstrap.Modal.getInstance(modalEl)
        modal.hide()

        Swal.fire({
            icon: 'success',
            title: '修改成功',
            text: '寵物資料已更新'
        })

        loadPets()
    } catch (err) {
        console.log(err)

        Swal.fire({
            icon: 'error',
            title: '修改失敗',
            text: '請稍後再試'
        })
    }
}
const deletePet = async (petId) => {

    const result = await Swal.fire({
        title: '確定刪除寵物？',
        text: '刪除後將無法恢復',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消'
    })

    if (!result.isConfirmed) return

    try {
        await axios.delete(`/api/pets/${petId}`)

        Swal.fire({
            icon: 'success',
            title: '刪除成功',
            text: '寵物已成功刪除'
        })
        loadPets()
    } catch (err) {
        console.log(err)

        Swal.fire({
            icon: 'error',
            title: '刪除失敗，請稍後再試'
        })
    }


}
onMounted(() => {
    loadPets();
})

</script>
<template>
    <div class="container py-4">
        <!-- 標題 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>我的寵物</h2>
            <button @click="router.push('/member/center/pets/add')" class="btn btn-primary">新增寵物</button>
        </div>
        <!-- 沒有寵物 -->
        <div v-if="pets.length === 0" class="text-center py-5">
            <h3 class="mb-3">目前沒有寵物喔！🐾</h3>
            <p class="text-muted mb-4">快去新增你的毛孩吧！</p>
        </div>
        <!-- 有寵物 -->
        <div v-else class="row">
            <div v-for="pet in pets" :key="pet.petId" class="col-md-4 mb-4">
                <div class="card shadow-sm h-100">
                    <img :src="`http://localhost:8082${pet.petPhoto}`" class="card-img-top"
                        style="height: 250px; object-fit: cover;">
                    <div class="card-body">
                        <h4>{{ pet.petName }}</h4>
                        <p>種類：{{ pet.species }}</p>
                        <p>品種：{{ pet.breed }}</p>
                        <p>年齡：{{ pet.age }} 歲</p>
                        <p>體重：{{ pet.weight }} kg</p>
                        <div class="d-flex gap-2 mt-3">
                            <button @click="openEditModal(pet)" class="btn btn-warning flex-fill">修改</button>
                            <button class="btn btn-danger flex-fill" @click="deletePet(pet.petId)">刪除</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- 修改寵物 Modal -->
        <div class="modal fade" id="editPetModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">修改寵物資料</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <form @submit.prevent="updatePet">
                        <div class="modal-body">
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
                                <input type="text" v-model="editPetForm.petName" class="form-control" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">種類</label>
                                <select v-model="editPetForm.species" class="form-select" required>
                                    <option value="" disabled>請選擇種類</option>
                                    <option value="狗">狗</option>
                                    <option value="貓">貓</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">品種</label>
                                <input type="text" v-model="editPetForm.breed" class="form-control" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">年齡</label>
                                <input type="number" v-model="editPetForm.age" class="form-control" min="0" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">體重 kg</label>
                                <input type="number" v-model="editPetForm.weight" class="form-control" step="0.1"
                                    min="0" />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">醫療紀錄</label>
                                <textarea v-model="editPetForm.medicalHistory" class="form-control" rows="4"></textarea>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="submit" class="btn btn-primary"> 儲存修改</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>
.card {
    border-radius: 15px;
    overflow: hidden;
}

.card img {
    border-bottom: 1px solid #eee;
}

.card-body h4 {
    font-weight: bold;
    margin-bottom: 15px;
}
.preview-img {
  width: 180px;
  height: 180px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #ddd;
}
</style>
<script setup>
import { reactive, ref } from 'vue'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const petForm = reactive({
    petName: '',
    species: '',
    breed: '',
    age: '',
    weight: '',
    medicalHistory: ''
})

const fillRegisterDemo = () =>{
    petForm.petName = '小黑',
    petForm.species = '狗',
    petForm.breed = '米克斯',
    petForm.age = '3',
    petForm.weight = '6.5',
    petForm.medicalHistory = '已施打疫苗，無重大疾病紀錄'
}

const selectedFile = ref(null)
const previewUrl = ref(null)

const handleFileChange = (e) => {
    const file = e.target.files[0]

    if (!file) return

    selectedFile.value = file
    previewUrl.value = URL.createObjectURL(file)
}

const addPet = async () => {
    try {
        const formData = new FormData()

        formData.append('memberId', userStore.memberId)
        formData.append('petName', petForm.petName)
        formData.append('species', petForm.species)
        formData.append('breed', petForm.breed)
        formData.append('age', petForm.age)
        formData.append('weight', petForm.weight)
        formData.append('medicalHistory', petForm.medicalHistory)

        if (selectedFile.value) {
            formData.append('file', selectedFile.value)
        }

        await axios.post('/api/pets', formData)

        Swal.fire({
            icon: 'success',
            title: '新增成功',
            text: '寵物資料已新增'
        }).then(() => {
            router.push('/member/center/pets')
        })

    } catch (err) {
        console.log(err)

        Swal.fire({
            icon: 'error',
            title: '新增失敗',
            text: '請稍後再試'
        })
    }
}
</script>

<template>
    <div class="container py-4">
        <h2 class="mb-4">新增寵物</h2>

        <form @submit.prevent="addPet" class="pet-form">

            <div class="mb-3">
                <label class="form-label">寵物照片</label>
                <input type="file" accept="image/*" class="form-control" @change="handleFileChange" />
                <div v-if="previewUrl" class="mt-3">
                    <img :src="previewUrl" class="preview-img" />
                </div>
            </div>
            <div class="mb-3">
                <label class="form-label">寵物名稱</label>
                <input type="text" v-model="petForm.petName" class="form-control" required />
            </div>
            <div class="mb-3">
                <label class="form-label">種類</label>
                <select v-model="petForm.species" class="form-select" required>
                    <option value="" disabled>請選擇種類</option>
                    <option value="狗">狗</option>
                    <option value="貓">貓</option>
                </select>
            </div>
            <div class="mb-3">
                <label class="form-label">品種</label>
                <input type="text" v-model="petForm.breed" class="form-control" />
            </div>

            <div class="mb-3">
                <label class="form-label">年齡</label>
                <input type="number" v-model="petForm.age" class="form-control" min="0" />
            </div>

            <div class="mb-3">
                <label class="form-label">體重 kg</label>
                <input type="number" v-model="petForm.weight" class="form-control" step="0.1" min="0" />
            </div>

            <div class="mb-3">
                <label class="form-label">醫療紀錄</label>
                <textarea v-model="petForm.medicalHistory" class="form-control" rows="4"></textarea>
            </div>

            <div class="d-flex gap-2">
                <button type="button" @click="fillRegisterDemo" class="btn btn-secondary">一鍵輸入</button>
                <button type="submit" class="btn btn-primary">新增寵物</button>
                <button type="button" class="btn btn-secondary" @click="router.push('/member/center/pets')">
                    返回
                </button>
            </div>

        </form>
    </div>
</template>

<style scoped>
.pet-form {
    max-width: 600px;
}

.preview-img {
    width: 180px;
    height: 180px;
    object-fit: cover;
    border-radius: 12px;
    border: 1px solid #ddd;
}
</style>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import '@/assets/css/BeautyAdmin.css'

const groomers = ref([])
const employees = ref([])
const beautyItems = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const bookableFilter = ref('')

const form = reactive({
  groomerId: null,
  displayName: '',
  intro: '',
  seniorityYears: 0,
  isBookable: true,
  beautyIds: [],
})

const filteredGroomers = computed(() => {
  return groomers.value.filter(groomer => {
    const name = groomer.displayName || String(groomer.groomerId)
    const matchKeyword = !keyword.value || name.includes(keyword.value)
    const matchBookable =
      bookableFilter.value === '' || String(groomer.isBookable) === bookableFilter.value
    return matchKeyword && matchBookable
  })
})

const loadGroomers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin/beauty/groomers')
    groomers.value = res.data || []
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '美容師資料載入失敗', 'error')
  } finally {
    loading.value = false
  }
}

const loadEmployees = async () => {
  try {
    const res = await request.get('/api/admin/employees', { params: { page: 0, size: 100 } })
    employees.value = res.data.content || []
  } catch (err) {
    console.log(err)
  }
}

const loadBeautyItems = async () => {
  try {
    const res = await request.get('/api/admin/beauty/items')
    beautyItems.value = (res.data || []).filter(item => item.isActive)
  } catch (err) {
    console.log(err)
  }
}

const resetForm = () => {
  form.groomerId = null
  form.displayName = ''
  form.intro = ''
  form.seniorityYears = 0
  form.isBookable = true
  form.beautyIds = []
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async groomer => {
  resetForm()
  form.groomerId = groomer.groomerId
  form.displayName = groomer.displayName
  form.intro = groomer.intro
  form.seniorityYears = groomer.seniorityYears || 0
  form.isBookable = groomer.isBookable

  try {
    const res = await request.get(`/api/admin/beauty/groomers/${groomer.groomerId}/services`)
    form.beautyIds = (res.data || []).filter(row => row.isActive).map(row => row.beautyId)
  } catch (err) {
    console.log(err)
  }

  dialogVisible.value = true
}

const saveGroomer = async () => {
  try {
    await request.put('/api/admin/beauty/groomers', form)
    Swal.fire('成功', '美容師資料已儲存', 'success')
    dialogVisible.value = false
    loadGroomers()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '儲存失敗', 'error')
  }
}

const serviceNames = async groomer => {
  try {
    const res = await request.get(`/api/admin/beauty/groomers/${groomer.groomerId}/services`)
    const ids = (res.data || []).map(row => row.beautyId)
    const itemMap = new Map(beautyItems.value.map(item => [item.beautyId, item.itemName]))
    Swal.fire({
      title: groomer.displayName || `美容師 ${groomer.groomerId}`,
      html: ids.map(id => itemMap.get(id) || `項目 ${id}`).join('<br>') || '尚未設定服務項目',
      icon: 'info',
      iconHtml: '<span class="material-symbols-outlined">pets</span>',
      customClass: {
        icon: 'swal-google-icon',
      },
    })
  } catch (err) {
    console.log(err)
  }
}

onMounted(() => {
  loadGroomers()
  loadEmployees()
  loadBeautyItems()
})
</script>

<template>
  <div class="beauty-admin-page">
    <div class="beauty-card">
      <div class="beauty-toolbar">
        <div>
          <h3 class="beauty-title">美容師管理</h3>
          <div class="beauty-subtitle">由既有員工建立美容師檔案，並設定可服務項目</div>
        </div>
        <div class="beauty-filter">
          <el-input v-model="keyword" clearable placeholder="搜尋美容師" style="width: 220px" />
          <el-select v-model="bookableFilter" placeholder="可預約" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="可預約" value="true" />
            <el-option label="不可預約" value="false" />
          </el-select>
          <el-button type="primary" @click="openAddDialog">新增美容師</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="filteredGroomers" stripe>
        <el-table-column prop="groomerId" label="美容師ID" width="90" />
        <el-table-column prop="displayName" label="顯示名稱" width="180" />
        <el-table-column prop="seniorityYears" label="年資" width="80" align="center" header-align="center" />
        <el-table-column prop="intro" label="簡介" min-width="240" show-overflow-tooltip />
        <el-table-column label="可預約" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isBookable ? 'success' : 'info'">
              {{ row.isBookable ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <div class="beauty-actions">
              <el-button size="small" @click="serviceNames(row)">服務項目</el-button>
              <el-button size="small" type="primary" @click="openEditDialog(row)">編輯</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="美容師資料" width="720px">
      <el-form label-position="top">
        <div class="beauty-form-grid">
          <el-form-item label="選擇既有員工">
            <el-select v-model="form.groomerId" filterable placeholder="選擇員工" style="width: 100%">
              <el-option
                v-for="emp in employees"
                :key="emp.empId"
                :label="`${emp.empId} - ${emp.empName || emp.username}`"
                :value="emp.empId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="顯示名稱">
            <el-input v-model="form.displayName" />
          </el-form-item>
          <el-form-item label="年資">
            <el-input-number v-model="form.seniorityYears" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="是否可預約">
            <el-switch v-model="form.isBookable" active-text="可預約" inactive-text="不可預約" />
          </el-form-item>
          <el-form-item class="full" label="簡介">
            <el-input v-model="form.intro" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item class="full" label="可服務美容項目">
            <el-select v-model="form.beautyIds" multiple filterable placeholder="選擇服務項目" style="width: 100%">
              <el-option v-for="item in beautyItems" :key="item.beautyId" :label="item.itemName" :value="item.beautyId" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroomer">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

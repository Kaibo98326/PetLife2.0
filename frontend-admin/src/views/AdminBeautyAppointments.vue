<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import { useEmployeeStore } from '@/stores/employee'
import '@/assets/css/BeautyAdmin.css'

const employeeStore = useEmployeeStore()
const appointments = ref([])
const groomers = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const statusVisible = ref(false)
const selectedAppointment = ref(null)
const pageSize = 10
const currentPage = ref(1)

const filters = ref({
  dateRange: [],
  status: '',
  groomerId: '',
})

const statusForm = ref({
  status: '',
  cancelReason: '',
})

const totalPages = computed(() => Math.max(1, Math.ceil(appointments.value.length / pageSize)))

const pagedAppointments = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return appointments.value.slice(start, start + pageSize)
})

const clampCurrentPage = () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value
  }
}

const appointmentStatuses = ['待確認', '已確認', '已完成', '已取消', '未到']
const lockedStatuses = ['已完成', '已取消', '未到']

const statusType = status => {
  const map = {
    待確認: 'warning',
    已確認: 'primary',
    已完成: 'success',
    已取消: 'danger',
    未到: 'info',
  }
  return map[status] || 'info'
}

const canEditStatus = row => !lockedStatuses.includes(row.appointmentStatus)

const canWriteBeauty = computed(() => {
  const roles = employeeStore.roles || []
  return !roles.includes('groomer') || roles.includes('superuser')
})

const loadGroomers = async () => {
  try {
    const res = await request.get('/api/admin/beauty/groomers')
    groomers.value = res.data || []
  } catch (err) {
    console.log(err)
  }
}

const loadAppointments = async (resetPage = false) => {
  if (resetPage) {
    currentPage.value = 1
  }
  loading.value = true
  try {
    const params = {}
    if (filters.value.dateRange?.length === 2) {
      params.startDate = filters.value.dateRange[0]
      params.endDate = filters.value.dateRange[1]
    }
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.groomerId) params.groomerId = filters.value.groomerId

    const res = await request.get('/api/admin/beauty/appointments', { params })
    appointments.value = res.data || []
    clampCurrentPage()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '預約資料載入失敗', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.value = { dateRange: [], status: '', groomerId: '' }
  loadAppointments(true)
}

const openDetail = async row => {
  try {
    const res = await request.get(`/api/admin/beauty/appointments/${row.appointmentId}`)
    selectedAppointment.value = res.data
    detailVisible.value = true
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '預約明細載入失敗', 'error')
  }
}

const openStatusDialog = row => {
  selectedAppointment.value = row
  statusForm.value = {
    status: row.appointmentStatus,
    cancelReason: row.cancelReason || '',
  }
  statusVisible.value = true
}

const updateStatus = async () => {
  try {
    await request.put(`/api/admin/beauty/appointments/${selectedAppointment.value.appointmentId}/status`, statusForm.value)
    Swal.fire('成功', '預約狀態已更新', 'success')
    statusVisible.value = false
    loadAppointments()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '狀態更新失敗', 'error')
  }
}

const totalAmountText = row => `$${Number(row.totalAmount || 0).toLocaleString()}`

const selectedDetails = computed(() => selectedAppointment.value?.details || [])

onMounted(() => {
  loadGroomers()
  loadAppointments()
})
</script>

<template>
  <div class="beauty-admin-page">
    <div class="beauty-card">
      <div class="beauty-toolbar">
        <div>
          <h3 class="beauty-title">美容預約管理</h3>
          <div class="beauty-subtitle">查詢預約單、查看明細並控管預約狀態</div>
        </div>
        <div class="beauty-filter">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
          />
          <el-select v-model="filters.status" clearable placeholder="狀態" style="width: 140px">
            <el-option v-for="status in appointmentStatuses" :key="status" :label="status" :value="status" />
          </el-select>
          <el-select v-model="filters.groomerId" clearable placeholder="美容師" style="width: 170px">
            <el-option v-for="groomer in groomers" :key="groomer.groomerId" :label="groomer.displayName || groomer.groomerId" :value="groomer.groomerId" />
          </el-select>
          <el-button type="primary" @click="loadAppointments(true)">查詢</el-button>
          <el-button @click="resetFilters">清除</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="pagedAppointments" stripe>
        <el-table-column prop="appointmentId" label="單號" width="80" />
        <el-table-column prop="petName" label="寵物" min-width="110" />
        <el-table-column prop="groomerName" label="美容師" min-width="120" />
        <el-table-column prop="appointDate" label="日期" width="120" />
        <el-table-column prop="startSlotName" label="起始時段" width="120" />
        <el-table-column prop="totalSlots" label="時段" width="80" align="center" />
        <el-table-column label="金額" width="110" align="right">
          <template #default="{ row }">{{ totalAmountText(row) }}</template>
        </el-table-column>
        <el-table-column label="狀態" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.appointmentStatus)">{{ row.appointmentStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <div class="beauty-actions">
              <el-button size="small" @click="openDetail(row)">明細</el-button>
              <el-button v-if="canWriteBeauty" size="small" type="primary" :disabled="!canEditStatus(row)" @click="openStatusDialog(row)">
                狀態
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="appointments.length > pageSize" class="beauty-pagination">
        <span>共 {{ appointments.length }} 筆，每頁 {{ pageSize }} 筆</span>
        <el-pagination
          v-model:current-page="currentPage"
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="appointments.length"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="預約明細" size="520px">
      <template v-if="selectedAppointment">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="預約單">{{ selectedAppointment.appointmentId }}</el-descriptions-item>
          <el-descriptions-item label="寵物">{{ selectedAppointment.petName }}</el-descriptions-item>
          <el-descriptions-item label="美容師">{{ selectedAppointment.groomerName }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ selectedAppointment.appointDate }}</el-descriptions-item>
          <el-descriptions-item label="狀態">{{ selectedAppointment.appointmentStatus }}</el-descriptions-item>
          <el-descriptions-item label="總金額">{{ totalAmountText(selectedAppointment) }}</el-descriptions-item>
          <el-descriptions-item label="取消原因">{{ selectedAppointment.cancelReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h5 class="mt-4 mb-2">服務項目</h5>
        <div class="detail-lines">
          <div v-for="line in selectedDetails" :key="line.detailId" class="detail-line">
            <span>#{{ line.lineNo }}</span>
            <strong>{{ line.itemNameSnapshot }}</strong>
            <span>{{ line.durationSlotsSnapshot }} 時段</span>
            <span>${{ Number(line.itemPriceSnapshot || 0).toLocaleString() }}</span>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="statusVisible" title="更新預約狀態" width="460px">
      <el-form label-position="top">
        <el-form-item label="預約狀態">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option v-for="status in appointmentStatuses" :key="status" :label="status" :value="status" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="statusForm.status === '已取消'" label="取消原因">
          <el-input v-model="statusForm.cancelReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" @click="updateStatus">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

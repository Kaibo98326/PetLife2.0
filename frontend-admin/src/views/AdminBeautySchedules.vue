<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import '@/assets/css/BeautyAdmin.css'

const groomers = ref([])
const selectedGroomerId = ref(null)
const calendarDate = ref(new Date())
const selectedDate = ref(formatDate(new Date()))
const monthlyData = ref([])
const daySlots = ref([])
const scheduleStatus = ref('未排班')
const loading = ref(false)
const dayLoading = ref(false)
const scheduleDialogVisible = ref(false)
const blockDialogVisible = ref(false)
const selectedRows = ref([])
const scheduleForm = ref({ scheduleStatus: '上班', note: '' })
const blockNote = ref('')

function formatDate(date) {
  const value = date instanceof Date ? date : new Date(date)
  const y = value.getFullYear()
  const m = String(value.getMonth() + 1).padStart(2, '0')
  const d = String(value.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatYearMonth(date) {
  const value = date instanceof Date ? date : new Date(date)
  const y = value.getFullYear()
  const m = String(value.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

const dayMap = computed(() => {
  return new Map(monthlyData.value.map(day => [day.workDate, day]))
})

const selectedDay = computed(() => dayMap.value.get(selectedDate.value))

const tagType = status => {
  if (status === '上班') return 'success'
  if (status === '休假') return 'danger'
  if (status === '預約占用') return 'primary'
  if (status === '手動封鎖') return 'warning'
  if (status === '可預約') return 'success'
  return 'info'
}

const loadGroomers = async () => {
  try {
    const res = await request.get('/api/admin/beauty/groomers')
    groomers.value = res.data || []
    if (!selectedGroomerId.value && groomers.value.length > 0) {
      selectedGroomerId.value = groomers.value[0].groomerId
    }
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '美容師資料載入失敗', 'error')
  }
}

const loadMonth = async () => {
  if (!selectedGroomerId.value) return
  loading.value = true
  try {
    const res = await request.get('/api/admin/beauty/schedules/month', {
      params: {
        groomerId: selectedGroomerId.value,
        yearMonth: formatYearMonth(calendarDate.value),
      },
    })
    monthlyData.value = res.data.days || []
    loadDaySlots()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '月班表載入失敗', 'error')
  } finally {
    loading.value = false
  }
}

const loadDaySlots = async () => {
  if (!selectedGroomerId.value || !selectedDate.value) return
  dayLoading.value = true
  selectedRows.value = []
  try {
    const res = await request.get('/api/admin/beauty/schedules/day-slots', {
      params: {
        groomerId: selectedGroomerId.value,
        workDate: selectedDate.value,
      },
    })
    scheduleStatus.value = res.data.scheduleStatus
    daySlots.value = res.data.slots || []
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '單日時段載入失敗', 'error')
  } finally {
    dayLoading.value = false
  }
}

const selectDate = day => {
  selectedDate.value = day
  loadDaySlots()
}

const openScheduleDialog = () => {
  scheduleForm.value = {
    scheduleStatus: scheduleStatus.value === '休假' ? '休假' : '上班',
    note: '',
  }
  scheduleDialogVisible.value = true
}

const saveSchedule = async () => {
  try {
    await request.put('/api/admin/beauty/schedules', {
      groomerId: selectedGroomerId.value,
      workDate: selectedDate.value,
      scheduleStatus: scheduleForm.value.scheduleStatus,
      note: scheduleForm.value.note,
    })
    Swal.fire('成功', '排班已儲存', 'success')
    scheduleDialogVisible.value = false
    loadMonth()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '排班儲存失敗', 'error')
  }
}

const selectableSlot = row => row.slotStatus === '可預約'

const handleSelectionChange = rows => {
  selectedRows.value = rows
}

const openBlockDialog = () => {
  if (selectedRows.value.length === 0) {
    Swal.fire('提示', '請先選擇可預約時段', 'info')
    return
  }
  blockNote.value = ''
  blockDialogVisible.value = true
}

const saveBlock = async () => {
  try {
    await request.post('/api/admin/beauty/schedules/work-slots/block', {
      groomerId: selectedGroomerId.value,
      workDate: selectedDate.value,
      slotIds: selectedRows.value.map(row => row.slotId),
      note: blockNote.value,
    })
    Swal.fire('成功', '時段已封鎖', 'success')
    blockDialogVisible.value = false
    loadMonth()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '封鎖失敗', 'error')
  }
}

const deleteBlock = async row => {
  const result = await Swal.fire({
    title: '確認解除封鎖？',
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '解除',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return

  try {
    await request.delete(`/api/admin/beauty/schedules/work-slots/block/${row.workSlotId}`)
    Swal.fire('成功', '封鎖已解除', 'success')
    loadMonth()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '解除封鎖失敗', 'error')
  }
}

watch([selectedGroomerId, calendarDate], () => {
  loadMonth()
})

onMounted(loadGroomers)
</script>

<template>
  <div class="beauty-admin-page">
    <div class="beauty-card">
      <div class="beauty-toolbar">
        <div>
          <h3 class="beauty-title">班表管理</h3>
          <div class="beauty-subtitle">以月曆查看排班，並管理單日時段封鎖</div>
        </div>
        <div class="beauty-filter">
          <el-select v-model="selectedGroomerId" filterable placeholder="選擇美容師" style="width: 220px">
            <el-option
              v-for="groomer in groomers"
              :key="groomer.groomerId"
              :label="groomer.displayName || `美容師 ${groomer.groomerId}`"
              :value="groomer.groomerId"
            />
          </el-select>
        </div>
      </div>

      <div class="schedule-shell">
        <div v-loading="loading">
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div
                class="calendar-day"
                :class="{ selected: selectedDate === data.day }"
                @click.stop="selectDate(data.day)"
              >
                <div class="calendar-date">{{ Number(data.day.slice(-2)) }}</div>
                <template v-if="dayMap.get(data.day)">
                  <el-tag size="small" :type="tagType(dayMap.get(data.day).scheduleStatus)">
                    {{ dayMap.get(data.day).scheduleStatus }}
                  </el-tag>
                  <div class="calendar-metrics">
                    <span>預約 {{ dayMap.get(data.day).bookedSlotCount }}</span>
                    <span>封鎖 {{ dayMap.get(data.day).blockedSlotCount }}</span>
                    <span>可約 {{ dayMap.get(data.day).availableSlotCount }}</span>
                  </div>
                </template>
              </div>
            </template>
          </el-calendar>
        </div>

        <div class="beauty-card day-panel">
          <div class="beauty-toolbar">
            <div>
              <h3 class="beauty-title">{{ selectedDate }}</h3>
              <div class="beauty-subtitle">
                排班狀態：
                <el-tag :type="tagType(scheduleStatus)">{{ scheduleStatus }}</el-tag>
              </div>
            </div>
            <div class="beauty-actions">
              <el-button size="small" @click="openScheduleDialog">編輯排班</el-button>
              <el-button size="small" type="warning" @click="openBlockDialog">封鎖時段</el-button>
            </div>
          </div>

          <el-table
            v-loading="dayLoading"
            :data="daySlots"
            size="small"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="44" :selectable="selectableSlot" />
            <el-table-column prop="slotName" label="時段" min-width="110" />
            <el-table-column label="狀態" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType(row.slotStatus)">{{ row.slotStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="appointmentId" label="預約單" width="90" />
            <el-table-column prop="note" label="備註" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.slotStatus === '手動封鎖'"
                  size="small"
                  type="danger"
                  link
                  @click="deleteBlock(row)"
                >
                  解除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="scheduleDialogVisible" title="編輯單日排班" width="420px">
      <el-form label-position="top">
        <el-form-item label="排班狀態">
          <el-radio-group v-model="scheduleForm.scheduleStatus">
            <el-radio-button label="上班" />
            <el-radio-button label="休假" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="備註">
          <el-input v-model="scheduleForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">儲存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="blockDialogVisible" title="封鎖時段" width="420px">
      <p>已選擇 {{ selectedRows.length }} 個時段。</p>
      <el-input v-model="blockNote" type="textarea" :rows="3" placeholder="封鎖原因或備註" />
      <template #footer>
        <el-button @click="blockDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="saveBlock">封鎖</el-button>
      </template>
    </el-dialog>
  </div>
</template>

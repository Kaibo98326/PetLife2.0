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
const scheduleNote = ref('')
const loading = ref(false)
const dayLoading = ref(false)
const scheduleDialogVisible = ref(false)
const blockDialogVisible = ref(false)
const scheduleSlotIds = ref([])
const blockSelectedSlotIds = ref([])
const editingBlockId = ref(null)
const blockEditNote = ref('')
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

const currentGroomerName = computed(() => {
  const groomer = groomers.value.find(item => item.groomerId === selectedGroomerId.value)
  return groomer?.displayName || (selectedGroomerId.value ? `美容師 ${selectedGroomerId.value}` : '-')
})

const availableSlots = computed(() => daySlots.value.filter(slot => slot.slotStatus === '可預約'))
const blockedSlots = computed(() => daySlots.value.filter(slot => slot.slotStatus === '手動封鎖'))
const scheduleSlotOptions = computed(() => daySlots.value.filter(slot => slot.slotBookable))

const tagType = status => {
  if (status === '上班') return 'success'
  if (status === '休假') return 'danger'
  if (status === '預約占用') return 'primary'
  if (status === '手動封鎖') return 'warning'
  if (status === '排班關閉') return 'info'
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
    await loadDaySlots()
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
  try {
    const res = await request.get('/api/admin/beauty/schedules/day-slots', {
      params: {
        groomerId: selectedGroomerId.value,
        workDate: selectedDate.value,
      },
    })
    scheduleStatus.value = res.data.scheduleStatus
    scheduleNote.value = res.data.scheduleNote || ''
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
  scheduleSlotIds.value = scheduleSlotOptions.value
    .filter(slot => slot.slotStatus !== '排班關閉' && slot.slotStatus !== '不可預約')
    .map(slot => slot.slotId)
  scheduleForm.value = {
    scheduleStatus: scheduleStatus.value === '休假' ? '休假' : '上班',
    note: scheduleNote.value,
  }
  scheduleDialogVisible.value = true
}

const scheduleSlotDisabled = slot => {
  return scheduleForm.value.scheduleStatus === '休假'
    || slot.slotStatus === '預約占用'
    || slot.slotStatus === '手動封鎖'
}

const saveSchedule = async () => {
  try {
    await request.put('/api/admin/beauty/schedules/day-slots', {
      groomerId: selectedGroomerId.value,
      workDate: selectedDate.value,
      scheduleStatus: scheduleForm.value.scheduleStatus,
      note: scheduleForm.value.note,
      bookableSlotIds: scheduleForm.value.scheduleStatus === '休假' ? [] : scheduleSlotIds.value,
    })
    Swal.fire('成功', '排班已儲存', 'success')
    scheduleDialogVisible.value = false
    await loadMonth()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '排班儲存失敗', 'error')
  }
}

const openBlockDialog = () => {
  blockSelectedSlotIds.value = []
  blockNote.value = ''
  editingBlockId.value = null
  blockEditNote.value = ''
  blockDialogVisible.value = true
}

const saveBlock = async () => {
  if (blockSelectedSlotIds.value.length === 0) {
    Swal.fire('提示', '請選擇要封鎖的可預約時段', 'info')
    return
  }

  try {
    await request.post('/api/admin/beauty/schedules/work-slots/block', {
      groomerId: selectedGroomerId.value,
      workDate: selectedDate.value,
      slotIds: blockSelectedSlotIds.value,
      note: blockNote.value,
    })
    Swal.fire('成功', '時段已封鎖', 'success')
    blockSelectedSlotIds.value = []
    blockNote.value = ''
    await loadMonth()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '封鎖失敗', 'error')
  }
}

const startEditBlock = row => {
  editingBlockId.value = row.workSlotId
  blockEditNote.value = row.note || ''
}

const cancelEditBlock = () => {
  editingBlockId.value = null
  blockEditNote.value = ''
}

const saveBlockNote = async row => {
  try {
    await request.put(`/api/admin/beauty/schedules/work-slots/block/${row.workSlotId}`, {
      note: blockEditNote.value,
    })
    Swal.fire('成功', '封鎖備註已更新', 'success')
    cancelEditBlock()
    await loadMonth()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '封鎖備註更新失敗', 'error')
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
    cancelEditBlock()
    await loadMonth()
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
                <div v-if="dayMap.get(data.day)" class="calendar-summary">
                  <div class="calendar-status">
                    {{ dayMap.get(data.day).scheduleStatus }}
                  </div>
                  <div class="calendar-metrics">
                    <span>預約 {{ dayMap.get(data.day).bookedSlotCount }}</span>
                    <span>封鎖 {{ dayMap.get(data.day).blockedSlotCount }}</span>
                    <span>關閉 {{ dayMap.get(data.day).scheduleClosedSlotCount }}</span>
                    <span>可約 {{ dayMap.get(data.day).availableSlotCount }}</span>
                  </div>
                </div>
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

          <el-table v-loading="dayLoading" :data="daySlots" size="small">
            <el-table-column prop="slotName" label="時段" min-width="110" />
            <el-table-column label="狀態" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType(row.slotStatus)">{{ row.slotStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="appointmentId" label="預約單" width="90" />
            <el-table-column prop="note" label="備註" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="scheduleDialogVisible" title="編輯單日排班" width="520px">
      <el-form label-position="top">
        <el-form-item label="日期">
          <el-input :model-value="selectedDate" disabled />
        </el-form-item>
        <el-form-item label="美容師">
          <el-input :model-value="currentGroomerName" disabled />
        </el-form-item>
        <el-form-item label="排班狀態">
          <el-radio-group v-model="scheduleForm.scheduleStatus">
            <el-radio-button label="上班" />
            <el-radio-button label="休假" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排班時段">
          <el-empty v-if="scheduleSlotOptions.length === 0" description="目前沒有可排班時段" :image-size="72" />
          <el-checkbox-group v-else v-model="scheduleSlotIds">
            <div v-for="slot in scheduleSlotOptions" :key="slot.slotId" class="beauty-check-line">
              <el-checkbox :label="slot.slotId" :disabled="scheduleSlotDisabled(slot)">
                {{ slot.slotName }}
                <el-tag size="small" :type="tagType(slot.slotStatus)">{{ slot.slotStatus }}</el-tag>
              </el-checkbox>
            </div>
          </el-checkbox-group>
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

    <el-dialog v-model="blockDialogVisible" title="封鎖時段" width="560px">
      <el-form label-position="top">
        <el-form-item label="日期">
          <el-input :model-value="selectedDate" disabled />
        </el-form-item>
        <el-form-item label="美容師">
          <el-input :model-value="currentGroomerName" disabled />
        </el-form-item>
        <el-form-item label="新增封鎖時段">
          <el-empty v-if="availableSlots.length === 0" description="目前沒有可封鎖的時段" :image-size="72" />
          <el-checkbox-group v-else v-model="blockSelectedSlotIds">
            <div v-for="slot in availableSlots" :key="slot.slotId" class="beauty-check-line">
              <el-checkbox :label="slot.slotId">{{ slot.slotName }} {{ slot.slotStatus }}</el-checkbox>
            </div>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="封鎖原因">
          <el-input v-model="blockNote" type="textarea" :rows="3" placeholder="封鎖原因或備註" />
        </el-form-item>
        <el-button type="warning" :disabled="availableSlots.length === 0" @click="saveBlock">新增封鎖</el-button>
      </el-form>

      <el-divider />

      <div>
        <h4 class="beauty-section-title">已封鎖時段</h4>
        <el-empty v-if="blockedSlots.length === 0" description="目前沒有封鎖時段" :image-size="72" />
        <el-table v-else :data="blockedSlots" size="small">
          <el-table-column prop="slotName" label="時段" width="120" />
          <el-table-column label="備註" min-width="180">
            <template #default="{ row }">
              <el-input
                v-if="editingBlockId === row.workSlotId"
                v-model="blockEditNote"
                type="textarea"
                :rows="2"
              />
              <span v-else>{{ row.note || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="center">
            <template #default="{ row }">
              <template v-if="editingBlockId === row.workSlotId">
                <el-button size="small" type="primary" link @click="saveBlockNote(row)">儲存</el-button>
                <el-button size="small" link @click="cancelEditBlock">取消</el-button>
              </template>
              <template v-else>
                <el-button size="small" type="primary" link @click="startEditBlock(row)">編輯</el-button>
                <el-button size="small" type="danger" link @click="deleteBlock(row)">解除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="blockDialogVisible = false">關閉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

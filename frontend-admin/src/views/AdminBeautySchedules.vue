<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '@/utils/request'
import Swal from 'sweetalert2'
import '@/assets/css/BeautyAdmin.css'

const groomers = ref([])
const selectedGroomerId = ref(null)
const calendarDate = ref(new Date())
const selectedDate = ref(formatDate(new Date()))
const overviewWeekStart = ref(formatDate(startOfWeek(new Date())))
const monthlyData = ref([])
const overviewMonths = ref(new Map())
const daySlots = ref([])
const scheduleStatus = ref('未排班')
const scheduleNote = ref('')
const loading = ref(false)
const overviewLoading = ref(false)
const dayLoading = ref(false)
const scheduleDialogVisible = ref(false)
const scheduleSlotIds = ref([])
const daySlotPageSize = 10
const daySlotCurrentPage = ref(1)
const scheduleForm = ref({ scheduleStatus: '上班', note: '' })

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

function addDays(date, amount) {
  const value = date instanceof Date ? new Date(date) : new Date(date)
  value.setDate(value.getDate() + amount)
  return value
}

function startOfWeek(date) {
  const value = date instanceof Date ? new Date(date) : new Date(date)
  value.setHours(0, 0, 0, 0)
  value.setDate(value.getDate() - value.getDay())
  return value
}

const dayMap = computed(() => {
  return new Map(monthlyData.value.map(day => [day.workDate, day]))
})

const daySlotTotalPages = computed(() => Math.max(1, Math.ceil(daySlots.value.length / daySlotPageSize)))

const pagedDaySlots = computed(() => {
  const start = (daySlotCurrentPage.value - 1) * daySlotPageSize
  return daySlots.value.slice(start, start + daySlotPageSize)
})

const clampDaySlotPage = () => {
  if (daySlotCurrentPage.value > daySlotTotalPages.value) {
    daySlotCurrentPage.value = daySlotTotalPages.value
  }
}

const visibleWeekDays = computed(() => {
  const start = new Date(overviewWeekStart.value)
  return Array.from({ length: 7 }, (_, index) => {
    const day = addDays(start, index)
    return {
      date: formatDate(day),
      label: `${day.getMonth() + 1}/${day.getDate()}`,
      weekday: ['日', '一', '二', '三', '四', '五', '六'][day.getDay()],
    }
  })
})

const overviewWeekLabel = computed(() => {
  const days = visibleWeekDays.value
  return `${days[0]?.date || ''} - ${days[6]?.date || ''}`
})

const currentGroomerName = computed(() => {
  const groomer = groomers.value.find(item => item.groomerId === selectedGroomerId.value)
  return groomer?.displayName || (selectedGroomerId.value ? `美容師 ${selectedGroomerId.value}` : '-')
})

const scheduleSlotOptions = computed(() => daySlots.value.filter(slot => slot.slotBookable))
const selectableScheduleSlotIds = computed(() =>
  scheduleSlotOptions.value
    .filter(slot => !scheduleSlotDisabled(slot))
    .map(slot => slot.slotId)
)

const scheduleAllChecked = computed({
  get: () =>
    selectableScheduleSlotIds.value.length > 0 &&
    selectableScheduleSlotIds.value.every(id => scheduleSlotIds.value.includes(id)),
  set: checked => {
    if (checked) {
      scheduleSlotIds.value = [...new Set([
        ...scheduleSlotIds.value,
        ...selectableScheduleSlotIds.value,
      ])]
      return
    }

    scheduleSlotIds.value = scheduleSlotIds.value.filter(
      id => !selectableScheduleSlotIds.value.includes(id)
    )
  },
})

const scheduleAllIndeterminate = computed(() => {
  const selectedCount = scheduleSlotIds.value.filter(id =>
    selectableScheduleSlotIds.value.includes(id)
  ).length

  return selectedCount > 0 && selectedCount < selectableScheduleSlotIds.value.length
})

const tagType = status => {
  if (status === '上班') return 'success'
  if (status === '休假') return 'danger'
  if (status === '預約占用') return 'primary'
  if (status === '排班關閉') return 'info'
  if (status === '可預約') return 'success'
  return 'info'
}

const overviewDay = (groomerId, workDate) => {
  return overviewMonths.value.get(groomerId)?.get(workDate) || null
}

const overviewStatusClass = day => {
  if (!day) return 'not-set'
  if (day.scheduleStatus === '上班') return 'work'
  if (day.scheduleStatus === '休假') return 'off'
  return 'not-set'
}

const overviewCellTitle = (groomer, day) => {
  const data = overviewDay(groomer.groomerId, day.date)
  const name = groomer.displayName || `美容師 ${groomer.groomerId}`
  if (!data) return `${name} ${day.date} 未排班`
  return `${name} ${day.date} ${data.scheduleStatus} 可約 ${data.availableSlotCount} 預約 ${data.bookedSlotCount} 關閉 ${data.scheduleClosedSlotCount}`
}

const syncOverviewWeekByDate = workDate => {
  overviewWeekStart.value = formatDate(startOfWeek(workDate))
}

const goPreviousWeek = () => {
  overviewWeekStart.value = formatDate(addDays(overviewWeekStart.value, -7))
}

const goCurrentWeek = () => {
  syncOverviewWeekByDate(new Date())
}

const goNextWeek = () => {
  overviewWeekStart.value = formatDate(addDays(overviewWeekStart.value, 7))
}

const goPreviousMonth = () => {
  const value = new Date(calendarDate.value)
  value.setMonth(value.getMonth() - 1)
  calendarDate.value = value
}

const goCurrentMonth = () => {
  calendarDate.value = new Date()
}

const goNextMonth = () => {
  const value = new Date(calendarDate.value)
  value.setMonth(value.getMonth() + 1)
  calendarDate.value = value
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

const loadOverviewWeek = async () => {
  if (groomers.value.length === 0) return
  overviewLoading.value = true
  try {
    const yearMonths = [...new Set(visibleWeekDays.value.map(day => formatYearMonth(day.date)))]
    const requests = groomers.value.flatMap(groomer => yearMonths.map(yearMonth => ({
      groomer,
      yearMonth,
      request: request.get('/api/admin/beauty/schedules/month', {
        params: {
          groomerId: groomer.groomerId,
          yearMonth,
        },
      }),
    })))
    const responses = await Promise.all(requests.map(item => item.request))

    const nextMap = new Map()
    responses.forEach((res, index) => {
      const groomerId = requests[index].groomer.groomerId
      if (!nextMap.has(groomerId)) {
        nextMap.set(groomerId, new Map())
      }
      const dateMap = nextMap.get(groomerId)
      ;(res.data.days || []).forEach(day => {
        dateMap.set(day.workDate, day)
      })
    })
    overviewMonths.value = nextMap
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '排班總覽載入失敗', 'error')
  } finally {
    overviewLoading.value = false
  }
}

const loadDaySlots = async () => {
  if (!selectedGroomerId.value || !selectedDate.value) return
  daySlotCurrentPage.value = 1
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
    clampDaySlotPage()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', '單日時段載入失敗', 'error')
  } finally {
    dayLoading.value = false
  }
}

const selectDate = day => {
  selectedDate.value = day
  syncOverviewWeekByDate(day)
  loadDaySlots()
}

const selectOverviewCell = (groomerId, workDate) => {
  const groomerChanged = selectedGroomerId.value !== groomerId
  const monthChanged = formatYearMonth(calendarDate.value) !== formatYearMonth(workDate)
  selectedGroomerId.value = groomerId
  selectedDate.value = workDate
  syncOverviewWeekByDate(workDate)
  if (monthChanged) {
    calendarDate.value = new Date(workDate)
    return
  }
  if (!groomerChanged) {
    loadDaySlots()
  }
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
    await loadOverviewWeek()
  } catch (err) {
    console.log(err)
    Swal.fire('錯誤', err.response?.data?.message || '排班儲存失敗', 'error')
  }
}

watch([selectedGroomerId, calendarDate], () => {
  loadMonth()
})

watch(calendarDate, () => {
  syncOverviewWeekByDate(calendarDate.value)
})

watch(overviewWeekStart, () => {
  loadOverviewWeek()
})

onMounted(async () => {
  await loadGroomers()
  await loadOverviewWeek()
})
</script>

<template>
  <div class="beauty-admin-page">
    <div class="beauty-card">
      <div class="beauty-toolbar">
        <div>
          <h3 class="beauty-title">班表管理</h3>
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

      <div class="schedule-workspace">
        <div class="schedule-main">
          <section class="schedule-overview" v-loading="overviewLoading">
            <div class="beauty-toolbar schedule-overview-head">
              <div>
                <h3 class="beauty-title">美容師排班總覽</h3>
                <div class="beauty-subtitle">{{ overviewWeekLabel }}</div>
              </div>
              <div class="schedule-week-actions">
                <el-button size="small" @click="goPreviousWeek">上一週</el-button>
                <el-button size="small" @click="goCurrentWeek">本週</el-button>
                <el-button size="small" @click="goNextWeek">下一週</el-button>
              </div>
            </div>

            <div class="schedule-matrix-wrap">
              <table class="schedule-matrix">
                <thead>
                  <tr>
                    <th class="groomer-head">美容師</th>
                    <th v-for="day in visibleWeekDays" :key="day.date" class="date-head">
                      <span>{{ day.label }}</span>
                      <small>{{ day.weekday }}</small>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="groomer in groomers" :key="groomer.groomerId">
                    <th class="groomer-cell">
                      {{ groomer.displayName || `美容師 ${groomer.groomerId}` }}
                    </th>
                    <td v-for="day in visibleWeekDays" :key="`${groomer.groomerId}-${day.date}`">
                      <button
                        type="button"
                        class="matrix-cell"
                        :class="[
                          overviewStatusClass(overviewDay(groomer.groomerId, day.date)),
                          {
                            selected: selectedGroomerId === groomer.groomerId && selectedDate === day.date,
                          },
                        ]"
                        :title="overviewCellTitle(groomer, day)"
                        @click="selectOverviewCell(groomer.groomerId, day.date)"
                      >
                        <template v-if="overviewDay(groomer.groomerId, day.date)">
                          <strong>{{ overviewDay(groomer.groomerId, day.date).scheduleStatus }}</strong>
                          <span>可 {{ overviewDay(groomer.groomerId, day.date).availableSlotCount }}</span>
                          <span>約 {{ overviewDay(groomer.groomerId, day.date).bookedSlotCount }}</span>
                        </template>
                        <template v-else>
                          <strong>未排</strong>
                          <span>-</span>
                        </template>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="schedule-matrix-legend">
              <span><i class="legend-dot work"></i>上班</span>
              <span><i class="legend-dot off"></i>休假</span>
              <span><i class="legend-dot not-set"></i>未排班</span>
              <span>可：可預約時段數</span>
              <span>約：已預約時段數</span>
            </div>
          </section>

          <div v-loading="loading" class="schedule-calendar-panel">
            <el-calendar v-model="calendarDate">
              <template #header="{ date }">
                <span>{{ date }}</span>
                <el-button-group>
                  <el-button size="small" @click="goPreviousMonth">上個月</el-button>
                  <el-button size="small" @click="goCurrentMonth">當月</el-button>
                  <el-button size="small" @click="goNextMonth">下個月</el-button>
                </el-button-group>
              </template>
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
                      <span>關閉 {{ dayMap.get(data.day).scheduleClosedSlotCount }}</span>
                      <span>可約 {{ dayMap.get(data.day).availableSlotCount }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </el-calendar>
          </div>
        </div>

        <aside class="schedule-side">
          <div class="beauty-card day-panel">
            <div class="beauty-toolbar">
              <div>
                <h3 class="beauty-title">{{ selectedDate }}</h3>
                <div class="beauty-subtitle">
                  {{ currentGroomerName }} ・ 排班狀態：
                  <el-tag :type="tagType(scheduleStatus)">{{ scheduleStatus }}</el-tag>
                </div>
              </div>
              <div class="beauty-actions">
                <el-button size="small" class="schedule-edit-button" @click="openScheduleDialog">編輯排班</el-button>
              </div>
            </div>

            <el-table v-loading="dayLoading" :data="pagedDaySlots" size="small">
              <el-table-column prop="slotName" label="時段" min-width="110" />
              <el-table-column label="狀態" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="tagType(row.slotStatus)">{{ row.slotStatus }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="appointmentId" label="預約單" width="90" />
              <el-table-column prop="note" label="備註" min-width="120" show-overflow-tooltip />
            </el-table>

            <div v-if="daySlots.length > daySlotPageSize" class="beauty-pagination compact">
              <span>共 {{ daySlots.length }} 筆，每頁 {{ daySlotPageSize }} 筆</span>
              <el-pagination
                v-model:current-page="daySlotCurrentPage"
                background
                layout="prev, pager, next"
                :page-size="daySlotPageSize"
                :total="daySlots.length"
                small
              />
            </div>
          </div>
        </aside>
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
          <div v-else class="schedule-slot-picker">
            <div class="beauty-check-line">
              <el-checkbox
                v-model="scheduleAllChecked"
                :disabled="selectableScheduleSlotIds.length === 0"
                :indeterminate="scheduleAllIndeterminate"
              >
                全選全部時段
              </el-checkbox>
            </div>
            <el-checkbox-group v-model="scheduleSlotIds" class="schedule-slot-option-list">
              <div v-for="slot in scheduleSlotOptions" :key="slot.slotId" class="beauty-check-line">
                <el-checkbox :label="slot.slotId" :disabled="scheduleSlotDisabled(slot)">
                  {{ slot.slotName }}
                  <el-tag size="small" :type="tagType(slot.slotStatus)">{{ slot.slotStatus }}</el-tag>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>
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
  </div>
</template>

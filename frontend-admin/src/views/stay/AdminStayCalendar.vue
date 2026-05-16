<script setup>
import request from '@/utils/request.js'
import { ref } from 'vue'

const allRooms = ref([])
const maintenanceRoomId = ref('')
const maintenanceStatus = ref('維護中')

const rooms = ref([])
const isLoading = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0]) // 預設今天

const fetchRooms = async () => {
  isLoading.value = true
  try {
    const res = await request.get('/api/stay/rooms/status', {
      params: { date: selectedDate.value },
    })
    rooms.value = res.data
  } catch (e) {
    console.error('載入失敗', e)
  } finally {
    isLoading.value = false
  }
}

const fetchAllRooms = async () => {
  try {
    const res = await request.get('/api/stay/rooms')
    allRooms.value = res.data
  } catch (e) {
    console.error('房間載入失敗', e)
  }
}

const confirmMaintenance = async () => {
  if (!maintenanceRoomId.value) {
    alert('請選擇房間')
    return
  }
  try {
    await request.patch(`/api/stay/rooms/${maintenanceRoomId.value}/status`, null, {
      params: { status: maintenanceStatus.value },
    })
    await fetchRooms()
  } catch (e) {
    alert('設定失敗')
  }
}

// onMounted 補上
fetchAllRooms()

fetchRooms()
</script>

<template>
  <div class="room-admin">
    <div class="page-header">
      <h2 class="page-title">住宿預約日曆</h2>
      <button class="btn-maintenance" data-bs-toggle="modal" data-bs-target="#maintenanceModal">
        🔧 房間維護
      </button>
    </div>

    <!-- 日期選擇 -->
    <div class="toolbar">
      <input type="date" v-model="selectedDate" @change="fetchRooms" class="date-input" />
    </div>

    <div v-if="isLoading" class="loading-tip">載入中...</div>

    <!-- 房間卡片 -->
    <div v-else class="room-grid">
      <div
        v-for="room in rooms"
        :key="room.roomId"
        class="room-card"
        :class="{
          maintenance: room.roomStatus === '維護中',
          occupied: room.isOccupied,
          available: room.roomStatus !== '維護中' && !room.isOccupied,
        }"
      >
        <div class="room-no">{{ room.roomNo }}</div>
        <div class="room-type">{{ room.roomTypeName }}</div>

        <span
          class="room-status-badge"
          :class="{
            'badge-maintenance': room.roomStatus === '維護中',
            'badge-occupied': room.isOccupied,
            'badge-available': room.roomStatus !== '維護中' && !room.isOccupied,
          }"
        >
          {{
            room.roomStatus === '維護中' ? '🔧 維護中' : room.isOccupied ? '🔒 已預約' : '✅ 可預約'
          }}
        </span>

        <div class="room-detail" v-if="room.isOccupied">
          <div>👤 {{ room.memberName }}</div>
          <div>🐾 {{ room.petName }}</div>
          <div>📅 {{ room.stayStartDate }} ~ {{ room.stayEndDate }}</div>
        </div>
      </div>
    </div>

    <div class="modal fade" id="maintenanceModal" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content custom-modal">
          <div class="modal-header-row">
            <h5 class="custom-modal-title">房間維護設定</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>

          <div class="modal-field">
            <label>選擇房間</label>
            <select v-model="maintenanceRoomId">
              <option value="">請選擇</option>
              <option v-for="room in allRooms" :key="room.roomId" :value="room.roomId">
                {{ room.roomNo }} - {{ room.roomTypeName }}
              </option>
            </select>
          </div>

          <div class="modal-field">
            <label>設定狀態</label>
            <select v-model="maintenanceStatus">
              <option value="維護中">維護中</option>
              <option value="可預約">恢復可預約</option>
            </select>
          </div>

          <div class="custom-modal-footer">
            <button class="btn-modal-confirm" data-bs-dismiss="modal" @click="confirmMaintenance">
              確認
            </button>
            <button class="btn-modal-cancel" data-bs-dismiss="modal">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.room-admin {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: #333;
}

/* 工具列 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.date-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}

.btn-maintenance {
  padding: 8px 18px;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-maintenance:hover {
  background: #d97706;
}

/* 房間卡片網格 */
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.room-card {
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.room-card.occupied {
  border-color: #fca5a5;
  background: #fff5f5;
}

.room-card.maintenance {
  border-color: #fcd34d;
  background: #fffbeb;
}

.room-card.available {
  border-color: #86efac;
  background: #f0fdf4;
}

.room-no {
  font-size: 1.1rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
}

.room-type {
  font-size: 0.82rem;
  color: #888;
  margin-bottom: 10px;
}

.room-status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 600;
  margin-bottom: 10px;
}

.badge-available {
  background: #dcfce7;
  color: #166534;
}

.badge-occupied {
  background: #fee2e2;
  color: #991b1b;
}

.badge-maintenance {
  background: #fef9c3;
  color: #854d0e;
}

.room-detail {
  font-size: 0.82rem;
  color: #555;
  line-height: 1.6;
}

/* Modal */
.custom-modal {
  border: none;
  border-radius: 16px;
  padding: 28px 24px;
}

.modal-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.custom-modal-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.modal-field {
  margin-bottom: 16px;
}

.modal-field label {
  display: block;
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 6px;
  font-weight: 600;
}

.modal-field input,
.modal-field select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
}

.custom-modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-modal-confirm {
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 9px 22px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-modal-confirm:hover {
  background: #d97706;
}

.btn-modal-cancel {
  background: #f3f4f6;
  color: #555;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 9px 22px;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-modal-cancel:hover {
  background: #e5e7eb;
}

.loading-tip {
  text-align: center;
  padding: 60px;
  color: #999;
}
</style>

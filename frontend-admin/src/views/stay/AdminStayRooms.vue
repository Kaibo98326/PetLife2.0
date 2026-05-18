<script setup>
import request from '@/utils/request.js'
import { ref } from 'vue'

const roomTypes = ref([])
const isLoading = ref(false)

// 記錄哪個房型在編輯中
const editingId = ref(null)
const editingData = ref({
  roomName: '',
  capacity: '',
  roomDescription: '',
  roomPrice: '',
})
const fetchRoomTypes = async () => {
  isLoading.value = true
  try {
    const res = await request.get('/api/stay/roomtype')
    roomTypes.value = res.data
  } catch (e) {
    console.error('載入失敗', e)
  } finally {
    isLoading.value = false
  }
}

const startEdit = (roomType) => {
  editingId.value = roomType.roomTypeId
  editingData.value = {
    roomName: roomType.roomName,
    capacity: roomType.capacity,
    roomDescription: roomType.roomDescription,
    roomPrice: roomType.roomPrice,
  }
}

const cancelEdit = () => {
  editingId.value = null
  editingData.value = {}
}

const confirmEdit = async (roomTypeId) => {
  try {
    await request.put(`/api/stay/roomtype/${roomTypeId}`, null, {
      params: {
        price: editingData.value.roomPrice,
        roomName: editingData.value.roomName,
        capacity: editingData.value.capacity,
        roomDescription: editingData.value.roomDescription,
      },
    })
    await fetchRoomTypes()
    cancelEdit()
  } catch (e) {
    alert('修改失敗')
  }
}

fetchRoomTypes()
</script>

<template>
  <div class="room-type-admin">
    <h2 class="page-title">房型管理</h2>

    <div v-if="isLoading" class="loading-tip">載入中...</div>

    <div v-else class="table-wrap">
      <table class="room-table">
        <thead>
          <tr>
            <th>房型編號</th>
            <th>房型名稱</th>
            <th>容納數量</th>
            <th>房型介紹</th>
            <th>每晚價格</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="room in roomTypes" :key="room.roomTypeId">
            <td># {{ room.roomTypeId }}</td>

            <td>
              <span v-if="editingId !== room.roomTypeId">{{ room.roomName }}</span>
              <input v-else v-model="editingData.roomName" class="edit-input" />
            </td>

            <td>
              <span v-if="editingId !== room.roomTypeId">{{ room.capacity }} 隻</span>
              <input v-else v-model="editingData.capacity" type="number" class="edit-input short" />
            </td>

            <td class="desc-cell">
              <span v-if="editingId !== room.roomTypeId">{{ room.roomDescription }}</span>
              <input v-else v-model="editingData.roomDescription" class="edit-input" />
            </td>

            <td>
              <span v-if="editingId !== room.roomTypeId"
                >NT$ {{ room.roomPrice?.toLocaleString() }}</span
              >
              <input
                v-else
                v-model="editingData.roomPrice"
                type="number"
                class="edit-input short"
              />
            </td>

            <td class="action-cell">
              <template v-if="editingId !== room.roomTypeId">
                <button class="btn-edit" @click="startEdit(room)">編輯</button>
              </template>
              <template v-else>
                <button class="btn-confirm" @click="confirmEdit(room.roomTypeId)">確認</button>
                <button class="btn-cancel" @click="cancelEdit">取消</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.room-type-admin {
  padding: 24px;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
}

.loading-tip {
  text-align: center;
  padding: 60px;
  color: #999;
}

.table-wrap {
  overflow-x: auto;
}

.room-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.room-table th {
  background: #f9fafb;
  padding: 10px 14px;
  text-align: left;
  border-bottom: 2px solid #e5e7eb;
  font-weight: 600;
  color: #555;
  white-space: nowrap;
}

.room-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
}

.room-table tr:hover td {
  background: #fafafa;
}

.desc-cell {
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price-input {
  width: 100px;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}

.action-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-edit {
  padding: 5px 14px;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-edit:hover {
  background: #4338ca;
}

.btn-confirm {
  padding: 5px 14px;
  background: #16a34a;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm:hover {
  background: #15803d;
}

.btn-cancel {
  padding: 5px 14px;
  background: #f3f4f6;
  color: #555;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #e5e7eb;
}
.edit-input {
  width: 140px;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}

.edit-input.short {
  width: 80px;
}
</style>

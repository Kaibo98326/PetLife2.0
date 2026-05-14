<template>
  <div class="admin-stay-rooms">
    <h1>後台房間管理</h1>

    <table class="room-table">
      <thead>
        <tr>
          <th>房間編號</th>
          <th>房號</th>
          <th>房型名稱</th>
          <th>房間狀態</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="room in rooms" :key="room.roomId">
          <td>{{ room.roomId }}</td>
          <td>{{ room.roomNo }}</td>
          <td>{{ room.roomTypeName }}</td>
          <td>
            <span :class="getStatusClass(room.roomStatus)">{{ room.roomStatus }}</span>
          </td>
          <td>
            <select
              v-model="room.roomStatus"
              @change="updateRoomStatus(room.roomId, room.roomStatus)"
            >
              <option value="可預約">可預約</option>
              <option value="維護中">維護中</option>
            </select>
          </td>
        </tr>
      </tbody>
    </table>

    <h2>房型價格管理</h2>
    <div class="room-type-price-management">
      <div v-for="roomType in uniqueRoomTypes" :key="roomType.roomTypeId" class="room-type-item">
        <span>{{ roomType.roomTypeName }}</span>
        <input type="number" v-model.number="roomType.newPrice" placeholder="新價格" />
        <button @click="updatePrice(roomType.roomTypeId, roomType.newPrice)">更新</button>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AdminStayRooms',
  data() {
    return {
      rooms: [],
      uniqueRoomTypes: [],
    }
  },
  created() {
    this.fetchRooms()
  },
  methods: {
    async fetchRooms() {
      try {
        const response = await request.get('/api/admin/stay/room')
        this.rooms = response.data || response
        this.extractUniqueRoomTypes(this.rooms)
      } catch (error) {
        console.error('獲取房間列表失敗:', error)
      }
    },
    async updateRoomStatus(roomId, status) {
      if (confirm(`確定更新房間狀態為 ${status} 嗎？`)) {
        try {
          await request.patch(`/api/admin/stay/room/${roomId}/status`, null, { params: { status } })
          alert('更新成功！')
          this.fetchRooms()
        } catch (error) {
          alert('更新失敗')
        }
      }
    },
    async updatePrice(roomTypeId, newPrice) {
      if (!newPrice || newPrice <= 0) return alert('請輸入有效價格')
      try {
        await request.patch(`/api/admin/stay/roomtype/${roomTypeId}/price`, null, {
          params: { newPrice },
        })
        alert('價格更新成功！')
        this.fetchRooms()
      } catch (error) {
        alert('更新失敗')
      }
    },
    extractUniqueRoomTypes(rooms) {
      const map = new Map()
      rooms.forEach((r) => {
        if (!map.has(r.roomTypeId)) {
          map.set(r.roomTypeId, {
            roomTypeId: r.roomTypeId,
            roomTypeName: r.roomTypeName,
            newPrice: null,
          })
        }
      })
      this.uniqueRoomTypes = Array.from(map.values())
    },
    getStatusClass(status) {
      return { 'status-available': status === '可預約', 'status-maintenance': status === '維護中' }
    },
  },
}
</script>

<style scoped>
.admin-stay-rooms {
  padding: 20px;
}
.room-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}
.room-table th,
.room-table td {
  border: 1px solid #eee;
  padding: 8px;
  text-align: left;
}
.status-available {
  color: green;
  font-weight: bold;
}
.status-maintenance {
  color: orange;
  font-weight: bold;
}
.room-type-price-management {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}
.room-type-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  padding: 10px;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
.room-type-item input {
  width: 80px;
  padding: 5px;
}
.room-type-item button {
  padding: 5px 10px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>

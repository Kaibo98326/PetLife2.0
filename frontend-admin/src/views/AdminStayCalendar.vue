<template>
  <div class="admin-stay-calendar">
    <h1>後台房間日曆</h1>

    <div class="date-selection">
      <label>開始日期:</label>
      <input type="date" v-model="startDate" />
      <label>結束日期:</label>
      <input type="date" v-model="endDate" />
      <button @click="fetchCalendar">查詢</button>
    </div>

    <div v-if="calendarData.length > 0">
      <div v-for="(dateGroup, date) in groupedData" :key="date" class="date-group">
        <h2>{{ date }}</h2>
        <div class="room-cards">
          <div v-for="room in dateGroup" :key="room.roomId" class="room-card">
            <h3>{{ room.roomNo }} ({{ room.roomTypeName }})</h3>
            <p>
              狀態: <span :class="getStatusClass(room.status)">{{ room.status }}</span>
            </p>
            <p v-if="room.memberName">預約人: {{ room.memberName }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AdminStayCalendar',
  data() {
    return {
      startDate: '',
      endDate: '',
      calendarData: [],
    }
  },
  computed: {
    groupedData() {
      const grouped = {}
      this.calendarData.forEach((item) => {
        if (!grouped[item.date]) grouped[item.date] = []
        grouped[item.date].push(item)
      })
      return grouped
    },
  },
  methods: {
    async fetchCalendar() {
      if (!this.startDate || !this.endDate) return alert('請選擇日期')
      try {
        const response = await request.get('/api/admin/stay/calendar/rooms', {
          params: { startDate: this.startDate, endDate: this.endDate },
        })
        this.calendarData = response.data || response
      } catch (error) {
        console.error('查詢失敗', error)
      }
    },
    getStatusClass(status) {
      return {
        'status-available': status === '可用',
        'status-booked': status === '已預約',
        'status-maintenance': status === '維護中',
      }
    },
  },
}
</script>

<style scoped>
.admin-stay-calendar {
  padding: 20px;
}
.date-selection {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}
.date-selection input,
.date-selection button {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.date-selection button {
  background: #007bff;
  color: white;
  border: none;
  cursor: pointer;
}
.date-group {
  margin-bottom: 30px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}
.room-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
}
.room-card {
  background: white;
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}
.status-available {
  color: green;
  font-weight: bold;
}
.status-booked {
  color: red;
  font-weight: bold;
}
.status-maintenance {
  color: orange;
  font-weight: bold;
}
</style>

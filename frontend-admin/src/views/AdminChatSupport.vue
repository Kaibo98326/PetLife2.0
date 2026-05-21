<template>
  <div class="chat-support-container">
    <div class="row g-3">
      <!-- 左側：Session 列表 -->
      <div class="col-md-4">
        <div class="session-list-card">
          <div class="session-list-header">
            <h5><i class="fas fa-headset me-2"></i>客服對話列表</h5>
            <el-badge :value="waitingCount" :max="99" class="ms-2" v-if="waitingCount > 0 && activeTab === 'active'" />
          </div>

          <div class="session-tab-toggle p-2 border-bottom text-center">
            <el-radio-group v-model="activeTab" size="small" @change="handleTabChange">
              <el-radio-button value="active">當前對話</el-radio-button>
              <el-radio-button value="history">歷史紀錄</el-radio-button>
            </el-radio-group>
          </div>

          <div class="session-list-body">
            <div v-if="sessions.length === 0" class="no-session">
              <i class="fas fa-inbox"></i>
              <p>{{ activeTab === 'active' ? '目前沒有待處理的對話' : '目前沒有歷史對話紀錄' }}</p>
            </div>

            <div 
              v-for="s in sessions" 
              :key="s.sessionId" 
              class="session-item"
              :class="{ active: selectedSession?.sessionId === s.sessionId, waiting: s.status === 'waiting' }"
              @click="selectSession(s)"
            >
              <div class="session-item-top">
                <span class="member-name">
                  <i class="fas fa-user me-1"></i>{{ s.memberName || '會員 #' + s.memberId }}
                </span>
                <el-tag v-if="s.status === 'waiting'" type="danger" size="small">等待中</el-tag>
                <el-tag v-else-if="s.status === 'active'" type="success" size="small">對話中</el-tag>
                <el-tag v-else-if="s.status === 'closed'" type="info" size="small">已結束</el-tag>
              </div>
              <div class="session-item-time">
                {{ formatTime(s.createdAt) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右側：對話內容 -->
      <div class="col-md-8">
        <div class="chat-panel-card">
          <!-- 無選擇時 -->
          <div v-if="!selectedSession" class="chat-empty">
            <i class="fas fa-comments"></i>
            <p>請從左側選擇一個對話</p>
          </div>

          <!-- 有選擇時 -->
          <template v-else>
            <!-- 對話頭部 -->
            <div class="chat-panel-header">
              <div>
                <h6 class="mb-0">
                  <i class="fas fa-user me-1"></i>
                  {{ selectedSession.memberName || '會員 #' + selectedSession.memberId }}
                </h6>
                <small class="text-muted">Session #{{ selectedSession.sessionId }}</small>
              </div>
              <div class="d-flex gap-2">
                <el-button 
                  v-if="selectedSession.status === 'waiting'" 
                  type="success" 
                  size="small" 
                  @click="acceptSession"
                >
                  <i class="fas fa-hand-paper me-1"></i>接手對話
                </el-button>
                <el-button 
                  v-if="selectedSession.status === 'active'" 
                  type="danger" 
                  size="small" 
                  @click="closeSession"
                >
                  <i class="fas fa-times me-1"></i>結束對話
                </el-button>
              </div>
            </div>

            <!-- 對話訊息區 -->
            <div class="chat-panel-messages" ref="msgBoxRef">
              <!-- 優化 1：載入更多 -->
              <div class="text-center mb-3" v-if="currentMessages.length >= historySize">
                <el-button link type="primary" size="small" @click="loadMoreHistory">
                  <i class="fas fa-history me-1"></i>載入更早的訊息...
                </el-button>
              </div>

              <template v-for="(msg, index) in currentMessages" :key="msg.id">
                <!-- 優化 2：日期分隔線 -->
                <div v-if="shouldShowDate(msg, index)" class="date-separator">
                  <span>{{ formatDate(msg.createdAt) }}</span>
                </div>

                <!-- 優化 4：Session 邊界線 -->
                <div v-if="shouldShowSessionStart(msg, index)" class="session-boundary">
                  <span v-if="msg.sessionId">--- 新的對話 Session #{{ msg.sessionId }} ---</span>
                  <span v-else>--- AI 智能客服對話 ---</span>
                </div>

                <div :class="['admin-msg-row', msg.role]">
                  <div class="admin-msg-bubble">
                    <div class="admin-msg-role">{{ msg.role === 'user' ? selectedSession.memberName : (msg.role === 'staff' ? '🧑‍💼 客服' : '系統') }}</div>
                    {{ msg.message }}
                    <span class="admin-msg-time">{{ msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString() : '' }}</span>
                  </div>
                </div>
              </template>
            </div>

            <!-- 輸入框（僅 active 狀態可用） -->
            <div class="chat-panel-input" v-if="selectedSession.status === 'active'">
              <form @submit.prevent="sendStaffMessage" class="d-flex gap-2">
                <input 
                  v-model="staffInput" 
                  type="text" 
                  class="form-control" 
                  placeholder="輸入回覆訊息..."
                />
                <el-button type="warning" native-type="submit" :disabled="!staffInput.trim()">
                  <i class="fas fa-paper-plane me-1"></i>發送
                </el-button>
              </form>
            </div>

            <div class="chat-panel-input text-center text-muted" v-else-if="selectedSession.status === 'waiting'">
              <i class="fas fa-info-circle me-1"></i>請先按「接手對話」才能回覆
            </div>

            <div class="chat-panel-input text-center text-danger" v-else-if="selectedSession.status === 'closed'">
              <i class="fas fa-lock me-1"></i>此對話已結束，無法再發送訊息
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { useEmployeeStore } from '@/stores/employee'

const employeeStore = useEmployeeStore()

const sessions = ref([])
const activeTab = ref('active') // 'active' 或 'history'
const selectedSession = ref(null)
const currentMessages = ref([])
const staffInput = ref('')
const waitingCount = ref(0)
const historySize = ref(50)
const msgBoxRef = ref(null)

let pollSessionTimer = null
let pollMsgTimer = null
let lastMsgId = 0

const API_BASE = 'http://localhost:8082/api/chat'

// ── 載入所有 Session ──
async function fetchSessions() {
  try {
    const endpoint = activeTab.value === 'active' ? '/session/waiting' : '/session/closed'
    const res = await axios.get(API_BASE + endpoint)
    sessions.value = res.data || []
    
    if (activeTab.value === 'active') {
      waitingCount.value = sessions.value.filter(s => s.status === 'waiting').length
    }

    // 如果選中的 session 已被更新，同步狀態
    if (selectedSession.value) {
      const updated = sessions.value.find(s => s.sessionId === selectedSession.value.sessionId)
      if (updated) selectedSession.value = updated
    }
  } catch (e) {
    console.error('取得 Session 列表失敗', e)
  }
}

// ── 切換標籤 ──
function handleTabChange() {
  selectedSession.value = null
  currentMessages.value = []
  historySize.value = 50 // 重置分頁
  stopMsgPolling()
  fetchSessions()
}

// ── 選擇 Session (現在改為按會員) ──
async function selectSession(session) {
  if (!session) return
  selectedSession.value = session
  lastMsgId = 0
  try {
    // 優化 1：支援分頁，預設抓 50 筆
    const res = await axios.get(API_BASE + '/history/' + session.memberId + '?size=' + historySize.value)
    currentMessages.value = res.data || []
    if (currentMessages.value.length > 0) {
      lastMsgId = currentMessages.value[currentMessages.value.length - 1].id
    }
    // 如果是第一次切換會員，滾動到底部；如果是載入更多，保持位置
    if (historySize.value === 50) {
      scrollMsgBox()
    }
    startMsgPolling()
  } catch (e) {
    console.error('取得訊息失敗', e)
  }
}

// ── 載入更多歷史 ──
async function loadMoreHistory() {
  historySize.value += 50
  await selectSession(selectedSession.value)
}

// ── 工具：判斷是否顯示日期 ──
function shouldShowDate(msg, index) {
  if (index === 0) return true
  const prev = currentMessages.value[index - 1]
  const d1 = new Date(msg.createdAt).toDateString()
  const d2 = new Date(prev.createdAt).toDateString()
  return d1 !== d2
}

// ── 工具：判斷是否顯示 Session 開始 ──
function shouldShowSessionStart(msg, index) {
  if (index === 0) return true
  const prev = currentMessages.value[index - 1]
  return msg.sessionId !== prev.sessionId
}

// ── 工具：格式化日期 ──
function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('zh-TW', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

// ── 接手 Session ──
async function acceptSession() {
  if (!selectedSession.value) return
  try {
    const empId = employeeStore.employee?.empId || 1
    const empName = employeeStore.employee?.empName || '客服人員'
    await axios.put(API_BASE + '/session/' + selectedSession.value.sessionId + '/accept', {
      empId: String(empId),
      empName: empName
    })
    await fetchSessions()
    await selectSession(selectedSession.value)
  } catch (e) {
    console.error('接手失敗', e)
  }
}

// ── 關閉 Session ──
async function closeSession() {
  if (!selectedSession.value) return
  try {
    await axios.put(API_BASE + '/session/' + selectedSession.value.sessionId + '/close')
    await fetchSessions()
    selectedSession.value = null
    currentMessages.value = []
    stopMsgPolling()
  } catch (e) {
    console.error('關閉失敗', e)
  }
}

// ── 發送客服訊息 ──
async function sendStaffMessage() {
  if (!staffInput.value.trim() || !selectedSession.value) return
  const text = staffInput.value.trim()
  staffInput.value = ''

  const empId = employeeStore.employee?.empId || 1
  try {
    await axios.post(API_BASE + '/session/' + selectedSession.value.sessionId + '/send', {
      role: 'staff',
      message: text,
      userId: String(empId)
    })
    // 立即刷新訊息
    await pollNewMessages()
  } catch (e) {
    console.error('發送失敗', e)
  }
}

// ── 輪詢新訊息 ──
function startMsgPolling() {
  stopMsgPolling()
  pollMsgTimer = setInterval(pollNewMessages, 3000)
}

function stopMsgPolling() {
  if (pollMsgTimer) {
    clearInterval(pollMsgTimer)
    pollMsgTimer = null
  }
}

async function pollNewMessages() {
  if (!selectedSession.value) return
  try {
    // 改為按會員 ID 輪詢，這樣可以看到所有 Session 的新訊息
    const res = await axios.get(API_BASE + '/member/' + selectedSession.value.memberId + '/poll?lastId=' + lastMsgId)
    const newMsgs = res.data.messages || []
    
    // 同步當前最新的 Session 狀態 (例如消費者結束了對話)
    if (res.data.session) {
      selectedSession.value.status = res.data.session.status
      selectedSession.value.sessionId = res.data.session.sessionId // 更新當前的 sessionId
    } else {
      // 如果完全沒活躍 session 了
      selectedSession.value.status = 'closed'
    }

    if (newMsgs.length > 0) {
      newMsgs.forEach(m => {
        currentMessages.value.push(m)
        lastMsgId = Math.max(lastMsgId, m.id)
      })
      scrollMsgBox()
    }
  } catch (e) {
    console.error('輪詢訊息失敗', e)
  }
}

function scrollMsgBox() {
  setTimeout(() => {
    if (msgBoxRef.value) msgBoxRef.value.scrollTop = msgBoxRef.value.scrollHeight
  }, 100)
}

function formatTime(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('zh-TW', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchSessions()
  // 每 5 秒刷新 Session 列表
  pollSessionTimer = setInterval(fetchSessions, 5000)
})

onUnmounted(() => {
  if (pollSessionTimer) clearInterval(pollSessionTimer)
  stopMsgPolling()
})
</script>

<style scoped>
.chat-support-container {
  padding: 0;
}

.session-list-card,
.chat-panel-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  overflow: hidden;
  height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
}

/* ── 左側列表 ── */
.session-list-header {
  background: linear-gradient(135deg, #2c3e50, #34495e);
  color: #fff;
  padding: 16px 20px;
  display: flex;
  align-items: center;
}

.session-list-header h5 {
  margin: 0;
  font-size: 15px;
}

.session-list-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-tab-toggle {
  background-color: #f8f9fa;
}

.no-session {
  text-align: center;
  padding: 60px 20px;
  color: #adb5bd;
}

.no-session i {
  font-size: 40px;
  margin-bottom: 12px;
}

.session-item {
  padding: 12px 14px;
  border-radius: 10px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
}

.session-item:hover {
  background: #fef8f0;
  border-color: #e67e22;
}

.session-item.active {
  background: #fff3e0;
  border-color: #e67e22;
}

.session-item.waiting {
  border-left: 3px solid #e74c3c;
}

.session-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.member-name {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.session-item-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

/* ── 右側對話 ── */
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.chat-empty i {
  font-size: 50px;
  margin-bottom: 16px;
}

.chat-panel-header {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.chat-panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: linear-gradient(180deg, #fefaf6 0%, #fff 100%);
}

.admin-msg-row {
  margin-bottom: 12px;
}

.admin-msg-row.user {
  text-align: left;
}

.admin-msg-row.staff {
  text-align: right;
}

.admin-msg-row.bot {
  text-align: center;
}

.admin-msg-bubble {
  display: inline-block;
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-line;
  word-break: break-word;
}

.admin-msg-row.user .admin-msg-bubble {
  background: #f0f0f0;
  color: #333;
  text-align: left;
  border-top-left-radius: 4px;
}

.admin-msg-row.staff .admin-msg-bubble {
  background: linear-gradient(135deg, #e67e22, #d35400);
  color: #fff;
  text-align: left;
  border-top-right-radius: 4px;
}

.admin-msg-row.bot .admin-msg-bubble {
  background: #fff3cd;
  color: #856404;
  font-size: 12px;
  max-width: 90%;
}

.admin-msg-role {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 2px;
  opacity: 0.7;
}

.admin-msg-time {
  display: block;
  font-size: 10px;
  margin-top: 4px;
  opacity: 0.5;
}

.chat-panel-input {
  padding: 14px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

/* 分隔線樣式 */
.date-separator, .session-boundary {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.date-separator span, .session-boundary span {
  background-color: #f0f2f5;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  color: #888;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.session-boundary span {
  background-color: #fff3cd;
  color: #856404;
  border: 1px dashed #ffeeba;
}

.chat-panel-input .form-control {
  border-radius: 8px;
}
</style>

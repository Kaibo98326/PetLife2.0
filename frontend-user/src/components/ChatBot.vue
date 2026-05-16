<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import axios from '@/axios.js'
import { useUserStore } from '@/stores/user'

// ── Props & Emits ──────────────────────────────────────────────────────────
const props = defineProps({
  isOpen: Boolean
})
const emit = defineEmits(['update:isOpen'])

// ── Pinia Store ───────────────────────────────────────────────────────────
const userStore = useUserStore()

// ── 聊天機器人狀態 ────────────────────────────────────────────────────────
const chatMessages = ref([])
const userInput = ref('')
const isTyping = ref(false)

// 引導流程狀態：'menu' / 'faq' / 'chat' / 'human'
const chatStep = ref('menu')
const selectedCategory = ref('')

// ── 真人客服狀態 ──
const currentSessionId = ref(null)
const lastMessageId = ref(0)
let pollTimer = null

// 分類選單
const categoryMenu = [
  { icon: '🛒', label: '購物相關', category: '購物' },
  { icon: '📦', label: '訂單/物流', category: '__ORDER__' },
  { icon: '⭐', label: '會員/點數', category: '會員' },
  { icon: '💬', label: '其他問題（AI 回答）', category: '__AI__' },
  { icon: '👤', label: '聯繫真人客服', category: '__HUMAN__' }
]

// 所有 FAQ（從 DB 載入）
const allFaqList = ref([])

// 依目前選擇的分類篩選 FAQ
const filteredFaqList = computed(() => {
  if (!selectedCategory.value) return allFaqList.value
  return allFaqList.value.filter(f => f.category === selectedCategory.value)
})

// 從資料庫讀取 FAQ
async function fetchFaqList() {
  try {
    const res = await axios.get('/chat/faq')
    allFaqList.value = (res.data || []).map(f => ({
      question: f.question, answer: f.answer, category: f.category || '其他'
    }))
  } catch (e) {
    console.error('讀取 FAQ 失敗', e)
  }
}

// 讀取歷史聊天紀錄
async function loadChatHistory() {
  if (!userStore.token || !userStore.user) return
  try {
    const res = await axios.get('/chat/history/' + userStore.user.memberId)
    const history = res.data || []
    if (history.length > 0) {
      chatMessages.value = [
        ...history.map((h, i) => ({
          id: i + 1,
          type: h.role === 'user' ? 'user' : 'bot',
          text: h.message,
          time: h.createdAt ? new Date(h.createdAt).toLocaleTimeString() : ''
        }))
      ]
      chatStep.value = 'menu'
    }
  } catch (e) {
    console.error('讀取聊天紀錄失敗', e)
  }
}

function toggleChat() {
  const newState = !props.isOpen
  emit('update:isOpen', newState)
  
  // 每次開啟時重置為全新對話
  if (newState) {
    chatMessages.value = [
      { 
        id: 1, 
        type: 'bot', 
        text: `你好！歡迎光臨 **PetLife 寵物商店**，很高興能為您服務！我是這裡的店長。\n\n不論您是想為家中的毛孩挑選最適合的飼料與零食、詢問寵物日常照護的小撇步，或是想了解我們商店的最新優惠與服務資訊，我都會竭誠為您解答。\n\n請問今天有什麼我可以幫上忙的地方嗎？或是您可以先跟我分享一下，您家裡養的是哪種可愛的小夥伴（貓咪、狗狗或是其他小動物）呢？我能更精準地為您提供建議喔！`, 
        time: new Date().toLocaleTimeString() 
      }
    ]
    chatStep.value = 'menu'
    userInput.value = ''
    isTyping.value = false
    
    // 清除可能殘留的真人客服狀態
    stopPolling()
    currentSessionId.value = null
  } else {
    // 關閉視窗時，如果有真人連線則自動結束
    if (currentSessionId.value) {
      axios.put('/chat/session/' + currentSessionId.value + '/close').catch(() => {})
      stopPolling()
      currentSessionId.value = null
    }
  }
}

// 選擇分類
async function selectCategory(cat) {
  if (cat.category === '__ORDER__') {
    chatMessages.value.push({ id: Date.now(), type: 'user', text: cat.icon + ' ' + cat.label, time: new Date().toLocaleTimeString() })
    sendFaq({ question: '📦 查詢我的訂單', answer: '__ORDER_QUERY__' })
    chatStep.value = 'chat'
    return
  }

  if (cat.category === '__AI__') {
    chatMessages.value.push({ id: Date.now(), type: 'user', text: cat.icon + ' ' + cat.label, time: new Date().toLocaleTimeString() })
    chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: '好的！請直接輸入您的問題，我會盡力為您解答 😊', time: new Date().toLocaleTimeString() })
    chatStep.value = 'chat'
    scrollToBottom()
    return
  }

  if (cat.category === '__HUMAN__') {
    chatMessages.value.push({ id: Date.now(), type: 'user', text: cat.icon + ' ' + cat.label, time: new Date().toLocaleTimeString() })
    if (!userStore.token || !userStore.user) {
      chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: '請先登入會員帳號，我才能為您轉接真人客服喔！😊', time: new Date().toLocaleTimeString() })
      chatStep.value = 'chat'
      scrollToBottom()
      return
    }

    try {
      const res = await axios.post('/chat/session/create', {
        memberId: String(userStore.user.memberId),
        memberName: userStore.user.memberName || '會員'
      })
      currentSessionId.value = res.data.sessionId
      chatStep.value = 'human'
      
      // 添加系統提示：開始聯繫真人客服
      chatMessages.value.push({ 
        id: Date.now() + 50, 
        type: 'system', 
        text: '系統：已為您轉接真人客服，請稍候。', 
        time: new Date().toLocaleTimeString() 
      })

      const msgRes = await axios.get('/chat/session/' + currentSessionId.value + '/messages')
      const msgs = msgRes.data || []
      msgs.forEach(m => {
        chatMessages.value.push({
          id: m.id,
          type: m.role === 'user' ? 'user' : 'bot',
          text: m.message,
          time: m.createdAt ? new Date(m.createdAt).toLocaleTimeString() : ''
        })
        lastMessageId.value = Math.max(lastMessageId.value, m.id)
      })
      startPolling()
    } catch (e) {
      chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: '建立客服連線失敗，請稍後再試。', time: new Date().toLocaleTimeString() })
      chatStep.value = 'chat'
    }
    scrollToBottom()
    return
  }

  selectedCategory.value = cat.category
  chatMessages.value.push({ id: Date.now(), type: 'user', text: cat.icon + ' ' + cat.label, time: new Date().toLocaleTimeString() })
  chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: '好的！以下是「' + cat.label + '」相關的常見問題，請點選您想了解的：', time: new Date().toLocaleTimeString() })
  chatStep.value = 'faq'
  scrollToBottom()
}

async function backToMenu() {
  if (currentSessionId.value) {
    try { await axios.put('/chat/session/' + currentSessionId.value + '/close') } catch (e) {}
  }
  stopPolling()
  currentSessionId.value = null
  
  // 添加系統提示：對話結束
  chatMessages.value.push({ 
    id: Date.now(), 
    type: 'system', 
    text: '系統：本次真人對話已結束。如有其他問題歡迎再次聯繫！', 
    time: new Date().toLocaleTimeString() 
  })
  
  chatMessages.value.push({ id: Date.now() + 10, type: 'bot', text: '請問還有什麼我可以幫您的嗎？ ', time: new Date().toLocaleTimeString() })
  chatStep.value = 'menu'
  scrollToBottom()
}

function handleHumanChatSubmit() {
  if (!userInput.value.trim() || !currentSessionId.value) return
  const text = userInput.value.trim()
  userInput.value = ''
  chatMessages.value.push({ id: Date.now(), type: 'user', text, time: new Date().toLocaleTimeString() })
  scrollToBottom()
  axios.post('/chat/session/' + currentSessionId.value + '/send', {
    role: 'user', message: text, userId: String(userStore.user.memberId)
  })
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!currentSessionId.value) return
    try {
      const res = await axios.get('/chat/session/' + currentSessionId.value + '/poll?lastId=' + lastMessageId.value)
      const newMsgs = res.data.messages || []
      const session = res.data.session
      newMsgs.forEach(m => {
        if (m.role === 'user' && m.userId === userStore.user?.memberId) return
        
        let msgType = m.role === 'user' ? 'user' : 'bot'
        let msgText = m.message
        
        // 💡 智能判斷：如果是系統生成的訊息，將其轉換為前台會員語氣
        if (m.role === 'system' || m.message.includes('已加入對話') || m.message.includes('結束對話')) {
          msgType = 'system'
          if (m.message.includes('已加入對話')) {
            msgText = '系統：客服人員已加入對話，很高興為您服務。'
          } else if (m.message.includes('結束對話')) {
            msgText = '系統：本次真人對話已結束。如有其他問題歡迎再次聯繫！'
          }
        }

        chatMessages.value.push({
          id: m.id, type: msgType, text: msgText,
          time: m.createdAt ? new Date(m.createdAt).toLocaleTimeString() : ''
        })
        lastMessageId.value = Math.max(lastMessageId.value, m.id)
      })
      if (newMsgs.length > 0) scrollToBottom()
      if (session && session.status === 'closed') {
        stopPolling()
        
        // 添加系統提示：客服已結束對話
        chatMessages.value.push({ 
          id: Date.now(), 
          type: 'system', 
          text: '系統：客服人員已結束本次對話。感謝您的耐心等候！', 
          time: new Date().toLocaleTimeString() 
        })
        
        chatStep.value = 'chat'
      }
    } catch (e) {}
  }, 3000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
}

function sendFaq(faq) {
  chatMessages.value.push({ id: Date.now(), type: 'user', text: faq.question, time: new Date().toLocaleTimeString() })
  isTyping.value = true
  if (faq.answer === '__ORDER_QUERY__') {
    if (!userStore.token || !userStore.user) {
      chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: '請先登入會員帳號，我才能幫您查詢訂單喔！😊', time: new Date().toLocaleTimeString() })
      isTyping.value = false; scrollToBottom(); return
    }
    axios.get('/chat/my-orders/' + userStore.user.memberId)
      .then(res => {
        const orders = res.data
        if (!orders || orders.length === 0) {
          chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: '目前沒有找到您的訂單紀錄，快去逛逛商城吧！🛒', time: new Date().toLocaleTimeString() })
        } else {
          let msg = '📋 以下是您最近的訂單：\n\n'
          orders.forEach((o) => {
            msg += `【訂單 #${o.orderId}】\n📅 日期：${o.orderDate}\n💰 金額：$${o.orderTotal}\n📌 狀態：${o.orderStatus}\n🛍️ 商品：${o.items}\n\n`
          })
          chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: msg, time: new Date().toLocaleTimeString() })
        }
      })
      .finally(() => { isTyping.value = false; scrollToBottom() })
    return
  }

  const userId = userStore.user?.memberId || null
  axios.post('/chat/ask', { prompt: faq.question + " (請根據這個常見問題給予詳細回覆)", userId: userId ? String(userId) : null })
    .then(res => {
      chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: res.data.answer || faq.answer, time: new Date().toLocaleTimeString() })
    })
    .finally(() => { isTyping.value = false; scrollToBottom() })
}

function handleChatSubmit() {
  if (!userInput.value.trim()) return
  const text = userInput.value.trim()
  chatMessages.value.push({ id: Date.now(), type: 'user', text: text, time: new Date().toLocaleTimeString() })
  userInput.value = ''; isTyping.value = true; scrollToBottom()
  const userId = userStore.user?.memberId || null
  axios.post('/chat/ask', { prompt: text, userId: userId ? String(userId) : null })
    .then(res => {
      chatMessages.value.push({ id: Date.now() + 1, type: 'bot', text: res.data.answer || '對不起，我暫時無法回答您的問題。', time: new Date().toLocaleTimeString() })
    })
    .finally(() => { isTyping.value = false; scrollToBottom() })
}

function scrollToBottom() {
  setTimeout(() => {
    const box = document.querySelector('.chat-messages-box')
    if (box) box.scrollTop = box.scrollHeight
  }, 100)
}

onMounted(() => {
  fetchFaqList()
  // 不再載入歷史紀錄，確保每次開啟都是新的
  // loadChatHistory()
})

// 暴露方法給外部
defineExpose({ toggleChat })
</script>

<template>
  <transition name="chat-fade">
    <div v-if="isOpen" class="smart-chat-window shadow-lg">
      <div class="chat-header">
        <div class="d-flex align-items-center">
          <div class="bot-avatar me-3"><i class="fas fa-paw"></i></div>
          <div>
            <h6 class="mb-0">PetLife 智能小助手</h6>
          </div>
        </div>
        <button class="btn-close" @click="toggleChat" style="font-size: 14px;"></button>
      </div>

      <div class="chat-messages-box p-3">
        <div v-for="msg in chatMessages" :key="msg.id" :class="['message-row', msg.type]">
          <div v-if="msg.type === 'system'" class="system-message">
            <div class="system-bubble">
              <span class="system-text">{{ msg.text }}</span>
              <span class="system-time">{{ msg.time }}</span>
            </div>
          </div>
          <div v-else class="message-bubble">{{ msg.text }}<span class="message-time">{{ msg.time }}</span></div>
        </div>
        <div v-if="isTyping" class="message-row bot">
          <div class="message-bubble typing"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
        </div>
      </div>

      <div v-if="chatStep === 'menu'" class="chat-menu-area">
        <button v-for="cat in categoryMenu" :key="cat.category" class="chat-menu-btn" :class="{ 'human-btn': cat.category === '__HUMAN__' }" @click="selectCategory(cat)">
          <span class="menu-icon">{{ cat.icon }}</span><span>{{ cat.label }}</span>
        </button>
      </div>

      <div v-if="chatStep === 'faq'" class="chat-faq-area">
        <button v-for="faq in filteredFaqList" :key="faq.question" class="btn btn-sm btn-outline-warning rounded-pill" @click="sendFaq(faq); chatStep = 'chat'">{{ faq.question }}</button>
        <button class="btn btn-sm btn-outline-secondary rounded-pill" @click="backToMenu">↩ 返回主選單</button>
      </div>

      <div v-if="chatStep === 'chat'" class="chat-faq-area">
        <button class="btn btn-sm btn-outline-secondary rounded-pill" @click="backToMenu">↩ 返回主選單</button>
      </div>

      <div v-if="chatStep === 'human'" class="chat-faq-area">
        <span class="human-status-badge">🟢 真人客服連線中</span>
        <button class="btn btn-sm btn-outline-danger rounded-pill" @click="backToMenu">結束對話</button>
      </div>

      <div class="chat-input-area p-3 border-top">
        <form @submit.prevent="chatStep === 'human' ? handleHumanChatSubmit() : handleChatSubmit()" class="input-group">
          <input v-model="userInput" type="text" class="form-control rounded-pill-start border-0 bg-light" :placeholder="chatStep === 'human' ? '輸入訊息給客服人員...' : '請輸入您的問題...'" :disabled="isTyping" />
          <button class="btn btn-warning rounded-pill-end px-3" type="submit" :disabled="isTyping"><i class="fas fa-paper-plane"></i></button>
        </form>
      </div>
    </div>
  </transition>
</template>

<style scoped>
@import '@/assets/css/ChatBot.css';
</style>

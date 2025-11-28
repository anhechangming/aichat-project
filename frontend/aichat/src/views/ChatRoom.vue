<template>
  <div class="chat-room-container">
    <!-- 房间号显示 -->
    <div class="room-header">
      <span>房间号: {{ roomId }}</span>
    </div>
    
    <div class="chat-content">
      <!-- 历史对话列表 -->
      <div class="history-panel">
        <div class="history-title">历史对话</div>
        
        <!-- 当前会话 -->
        <div class="history-item current-room" @click="loadCurrentChat">
          当前会话 (房间: {{ roomId }})
        </div>
        
        <!-- 本地存储的历史会话 -->
        <div 
          class="history-item" 
          v-for="(chat, index) in savedChats" 
          :key="index"
          @click="loadSavedChat(index)"
        >
          房间: {{ chat.roomId }} 
          <span class="chat-time">{{ formatTime(chat.createTime) }}</span>
        </div>
        
        <div v-if="savedChats.length === 0" class="empty-history">
          暂无历史对话
        </div>
      </div>
      
      <!-- 聊天区域 -->
      <div class="message-panel">
        <!-- 消息列表容器 -->
        <div class="message-container" ref="messageContainer">
          <!-- 消息列表 -->
          <div class="message-list">
            <div 
              class="message-item" 
              v-for="(msg, index) in messages" 
              :key="index"
              :class="{ 'ai-message': msg.isAI, 'user-message': !msg.isAI }"
            >
              <!-- AI消息（左侧） -->
              <template v-if="msg.isAI">
                <div class="avatar ai-avatar">AI</div>
                <div class="message-bubble ai-bubble">{{ msg.content }}</div>
              </template>
              
              <!-- 用户消息（右侧） -->
              <template v-else>
                <div class="message-bubble user-bubble">{{ msg.content }}</div>
                <div class="avatar user-avatar">我</div>
              </template>
            </div>
          </div>
        </div>
        
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <a-button 
            @click="handleStart" 
            :disabled="isGameStarted"
            type="primary"
          >
            开始
          </a-button>
          <a-button 
            @click="handleEnd" 
            danger 
            :disabled="isGameEnded"
          >
            结束游戏
          </a-button>
        </div>
        
        <!-- 输入区域 -->
        <div class="input-area">
          <a-input 
            v-model:value="userInput" 
            placeholder="请输入内容..." 
            @keyup.enter="handleSend"
            :disabled="isGameEnded"
          />
          <a-button 
            type="primary" 
            @click="handleSend"
            :disabled="!isGameStarted || isGameEnded"
          >
            发送
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { Button as AButton, Input as AInput } from 'ant-design-vue'
import axios from 'axios'

// 创建axios实例
const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

// 生成6位数字房间号
const generateRoomId = () => {
  return Math.floor(100000 + Math.random() * 900000).toString()
}

// 状态管理
const roomId = ref(generateRoomId())
const messages = ref([])
const userInput = ref('')
const isGameStarted = ref(false)
const isGameEnded = ref(false)
const savedChats = ref([])
const messageContainer = ref(null)

// 从本地存储加载历史对话
const loadSavedChats = () => {
  const saved = localStorage.getItem('chatHistoryRecords')
  if (saved) {
    savedChats.value = JSON.parse(saved)
  }
}

// 保存当前对话到本地存储
const saveCurrentChat = () => {
  if (messages.value.length === 0) return
  
  const chatRecord = {
    roomId: roomId.value,
    createTime: new Date().toISOString(),
    messages: [...messages.value],
    isEnded: true
  }
  
  // 去重：避免重复保存同一房间
  const existingIndex = savedChats.value.findIndex(
    chat => chat.roomId === roomId.value
  )
  
  if (existingIndex >= 0) {
    savedChats.value[existingIndex] = chatRecord
  } else {
    savedChats.value.unshift(chatRecord) // 最新的放在最前面
  }
  
  // 限制最多保存10条记录
  if (savedChats.value.length > 10) {
    savedChats.value = savedChats.value.slice(0, 10)
  }
  
  // 保存到本地存储
  localStorage.setItem('chatHistoryRecords', JSON.stringify(savedChats.value))
}

// 加载保存的对话记录
const loadSavedChat = (index) => {
  const chat = savedChats.value[index]
  if (chat) {
    messages.value = [...chat.messages]
    roomId.value = chat.roomId
    isGameStarted.value = false
    isGameEnded.value = true
    scrollToBottom()
  }
}

// 加载当前对话（新建会话）
const loadCurrentChat = () => {
  messages.value = []
  roomId.value = generateRoomId()
  isGameStarted.value = false
  isGameEnded.value = false
  userInput.value = ''
}

// 格式化时间显示
const formatTime = (isoString) => {
  const date = new Date(isoString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 调用后端发送消息
const sendMessageToBackend = async (prompt) => {
  try {
    const response = await request.post(`/${roomId.value}/chat`, null, {
      params: { userPrompt: prompt }
    })
    return response.data
  } catch (error) {
    console.error('发送消息失败:', error)
    return '抱歉，服务器连接失败，请稍后再试。'
  }
}

// 处理开始游戏
const handleStart = async () => {
  if (isGameStarted.value) return
  
  // 添加用户"开始"消息
  messages.value.push({ content: '开始', isAI: false })
  scrollToBottom()
  
  // 调用后端获取AI回复
  const aiResponse = await sendMessageToBackend('开始')
  
  // 添加AI回复
  messages.value.push({ content: aiResponse, isAI: true })
  
  // 更新状态
  isGameStarted.value = true
  isGameEnded.value = false
  
  // 检查是否游戏结束
  if (aiResponse.includes('游戏结束')) {
    isGameEnded.value = true
    isGameStarted.value = false
    saveCurrentChat()
  }
  
  scrollToBottom()
}

// 处理结束游戏
const handleEnd = async () => {
  if (isGameEnded.value) return
  
  // 添加用户"结束游戏"消息
  messages.value.push({ content: '结束游戏', isAI: false })
  scrollToBottom()
  
  // 调用后端获取AI回复
  const aiResponse = await sendMessageToBackend('结束游戏')
  
  // 添加AI回复
  messages.value.push({ content: aiResponse, isAI: true })
  
  // 更新状态
  isGameEnded.value = true
  isGameStarted.value = false
  
  // 保存到历史记录
  saveCurrentChat()
  
  scrollToBottom()
}

// 处理发送消息
const handleSend = async () => {
  if (!userInput.value.trim() || !isGameStarted.value || isGameEnded.value) return
  
  const input = userInput.value.trim()
  
  // 添加用户消息
  messages.value.push({ content: input, isAI: false })
  
  // 清空输入框
  userInput.value = ''
  scrollToBottom()
  
  // 调用后端获取AI回复
  const aiResponse = await sendMessageToBackend(input)
  
  // 添加AI回复
  messages.value.push({ content: aiResponse, isAI: true })
  scrollToBottom()
  
  // 检查是否游戏结束
  if (aiResponse.includes('游戏结束')) {
    isGameEnded.value = true
    isGameStarted.value = false
    saveCurrentChat()
  }
}

// 自动滚动到最新消息
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

// 监听用户输入"开始"
watch(userInput, (newVal) => {
  if (newVal.trim() === '开始' && !isGameStarted.value) {
    handleStart()
    userInput.value = ''
  }
})

// 页面加载时初始化
onMounted(() => {
  loadSavedChats() // 加载本地历史记录
})
</script>

<style scoped>
.chat-room-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.room-header {
  padding: 16px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #1890ff;
}

.chat-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 历史对话面板 */
.history-panel {
  width: 250px;
  background-color: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.history-title {
  padding: 16px;
  font-weight: bold;
  border-bottom: 1px solid #e8e8e8;
  background-color: #f5f5f5;
}

.history-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f5f5f5;
}

.current-room {
  background-color: #e6f7ff;
  font-weight: bold;
}

.chat-time {
  font-size: 12px;
  color: #999;
}

.empty-history {
  padding: 16px;
  color: #999;
  text-align: center;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 消息面板 */
.message-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.message-list {
  max-width: 800px;
  margin: 0 auto;
}

.message-item {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
}

/* AI消息靠左 */
.ai-message {
  justify-content: flex-start;
}

/* 用户消息靠右 */
.user-message {
  justify-content: flex-end;
}

/* 头像样式 */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0; /* 防止头像被压缩 */
}

.ai-avatar {
  background-color: #1890ff;
  color: white;
  margin-right: 10px;
}

.user-avatar {
  background-color: #52c41a;
  color: white;
  margin-left: 10px;
}

/* 对话框样式 */
.message-bubble {
  padding: 10px 16px;
  border-radius: 20px;
  max-width: calc(100% - 60px); /* 减去头像宽度和间距 */
  word-wrap: break-word;
  line-height: 1.5;
}

.ai-bubble {
  background-color: #e6f7ff;
  color: #333;
  border-top-left-radius: 4px;
}

.user-bubble {
  background-color: #95ec69;
  color: #333;
  border-top-right-radius: 4px;
}

/* 操作按钮区域 */
.action-buttons {
  padding: 12px 20px;
  border-top: 1px solid #e8e8e8;
  background-color: #fff;
  display: flex;
  gap: 10px;
  justify-content: center;
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
  background-color: #fff;
  display: flex;
  gap: 10px;
}

.input-area .ant-input {
  flex: 1;
}
</style>
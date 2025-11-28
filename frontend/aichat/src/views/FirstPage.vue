<template>
  <div class="first-page-container">
    <div class="title">AI 脑筋急转弯</div>
    <a-button 
      class="start-button" 
      type="primary" 
      size="large" 
      @click="handleStartGame"
    >
      开始游戏
    </a-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Button as AButton } from 'ant-design-vue'

const router = useRouter()

// 生成随机房间号（100000-999999之间）
const generateRoomId = () => {
  return Math.floor(100000 + Math.random() * 900000)
}

const handleStartGame = () => {
  const roomId = generateRoomId()
  router.push(`/chat/${roomId}`)
}
</script>

<style scoped>
.first-page-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.title {
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 40px;
  color: #333;
}

.start-button {
  width: 200px;
  height: 60px;
  font-size: 20px;
  border-radius: 30px;
}
</style>
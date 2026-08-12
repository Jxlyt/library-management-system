<template>
  <div class="voice-search-wrapper">
    <el-button
      :type="isListening ? 'danger' : 'default'"
      :icon="isListening ? 'Microphone' : 'Microphone'"
      circle
      size="small"
      :class="{ listening: isListening }"
      @click="toggleListening"
      :title="isListening ? '正在聆听...' : '语音搜索'"
    />
    <div v-if="isListening" class="listening-indicator">
      <div class="pulse-ring"></div>
      <span>{{ transcript || '正在聆听...' }}</span>
    </div>
    <div v-if="result" class="voice-result">
      {{ result }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['search', 'command'])
const isListening = ref(false)
const transcript = ref('')
const result = ref('')
let recognition = null

function toggleListening() {
  if (isListening.value) {
    stopListening()
  } else {
    startListening()
  }
}

function startListening() {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    result.value = '浏览器不支持语音识别'
    setTimeout(() => result.value = '', 3000)
    return
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = true

  recognition.onstart = () => { isListening.value = true; transcript.value = '' }
  recognition.onresult = (event) => {
    transcript.value = event.results[0][0].transcript
    if (event.results[0].isFinal) {
      processCommand(transcript.value)
    }
  }
  recognition.onerror = () => { isListening.value = false }
  recognition.onend = () => { isListening.value = false }

  recognition.start()
}

function stopListening() {
  if (recognition) recognition.stop()
  isListening.value = false
}

function processCommand(text) {
  const t = text.trim()
  // 指令识别
  if (t.includes('找') || t.includes('搜索') || t.includes('帮')) {
    const keyword = t.replace(/找|搜索|帮我|一下|搜|查找/g, '').trim()
    if (keyword) {
      result.value = `正在搜索：${keyword}`
      emit('search', keyword)
    }
  } else if (t.includes('借') || t.includes('借阅')) {
    result.value = '正在跳转到借阅页面...'
    emit('command', 'borrow')
  } else if (t.includes('推荐') || t.includes('今日')) {
    result.value = '正在刷新推荐...'
    emit('command', 'recommend')
  } else if (t.includes('盲盒') || t.includes('福袋')) {
    result.value = '正在跳转到盲盒...'
    emit('command', 'blindbox')
  } else if (t.includes('拍卖')) {
    result.value = '正在跳转到拍卖...'
    emit('command', 'auction')
  } else {
    result.value = `正在搜索：${t}`
    emit('search', t)
  }
  setTimeout(() => result.value = '', 3000)
}
</script>

<style scoped>
.voice-search-wrapper { position: relative; display: inline-flex; align-items: center; }
.voice-search-wrapper .el-button.listening { animation: pulse 1.5s infinite; box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.5); }
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.5); } 70% { box-shadow: 0 0 0 12px rgba(245, 108, 108, 0); } 100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0); } }
.listening-indicator { position: absolute; top: 50%; left: 44px; transform: translateY(-50%); display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.pulse-ring { width: 10px; height: 10px; border-radius: 50%; background: #f56c6c; animation: ringPulse 1s infinite; }
@keyframes ringPulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.voice-result { position: absolute; top: 100%; left: 0; margin-top: 8px; background: #303133; color: #fff; padding: 8px 16px; border-radius: 20px; font-size: 14px; white-space: nowrap; z-index: 100; }
</style>
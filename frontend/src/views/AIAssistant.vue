<template>
  <div class="ai-assistant-page">
    <el-card shadow="hover" class="chat-card">
      <template #header>
        <div class="chat-header">
          <div class="chat-title">
            <el-icon :size="22" color="#409eff"><Service /></el-icon>
            <span>馆长AI助手</span>
          </div>
          <el-tag type="success" size="small">在线</el-tag>
        </div>
      </template>

      <div class="chat-body" ref="chatBodyRef">
        <div class="chat-messages">
          <!-- 欢迎消息 -->
          <div class="message assistant">
            <div class="msg-avatar">
              <el-icon :size="18" color="#fff"><Service /></el-icon>
            </div>
            <div class="msg-bubble">
              <p>你好！我是图书馆馆长AI助手，有什么可以帮你的吗？</p>
              <p class="msg-hint">你可以问我：开放时间、借阅规则、如何预约、如何续借、借阅数量、逾期处理等</p>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
            <div class="msg-avatar" v-if="msg.role === 'assistant'">
              <el-icon :size="18" color="#fff"><Service /></el-icon>
            </div>
            <div class="msg-bubble" v-html="msg.content"></div>
            <div class="msg-avatar" v-if="msg.role === 'user'">
              <el-icon :size="18" color="#fff"><User /></el-icon>
            </div>
          </div>

          <div v-if="typing" class="message assistant">
            <div class="msg-avatar">
              <el-icon :size="18" color="#fff"><Service /></el-icon>
            </div>
            <div class="msg-bubble typing">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <div class="quick-questions">
          <el-tag
            v-for="q in quickQuestions"
            :key="q"
            class="quick-tag"
            @click="sendQuick(q)"
            type="info"
          >
            {{ q }}
          </el-tag>
        </div>
        <div class="input-row">
          <el-input
            v-model="inputText"
            placeholder="输入你的问题..."
            @keyup.enter="sendMessage"
            :disabled="typing"
            clearable
          />
          <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim() || typing">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { Service, User, Promotion } from '@element-plus/icons-vue'

const messages = ref([])
const inputText = ref('')
const typing = ref(false)
const chatBodyRef = ref(null)

const quickQuestions = [
  '开放时间',
  '借阅规则',
  '如何预约',
  '如何续借',
  '借阅数量',
  '逾期处理',
  '如何收藏',
  '如何打卡'
]

const qaDatabase = {
  '开放时间': '图书馆开放时间为：<br><strong>周一至周五：</strong>8:00 - 22:00<br><strong>周六至周日：</strong>9:00 - 18:00<br><strong>法定节假日：</strong>10:00 - 16:00<br><br>如有临时调整，请关注公告栏通知。',
  '借阅规则': '借阅规则如下：<br>1. 每位用户最多同时借阅 <strong>2本</strong> 图书<br>2. 借阅期限为 <strong>30天</strong>，可续借一次（延长15天）<br>3. 借阅时需保证账户状态正常<br>4. 归还时请确保图书完好无损<br><br>如有疑问，请联系管理员。',
  '如何预约': '预约图书的步骤：<br>1. 在首页或图书管理页面找到需要预约的图书<br>2. 如果图书库存为 <strong>0</strong>，会出现"预约"按钮<br>3. 点击"预约"按钮，确认预约信息<br>4. 预约成功后，当图书归还时系统会通知您<br>5. 可在"借阅管理 → 我的预约"中查看预约状态<br><br>注意：预约后请及时借阅，逾期未借将自动取消。',
  '如何续借': '续借图书的步骤：<br>1. 进入"借阅管理"页面<br>2. 在"我的借阅"标签页找到当前借阅记录<br>3. 点击"续借"按钮<br>4. 每本书只能续借一次，延长 <strong>15天</strong><br><br>注：如果图书已被他人预约，则无法续借。',
  '借阅数量': '借阅数量限制：<br>每位用户最多同时借阅 <strong>2本</strong> 图书。<br><br>如果您已借满2本，需要先归还其中一本才能继续借阅新书。',
  '逾期处理': '逾期处理规则：<br>1. 逾期未归还的图书，账户将被标记<br>2. 逾期期间无法借阅新书<br>3. 请尽快归还逾期图书<br>4. 归还后账户将恢复正常<br><br>建议在到期前及时续借或归还，避免影响正常使用。',
  '如何收藏': '收藏图书的步骤：<br>1. 在首页或图书列表中，找到喜欢的图书<br>2. 点击图书卡片上的 <strong>"收藏"</strong> 按钮<br>3. 收藏后可在"我的收藏"页面查看<br>4. 再次点击可取消收藏<br><br>收藏功能帮助你快速找到心仪的图书！',
  '如何打卡': '阅读打卡的步骤：<br>1. 进入"个人中心"页面<br>2. 在左侧"阅读打卡"卡片中点击 <strong>"立即打卡"</strong><br>3. 每天仅可打卡一次<br>4. 连续打卡可积累连续天数<br><br>坚持每天阅读，养成好习惯！',
  '图书评分': '图书评分的步骤：<br>1. 在首页点击图书卡片，进入详情页<br>2. 在评价区域选择 <strong>1-5星</strong> 评分<br>3. 填写简短评论（可选）<br>4. 点击"提交评价"<br><br>注意：需要先借阅该书才能评价哦！',
  '盲盒': '"今天读什么"是图书盲盒功能：<br>1. 在首页搜索栏右侧找到 <strong>"今天读什么"</strong> 按钮<br>2. 点击后系统会随机推荐一本图书<br>3. 如果喜欢可以立即借阅<br>4. 不喜欢可以"再抽一本"<br><br>帮助有阅读选择困难症的你发现好书！',
  '公告': '公告栏位于首页顶部，会滚动展示管理员发布的最新通知，包括：<br>• 图书馆开放时间调整<br>• 新书上架通知<br>• 活动公告<br>• 系统维护通知<br><br>请关注公告栏获取最新信息。',
  'default': '您好！我是图书馆馆长AI助手，可以回答以下问题：<br><br>• 开放时间、借阅规则<br>• 如何预约、续借图书<br>• 借阅数量限制、逾期处理<br>• 如何收藏、打卡、评分<br>• 图书盲盒功能<br>• 公告栏信息<br><br>请点击下方快捷问题或直接输入您的问题。'
}

function findAnswer(question) {
  const q = question.toLowerCase()
  for (const [key, answer] of Object.entries(qaDatabase)) {
    if (key === 'default') continue
    if (q.includes(key.toLowerCase())) {
      return answer
    }
  }
  return qaDatabase.default
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || typing.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  typing.value = true
  // 模拟AI思考延迟
  await new Promise(r => setTimeout(r, 600 + Math.random() * 800))
  typing.value = false

  const answer = findAnswer(text)
  messages.value.push({ role: 'assistant', content: answer })
  scrollToBottom()
}

function sendQuick(question) {
  inputText.value = question
  sendMessage()
}
</script>

<style scoped>
.ai-assistant-page {
  display: flex;
  justify-content: center;
  height: calc(100vh - 120px);
}

.chat-card {
  width: 100%;
  max-width: 700px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f5f7fa;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.assistant .msg-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.message.user .msg-avatar {
  background: linear-gradient(135deg, #409eff, #36d1dc);
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
}

.message.assistant .msg-bubble {
  background: #fff;
  color: #303133;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border-top-left-radius: 4px;
}

.message.user .msg-bubble {
  background: linear-gradient(135deg, #409eff, #36d1dc);
  color: #fff;
  border-top-right-radius: 4px;
}

.msg-bubble p {
  margin: 0;
}

.msg-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.msg-bubble.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 20px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: dot-bounce 1.4s infinite;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-6px); opacity: 1; }
}

.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.input-row {
  display: flex;
  gap: 10px;
}
</style>
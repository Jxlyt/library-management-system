<template>
  <div class="messages-page">
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="24" color="#409eff"><ChatDotRound /></el-icon>
        <span>私信</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="newChatVisible = true">新对话</el-button>
    </div>

    <div class="messages-container">
      <!-- 会话列表 -->
      <div class="conversations-sidebar">
        <div class="sidebar-title">会话列表</div>
        <div v-loading="convLoading" class="conv-list">
          <el-empty v-if="!convLoading && conversations.length === 0" description="暂无会话" />
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: activeChat === conv.otherUsername }"
            @click="openChat(conv)"
          >
            <el-avatar :size="40" class="conv-avatar">{{ (conv.otherNickname || conv.otherUsername || '?').charAt(0) }}</el-avatar>
            <div class="conv-info">
              <div class="conv-name">
                {{ conv.otherNickname || conv.otherUsername || '匿名' }}
                <el-badge v-if="conv.unread > 0" :value="conv.unread" class="unread-badge" />
              </div>
              <div class="conv-preview">{{ conv.lastContent?.substring(0, 30) || '' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天区域 -->
      <div class="chat-area">
        <div v-if="!activeChat" class="chat-placeholder">
          <el-icon :size="64" color="#dcdfe6"><ChatDotRound /></el-icon>
          <p>选择一个会话开始聊天</p>
        </div>
        <div v-else class="chat-content">
          <div class="chat-header">
            <el-avatar :size="36">{{ activeChat.charAt(0) }}</el-avatar>
            <span class="chat-username">{{ activeChat }}</span>
          </div>
          <div class="chat-messages" ref="chatMsgs" v-loading="msgLoading">
            <el-empty v-if="!msgLoading && messages.length === 0" description="暂无消息，发送第一条消息吧" />
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="msg-item"
              :class="{ 'msg-mine': msg.sender?.username === currentUser }"
            >
              <el-avatar :size="32" class="msg-avatar">{{ (msg.sender?.nickname || msg.sender?.username || '?').charAt(0) }}</el-avatar>
              <div class="msg-bubble">
                <div class="msg-text">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="newMessage"
              type="textarea"
              :rows="2"
              placeholder="输入消息..."
              @keyup.enter.ctrl="sendMsg"
            />
            <el-button type="primary" :icon="Promotion" @click="sendMsg" :loading="sendLoading" style="margin-top:8px">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新对话弹窗 -->
    <el-dialog v-model="newChatVisible" title="发起新对话" width="400px">
      <el-form label-width="80px">
        <el-form-item label="对方用户名">
          <el-input v-model="targetUser" placeholder="请输入对方用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newChatVisible = false">取消</el-button>
        <el-button type="primary" @click="startNewChat">开始对话</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getConversations, getConversation, sendMessage, markAsRead } from '../api/community'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Plus, Promotion } from '@element-plus/icons-vue'

const currentUser = ref(localStorage.getItem('username') || '')

const convLoading = ref(false)
const conversations = ref([])
const activeChat = ref(null)
const msgLoading = ref(false)
const messages = ref([])
const newMessage = ref('')
const sendLoading = ref(false)
const chatMsgs = ref(null)
const newChatVisible = ref(false)
const targetUser = ref('')

async function fetchConversations() {
  convLoading.value = true
  try {
    const res = await getConversations({ page: 0, size: 50 })
    const list = (res.data?.content || []).map(m => {
      const isSent = m.sender?.username === currentUser.value
      const other = isSent ? m.receiver : m.sender
      return {
        id: m.id,
        otherUsername: other?.username,
        otherNickname: other?.nickname,
        lastContent: m.content,
        lastTime: m.createdAt,
        unread: !isSent && !m.isRead ? 1 : 0
      }
    })
    // 合并相同用户
    const merged = []
    const seen = new Set()
    list.forEach(item => {
      if (!seen.has(item.otherUsername)) {
        seen.add(item.otherUsername)
        const dups = list.filter(l => l.otherUsername === item.otherUsername)
        item.unread = dups.filter(d => d.unread > 0).length
        merged.push(item)
      }
    })
    conversations.value = merged
  } catch (e) { /* handled */ } finally {
    convLoading.value = false
  }
}

async function openChat(conv) {
  activeChat.value = conv.otherUsername
  msgLoading.value = true
  try {
    const res = await getConversation(conv.otherUsername, { page: 0, size: 50 })
    messages.value = (res.data?.content || []).reverse()
    await nextTick()
    scrollToBottom()
    // 标记已读
    await markAsRead(conv.otherUsername)
    // 刷新会话列表（消除红点）
    fetchConversations()
  } catch (e) { /* handled */ } finally {
    msgLoading.value = false
  }
}

async function sendMsg() {
  if (!newMessage.value.trim()) return
  sendLoading.value = true
  try {
    await sendMessage({ receiver: activeChat.value, content: newMessage.value.trim() })
    messages.value.push({
      id: Date.now(),
      sender: { username: currentUser.value },
      content: newMessage.value.trim(),
      createdAt: new Date().toISOString()
    })
    newMessage.value = ''
    await nextTick()
    scrollToBottom()
    fetchConversations()
  } catch (e) { /* handled */ } finally {
    sendLoading.value = false
  }
}

function scrollToBottom() {
  if (chatMsgs.value) {
    chatMsgs.value.scrollTop = chatMsgs.value.scrollHeight
  }
}

function startNewChat() {
  if (!targetUser.value.trim()) {
    ElMessage.warning('请输入对方用户名')
    return
  }
  newChatVisible.value = false
  activeChat.value = targetUser.value.trim()
  messages.value = []
  targetUser.value = ''
}

function formatTime(date) {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchConversations()
})
</script>

<style scoped>
.messages-page { background: #fff; border-radius: 12px; overflow: hidden; }
.page-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #ebeef5; }
.page-title { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 600; }

.messages-container { display: flex; height: calc(100vh - 180px); }
.conversations-sidebar { width: 280px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.sidebar-title { padding: 16px; font-weight: 600; color: #606266; border-bottom: 1px solid #ebeef5; }
.conv-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; cursor: pointer; border-bottom: 1px solid #f5f7fa; }
.conv-item:hover { background: #f5f7fa; }
.conv-item.active { background: #ecf5ff; }
.conv-avatar { flex-shrink: 0; }
.conv-info { flex: 1; overflow: hidden; }
.conv-name { font-weight: 500; font-size: 14px; display: flex; align-items: center; gap: 6px; }
.unread-badge { flex-shrink: 0; }
.conv-preview { font-size: 12px; color: #909399; margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.chat-area { flex: 1; display: flex; flex-direction: column; }
.chat-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #c0c4cc; }
.chat-placeholder p { margin-top: 16px; font-size: 14px; }
.chat-content { flex: 1; display: flex; flex-direction: column; }
.chat-header { display: flex; align-items: center; gap: 12px; padding: 12px 20px; border-bottom: 1px solid #ebeef5; }
.chat-username { font-weight: 600; font-size: 16px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; }
.msg-item { display: flex; gap: 10px; margin-bottom: 16px; }
.msg-item.msg-mine { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.msg-bubble { max-width: 60%; background: #f5f7fa; border-radius: 12px; padding: 10px 14px; }
.msg-mine .msg-bubble { background: #ecf5ff; }
.msg-text { font-size: 14px; line-height: 1.6; word-break: break-word; }
.msg-time { font-size: 11px; color: #c0c4cc; margin-top: 4px; text-align: right; }
.chat-input { padding: 12px 20px; border-top: 1px solid #ebeef5; }
</style>
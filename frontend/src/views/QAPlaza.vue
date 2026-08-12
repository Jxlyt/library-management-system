<template>
  <div class="qa-plaza">
    <div class="qa-header">
      <div class="qa-title">
        <el-icon :size="24" color="#409eff"><ChatDotRound /></el-icon>
        <span>问答广场</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">发布问题</el-button>
    </div>

    <div v-loading="loading" class="qa-list">
      <el-empty v-if="!loading && questions.length === 0" description="暂无问题，快来发布第一个吧！" />
      <div
        v-for="q in questions"
        :key="q.id"
        class="qa-card"
        @click="openDetailDialog(q)"
      >
        <div class="qa-card-body">
          <h3 class="qa-card-title">{{ q.title }}</h3>
          <p class="qa-card-preview">{{ q.content?.substring(0, 120) }}{{ q.content?.length > 120 ? '...' : '' }}</p>
          <div class="qa-card-meta">
            <div class="qa-card-author">
              <el-avatar :size="24" class="qa-avatar">{{ (q.user?.nickname || q.user?.username || '?').charAt(0) }}</el-avatar>
              <span>{{ q.user?.nickname || q.user?.username || '匿名' }}</span>
            </div>
            <div class="qa-card-stats">
              <span class="qa-stat"><el-icon :size="14"><ChatDotRound /></el-icon> {{ q.answerCount || 0 }} 回答</span>
              <span class="qa-stat"><el-icon :size="14"><el-icon><svg viewBox="0 0 1024 1024" width="14" height="14"><path d="M512 160c-212.1 0-384 144.5-384 322.7 0 104.5 58.3 199.1 153.5 253.8-10.5 43.5-33.2 78.4-31.1 80.6 4.2 4.3 76.4-22.8 139.6-59.6 37.4 12.1 78.8 18.8 122 18.8 212.1 0 384-144.5 384-322.7S724.1 160 512 160z" fill="currentColor"/></svg></el-icon></el-icon> {{ q.viewCount || 0 }} 浏览</span>
              <span class="qa-stat qa-time">{{ formatTime(q.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="qa-card-right" v-if="q.answerCount > 0">
          <div class="qa-answer-count">{{ q.answerCount }}</div>
          <div class="qa-answer-label">回答</div>
        </div>
      </div>
    </div>

    <div class="qa-pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchQuestions"
        background
      />
    </div>

    <!-- 发布问题弹窗 -->
    <el-dialog v-model="createVisible" title="发布问题" width="560px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="0">
        <el-form-item>
          <el-input v-model="createForm.title" placeholder="请输入问题标题（必填）" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="6"
            placeholder="请详细描述你的问题..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuestion" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>

    <!-- 问题详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailQuestion?.title" width="720px" :close-on-click-modal="false" @close="closeDetail">
      <div v-if="detailQuestion" class="detail-wrap">
        <div class="detail-question">
          <div class="detail-question-header">
            <el-avatar :size="40" class="qa-avatar">{{ (detailQuestion.user?.nickname || detailQuestion.user?.username || '?').charAt(0) }}</el-avatar>
            <div class="detail-author-info">
              <span class="detail-author-name">{{ detailQuestion.user?.nickname || detailQuestion.user?.username || '匿名' }}</span>
              <span class="detail-time">{{ formatTime(detailQuestion.createdAt) }}</span>
            </div>
            <el-button
              v-if="isOwner(detailQuestion)"
              type="danger"
              size="small"
              text
              @click="handleDeleteQuestion(detailQuestion.id)"
              :loading="deleting"
            >
              删除
            </el-button>
          </div>
          <div class="detail-question-content">{{ detailQuestion.content }}</div>
        </div>

        <el-divider>{{ answers.length }} 个回答</el-divider>

        <div v-if="answers.length === 0 && !answersLoading" class="no-answers">
          <el-empty description="暂无回答，来写第一个回答吧" :image-size="80" />
        </div>

        <div
          v-for="a in answers"
          :key="a.id"
          class="answer-card"
          :class="{ 'best-answer': a.isAccepted }"
        >
          <div class="answer-badge" v-if="a.isAccepted">
            <el-icon :size="16" color="#67c23a"><Trophy /></el-icon>
            <span>最佳答案</span>
          </div>
          <div class="answer-header">
            <el-avatar :size="32" class="qa-avatar">{{ (a.user?.nickname || a.user?.username || '?').charAt(0) }}</el-avatar>
            <div class="answer-author-info">
              <span class="answer-author-name">{{ a.user?.nickname || a.user?.username || '匿名' }}</span>
              <span class="answer-time">{{ formatTime(a.createdAt) }}</span>
            </div>
          </div>
          <div class="answer-content">{{ a.content }}</div>
          <div class="answer-actions">
            <el-button
              :type="a.isLiked ? 'primary' : 'default'"
              size="small"
              :icon="ThumbsUp"
              @click="handleLikeAnswer(a)"
              :loading="a.likeLoading"
              text
            >
              {{ a.likeCount || 0 }}
            </el-button>
            <el-button
              v-if="isOwner(detailQuestion) && !a.isAccepted"
              type="success"
              size="small"
              @click="handleAcceptAnswer(a)"
              :loading="a.acceptLoading"
              text
            >
              <el-icon :size="14"><Trophy /></el-icon>
              采纳
            </el-button>
          </div>
        </div>

        <el-divider>写回答</el-divider>
        <div class="answer-form">
          <el-input
            v-model="answerForm.content"
            type="textarea"
            :rows="3"
            placeholder="写下你的回答..."
            maxlength="2000"
            show-word-limit
          />
          <el-button
            type="primary"
            @click="submitAnswer"
            :loading="answerSubmitting"
            style="margin-top: 12px"
          >
            提交回答
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChatDotRound, Trophy, ThumbsUp } from '@element-plus/icons-vue'
import {
  getQuestions,
  getQuestion,
  createQuestion,
  deleteQuestion,
  createAnswer,
  getAnswers,
  acceptAnswer,
  likeAnswer
} from '../api/community'

const loading = ref(false)
const questions = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createVisible = ref(false)
const createForm = ref({ title: '', content: '' })
const submitting = ref(false)

const detailVisible = ref(false)
const detailQuestion = ref(null)
const answers = ref([])
const answersLoading = ref(false)
const answerForm = ref({ content: '' })
const answerSubmitting = ref(false)
const deleting = ref(false)

const currentUsername = ref(localStorage.getItem('username') || '')

function formatTime(date) {
  if (!date) return '-'
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + ' 天前'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function isOwner(item) {
  if (!currentUsername.value) return false
  const itemUsername = item.user?.username
  return itemUsername === currentUsername.value
}

async function fetchQuestions() {
  loading.value = true
  try {
    const res = await getQuestions({ page: page.value - 1, size: pageSize.value })
    questions.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  createForm.value = { title: '', content: '' }
  createVisible.value = true
}

async function submitQuestion() {
  if (!createForm.value.title.trim()) {
    ElMessage.warning('请输入问题标题')
    return
  }
  submitting.value = true
  try {
    await createQuestion({ title: createForm.value.title, content: createForm.value.content })
    ElMessage.success('问题发布成功')
    createVisible.value = false
    page.value = 1
    fetchQuestions()
  } catch (e) {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function openDetailDialog(question) {
  detailQuestion.value = question
  detailVisible.value = true
  answers.value = []
  answerForm.value = { content: '' }
  await fetchQuestionDetail(question.id)
  await fetchAnswersList(question.id)
}

async function fetchQuestionDetail(id) {
  try {
    const res = await getQuestion(id)
    detailQuestion.value = res.data
  } catch (e) {
    // handled
  }
}

async function fetchAnswersList(questionId) {
  answersLoading.value = true
  try {
    const res = await getAnswers(questionId, { page: 0, size: 50 })
    answers.value = (res.data.content || []).map(a => ({ ...a, likeLoading: false, acceptLoading: false }))
  } catch (e) {
    // handled
  } finally {
    answersLoading.value = false
  }
}

async function handleDeleteQuestion(id) {
  try {
    await ElMessageBox.confirm(
      `确定要删除问题「${detailQuestion.value?.title || '此问题'}」吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning', center: true }
    )
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteQuestion(id)
    ElMessage.success('问题已删除')
    detailVisible.value = false
    fetchQuestions()
  } catch (e) {
    // handled
  } finally {
    deleting.value = false
  }
}

async function submitAnswer() {
  if (!answerForm.value.content.trim()) {
    ElMessage.warning('请输入回答内容')
    return
  }
  answerSubmitting.value = true
  try {
    await createAnswer(detailQuestion.value.id, { content: answerForm.value.content })
    ElMessage.success('回答提交成功')
    answerForm.value = { content: '' }
    fetchAnswersList(detailQuestion.value.id)
    fetchQuestions()
  } catch (e) {
    // handled
  } finally {
    answerSubmitting.value = false
  }
}

async function handleLikeAnswer(answer) {
  if (answer.likeLoading) return
  answer.likeLoading = true
  try {
    await likeAnswer(answer.id)
    answer.isLiked = !answer.isLiked
    answer.likeCount = (answer.likeCount || 0) + (answer.isLiked ? 1 : -1)
  } catch (e) {
    // handled
  } finally {
    answer.likeLoading = false
  }
}

async function handleAcceptAnswer(answer) {
  if (answer.acceptLoading) return
  answer.acceptLoading = true
  try {
    await acceptAnswer(answer.id, { questionId: detailQuestion.value.id })
    ElMessage.success('已采纳为最佳答案')
    answers.value.forEach(a => { a.isAccepted = a.id === answer.id })
  } catch (e) {
    // handled
  } finally {
    answer.acceptLoading = false
  }
}

function closeDetail() {
  detailQuestion.value = null
  answers.value = []
}

onMounted(() => {
  fetchQuestions()
})
</script>

<style scoped>
.qa-plaza {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.qa-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.qa-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.qa-list {
  min-height: 300px;
}

.qa-card {
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 12px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
}

.qa-card:hover {
  border-color: #d0d0d0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.qa-card-body {
  flex: 1;
  min-width: 0;
}

.qa-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-card-preview {
  font-size: 14px;
  color: #909399;
  margin: 0 0 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.qa-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.qa-card-author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.qa-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.qa-card-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.qa-stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.qa-time {
  color: #c0c4cc;
}

.qa-card-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 64px;
  margin-left: 20px;
  background: #f5f7fa;
  border-radius: 10px;
  flex-shrink: 0;
}

.qa-answer-count {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}

.qa-answer-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.qa-pagination {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
}

.detail-question {
  padding: 0 0 8px;
}

.detail-question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-author-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-author-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.detail-time {
  font-size: 12px;
  color: #c0c4cc;
}

.detail-question-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.no-answers {
  padding: 24px 0;
}

.answer-card {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 8px;
  transition: border-color 0.3s;
}

.answer-card:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.answer-card.best-answer {
  border: 2px solid #67c23a;
  background: #f0f9eb;
  border-radius: 10px;
  padding: 16px;
}

.answer-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #67c23a;
  margin-bottom: 10px;
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.answer-author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.answer-author-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.answer-time {
  font-size: 12px;
  color: #c0c4cc;
}

.answer-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 10px;
}

.answer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-form {
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  .qa-card {
    flex-direction: column;
  }
  .qa-card-right {
    flex-direction: row;
    width: 100%;
    margin-left: 0;
    margin-top: 12px;
    padding: 8px 12px;
    gap: 6px;
    justify-content: center;
  }
  .qa-card-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
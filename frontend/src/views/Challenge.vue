<template>
  <div class="challenge-page">
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="24" color="#409eff"><Flag /></el-icon>
        <span>阅读挑战</span>
      </div>
      <el-button v-if="isAdmin" type="primary" :icon="Plus" @click="openCreateDialog">
        创建挑战
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="challenge-tabs">
      <el-tab-pane label="进行中的活动" name="active">
        <div v-loading="loading" class="challenge-grid">
          <el-empty v-if="!loading && challenges.length === 0" description="暂无进行中的挑战活动" />
          <el-card
            v-for="c in challenges"
            :key="c.id"
            shadow="hover"
            class="challenge-card"
          >
            <div class="card-banner" :style="{ background: bannerColors[c.id % bannerColors.length] }">
              <el-icon :size="36" color="rgba(255,255,255,0.9)"><Flag /></el-icon>
              <span class="banner-label">阅读挑战</span>
            </div>
            <div class="card-body">
              <h3 class="card-title">{{ c.title }}</h3>
              <p class="card-desc">{{ c.description }}</p>
              <div class="card-meta">
                <div class="meta-item">
                  <el-icon :size="14"><Calendar /></el-icon>
                  <span>{{ formatDate(c.startDate) }} ~ {{ formatDate(c.endDate) }}</span>
                </div>
                <div class="meta-item">
                  <el-icon :size="14"><UserFilled /></el-icon>
                  <span>{{ c.participantCount || 0 }} 人参与</span>
                </div>
              </div>
              <div class="progress-section">
                <span class="progress-label">活动进度</span>
                <el-progress
                  :percentage="calcProgress(c.startDate, c.endDate)"
                  :stroke-width="8"
                  :color="progressColor"
                />
              </div>
              <div class="card-actions">
                <el-button
                  type="primary"
                  :icon="Plus"
                  :disabled="c.joined"
                  @click="handleJoin(c)"
                  :loading="c.joining"
                >
                  {{ c.joined ? '已参与' : '参与挑战' }}
                </el-button>
                <el-button
                  v-if="isAdmin"
                  type="danger"
                  plain
                  :icon="Delete"
                  @click="handleDelete(c)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的活动" name="my">
        <div v-loading="myLoading" class="my-challenges">
          <el-empty v-if="!myLoading && myChallenges.length === 0" description="你还没有参与任何挑战" />
          <div v-for="mc in myChallenges" :key="mc.id" class="my-challenge-item">
            <el-card shadow="hover" class="my-card">
              <div class="my-card-header">
                <div class="my-card-banner" :style="{ background: bannerColors[mc.id % bannerColors.length] }">
                  <el-icon :size="28" color="rgba(255,255,255,0.9)"><Flag /></el-icon>
                </div>
                <div class="my-card-title">
                  <h3>{{ mc.title }}</h3>
                  <p class="my-card-desc">{{ mc.description }}</p>
                  <div class="my-card-meta">
                    <el-icon :size="14"><Calendar /></el-icon>
                    <span>{{ formatDate(mc.startDate) }} ~ {{ formatDate(mc.endDate) }}</span>
                  </div>
                </div>
              </div>
              <div class="my-progress">
                <span class="progress-label">打卡进度 ({{ mc.progress || 0 }}天)</span>
                <el-progress
                  :percentage="calcMyProgress(mc)"
                  :stroke-width="10"
                  :color="progressColor"
                />
              </div>
              <div class="my-actions">
                <el-button
                  type="primary"
                  :icon="Check"
                  @click="openCheckInDialog(mc)"
                  :disabled="mc.checkedInToday"
                >
                  {{ mc.checkedInToday ? '今日已打卡' : '每日打卡' }}
                </el-button>
                <el-button
                  type="info"
                  plain
                  :icon="List"
                  @click="openCheckInHistory(mc)"
                >
                  打卡记录
                </el-button>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建挑战弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      :title="editingChallenge ? '编辑挑战' : '创建挑战'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="createForm" label-width="100px" class="challenge-form">
        <el-form-item label="挑战标题" required>
          <el-input v-model="createForm.title" placeholder="请输入挑战标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="挑战描述" required>
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入挑战描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="关联图书ID">
          <el-input v-model="createForm.bookId" placeholder="可选，关联图书ID" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="createForm.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="createForm.endDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关联徽章ID">
          <el-input v-model="createForm.badgeId" placeholder="可选，关联徽章ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChallenge" :loading="submitLoading">
          {{ editingChallenge ? '保存修改' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 打卡弹窗 -->
    <el-dialog
      v-model="checkInDialogVisible"
      title="每日打卡"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="checkInForm" label-width="100px">
        <el-form-item label="阅读页数" required>
          <el-input-number
            v-model="checkInForm.pagesRead"
            :min="1"
            :max="9999"
            placeholder="请输入阅读页数"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="阅读笔记">
          <el-input
            v-model="checkInForm.note"
            type="textarea"
            :rows="3"
            placeholder="记录今天的阅读心得（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkInDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCheckIn" :loading="checkInSubmitting">
          确认打卡
        </el-button>
      </template>
    </el-dialog>

    <!-- 打卡记录弹窗 -->
    <el-dialog
      v-model="historyDialogVisible"
      title="打卡记录"
      width="600px"
    >
      <div v-loading="historyLoading">
        <el-empty v-if="!historyLoading && checkInHistory.length === 0" description="暂无打卡记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="item in checkInHistory"
            :key="item.id"
            :timestamp="formatDateTime(item.checkInTime || item.createdAt)"
            placement="top"
            :color="progressColor"
          >
            <el-card shadow="never" size="small">
              <div class="checkin-record">
                <div class="checkin-pages">
                  <el-icon :size="16" color="#409eff"><Reading /></el-icon>
                  <span class="pages-text">阅读 <strong>{{ item.pagesRead }}</strong> 页</span>
                </div>
                <p v-if="item.note" class="checkin-note">{{ item.note }}</p>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="historyDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getChallenges,
  getChallenge,
  joinChallenge,
  challengeCheckIn,
  getMyChallenges,
  getChallengeParticipants,
  createChallenge,
  updateChallenge,
  deleteChallenge,
  getCheckIns
} from '../api/community'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Flag, Plus, Check, Calendar, UserFilled, Delete, List, Reading } from '@element-plus/icons-vue'

const isAdmin = ref(localStorage.getItem('role') === 'ADMIN')

const activeTab = ref('active')
const loading = ref(false)
const myLoading = ref(false)

const challenges = ref([])
const myChallenges = ref([])

const bannerColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
  'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

const progressColor = '#409eff'

// 创建/编辑弹窗
const createDialogVisible = ref(false)
const editingChallenge = ref(null)
const submitLoading = ref(false)
const createForm = ref({
  title: '',
  description: '',
  bookId: '',
  startDate: '',
  endDate: '',
  badgeId: ''
})

// 打卡弹窗
const checkInDialogVisible = ref(false)
const checkInChallenge = ref(null)
const checkInSubmitting = ref(false)
const checkInForm = ref({
  pagesRead: 1,
  note: ''
})

// 打卡记录弹窗
const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const checkInHistory = ref([])

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDateTime(date) {
  if (!date) return '-'
  const d = new Date(date)
  return formatDate(date) + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function calcProgress(startDate, endDate) {
  if (!startDate || !endDate) return 0
  const start = new Date(startDate).getTime()
  const end = new Date(endDate).getTime()
  const now = Date.now()
  if (now <= start) return 0
  if (now >= end) return 100
  const total = end - start
  const elapsed = now - start
  return Math.min(100, Math.round((elapsed / total) * 100))
}

function calcMyProgress(challenge) {
  if (!challenge.startDate || !challenge.endDate) return 0
  const start = new Date(challenge.startDate)
  const end = new Date(challenge.endDate)
  const totalDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
  if (totalDays <= 0) return 0
  const progress = challenge.progress || 0
  return Math.min(100, Math.round((progress / totalDays) * 100))
}

async function fetchChallenges() {
  loading.value = true
  try {
    const res = await getChallenges({ page: 0, size: 50 })
    const list = res.data?.content || []
    // 标记已参与的挑战
    const myRes = await getMyChallenges({ page: 0, size: 50 })
    const myIds = (myRes.data?.content || []).map(p => p.challenge?.id || p.id)
    list.forEach(c => {
      c.joined = myIds.includes(c.id)
      c.joining = false
    })
    challenges.value = list
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

async function fetchMyChallenges() {
  myLoading.value = true
  try {
    const res = await getMyChallenges({ page: 0, size: 50 })
    const list = (res.data?.content || []).map(p => ({
      ...p.challenge,
      participantId: p.id,
      progress: p.progress,
      completed: p.completed,
      checkedInToday: false
    }))
    myChallenges.value = list
  } catch (e) {
    // handled
  } finally {
    myLoading.value = false
  }
}

async function handleJoin(challenge) {
  challenge.joining = true
  try {
    await joinChallenge(challenge.id)
    ElMessage.success('成功参与挑战！')
    challenge.joined = true
  } catch (e) {
    // handled
  } finally {
    challenge.joining = false
  }
}

function openCreateDialog() {
  editingChallenge.value = null
  createForm.value = {
    title: '',
    description: '',
    bookId: '',
    startDate: '',
    endDate: '',
    badgeId: ''
  }
  createDialogVisible.value = true
}

async function submitChallenge() {
  if (!createForm.value.title || !createForm.value.description) {
    ElMessage.warning('请填写挑战标题和描述')
    return
  }
  if (!createForm.value.startDate || !createForm.value.endDate) {
    ElMessage.warning('请选择开始和结束日期')
    return
  }
  submitLoading.value = true
  try {
    const data = {
      title: createForm.value.title,
      description: createForm.value.description,
      startDate: createForm.value.startDate + 'T00:00:00',
      endDate: createForm.value.endDate + 'T00:00:00'
    }
    if (createForm.value.bookId) data.bookId = createForm.value.bookId
    if (createForm.value.badgeId) data.badgeId = createForm.value.badgeId

    if (editingChallenge.value) {
      await updateChallenge(editingChallenge.value.id, data)
      ElMessage.success('挑战已更新')
    } else {
      await createChallenge(data)
      ElMessage.success('挑战创建成功')
    }
    createDialogVisible.value = false
    fetchChallenges()
  } catch (e) {
    // handled
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(challenge) {
  try {
    await ElMessageBox.confirm(
      `确定要删除挑战「${challenge.title}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteChallenge(challenge.id)
    ElMessage.success('挑战已删除')
    fetchChallenges()
  } catch (e) {
    if (e !== 'cancel') {
      // handled
    }
  }
}

function openCheckInDialog(challenge) {
  checkInChallenge.value = challenge
  checkInForm.value = { pagesRead: 1, note: '' }
  checkInDialogVisible.value = true
}

async function submitCheckIn() {
  if (!checkInForm.value.pagesRead || checkInForm.value.pagesRead < 1) {
    ElMessage.warning('请输入阅读页数')
    return
  }
  checkInSubmitting.value = true
  try {
    await challengeCheckIn(checkInChallenge.value.id, {
      pagesRead: checkInForm.value.pagesRead,
      note: checkInForm.value.note
    })
    ElMessage.success('打卡成功！')
    checkInDialogVisible.value = false
    checkInChallenge.value.checkedInToday = true
    checkInChallenge.value.progress = (checkInChallenge.value.progress || 0) + 1
    // 刷新后端数据确保进度同步
    await fetchMyChallenges()
  } catch (e) {
    // handled
  } finally {
    checkInSubmitting.value = false
  }
}

async function openCheckInHistory(challenge) {
  historyDialogVisible.value = true
  historyLoading.value = true
  checkInHistory.value = []
  try {
    // 获取参与者信息以获取 participantId
    const participantsRes = await getChallengeParticipants(challenge.id, { page: 0, size: 50 })
    const participants = participantsRes.data?.content || []
    const username = localStorage.getItem('username')
    const myParticipant = participants.find(p => p.user?.username === username)
    if (myParticipant) {
      const res = await getCheckIns(myParticipant.id, { page: 0, size: 100 })
      checkInHistory.value = res.data?.content || []
    }
  } catch (e) {
    // handled
  } finally {
    historyLoading.value = false
  }
}

function handleTabChange(name) {
  if (name === 'active') {
    fetchChallenges()
  } else if (name === 'my') {
    fetchMyChallenges()
  }
}

onMounted(() => {
  fetchChallenges()
})
</script>

<style scoped>
.challenge-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.challenge-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 0 24px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.challenge-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.challenge-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.challenge-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-banner {
  height: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.banner-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.card-body {
  padding: 6px 0 0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.progress-section {
  margin-bottom: 16px;
}

.progress-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* 我的挑战 */
.my-challenges {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.my-card {
  border-radius: 12px;
  transition: all 0.3s ease;
}

.my-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.my-card-header {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.my-card-banner {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.my-card-title {
  flex: 1;
  min-width: 0;
}

.my-card-title h3 {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px;
}

.my-card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px;
  line-height: 1.5;
}

.my-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.my-progress {
  margin-bottom: 16px;
}

.my-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 打卡记录 */
.checkin-record {
  padding: 4px 0;
}

.checkin-pages {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.pages-text {
  font-size: 14px;
  color: #303133;
}

.pages-text strong {
  color: #409eff;
  font-size: 16px;
}

.checkin-note {
  font-size: 13px;
  color: #606266;
  margin: 6px 0 0;
  line-height: 1.5;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 6px;
}

/* 表单样式 */
.challenge-form {
  padding: 8px 0;
}

@media (max-width: 1200px) {
  .challenge-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .challenge-grid {
    grid-template-columns: 1fr;
  }
  .my-card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
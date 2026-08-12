<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 左侧：个人信息卡片 -->
      <el-col :span="8">
        <el-card shadow="hover" class="profile-card">
          <div class="profile-header">
            <div class="avatar-section">
              <el-avatar :size="80" :src="user.avatar" class="user-avatar">
                <span class="avatar-text">{{ (user.nickname || user.username || '?').charAt(0).toUpperCase() }}</span>
              </el-avatar>
            </div>
            <h3>{{ user.nickname || user.username }}</h3>
            <el-tag :type="user.role === 'ADMIN' ? 'danger' : 'success'">
              {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </div>
          <el-divider />
          <div class="profile-info">
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>用户名：{{ user.username }}</span>
            </div>
            <div class="info-item">
              <el-icon><Phone /></el-icon>
              <span>手机号：{{ user.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span>邮箱：{{ user.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Calendar /></el-icon>
              <span>注册时间：{{ formatDate(user.createdAt) }}</span>
            </div>
          </div>
        </el-card>

        <!-- 阅读打卡 -->
        <el-card shadow="hover" class="checkin-card">
          <div class="checkin-header">
            <el-icon :size="20" color="#409eff"><Sunny /></el-icon>
            <span>阅读打卡</span>
          </div>
          <div class="checkin-stats">
            <div class="stat-item">
              <span class="stat-num">{{ checkInStats.totalCheckIns || 0 }}</span>
              <span class="stat-label">累计打卡</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ checkInStats.currentStreak || 0 }}</span>
              <span class="stat-label">连续打卡</span>
            </div>
          </div>
          <el-button
            type="primary"
            :disabled="checkInStats.todayChecked"
            @click="handleCheckIn"
            :loading="checkingIn"
            class="checkin-btn"
          >
            {{ checkInStats.todayChecked ? '今日已打卡' : '立即打卡' }}
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧：编辑表单 -->
      <el-col :span="16">
        <el-card shadow="hover" class="edit-card">
          <template #header>
            <div class="card-header">
              <el-icon><EditPen /></el-icon>
              <span>编辑个人资料</span>
            </div>
          </template>
          <el-form :model="form" label-width="80px" label-position="left" class="profile-form">
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="50" />
            </el-form-item>
            <el-form-item label="头像URL">
              <el-input v-model="form.avatar" placeholder="请输入头像图片URL" maxlength="500" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="100" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card shadow="hover" class="password-card">
          <template #header>
            <div class="card-header">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </div>
          </template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" label-position="left" class="profile-form">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="handleChangePassword" :loading="pwdChanging">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 我的笔记 -->
        <el-card shadow="hover" class="notes-card">
          <template #header>
            <div class="card-header">
              <el-icon><Notebook /></el-icon>
              <span>我的笔记</span>
            </div>
          </template>
          <div v-loading="notesLoading">
            <el-empty v-if="notes.length === 0" description="暂无笔记" />
            <div v-for="note in notes" :key="note.id" class="note-item">
              <div class="note-header">
                <h4>{{ note.title }}</h4>
                <el-tag size="small" :type="note.isPublic ? 'success' : 'info'">
                  {{ note.isPublic ? '公开' : '私密' }}
                </el-tag>
              </div>
              <p class="note-book">《{{ note.book?.title }}》</p>
              <p class="note-content">{{ note.content?.substring(0, 150) }}{{ note.content?.length > 150 ? '...' : '' }}</p>
              <span class="note-time">{{ formatDate(note.updatedAt) }}</span>
            </div>
            <div class="pagination-wrap" v-if="notesTotal > notesPageSize">
              <el-pagination
                v-model:current-page="notesPage"
                :page-size="notesPageSize"
                :total="notesTotal"
                layout="prev, pager, next"
                @current-change="fetchNotes"
                small
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { User, Phone, Message, Calendar, Sunny, EditPen, Notebook, Lock } from '@element-plus/icons-vue'
import { getProfile, updateProfile, changePassword } from '../api/stats'
import { checkIn, getCheckInStats, getMyNotes } from '../api/social'
import { ElMessage } from 'element-plus'

const user = ref({})
const saving = ref(false)
const checkingIn = ref(false)

const form = reactive({
  nickname: '',
  avatar: '',
  phone: '',
  email: ''
})

const checkInStats = ref({
  totalCheckIns: 0,
  currentStreak: 0,
  todayChecked: false
})

const notes = ref([])
const notesLoading = ref(false)
const notesPage = ref(1)
const notesPageSize = ref(5)
const notesTotal = ref(0)

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function fetchProfile() {
  try {
    const res = await getProfile()
    user.value = res.data
    form.nickname = res.data.nickname || ''
    form.avatar = res.data.avatar || ''
    form.phone = res.data.phone || ''
    form.email = res.data.email || ''
  } catch (e) {
    // handled
  }
}

async function fetchCheckInStats() {
  try {
    const res = await getCheckInStats()
    checkInStats.value = res.data
  } catch (e) {
    // handled
  }
}

async function fetchNotes() {
  notesLoading.value = true
  try {
    const res = await getMyNotes({ page: notesPage.value - 1, size: notesPageSize.value })
    notes.value = res.data.content
    notesTotal.value = res.data.totalElements
  } catch (e) {
    // handled
  } finally {
    notesLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      avatar: form.avatar,
      phone: form.phone,
      email: form.email
    })
    ElMessage.success('个人信息更新成功')
    fetchProfile()
  } catch (e) {
    // handled
  } finally {
    saving.value = false
  }
}

async function handleCheckIn() {
  checkingIn.value = true
  try {
    await checkIn()
    ElMessage.success('打卡成功！')
    fetchCheckInStats()
  } catch (e) {
    // handled
  } finally {
    checkingIn.value = false
  }
}

const pwdFormRef = ref(null)
const pwdChanging = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdChanging.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    // 清除登录状态，跳转登录页
    setTimeout(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      window.location.href = '/login'
    }, 1500)
  } catch (e) {
    // handled
  } finally {
    pwdChanging.value = false
  }
}

onMounted(() => {
  fetchProfile()
  fetchCheckInStats()
  fetchNotes()
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card, .checkin-card, .edit-card, .notes-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.profile-header {
  text-align: center;
  padding: 8px 0;
}

.avatar-section {
  margin-bottom: 12px;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.avatar-text {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
}

.profile-header h3 {
  margin: 8px 0 8px;
  font-size: 18px;
  color: #303133;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.info-item .el-icon {
  color: #909399;
}

.checkin-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.checkin-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.checkin-btn {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.profile-form {
  max-width: 500px;
}

.note-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.note-item:last-child {
  border-bottom: none;
}

.note-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.note-header h4 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}

.note-book {
  font-size: 13px;
  color: #409eff;
  margin: 4px 0;
}

.note-content {
  font-size: 13px;
  color: #606266;
  margin: 4px 0;
  line-height: 1.5;
}

.note-time {
  font-size: 12px;
  color: #c0c4cc;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
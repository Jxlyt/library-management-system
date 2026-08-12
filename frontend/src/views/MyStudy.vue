<template>
  <div class="mystudy-page">
    <!-- 用户资料头部 -->
    <el-card shadow="hover" class="profile-header-card">
      <div class="profile-header">
        <div class="profile-left">
          <el-avatar :size="80" :src="currentUser.avatar" class="user-avatar">
            <span class="avatar-text">{{ firstChar }}</span>
          </el-avatar>
          <div class="profile-name-role">
            <h2>{{ currentUser.nickname || currentUser.username }}</h2>
            <span class="profile-username">@{{ currentUser.username }}</span>
          </div>
        </div>
        <div class="profile-right">
          <div class="follow-stat" @click="handleViewFollowers">
            <span class="follow-num">{{ followStats.followerCount || 0 }}</span>
            <span class="follow-label">粉丝</span>
          </div>
          <el-divider direction="vertical" />
          <div class="follow-stat" @click="handleViewFollowing">
            <span class="follow-num">{{ followStats.followingCount || 0 }}</span>
            <span class="follow-label">关注</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 统计行 + 打卡按钮 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon streak-icon">
            <el-icon :size="24"><Calendar /></el-icon>
          </div>
          <div class="stat-value">{{ checkInStats.currentStreak || 0 }}</div>
          <div class="stat-label">连续打卡（天）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon total-icon">
            <el-icon :size="24"><Check /></el-icon>
          </div>
          <div class="stat-value">{{ checkInStats.totalCheckIns || 0 }}</div>
          <div class="stat-label">累计打卡</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon fav-icon">
            <el-icon :size="24"><Star /></el-icon>
          </div>
          <div class="stat-value">{{ favoritesCount }}</div>
          <div class="stat-label">收藏图书</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon badge-icon">
            <el-icon :size="24"><Bell /></el-icon>
          </div>
          <div class="stat-value">{{ badges.length }}</div>
          <div class="stat-label">获得徽章</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon level-icon">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="stat-value">{{ levelInfo.points || 0 }}</div>
          <div class="stat-label">积分</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon rank-icon">
            <el-icon :size="24"><Trophy /></el-icon>
          </div>
          <div class="stat-value">{{ levelInfo.levelName || '书虫' }}</div>
          <div class="stat-label">等级</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 阅读打卡按钮 -->
    <div class="checkin-section">
      <el-button
        type="primary"
        size="large"
        :disabled="checkInStats.todayChecked"
        :loading="checkingIn"
        @click="handleCheckIn"
        class="checkin-btn"
      >
        <el-icon><Edit /></el-icon>
        {{ checkInStats.todayChecked ? '今日已打卡' : '阅读打卡' }}
      </el-button>
    </div>

    <!-- 阅读足迹时间轴 -->
    <el-card shadow="hover" class="timeline-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="18" color="#e6a23c"><Timer /></el-icon>
          <span>阅读足迹</span>
          <span class="timeline-summary" v-if="timelineData.totalBooks > 0">
            共读 {{ timelineData.totalBooks }} 本书，{{ timelineData.totalPages }} 页
            <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">
              摞起来 {{ timelineData.stackHeight }} cm
            </el-tag>
          </span>
        </div>
      </template>
      <div v-loading="timelineLoading">
        <el-empty v-if="!timelineLoading && timelineData.timeline?.length === 0" description="暂无借阅记录" />
        <div class="timeline-wrap" v-else>
          <div v-for="(month, mi) in timelineData.timeline" :key="month.month" class="timeline-month">
            <div class="timeline-month-header">
              <span class="month-dot" :style="{ background: monthColors[mi % monthColors.length] }"></span>
              <span class="month-label">{{ month.month }}</span>
              <span class="month-stats">{{ month.bookCount }} 本 · {{ month.monthPages }} 页</span>
            </div>
            <div class="timeline-items">
              <div v-for="item in month.items" :key="item.id" class="timeline-item">
                <div class="timeline-item-dot"></div>
                <div class="timeline-item-content">
                  <div class="timeline-item-header">
                    <span class="timeline-book-title">《{{ item.bookTitle }}》</span>
                    <el-tag size="small" :type="item.status === 'RETURNED' ? 'success' : 'warning'" effect="plain">
                      {{ item.status === 'RETURNED' ? '已归还' : '借阅中' }}
                    </el-tag>
                  </div>
                  <span class="timeline-book-author">{{ item.bookAuthor }}</span>
                  <span class="timeline-date">{{ formatDate(item.borrowDate) }}
                    <template v-if="item.returnDate"> → {{ formatDate(item.returnDate) }}</template>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 主体内容：动态 + 书单/徽章 -->
    <el-row :gutter="16" class="main-content">
      <!-- 左侧：关注动态 -->
      <el-col :span="14">
        <el-card shadow="hover" class="feed-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#409eff"><Bell /></el-icon>
              <span>关注动态</span>
            </div>
          </template>
          <div v-loading="feedLoading">
            <el-empty v-if="!feedLoading && activities.length === 0" description="暂无关注动态" />
            <div v-for="act in activities" :key="act.id" class="activity-item">
              <div class="activity-avatar">
                <el-avatar :size="36" :src="act.user?.avatar">
                  {{ (act.user?.nickname || act.user?.username || '?').charAt(0).toUpperCase() }}
                </el-avatar>
              </div>
              <div class="activity-body">
                <div class="activity-header">
                  <span class="activity-user">{{ act.user?.nickname || act.user?.username }}</span>
                  <span class="activity-type">{{ formatActivityType(act.type) }}</span>
                </div>
                <p class="activity-desc" v-if="act.description">{{ act.description }}</p>
                <span class="activity-time">{{ formatDate(act.createdAt) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：书单 + 徽章 -->
      <el-col :span="10">
        <!-- 我的书单 -->
        <el-card shadow="hover" class="booklist-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#e6a23c"><Star /></el-icon>
              <span>我的书单</span>
            </div>
          </template>
          <div v-loading="bookListsLoading">
            <el-empty v-if="!bookListsLoading && bookLists.length === 0" description="暂无书单" />
            <div v-for="bl in bookLists" :key="bl.id" class="booklist-item">
              <div class="booklist-info">
                <h4 class="booklist-name">{{ bl.name }}</h4>
                <span class="booklist-meta">{{ bl.bookCount || 0 }} 本书</span>
              </div>
              <el-tag size="small" :type="bl.isPublic ? 'success' : 'info'">
                {{ bl.isPublic ? '公开' : '私密' }}
              </el-tag>
            </div>
          </div>
        </el-card>

        <!-- 我的徽章 -->
        <el-card shadow="hover" class="badge-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#67c23a"><Bell /></el-icon>
              <span>我的徽章</span>
            </div>
          </template>
          <div v-loading="badgesLoading">
            <el-empty v-if="!badgesLoading && badges.length === 0" description="暂无徽章" />
            <div class="badge-grid">
              <div v-for="badge in badges" :key="badge.id" class="badge-item">
                <div class="badge-icon-wrap" :style="{ background: badgeColors[badge.id % badgeColors.length] }">
                  <el-icon :size="22"><Star /></el-icon>
                </div>
                <span class="badge-name">{{ badge.name }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyFavorites, getMyNotes, getCheckInStats, checkIn, getFollowedActivities, getFollowStats, followUser, unfollowUser, isFollowing } from '../api/social'
import { getUserBadges, getMyBookLists, getMyLevel } from '../api/community'
import { getTimeline } from '../api/borrow'
import { ElMessage } from 'element-plus'
import { User, Star, Edit, Check, Calendar, Bell, TrendCharts, Trophy, Timer } from '@element-plus/icons-vue'

const currentUser = ref({
  username: localStorage.getItem('username') || '',
  nickname: localStorage.getItem('nickname') || '',
  avatar: localStorage.getItem('avatar') || ''
})

const firstChar = computed(() => {
  return (currentUser.value.nickname || currentUser.value.username || '?').charAt(0).toUpperCase()
})

const followStats = ref({ followerCount: 0, followingCount: 0 })
const checkInStats = ref({ currentStreak: 0, totalCheckIns: 0, todayChecked: false })
const checkingIn = ref(false)
const favoritesCount = ref(0)

const activities = ref([])
const feedLoading = ref(false)

const bookLists = ref([])
const bookListsLoading = ref(false)

const badges = ref([])
const badgesLoading = ref(false)

const levelInfo = ref({ points: 0, levelName: '书虫' })

// 阅读足迹时间轴
const timelineData = ref({ timeline: [], totalBooks: 0, totalPages: 0, stackHeight: 0 })
const timelineLoading = ref(false)
const monthColors = [
  '#667eea', '#f093fb', '#4facfe', '#43e97b', '#fa709a', '#a18cd1',
  '#f6d365', '#f5576c', '#00f2fe', '#38f9d7', '#fee140', '#fbc2eb'
]

const badgeColors = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
]

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatActivityType(type) {
  const map = {
    CHECK_IN: '打卡了',
    FAVORITE: '收藏了',
    REVIEW: '评价了',
    NOTE: '写了笔记',
    FOLLOW: '关注了',
    BORROW: '借阅了',
    BOOKLIST: '创建了书单',
    BADGE: '获得了徽章',
  }
  return map[type] || type || '动态'
}

async function fetchFollowStats() {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) return
    const res = await getFollowStats(userId)
    followStats.value = res.data || {}
  } catch (e) {
    // handled
  }
}

async function fetchCheckInStats() {
  try {
    const res = await getCheckInStats()
    checkInStats.value = res.data || {}
  } catch (e) {
    // handled
  }
}

async function fetchFavoritesCount() {
  try {
    const res = await getMyFavorites({ page: 0, size: 1 })
    favoritesCount.value = res.data?.totalElements || 0
  } catch (e) {
    // handled
  }
}

async function fetchActivities() {
  feedLoading.value = true
  try {
    const res = await getFollowedActivities({ page: 0, size: 10 })
    activities.value = res.data?.content || []
  } catch (e) {
    // handled
  } finally {
    feedLoading.value = false
  }
}

async function fetchBookLists() {
  bookListsLoading.value = true
  try {
    const res = await getMyBookLists({ page: 0, size: 5 })
    bookLists.value = res.data?.content || []
  } catch (e) {
    // handled
  } finally {
    bookListsLoading.value = false
  }
}

async function fetchBadges() {
  badgesLoading.value = true
  try {
    const res = await getUserBadges()
    const rawBadges = res.data || []
    badges.value = rawBadges.map(ub => ub.badge || ub)
  } catch (e) {
    // handled
  } finally {
    badgesLoading.value = false
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

function handleViewFollowers() {
  ElMessage.info('粉丝列表功能开发中')
}

function handleViewFollowing() {
  ElMessage.info('关注列表功能开发中')
}

async function fetchLevel() {
  try {
    const res = await getMyLevel()
    levelInfo.value = res.data || { points: 0, levelName: '书虫' }
  } catch (e) { /* handled */ }
}

async function fetchTimeline() {
  timelineLoading.value = true
  try {
    const res = await getTimeline()
    timelineData.value = res.data || { timeline: [], totalBooks: 0, totalPages: 0, stackHeight: 0 }
  } catch (e) { /* handled */ } finally {
    timelineLoading.value = false
  }
}

onMounted(() => {
  fetchFollowStats()
  fetchCheckInStats()
  fetchFavoritesCount()
  fetchActivities()
  fetchBookLists()
  fetchBadges()
  fetchLevel()
  fetchTimeline()
})
</script>

<style scoped>
.mystudy-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 资料头部 */
.profile-header-card {
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #fff;
}

.profile-header-card :deep(.el-card__body) {
  padding: 28px 32px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  background: rgba(255, 255, 255, 0.25);
  border: 3px solid rgba(255, 255, 255, 0.5);
}

.avatar-text {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
}

.profile-name-role h2 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.profile-username {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
}

.profile-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.follow-stat {
  text-align: center;
  padding: 4px 16px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.3s;
}

.follow-stat:hover {
  background: rgba(255, 255, 255, 0.15);
}

.follow-num {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.follow-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.profile-header-card :deep(.el-divider--vertical) {
  height: 36px;
  border-color: rgba(255, 255, 255, 0.3);
}

/* 统计行 */
.stats-row {
  margin: 0 !important;
}

.stat-card {
  border-radius: 12px;
  text-align: center;
  cursor: default;
}

.stat-card :deep(.el-card__body) {
  padding: 20px 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  color: #fff;
}

.streak-icon {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.total-icon {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.fav-icon {
  background: linear-gradient(135deg, #e6a23c, #f7ba2a);
}

.badge-icon {
  background: linear-gradient(135deg, #f56c6c, #f89898);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

/* 打卡按钮 */
.checkin-section {
  display: flex;
  justify-content: center;
}

.checkin-btn {
  min-width: 200px;
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}

.checkin-btn:hover {
  background: linear-gradient(135deg, #5a6fd6, #6a3f96);
}

.checkin-btn:disabled {
  background: linear-gradient(135deg, #c0c4cc, #dcdfe6);
}

/* 主体内容 */
.main-content {
  margin: 0 !important;
}

.feed-card,
.booklist-card,
.badge-card {
  border-radius: 12px;
}

.timeline-card {
  border-radius: 12px;
}

.timeline-summary {
  font-size: 13px;
  color: #909399;
  margin-left: auto;
}

.timeline-wrap {
  padding-left: 8px;
}

.timeline-month {
  margin-bottom: 20px;
}

.timeline-month-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.month-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
}

.month-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.month-stats {
  font-size: 12px;
  color: #909399;
}

.timeline-items {
  margin-left: 7px;
  border-left: 2px solid #e0e0e0;
  padding-left: 16px;
}

.timeline-item {
  position: relative;
  padding: 10px 0 10px 0;
}

.timeline-item-dot {
  position: absolute;
  left: -22px;
  top: 15px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #409eff;
}

.timeline-item-content {
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 14px;
  border: 1px solid #f0f0f0;
}

.timeline-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.timeline-book-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.timeline-book-author {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.timeline-date {
  font-size: 12px;
  color: #c0c4cc;
}

.booklist-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

/* 动态列表 */
.activity-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-avatar {
  flex-shrink: 0;
}

.activity-body {
  flex: 1;
  min-width: 0;
}

.activity-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.activity-user {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.activity-type {
  font-size: 12px;
  color: #909399;
}

.activity-desc {
  font-size: 13px;
  color: #606266;
  margin: 4px 0;
  line-height: 1.5;
  word-break: break-all;
}

.activity-time {
  font-size: 12px;
  color: #c0c4cc;
}

/* 书单 */
.booklist-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.booklist-item:last-child {
  border-bottom: none;
}

.booklist-info {
  min-width: 0;
}

.booklist-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.booklist-meta {
  font-size: 12px;
  color: #909399;
}

/* 徽章网格 */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.badge-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.badge-name {
  font-size: 12px;
  color: #606266;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

@media (max-width: 992px) {
  .stats-row .el-col {
    flex: 0 0 50%;
    max-width: 50%;
    margin-bottom: 12px;
  }

  .main-content .el-col {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .badge-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 600px) {
  .profile-header {
    flex-direction: column;
    gap: 16px;
  }

  .profile-right {
    width: 100%;
    justify-content: center;
  }

  .stats-row .el-col {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .badge-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
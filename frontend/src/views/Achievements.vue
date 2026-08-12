<template>
  <div class="achievements-page">
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="24" color="#e6a23c"><TrophyBase /></el-icon>
        <span>成就徽章</span>
      </div>
      <el-button type="primary" :icon="Medal" @click="handleRefresh" :loading="refreshing">
        刷新成就
      </el-button>
    </div>

    <div class="stats-row">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon earned">
            <el-icon :size="28"><TrophyBase /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.earned }}</span>
            <span class="stat-label">已获得徽章</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon total">
            <el-icon :size="28"><Medal /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.total }}</span>
            <span class="stat-label">全部徽章</span>
          </div>
        </div>
      </el-card>
    </div>

    <div v-loading="loading" class="badge-grid">
      <el-empty v-if="!loading && badges.length === 0" description="暂无徽章数据" />
      <div
        v-for="(badge, idx) in badges"
        :key="badge.id"
        class="badge-card"
        :class="{ earned: badge.earned, unearned: !badge.earned }"
      >
        <div class="badge-cover" :style="{ background: coverColors[idx % coverColors.length] }">
          <span class="badge-emoji">{{ badge.icon || '🏅' }}</span>
          <div class="badge-status" v-if="badge.earned">
            <el-tag type="success" size="small" effect="dark">已获得</el-tag>
          </div>
        </div>
        <div class="badge-info">
          <h4 class="badge-name">{{ badge.name }}</h4>
          <p class="badge-desc">{{ badge.description || '暂无描述' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllBadges, getUserBadgeStats, checkBadges } from '../api/community'
import { ElMessage } from 'element-plus'
import { TrophyBase, Medal } from '@element-plus/icons-vue'

const loading = ref(false)
const refreshing = ref(false)
const badges = ref([])
const stats = ref({ earned: 0, total: 0 })

const coverColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
  'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

async function fetchData() {
  loading.value = true
  try {
    const [badgesRes, statsRes] = await Promise.all([
      getAllBadges(),
      getUserBadgeStats()
    ])
    const allBadges = badgesRes.data || []
    const badgeStats = statsRes.data || {}
    const earnedIds = new Set(badgeStats.earnedBadgeIds || [])

    badges.value = allBadges.map(b => ({
      ...b,
      earned: earnedIds.has(b.id)
    }))

    stats.value = {
      earned: badgeStats.earnedCount || 0,
      total: allBadges.length
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await checkBadges()
    ElMessage.success('成就已刷新')
    await fetchData()
  } catch (e) {
    // handled by interceptor
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.achievements-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
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

.stats-row {
  display: flex;
  gap: 20px;
}

.stat-card {
  flex: 1;
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 0;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.earned {
  background: linear-gradient(135deg, #f6d365, #fda085);
  color: #fff;
}

.stat-icon.total {
  background: linear-gradient(135deg, #a18cd1, #fbc2eb);
  color: #fff;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
}

.badge-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.badge-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #e0e0e0;
}

.badge-card.unearned {
  opacity: 0.5;
  filter: grayscale(0.6);
}

.badge-card.unearned:hover {
  opacity: 0.7;
  filter: grayscale(0.3);
}

.badge-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.badge-emoji {
  font-size: 48px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.15));
}

.badge-status {
  position: absolute;
  top: 8px;
  right: 8px;
}

.badge-info {
  padding: 14px 16px;
}

.badge-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .badge-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .badge-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stats-row {
    flex-direction: column;
  }
}

@media (max-width: 600px) {
  .badge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
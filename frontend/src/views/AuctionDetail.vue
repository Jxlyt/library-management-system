<template>
  <div class="auction-detail-page">
    <div v-if="auction" class="detail-container">
      <div class="detail-header">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <div class="header-tags">
          <el-tag v-if="auction.status === 'ACTIVE'" type="warning" size="large" effect="dark">进行中</el-tag>
          <el-tag v-else-if="auction.status === 'ENDED'" type="success" size="large" effect="dark">已成交</el-tag>
          <el-tag v-else-if="auction.status === 'FAILED'" type="info" size="large" effect="dark">已流拍</el-tag>
          <el-tag v-else-if="auction.status === 'CANCELLED'" type="danger" size="large" effect="dark">已取消</el-tag>
        </div>
      </div>

      <div class="detail-main">
        <div class="book-section">
          <div class="book-cover" :style="{ background: coverColors[(auction.book?.id || 0) % coverColors.length] }">
            <span>{{ auction.book?.title?.charAt(0) }}</span>
          </div>
          <div class="book-info">
            <h2>《{{ auction.book?.title }}》</h2>
            <p class="book-author">{{ auction.book?.author }}</p>
            <p class="book-category" v-if="auction.book?.category">{{ auction.book?.category }}</p>
            <p class="book-desc" v-if="auction.book?.description">{{ auction.book?.description?.substring(0, 100) }}</p>
          </div>
        </div>

        <div class="bid-section">
          <div class="current-price">
            <span>当前最高出价</span>
            <strong>¥{{ (auction.currentPrice || auction.startPrice).toFixed(2) }}</strong>
          </div>
          <div class="bid-info">
            <span>起拍价：¥{{ auction.startPrice?.toFixed(2) }}</span>
            <span>加价幅度：≥ ¥{{ auction.minIncrement?.toFixed(2) }}</span>
            <span>{{ auction.bidCount || 0 }} 次出价</span>
          </div>
          <div class="countdown" v-if="auction.status === 'ACTIVE'">
            <div class="countdown-ring">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="45" fill="none" stroke="#ebeef5" stroke-width="6" />
                <circle cx="50" cy="50" r="45" fill="none" stroke="#e6a23c" stroke-width="6" stroke-linecap="round" :stroke-dasharray="283" :stroke-dashoffset="283 - (283 * countdownPercent / 100)" transform="rotate(-90 50 50)" />
              </svg>
              <div class="countdown-text">{{ countdownDisplay }}</div>
            </div>
          </div>

          <div class="bid-input" v-if="auction.status === 'ACTIVE'">
            <el-input-number v-model="bidAmount" :min="(auction.currentPrice || auction.startPrice) + (auction.minIncrement || 1)" :step="auction.minIncrement || 1" :precision="2" size="large" style="width: 100%" />
            <el-button type="warning" size="large" @click="handleBid" :loading="bidding" class="bid-btn">立即出价</el-button>
          </div>

          <!-- 已成交 -->
          <div v-if="auction.status === 'ENDED'" class="result-banner success-banner">
            <div class="result-icon">🎉</div>
            <h3>拍卖已成交</h3>
            <div class="result-info">
              <p>中标者：<strong>{{ auction.winner?.nickname || auction.winner?.username }}</strong></p>
              <p>成交价：<strong class="price">¥{{ (auction.currentPrice || auction.startPrice).toFixed(2) }}</strong></p>
            </div>
            <el-button v-if="auction.orderId" type="primary" @click="$router.push('/orders')" style="margin-top:12px">
              查看拍卖订单
            </el-button>
          </div>

          <!-- 流拍 -->
          <div v-if="auction.status === 'FAILED'" class="result-banner fail-banner">
            <div class="result-icon">😔</div>
            <h3>拍卖已流拍</h3>
            <p class="fail-text">无人出价，该拍卖已结束</p>
          </div>

          <!-- 已取消 -->
          <div v-if="auction.status === 'CANCELLED'" class="result-banner cancel-banner">
            <div class="result-icon">🚫</div>
            <h3>拍卖已取消</h3>
            <p>管理员已取消该拍卖</p>
          </div>
        </div>
      </div>

      <div class="bids-section">
        <h3>出价记录</h3>
        <el-table :data="bids" stripe style="width: 100%">
          <el-table-column prop="user.username" label="用户" width="150" />
          <el-table-column prop="bidAmount" label="出价" width="150">
            <template #default="{ row }">¥{{ row.bidAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="bidTime" label="时间">
            <template #default="{ row }">{{ formatTime(row.bidTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getAuctionDetail, getAuctionBids, placeBid } from '../api/market'
import { ElMessage } from 'element-plus'

const route = useRoute()
const auction = ref(null)
const bids = ref([])
const bidAmount = ref(0)
const bidding = ref(false)
let timer = null

const coverColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)', 'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

const countdownDisplay = computed(() => {
  if (!auction.value || !auction.value.endTime) return ''
  const diff = new Date(auction.value.endTime) - new Date()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const countdownPercent = computed(() => {
  if (!auction.value || !auction.value.endTime || !auction.value.startTime) return 100
  const total = new Date(auction.value.endTime) - new Date(auction.value.startTime)
  const remaining = new Date(auction.value.endTime) - new Date()
  return Math.max(0, Math.min(100, (remaining / total) * 100))
})

function formatTime(t) { return t ? t.substring(0, 16).replace('T', ' ') : '-' }

async function fetchDetail() {
  try {
    const res = await getAuctionDetail(route.params.id)
    auction.value = res.data
    bidAmount.value = (auction.value.currentPrice || auction.value.startPrice) + (auction.value.minIncrement || 1)
  } catch (e) { /* */ }
}

async function fetchBids() {
  try {
    const res = await getAuctionBids(route.params.id)
    bids.value = res.data || []
  } catch (e) { /* */ }
}

async function handleBid() {
  bidding.value = true
  try {
    await placeBid(route.params.id, bidAmount.value)
    ElMessage.success('出价成功！')
    fetchDetail()
    fetchBids()
  } catch (e) { /* */ } finally { bidding.value = false }
}

onMounted(() => {
  fetchDetail()
  fetchBids()
  timer = setInterval(() => {
    fetchDetail()
    fetchBids()
  }, 5000)
})

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.auction-detail-page { padding: 24px; max-width: 960px; margin: 0 auto; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.detail-main { display: flex; gap: 32px; background: rgba(255,255,255,0.75); backdrop-filter: blur(10px); border-radius: 24px; padding: 32px; border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 8px 32px rgba(0,0,0,0.06); }
.book-section { display: flex; gap: 20px; flex: 1; }
.book-cover { width: 140px; height: 190px; border-radius: 16px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 8px 30px rgba(0,0,0,0.15); }
.book-cover span { font-size: 48px; font-weight: 800; color: #fff; }
.book-info h2 { margin: 0 0 8px; }
.book-author { color: #909399; margin: 0 0 4px; }
.book-category { margin: 0 0 4px; }
.book-desc { color: #909399; font-size: 14px; line-height: 1.6; }

.bid-section { width: 320px; flex-shrink: 0; }
.current-price { text-align: center; margin-bottom: 12px; }
.current-price span { color: #909399; font-size: 13px; display: block; }
.current-price strong { font-size: 40px; color: #f56c6c; font-weight: 800; }
.bid-info { display: flex; flex-direction: column; gap: 4px; color: #909399; font-size: 13px; margin-bottom: 16px; }
.countdown { display: flex; justify-content: center; margin-bottom: 16px; }
.countdown-ring { position: relative; width: 130px; height: 130px; }
.countdown-ring svg { width: 100%; height: 100%; filter: drop-shadow(0 2px 8px rgba(230, 162, 60, 0.3)); }
.countdown-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 20px; font-weight: 800; color: #f59e0b; }
.bid-btn { width: 100%; margin-top: 12px; height: 50px; font-size: 18px; font-weight: 600; border-radius: 14px; background: linear-gradient(135deg, #f59e0b, #ef4444); border: none; }
.bid-btn:hover { opacity: 0.9; }
.result-banner { text-align: center; padding: 24px 16px; border-radius: 16px; margin-top: 8px; }
.result-banner .result-icon { font-size: 48px; margin-bottom: 8px; }
.result-banner h3 { margin: 0 0 12px; font-size: 20px; }
.result-banner p { margin: 4px 0; color: #606266; }
.result-banner .price { font-size: 28px; color: #f56c6c; }
.success-banner { background: linear-gradient(135deg, #f0fdf4, #dcfce7); border: 1px solid #bbf7d0; }
.fail-banner { background: linear-gradient(135deg, #f9fafb, #f3f4f6); border: 1px solid #e5e7eb; }
.fail-banner .fail-text { color: #9ca3af; font-size: 14px; }
.cancel-banner { background: linear-gradient(135deg, #fef2f2, #fee2e2); border: 1px solid #fecaca; }
.winner-banner { padding: 20px 0; }
.bids-section { margin-top: 32px; background: rgba(255,255,255,0.75); backdrop-filter: blur(10px); border-radius: 24px; padding: 24px; border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 4px 24px rgba(0,0,0,0.06); }
.bids-section h3 { margin: 0 0 16px; font-size: 20px; }
</style>
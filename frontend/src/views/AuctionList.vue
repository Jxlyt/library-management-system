<template>
  <div class="auction-page">
    <div class="page-header">
      <h2>🔨 图书拍卖</h2>
      <p class="subtitle">珍稀图书，价高者得</p>
      <el-button v-if="isAdmin" type="warning" @click="showCreateDialog" style="margin-top:12px">
        <el-icon><Plus /></el-icon> 发起拍卖
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-click="onTabClick" class="auction-tabs">
      <el-tab-pane label="进行中" name="ACTIVE" />
      <el-tab-pane label="已结束" name="ENDED" />
    </el-tabs>

    <div v-loading="loading" class="auction-grid">
      <el-empty v-if="!loading && auctions.length === 0" :description="activeTab === 'ACTIVE' ? '暂无进行中的拍卖' : '暂无已结束的拍卖'" />
      <div v-for="auction in auctions" :key="auction.id" class="auction-card" @click="$router.push('/auction/' + auction.id)">
        <div class="auction-book-cover" :style="{ background: coverColors[(auction.book?.id || 0) % coverColors.length] }">
          <span>{{ auction.book?.title?.charAt(0) }}</span>
        </div>
        <div class="auction-info">
          <h4>{{ auction.book?.title }}</h4>
          <p class="auction-author">{{ auction.book?.author }}</p>
          <div class="auction-price">
            <span>当前价</span>
            <strong>¥{{ (auction.currentPrice || auction.startPrice).toFixed(2) }}</strong>
          </div>
          <div class="auction-meta">
            <span>{{ auction.bidCount || 0 }} 次出价</span>
            <span v-if="auction.status === 'ACTIVE'">加价 ≥ ¥{{ auction.minIncrement?.toFixed(2) }}</span>
            <span v-if="auction.status === 'ENDED' && auction.winner" style="color:#67c23a">🏆 {{ auction.winner?.nickname || auction.winner?.username }}</span>
          </div>
          <div class="auction-time" v-if="auction.status === 'ACTIVE'">
            <el-icon><Clock /></el-icon>
            <span>{{ countdownText(auction.endTime) }}</span>
          </div>
          <el-tag v-else-if="auction.status === 'ENDED'" type="success" size="small">已成交</el-tag>
          <el-tag v-else-if="auction.status === 'FAILED'" type="info" size="small">已流拍</el-tag>
          <el-tag v-else-if="auction.status === 'CANCELLED'" type="danger" size="small">已取消</el-tag>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > 0" class="pagination" v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="fetchAuctions" />

    <!-- 创建拍卖弹窗 -->
    <el-dialog v-model="createDialogVisible" title="发起拍卖" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="createForm" label-width="100px" ref="createFormRef">
        <el-form-item label="选择图书" required>
          <el-select v-model="createForm.bookId" placeholder="请选择图书" filterable style="width:100%">
            <el-option v-for="b in bookList" :key="b.id" :label="b.title + ' - ' + b.author" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起拍价(元)" required>
          <el-input-number v-model="createForm.startPrice" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="加价幅度(元)">
          <el-input-number v-model="createForm.minIncrement" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="createForm.endTime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAuction" :loading="creating">确认发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getAuctions, createAuction } from '../api/market'
import { getBooks } from '../api/book'
import { ElMessage } from 'element-plus'
import { Plus, Clock } from '@element-plus/icons-vue'

const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN')

const activeTab = ref('ACTIVE')
const loading = ref(false)
const auctions = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
let timer = null

const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = ref({ bookId: null, startPrice: 9.9, minIncrement: 1.0, endTime: '' })
const bookList = ref([])

const coverColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)', 'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

function countdownText(endTime) {
  if (!endTime) return ''
  const diff = new Date(endTime) - new Date()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${h}时 ${m}分 ${s}秒`
}

async function fetchAuctions() {
  loading.value = true
  try {
    const res = await getAuctions({ status: activeTab.value, page: page.value - 1, size: size.value })
    auctions.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch (e) {
    console.error('Failed to fetch auctions:', e)
  } finally { loading.value = false }
}

async function showCreateDialog() {
  createDialogVisible.value = true
  try {
    const res = await getBooks({ page: 0, size: 100 })
    bookList.value = res.data?.content || []
  } catch (e) { /* */ }
}

async function handleCreateAuction() {
  if (!createForm.value.bookId || !createForm.value.startPrice || !createForm.value.endTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  creating.value = true
  try {
    await createAuction(createForm.value)
    ElMessage.success('拍卖已发起')
    createDialogVisible.value = false
    createForm.value = { bookId: null, startPrice: 9.9, minIncrement: 1.0, endTime: '' }
    activeTab.value = 'ACTIVE'
    page.value = 1
    fetchAuctions()
  } catch (e) { /* */ } finally { creating.value = false }
}

function onTabClick(tab) {
  activeTab.value = tab.paneName || tab.props?.name || tab.name
  page.value = 1
  fetchAuctions()
}

onMounted(() => {
  fetchAuctions()
  timer = setInterval(() => { auctions.value = [...auctions.value] }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.auction-page { padding: 24px; }
.page-header { text-align: center; margin-bottom: 24px; overflow: hidden; }
.page-header h2 { font-size: 32px; margin: 0; background: linear-gradient(135deg, #f59e0b, #ef4444, #ec4899); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; font-weight: 800; letter-spacing: 2px; }
.subtitle { color: #909399; margin-top: 6px; }
.auction-tabs { position: relative; z-index: 1; }
.auction-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; }
.auction-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.auction-tabs :deep(.el-tabs__item) { font-size: 15px; padding: 0 24px; }
.auction-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 20px; min-height: 200px; }
.auction-card { display: flex; gap: 16px; background: rgba(255,255,255,0.75); backdrop-filter: blur(10px); border-radius: 20px; padding: 20px; border: 1px solid rgba(255,255,255,0.5); cursor: pointer; transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1); }
.auction-card:hover { box-shadow: 0 12px 36px rgba(0,0,0,0.1); transform: translateY(-4px); border-color: rgba(245, 158, 11, 0.3); }
.auction-book-cover { width: 90px; height: 120px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 6px 20px rgba(0,0,0,0.12); }
.auction-book-cover span { font-size: 34px; font-weight: 800; color: #fff; }
.auction-info { flex: 1; min-width: 0; }
.auction-info h4 { margin: 0 0 4px; font-size: 18px; }
.auction-author { color: #909399; margin: 0 0 8px; font-size: 13px; }
.auction-price { margin-bottom: 8px; }
.auction-price span { color: #909399; font-size: 12px; }
.auction-price strong { font-size: 24px; color: #f56c6c; margin-left: 8px; }
.auction-meta { display: flex; gap: 16px; color: #909399; font-size: 13px; margin-bottom: 6px; }
.auction-time { display: flex; align-items: center; gap: 4px; color: #f59e0b; font-size: 14px; font-weight: 600; }
.pagination { margin-top: 24px; justify-content: center; }
</style>
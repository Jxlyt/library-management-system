<template>
  <div class="blind-box-page">
    <div class="page-header">
      <h2>🎁 图书盲盒</h2>
      <p class="subtitle">打开盲盒，邂逅你的命运之书</p>
    </div>

    <div class="box-area">
      <div class="mystery-box" :class="{ opening: isOpening }" @click="handleOpen">
        <div class="box-wrapper">
          <div class="box-lid" :class="{ open: isOpening }"></div>
          <div class="box-body">
            <div class="box-question">?</div>
            <div class="box-label">29.9元</div>
          </div>
        </div>
        <div class="box-glow"></div>
        <div class="sparkles" v-if="isOpening">
          <span v-for="i in 12" :key="i" class="sparkle" :style="{ '--i': i }"></span>
        </div>
      </div>

      <div class="box-controls">
        <el-select v-model="selectedCategory" placeholder="偏好分类（可选）" clearable style="width: 200px">
          <el-option label="文学" value="文学" />
          <el-option label="科幻" value="科幻" />
          <el-option label="小说" value="小说" />
          <el-option label="历史" value="历史" />
          <el-option label="计算机" value="计算机" />
          <el-option label="古典文学" value="古典文学" />
        </el-select>
        <el-button type="primary" size="large" @click="handleOpen" :loading="isOpening" class="open-btn">
          {{ isOpening ? '开启中...' : '🔮 打开盲盒 ¥29.9' }}
        </el-button>
      </div>

      <div class="active-count">当前剩余 {{ activeCount }} 个盲盒
        <el-button v-if="isAdmin" type="warning" size="small" @click="showCreateBox" style="margin-left:12px;border-radius:16px">
          <el-icon><Plus /></el-icon> 添加盲盒
        </el-button>
      </div>
    </div>

    <!-- 支付确认弹窗 -->
    <el-dialog v-model="payDialogVisible" title="确认支付" width="460px" center :close-on-click-modal="false" class="pay-dialog">
      <div class="pay-content">
        <div class="pay-amount">
          <span class="pay-label">支付金额</span>
          <span class="pay-price">¥29.90</span>
        </div>
        <el-divider />
        <div class="pay-method">
          <span class="pay-label">支付方式</span>
          <el-radio-group v-model="payMethod">
            <el-radio value="wechat" size="large">微信支付</el-radio>
            <el-radio value="alipay" size="large">支付宝</el-radio>
          </el-radio-group>
        </div>
        <el-alert title="提示：此为模拟支付，点击确认后即刻开盒" type="info" :closable="false" show-icon style="margin-top:16px" />
      </div>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPay" :loading="paying" size="large">
          确认支付 ¥29.90
        </el-button>
      </template>
    </el-dialog>

    <!-- 开盒结果 -->
    <el-dialog v-model="resultVisible" title="🎉 恭喜获得！" width="540px" center :close-on-click-modal="false" class="result-dialog" @close="resultStep = 1">
      <!-- 步骤1：展示书籍信息 -->
      <div v-if="resultBook && resultStep === 1" class="result-content">
        <div class="result-cover" :style="{ background: coverColors[(resultBook.id || 0) % coverColors.length] }">
          <span>{{ resultBook.title?.charAt(0) }}</span>
        </div>
        <h3>《{{ resultBook.title }}》</h3>
        <p class="result-author">{{ resultBook.author }}</p>
        <p class="result-category" v-if="resultBook.category">{{ resultBook.category }}</p>
        <p class="result-price">价值 ¥{{ (resultBook.salePrice || 0).toFixed(2) }}</p>
        <el-divider />
        <el-alert title="请选择收货地址，确认后订单将自动发货" type="info" :closable="false" show-icon />
      </div>
      <template #footer v-if="resultStep === 1">
        <el-button @click="resultVisible = false">收下</el-button>
        <el-button type="primary" @click="goToAddressStep">选择收货地址</el-button>
      </template>

      <!-- 步骤2：选择收货地址 -->
      <div v-if="resultStep === 2" class="address-step">
        <div class="address-step-title">选择收货地址</div>
        <el-empty v-if="addresses.length === 0" description="暂无收货地址，请先添加" />
        <div v-else class="address-list">
          <div v-for="addr in addresses" :key="addr.id" class="address-item" :class="{ selected: selectedAddressId === addr.id }" @click="selectedAddressId = addr.id">
            <div class="addr-radio">
              <el-radio :model-value="selectedAddressId" :value="addr.id" />
            </div>
            <div class="addr-info">
              <div class="addr-name">{{ addr.name }} <span class="addr-phone">{{ addr.phone }}</span></div>
              <div class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</div>
            </div>
          </div>
        </div>
        <div class="addr-quick-add" v-if="addresses.length === 0">
          <el-button type="primary" @click="router.push('/addresses'); resultVisible = false">去添加地址</el-button>
        </div>
      </div>
      <template #footer v-if="resultStep === 2">
        <el-button @click="resultStep = 1">返回</el-button>
        <el-button type="primary" @click="confirmAddress" :loading="confirmingAddress" :disabled="!selectedAddressId">
          确认收货地址并查看订单
        </el-button>
      </template>
    </el-dialog>

    <!-- 管理员创建盲盒弹窗 -->
    <el-dialog v-model="createBoxVisible" title="添加盲盒" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="createBoxForm" label-width="100px">
        <el-form-item label="选择图书" required>
          <el-select v-model="createBoxForm.bookId" placeholder="请选择图书" filterable style="width:100%">
            <el-option v-for="b in bookList" :key="b.id" :label="b.title + ' - ' + b.author" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盲盒价格(元)">
          <el-input-number v-model="createBoxForm.price" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createBoxForm.category" placeholder="选择分类（可选）" clearable style="width:100%">
            <el-option label="文学" value="文学" />
            <el-option label="科幻" value="科幻" />
            <el-option label="小说" value="小说" />
            <el-option label="历史" value="历史" />
            <el-option label="计算机" value="计算机" />
            <el-option label="古典文学" value="古典文学" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建数量">
          <el-input-number v-model="createBoxForm.count" :min="1" :max="50" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBoxVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateBox" :loading="creatingBox">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 历史记录 -->
    <div class="history-section">
      <h3>我的盲盒</h3>
      <div v-loading="loading">
        <el-empty v-if="!loading && boxes.length === 0" description="还没有开过盲盒" />
        <div v-else class="box-list">
          <div v-for="box in boxes" :key="box.id" class="box-card">
            <div class="box-card-cover" :style="{ background: coverColors[(box.book?.id || 0) % coverColors.length] }">
              <span>{{ box.book?.title?.charAt(0) }}</span>
            </div>
            <div class="box-card-info">
              <h4>{{ box.book?.title }}</h4>
              <p>{{ box.book?.author }}</p>
              <p class="box-card-price">¥{{ box.price?.toFixed(2) }}</p>
              <p class="box-card-time">{{ formatDate(box.soldAt) }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { openBlindBox, getMyBlindBoxes, getBlindBoxCount, createBlindBox } from '../api/market'
import { getMyAddresses, updateOrderAddress } from '../api/order'
import { getBooks } from '../api/book'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN')

const isOpening = ref(false)
const payDialogVisible = ref(false)
const paying = ref(false)
const payMethod = ref('wechat')
const resultVisible = ref(false)
const resultStep = ref(1)
const resultBook = ref(null)
const orderInfo = ref(null)
const selectedCategory = ref('')
const activeCount = ref(0)
const loading = ref(false)
const boxes = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const confirmingAddress = ref(false)

  // 管理员创建盲盒
  const createBoxVisible = ref(false)
  const creatingBox = ref(false)
  const createBoxForm = ref({ bookId: null, price: 29.9, category: '', count: 1 })
  const bookList = ref([])

const coverColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)', 'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

function formatDate(d) { return d ? d.substring(0, 16).replace('T', ' ') : '-' }

function handleOpen() {
  payDialogVisible.value = true
}

async function confirmPay() {
    paying.value = true
    isOpening.value = true
    try {
      const res = await openBlindBox(selectedCategory.value || null)
      resultBook.value = res.data?.book
      orderInfo.value = res.extra || null
      resultStep.value = 1
      resultVisible.value = true
      payDialogVisible.value = false
      fetchActiveCount()
      fetchMyBoxes()
    } catch (e) {
      // handled
    } finally {
      isOpening.value = false
      paying.value = false
    }
  }

  async function goToAddressStep() {
    await loadAddresses()
    resultStep.value = 2
  }

  async function loadAddresses() {
    try {
      const res = await getMyAddresses()
      addresses.value = res.data || []
      if (addresses.value.length > 0) {
        const defaultAddr = addresses.value.find(a => a.isDefault)
        selectedAddressId.value = defaultAddr ? defaultAddr.id : addresses.value[0].id
      }
    } catch (e) { /* */ }
  }

  async function confirmAddress() {
    if (!selectedAddressId.value || !orderInfo.value?.orderId) {
      ElMessage.warning('请选择收货地址')
      return
    }
    confirmingAddress.value = true
    try {
      await updateOrderAddress(orderInfo.value.orderId, selectedAddressId.value)
      ElMessage.success('收货地址已确认，订单生成成功！')
      resultVisible.value = false
      router.push('/orders')
    } catch (e) { /* */ } finally {
      confirmingAddress.value = false
    }
  }

async function fetchActiveCount() {
  try {
    const res = await getBlindBoxCount()
    activeCount.value = res.data?.count || 0
  } catch (e) { /* */ }
}

async function fetchMyBoxes() {
  loading.value = true
  try {
    const res = await getMyBlindBoxes({ page: 0, size: 20 })
    boxes.value = res.data?.content || []
  } catch (e) { /* */ } finally { loading.value = false }
}

async function showCreateBox() {
    createBoxVisible.value = true
    try {
      const res = await getBooks({ page: 0, size: 100 })
      bookList.value = res.data?.content || []
    } catch (e) { /* */ }
  }

  async function handleCreateBox() {
    if (!createBoxForm.value.bookId) {
      ElMessage.warning('请选择图书')
      return
    }
    creatingBox.value = true
    try {
      const count = createBoxForm.value.count || 1
      for (let i = 0; i < count; i++) {
        await createBlindBox({
          bookId: createBoxForm.value.bookId,
          price: createBoxForm.value.price || 29.9,
          category: createBoxForm.value.category || null
        })
      }
      ElMessage.success(`成功创建 ${count} 个盲盒`)
      createBoxVisible.value = false
      createBoxForm.value = { bookId: null, price: 29.9, category: '', count: 1 }
      fetchActiveCount()
    } catch (e) { /* */ } finally { creatingBox.value = false }
  }

  onMounted(() => { fetchActiveCount(); fetchMyBoxes() })
</script>

<style scoped>
.blind-box-page { padding: 24px; min-height: 400px; }
.page-header { text-align: center; margin-bottom: 32px; }
.page-header h2 { font-size: 36px; margin: 0; background: linear-gradient(135deg, #a78bfa, #f472b6, #fbbf24); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; font-weight: 800; letter-spacing: 2px; }
.subtitle { color: #909399; margin-top: 8px; font-size: 15px; }

.box-area { display: flex; flex-direction: column; align-items: center; gap: 24px; margin-bottom: 48px; background: rgba(255,255,255,0.6); backdrop-filter: blur(20px); border-radius: 24px; padding: 40px; border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 8px 32px rgba(0,0,0,0.06); }
.mystery-box { position: relative; cursor: pointer; }
.box-wrapper { position: relative; width: 200px; height: 200px; }
.box-body { position: absolute; bottom: 0; width: 100%; height: 155px; background: linear-gradient(135deg, #a78bfa, #7c3aed); border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; box-shadow: 0 12px 40px rgba(124, 58, 237, 0.4); transition: transform 0.3s; }
.mystery-box:hover .box-body { transform: scale(1.08); }
.box-question { font-size: 56px; font-weight: 800; color: #fff; text-shadow: 0 4px 12px rgba(0,0,0,0.2); animation: float 2s ease-in-out infinite; }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
.box-label { color: rgba(255,255,255,0.85); font-size: 15px; margin-top: 6px; font-weight: 600; }
.box-lid { position: absolute; top: 0; width: 100%; height: 55px; background: linear-gradient(135deg, #c4b5fd, #a78bfa); border-radius: 20px 20px 0 0; z-index: 2; transition: transform 0.7s cubic-bezier(0.34, 1.56, 0.64, 1); }
.box-lid.open { transform: translateY(-70px) rotateX(60deg); opacity: 0; }
.box-glow { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 240px; height: 240px; border-radius: 50%; background: radial-gradient(circle, rgba(167,139,250,0.2), transparent); pointer-events: none; animation: glowPulse 2s ease-in-out infinite; }
@keyframes glowPulse { 0%, 100% { opacity: 0.5; transform: translate(-50%, -50%) scale(1); } 50% { opacity: 1; transform: translate(-50%, -50%) scale(1.15); } }
.box-controls { display: flex; gap: 12px; align-items: center; }
.open-btn { height: 52px; font-size: 18px; border-radius: 26px; padding: 0 36px; background: linear-gradient(135deg, #a78bfa, #ec4899); border: none; font-weight: 600; letter-spacing: 1px; }
.open-btn:hover { opacity: 0.9; transform: scale(1.03); }
.active-count { color: #909399; font-size: 14px; }

.sparkles { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); }
.sparkle { position: absolute; width: 8px; height: 8px; background: #ffd700; border-radius: 50%; animation: sparkleAnim 1.5s ease-out infinite; animation-delay: calc(var(--i) * 0.1s); box-shadow: 0 0 6px #ffd700; }
@keyframes sparkleAnim {
  0% { transform: rotate(calc(var(--i) * 30deg)) translateY(0); opacity: 1; }
  100% { transform: rotate(calc(var(--i) * 30deg)) translateY(-90px); opacity: 0; }
}

.result-dialog :deep(.el-dialog) { border-radius: 24px !important; }
.result-content { text-align: center; }
.result-cover { width: 120px; height: 160px; border-radius: 16px; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; box-shadow: 0 8px 30px rgba(0,0,0,0.15); }
.result-cover span { font-size: 42px; font-weight: 800; color: #fff; }
.result-content h3 { font-size: 24px; margin: 0 0 8px; }
.result-author { color: #909399; margin: 0 0 4px; }
.result-category { margin: 0 0 4px; }
.result-price { font-size: 22px; font-weight: 700; color: #f56c6c; }

.history-section { background: rgba(255,255,255,0.6); backdrop-filter: blur(20px); border-radius: 20px; padding: 28px; border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 4px 24px rgba(0,0,0,0.06); }
.history-section h3 { margin-bottom: 16px; font-size: 20px; }
.box-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.box-card { display: flex; gap: 16px; background: rgba(255,255,255,0.8); border-radius: 16px; padding: 16px; border: 1px solid rgba(255,255,255,0.5); transition: all 0.35s ease; }
.box-card:hover { box-shadow: 0 8px 30px rgba(0,0,0,0.08); transform: translateY(-2px); }
.box-card-cover { width: 70px; height: 90px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.box-card-cover span { font-size: 24px; font-weight: 700; color: #fff; }
.box-card-info h4 { margin: 0 0 4px; font-size: 16px; }
.box-card-info p { margin: 0 0 2px; color: #909399; font-size: 13px; }
.box-card-price { color: #f56c6c !important; font-weight: 600 !important; }
.box-card-time { font-size: 12px !important; color: #c0c4cc !important; }

/* 支付弹窗 */
.pay-dialog :deep(.el-dialog) { border-radius: 24px !important; }
.pay-content { padding: 0 8px; }
.pay-amount { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; }
.pay-label { font-size: 15px; color: #606266; }
.pay-price { font-size: 32px; font-weight: 800; color: #f56c6c; }
.pay-method { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; }
.result-order-info { margin-top: 12px; text-align: center; color: #909399; font-size: 13px; }

/* 地址选择步骤 */
.address-step { padding: 0 4px; }
.address-step-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #303133; }
.address-list { max-height: 280px; overflow-y: auto; }
.address-item { display: flex; align-items: flex-start; gap: 12px; padding: 14px; border: 2px solid #ebeef5; border-radius: 12px; margin-bottom: 10px; cursor: pointer; transition: all 0.25s; }
.address-item:hover { border-color: #a78bfa; background: rgba(167,139,250,0.04); }
.address-item.selected { border-color: #a78bfa; background: rgba(167,139,250,0.08); }
.addr-radio { padding-top: 2px; }
.addr-info { flex: 1; }
.addr-name { font-size: 15px; font-weight: 600; color: #303133; }
.addr-phone { font-size: 14px; color: #909399; margin-left: 12px; font-weight: 400; }
.addr-detail { font-size: 13px; color: #909399; margin-top: 4px; }
.addr-quick-add { text-align: center; margin-top: 16px; }
</style>
<template>
  <div class="borrow-page">
    <el-tabs v-model="activeTab" type="card" class="borrow-tabs">
      <el-tab-pane label="我的借阅" name="myBorrows">
        <div v-loading="loading" class="tab-content">
          <el-empty v-if="!loading && borrows.length === 0" description="暂无借阅记录" />
          <template v-else-if="borrows.length > 0">
            <el-table :data="borrows" stripe style="width: 100%">
              <el-table-column prop="book.title" label="书名" min-width="150" />
              <el-table-column prop="book.author" label="作者" width="120" />
              <el-table-column prop="borrowDate" label="借阅日期" width="120">
                <template #default="{ row }">
                  {{ formatDate(row.borrowDate) }}
                </template>
              </el-table-column>
              <el-table-column prop="dueDate" label="应还日期" width="140">
                <template #default="{ row }">
                  <span :class="{ 'overdue-text': isOverdue(row) && row.status === 'BORROWING' }">
                    {{ formatDate(row.dueDate) }}
                    <span v-if="row.status === 'BORROWING'" class="heartbeat-icon" :class="heartClass(row)">❤</span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="returnDate" label="归还日期" width="120">
                <template #default="{ row }">
                  {{ row.returnDate ? formatDate(row.returnDate) : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="renewCount" label="续借" width="70" />
              <el-table-column label="逾期罚款" width="120">
                <template #default="{ row }">
                  <span v-if="calcFine(row) > 0" class="fine-text">
                    ¥{{ calcFine(row).toFixed(1) }}
                    <el-tag v-if="row.finePaid" type="success" size="small" effect="plain">已支付</el-tag>
                    <el-tag v-else type="danger" size="small" effect="plain">未支付</el-tag>
                  </span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="320" fixed="right">
                <template #default="{ row }">
                  <div class="action-btns">
                    <el-button v-if="row.status === 'BORROWING' && canCancel(row)" type="info" size="small" @click="handleCancelBorrow(row)">撤销</el-button>
                    <el-button v-if="row.status === 'BORROWING' && isOverdue(row) && calcFine(row) > 0 && !row.finePaid" type="danger" size="small" @click="handlePayFine(row)">缴纳罚款</el-button>
                    <el-button v-if="row.status === 'BORROWING' && (!isOverdue(row) || calcFine(row) <= 0 || row.finePaid)" type="success" size="small" @click="handleReturn(row)">归还</el-button>
                    <el-button v-if="row.status === 'BORROWING' && row.renewCount < 2" type="warning" size="small" @click="handleRenew(row)">续借</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination v-if="borrowTotal > 0" class="pagination" v-model:current-page="borrowPage" :page-size="borrowSize" :total="borrowTotal" layout="prev, pager, next" @current-change="fetchMyBorrows" />
          </template>
        </div>
      </el-tab-pane>

      <el-tab-pane name="myReservations">
        <template #label>
          我的预约
          <el-badge v-if="pendingResCount > 0" :value="pendingResCount" class="tab-badge" />
        </template>
        <div v-loading="resLoading" class="tab-content">
          <el-empty v-if="!resLoading && reservations.length === 0" description="暂无预约记录" />
          <template v-else-if="reservations.length > 0">
            <el-table :data="reservations" stripe style="width: 100%">
              <el-table-column prop="book.title" label="书名" min-width="150" />
              <el-table-column prop="book.author" label="作者" width="120" />
              <el-table-column prop="reserveDate" label="预约日期" width="120">
                <template #default="{ row }">{{ formatDate(row.reserveDate) }}</template>
              </el-table-column>
              <el-table-column prop="expireDate" label="过期日期" width="120">
                <template #default="{ row }">{{ formatDate(row.expireDate) }}</template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="resStatusType(row.status)" size="small">{{ resStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'PENDING'" type="danger" size="small" @click="handleCancelReservation(row)">取消预约</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination v-if="resTotal > 0" class="pagination" v-model:current-page="resPage" :page-size="resSize" :total="resTotal" layout="prev, pager, next" @current-change="fetchMyReservations" />
          </template>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="isAdmin" label="全部记录" name="allBorrows">
        <div class="tab-content">
          <div class="search-bar">
            <el-input v-model="allKeyword" placeholder="搜索书名或用户名..." clearable style="width: 300px" @keyup.enter="fetchAllBorrows" @clear="fetchAllBorrows" />
            <el-button type="primary" @click="fetchAllBorrows">搜索</el-button>
          </div>
          <div v-loading="allLoading">
            <el-table :data="allBorrows" stripe style="width: 100%" v-if="allBorrows.length > 0">
              <el-table-column prop="book.title" label="书名" min-width="150" />
              <el-table-column prop="user.username" label="借阅人" width="100" />
              <el-table-column prop="borrowDate" label="借阅日期" width="120">
                <template #default="{ row }">{{ formatDate(row.borrowDate) }}</template>
              </el-table-column>
              <el-table-column prop="dueDate" label="应还日期" width="120">
                <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
              </el-table-column>
              <el-table-column prop="returnDate" label="归还日期" width="120">
                <template #default="{ row }">{{ row.returnDate ? formatDate(row.returnDate) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="fine" label="罚款" width="90">
                <template #default="{ row }">
                  <span v-if="row.fine > 0" class="fine-text">¥{{ row.fine.toFixed(1) }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="260" fixed="right">
                <template #default="{ row }">
                  <div class="action-btns">
                    <el-button v-if="row.status === 'BORROWING' && isOverdue(row) && calcFine(row) > 0 && !row.finePaid" type="danger" size="small" @click="handlePayFine(row)">缴纳罚款</el-button>
                    <el-button v-if="row.status === 'BORROWING' && (!isOverdue(row) || calcFine(row) <= 0 || row.finePaid)" type="success" size="small" @click="handleReturn(row)">归还</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!allLoading && allBorrows.length === 0" description="暂无数据" />
            <el-pagination v-if="allTotal > 0" class="pagination" v-model:current-page="allPage" :page-size="allSize" :total="allTotal" layout="prev, pager, next" @current-change="fetchAllBorrows" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 归还弹窗 -->
    <el-dialog v-model="returnDialogVisible" title="归还图书" width="500px" :close-on-click-modal="false" class="return-dialog">
      <div v-if="returnRecord" class="return-dialog-content">
        <div class="return-book-info">
          <div class="return-book-cover" :style="{ background: coverColors[(returnRecord.book?.id || 0) % coverColors.length] }">
            <span>{{ returnRecord.book?.title?.charAt(0) }}</span>
          </div>
          <div class="return-book-meta">
            <h4>{{ returnRecord.book?.title }}</h4>
            <p>作者：{{ returnRecord.book?.author }}</p>
          </div>
        </div>
        <el-descriptions :column="1" border style="margin-top:16px">
          <el-descriptions-item label="借阅日期">{{ formatDate(returnRecord.borrowDate) }}</el-descriptions-item>
          <el-descriptions-item label="应还日期">
            <span :class="{ 'overdue-text': isOverdue(returnRecord) }">{{ formatDate(returnRecord.dueDate) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="isOverdue(returnRecord)" label="逾期天数">
            <span class="overdue-text">{{ overdueDays(returnRecord) }} 天</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="calcFine(returnRecord) > 0" label="逾期罚款">
            <span class="fine-text">¥{{ calcFine(returnRecord).toFixed(1) }}</span>
            <el-tag v-if="returnRecord.finePaid" type="success" size="small" style="margin-left:8px">已支付</el-tag>
            <el-tag v-else type="danger" size="small" style="margin-left:8px">未支付</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="isOverdue(returnRecord) && calcFine(returnRecord) > 0 && !returnRecord.finePaid" class="return-warning">
          <el-alert title="您有逾期罚款未支付，请先支付罚款后再归还" type="warning" :closable="false" show-icon />
          <el-button type="danger" style="margin-top:12px;width:100%" @click="handlePayFineInDialog" :loading="payingFine">
            支付罚款 ¥{{ calcFine(returnRecord).toFixed(1) }} 并归还
          </el-button>
        </div>
        <div v-else-if="isOverdue(returnRecord) && calcFine(returnRecord) > 0 && returnRecord.finePaid" class="return-info">
          <el-alert title="罚款已支付，可以归还" type="success" :closable="false" show-icon />
        </div>
        <div v-else class="return-info">
          <el-alert title="确认归还此书？" type="info" :closable="false" show-icon />
        </div>
      </div>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReturn" :loading="returnSubmitting" :disabled="isOverdue(returnRecord) && calcFine(returnRecord) > 0 && !returnRecord.finePaid">确认归还</el-button>
      </template>
    </el-dialog>
  <!-- 罚款支付弹窗 -->
    <el-dialog v-model="fineDialogVisible" title="缴纳罚款" width="500px" :close-on-click-modal="false" class="fine-dialog">
      <div v-if="fineRecord" class="fine-dialog-content">
        <div class="fine-book-info">
          <div class="fine-book-cover" :style="{ background: coverColors[(fineRecord.book?.id || 0) % coverColors.length] }">
            <span>{{ fineRecord.book?.title?.charAt(0) }}</span>
          </div>
          <div class="fine-book-meta">
            <h4>{{ fineRecord.book?.title }}</h4>
            <p>作者：{{ fineRecord.book?.author }}</p>
          </div>
        </div>
        <el-descriptions :column="1" border style="margin-top:16px">
          <el-descriptions-item label="借阅日期">{{ formatDate(fineRecord.borrowDate) }}</el-descriptions-item>
          <el-descriptions-item label="应还日期">
            <span class="overdue-text">{{ formatDate(fineRecord.dueDate) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="逾期天数">
            <span class="overdue-text">{{ overdueDays(fineRecord) }} 天</span>
          </el-descriptions-item>
          <el-descriptions-item label="罚款金额">
            <span class="fine-text">¥{{ calcFine(fineRecord).toFixed(1) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-alert title="支付罚款后即可归还图书" type="warning" :closable="false" show-icon style="margin-top:16px" />
      </div>
      <template #footer>
        <el-button @click="fineDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmPayFine" :loading="finePaying">确认支付 ¥{{ fineRecord ? calcFine(fineRecord).toFixed(1) : '0.00' }}</el-button>
      </template>
    </el-dialog>
    <!-- 时空胶囊留言弹窗 -->
    <el-dialog v-model="capsuleDialogVisible" title="时空胶囊" width="480px" :close-on-click-modal="false" center class="capsule-dialog">
      <div class="capsule-content">
        <div class="capsule-icon">📮</div>
        <p class="capsule-desc">请留下时空胶囊留言（可选）</p>
        <el-input
          v-model="capsuleContent"
          type="textarea"
          :rows="3"
          placeholder="比如：希望下一个读这本书的你，能感受到温暖"
          maxlength="200"
          show-word-limit
        />
      </div>
      <template #footer>
        <el-button @click="capsuleDialogVisible = false; resolveCapsule(null)">跳过</el-button>
        <el-button type="primary" @click="capsuleDialogVisible = false; resolveCapsule(capsuleContent)">留下留言</el-button>
      </template>
    </el-dialog>

    <!-- 取消预约确认弹窗 -->
    <el-dialog v-model="cancelReservationVisible" title="取消预约" width="440px" top="20vh" :close-on-click-modal="false" center append-to-body>
      <div class="cancel-res-body">
        <div class="cancel-res-icon">⚠️</div>
        <p class="cancel-res-text">确定要取消对《{{ cancelReservationBook?.title }}》的预约吗？</p>
        <p class="cancel-res-hint">取消后不可恢复，如有需要请重新预约。</p>
      </div>
      <template #footer>
        <el-button @click="cancelReservationVisible = false">保留预约</el-button>
        <el-button type="danger" @click="confirmCancelReservation" :loading="cancelResSubmitting">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { getMyBorrowHistory, getMyReservations, getAllBorrows, returnBook, renewBook, cancelReservation, payFine, cancelBorrow, leaveCapsule } from '../api/borrow'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('myBorrows')
const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN')

const loading = ref(false)
const borrows = ref([])
const borrowPage = ref(1)
const borrowSize = ref(10)
const borrowTotal = ref(0)

const resLoading = ref(false)
const reservations = ref([])
const resPage = ref(1)
const resSize = ref(10)
const resTotal = ref(0)
const pendingResCount = ref(0)

const allLoading = ref(false)
const allBorrows = ref([])
const allPage = ref(1)
const allSize = ref(10)
const allTotal = ref(0)
const allKeyword = ref('')

const returnDialogVisible = ref(false)
const returnRecord = ref(null)
const returnSubmitting = ref(false)
const payingFine = ref(false)

const fineDialogVisible = ref(false)
const fineRecord = ref(null)
const finePaying = ref(false)

const capsuleDialogVisible = ref(false)
const capsuleContent = ref('')
const capsuleBookId = ref(null)
let resolveCapsule = () => {}

const cancelReservationVisible = ref(false)
const cancelReservationBook = ref(null)
const cancelResSubmitting = ref(false)

function showCapsuleDialog(bookId) {
  return new Promise((resolve) => {
    capsuleBookId.value = bookId
    capsuleContent.value = ''
    resolveCapsule = (value) => {
      resolve(value)
    }
    capsuleDialogVisible.value = true
  })
}

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

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

function isOverdue(row) {
  if (!row || !row.dueDate) return false
  return new Date(row.dueDate) < new Date()
}

function overdueDays(row) {
  if (!row || !row.dueDate) return 0
  const due = new Date(row.dueDate)
  const now = new Date()
  return Math.max(0, Math.ceil((now - due) / (1000 * 60 * 60 * 24)))
}

function calcFine(row) {
  if (!row || !row.dueDate || row.status !== 'BORROWING') return row?.fine || 0
  // If fine is already calculated and saved, use it
  if (row.fine > 0) return row.fine
  // Otherwise calculate locally
  if (!isOverdue(row)) return 0
  return overdueDays(row) * 0.5
}

function statusType(status) {
  const map = { BORROWING: 'warning', RETURNED: 'success', OVERDUE: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { BORROWING: '借阅中', RETURNED: '已归还', OVERDUE: '已逾期' }
  return map[status] || status
}

function resStatusType(status) {
  const map = { PENDING: 'warning', FULFILLED: 'success', CANCELLED: 'info', EXPIRED: 'danger' }
  return map[status] || 'info'
}

function resStatusLabel(status) {
  const map = { PENDING: '待处理', FULFILLED: '已完成', CANCELLED: '已取消', EXPIRED: '已过期' }
  return map[status] || status
}

function heartClass(row) {
  if (!row.dueDate) return 'heart-green'
  const now = new Date()
  const due = new Date(row.dueDate)
  const daysLeft = Math.ceil((due - now) / (1000 * 60 * 60 * 24))
  if (daysLeft <= 1) return 'heart-red heart-shake'
  if (daysLeft <= 3) return 'heart-red'
  if (daysLeft <= 7) return 'heart-yellow'
  return 'heart-green'
}

function canCancel(row) {
  if (!row.createdAt) return false
  const created = new Date(row.createdAt)
  const now = new Date()
  const diffMinutes = (now - created) / 1000 / 60
  return diffMinutes <= 5
}

async function fetchMyBorrows() {
  loading.value = true
  try {
    const res = await getMyBorrowHistory({ page: borrowPage.value - 1, size: borrowSize.value })
    borrows.value = res.data?.content || []
    borrowTotal.value = res.data?.totalElements || 0
  } catch (e) {
    borrows.value = []
    borrowTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchMyReservations() {
  resLoading.value = true
  try {
    const res = await getMyReservations({ page: resPage.value - 1, size: resSize.value })
    reservations.value = res.data?.content || []
    resTotal.value = res.data?.totalElements || 0
    pendingResCount.value = (res.data?.content || []).filter(r => r.status === 'PENDING').length
  } catch (e) {
    reservations.value = []
    resTotal.value = 0
    // 保留之前的pendingResCount，不重置为0
  } finally {
    resLoading.value = false
  }
}

async function fetchAllBorrows() {
  allLoading.value = true
  try {
    const res = await getAllBorrows({ keyword: allKeyword.value, page: allPage.value - 1, size: allSize.value })
    allBorrows.value = res.data.content
    allTotal.value = res.data.totalElements
  } catch (e) { /* handled */ } finally {
    allLoading.value = false
  }
}

async function handleReturn(row) {
  returnRecord.value = row
  returnDialogVisible.value = true
}

async function confirmReturn() {
  if (!returnRecord.value) return
  returnSubmitting.value = true
  try {
    await returnBook(returnRecord.value.id)
    ElMessage.success('归还成功')
    returnDialogVisible.value = false
    fetchMyBorrows()
    // 弹出居中的时空胶囊留言弹窗
    try {
      const capsuleContent = await showCapsuleDialog(returnRecord.value.book.id)
      if (capsuleContent && capsuleContent.trim()) {
        await leaveCapsule(returnRecord.value.book.id, capsuleContent.trim())
        ElMessage.success('时空胶囊已留下')
      }
    } catch (e) { /* user skipped */ }
  } catch (e) {
    // error shown by interceptor
  } finally {
    returnSubmitting.value = false
  }
}

async function handlePayFineInDialog() {
  if (!returnRecord.value) return
  payingFine.value = true
  try {
    await payFine(returnRecord.value.id)
    returnRecord.value.finePaid = true
    ElMessage.success('罚款已支付，正在归还...')
    await returnBook(returnRecord.value.id)
    ElMessage.success('归还成功')
    returnDialogVisible.value = false
    fetchMyBorrows()
    // 弹出居中的时空胶囊留言弹窗
    try {
      const capsuleContent = await showCapsuleDialog(returnRecord.value.book.id)
      if (capsuleContent && capsuleContent.trim()) {
        await leaveCapsule(returnRecord.value.book.id, capsuleContent.trim())
        ElMessage.success('时空胶囊已留下')
      }
    } catch (e) { /* user skipped */ }
  } catch (e) {
    // error shown by interceptor
  } finally {
    payingFine.value = false
  }
}

async function handlePayFine(row) {
  const fine = calcFine(row)
  if (fine <= 0) {
    ElMessage.warning('该记录无逾期罚款')
    return
  }
  fineRecord.value = { ...row }
  await nextTick()
  fineDialogVisible.value = true
}

async function confirmPayFine() {
  if (!fineRecord.value) return
  finePaying.value = true
  try {
    const res = await payFine(fineRecord.value.id)
    // 更新本地记录状态
    fineRecord.value.finePaid = true
    fineRecord.value.fine = res.data?.fine || fineRecord.value.fine
    ElMessage.success('罚款已支付，请点击归还按钮完成归还')
    fineDialogVisible.value = false
    fineRecord.value = null
    fetchMyBorrows()
  } catch (e) {
    // error shown by interceptor
  } finally {
    finePaying.value = false
  }
}

async function handleRenew(row) {
  try {
    await renewBook(row.id)
    ElMessage.success('续借成功')
    fetchMyBorrows()
  } catch (e) { /* handled */ }
}

async function handleCancelReservation(row) {
  cancelReservationBook.value = row.book
  cancelReservationVisible.value = true
}

async function confirmCancelReservation() {
  cancelResSubmitting.value = true
  try {
    const row = reservations.value.find(r => r.book?.id === cancelReservationBook.value?.id && r.status === 'PENDING')
    if (!row) {
      ElMessage.error('预约信息不存在')
      return
    }
    await cancelReservation(row.id)
    ElMessage.success('已取消预约')
    cancelReservationVisible.value = false
    fetchMyReservations()
  } catch (e) {
    // error shown by interceptor
  } finally {
    cancelResSubmitting.value = false
  }
}

async function handleCancelBorrow(row) {
  try {
    await ElMessageBox.confirm(
      `确定要撤销《${row.book?.title || '此书'}》的借阅记录吗？撤销后图书库存将恢复。`,
      '撤销借阅',
      { confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning' }
    )
    await cancelBorrow(row.id)
    ElMessage.success('借阅已撤销')
    fetchMyBorrows()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      // error already shown by interceptor
    }
  }
}

watch(activeTab, (tab) => {
  if (tab === 'myBorrows') fetchMyBorrows()
  else if (tab === 'myReservations') fetchMyReservations()
  else if (tab === 'allBorrows') fetchAllBorrows()
})

onMounted(() => {
  fetchMyBorrows()
  fetchMyReservations()
})
</script>

<style scoped>
.borrow-page {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  min-height: 400px;
}

.borrow-tabs {
  width: 100%;
}

.tab-content {
  padding: 16px 0;
  min-height: 300px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}

.overdue-text {
  color: #f56c6c;
  font-weight: 600;
}

.fine-text {
  color: #f56c6c;
  font-weight: 600;
}

.tab-badge {
  margin-left: 6px;
}

.action-btns {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 归还弹窗 */
.return-dialog-content {
  padding: 0;
}

.return-book-info {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.return-book-cover {
  width: 70px;
  height: 95px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
  color: rgba(255,255,255,0.85);
  flex-shrink: 0;
}

.return-book-meta h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #303133;
}

.return-book-meta p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.return-warning {
  margin-top: 16px;
}

.return-info {
  margin-top: 16px;
}

/* 罚款弹窗 */
.fine-dialog-content {
  padding: 0;
}

.fine-book-info {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 16px;
  background: #fef0f0;
  border-radius: 12px;
}

.fine-book-cover {
  width: 70px;
  height: 95px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
  color: rgba(255,255,255,0.85);
  flex-shrink: 0;
}

.fine-book-meta h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #303133;
}

.fine-book-meta p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 心跳倒计时 */
.heartbeat-icon {
  display: inline-block;
  margin-left: 4px;
  font-size: 14px;
  animation: heartbeat 1.2s ease-in-out infinite;
  vertical-align: middle;
}

.heart-green {
  color: #67c23a;
  animation: heartbeat 1.5s ease-in-out infinite;
}

.heart-yellow {
  color: #e6a23c;
  animation: heartbeat 0.9s ease-in-out infinite;
}

.heart-red {
  color: #f56c6c;
  animation: heartbeat 0.5s ease-in-out infinite;
}

.heart-shake {
  animation: heartShake 0.3s ease-in-out infinite;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  15% { transform: scale(1.3); }
  30% { transform: scale(1); }
  45% { transform: scale(1.2); }
  60% { transform: scale(1); }
}

@keyframes heartShake {
  0%, 100% { transform: scale(1) rotate(0); }
  25% { transform: scale(1.4) rotate(-5deg); }
  50% { transform: scale(1.2) rotate(0); }
  75% { transform: scale(1.4) rotate(5deg); }
}

/* 时空胶囊弹窗 */
.capsule-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.capsule-content {
  text-align: center;
  padding: 10px 0;
}

.capsule-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: float 2s ease-in-out infinite;
}

.capsule-desc {
  color: #606266;
  font-size: 15px;
  margin-bottom: 16px;
  line-height: 1.6;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.cancel-res-body { text-align: center; padding: 10px 0; }
.cancel-res-icon { font-size: 48px; margin-bottom: 16px; }
.cancel-res-text { font-size: 15px; color: #303133; margin-bottom: 8px; font-weight: 500; }
.cancel-res-hint { font-size: 13px; color: #909399; }
</style>
<template>
  <div class="blind-box-page">
    <div class="blind-box-container">
      <h2 class="blind-box-title">
        <el-icon :size="28" color="#e6a23c"><Present /></el-icon>
        今天读什么？
      </h2>
      <p class="subtitle">选择困难症？让AI帮你选一本好书</p>

      <div v-if="!revealed" class="box-stage">
        <div class="gift-box-wrapper" :class="{ shaking: loading }">
          <div class="gift-box">
            <div class="gift-box-lid"></div>
            <div class="gift-box-body">
              <span class="question-mark">?</span>
            </div>
          </div>
          <div class="sparkles" v-if="loading">
            <span class="sparkle" v-for="i in 8" :key="i" :style="sparkleStyle(i)">✦</span>
          </div>
        </div>
        <p v-if="!loading" class="hint-text">点击下方按钮，为你推荐一本好书</p>
        <p v-else class="loading-text">正在为你精心挑选...</p>
        <el-button
          type="warning"
          size="large"
          @click="openBox"
          :loading="loading"
          :disabled="loading"
          class="open-btn"
          round
        >
          <el-icon><Present /></el-icon>
          {{ loading ? '挑选中...' : '打开盲盒' }}
        </el-button>
      </div>

      <div v-else class="result-stage">
        <div class="book-reveal">
          <div class="book-cover" :style="{ background: coverColors[book.id % coverColors.length] }">
            <span class="cover-char">{{ book.title?.charAt(0) }}</span>
          </div>
          <div class="book-details">
            <h3>{{ book.title }}</h3>
            <div class="detail-row">
              <el-icon><User /></el-icon>
              <span>作者：{{ book.author }}</span>
            </div>
            <div class="detail-row">
              <el-icon><Document /></el-icon>
              <span>ISBN：{{ book.isbn }}</span>
            </div>
            <div class="detail-row">
              <el-tag type="primary" size="small">{{ book.category || '未分类' }}</el-tag>
              <el-tag type="success" size="small" v-if="book.publisher">{{ book.publisher }}</el-tag>
            </div>
            <div class="detail-row">
              <span>页数：{{ book.pageCount || '-' }} 页</span>
              <span>出版日期：{{ book.publicationDate || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="stock">库存：{{ book.availableCopies || 0 }}/{{ book.totalCopies || 0 }}</span>
            </div>
            <p class="book-desc">{{ book.description }}</p>
            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                @click="handleBorrow"
                v-if="(book.availableCopies || 0) > 0"
                round
              >
                立即借阅
              </el-button>
              <el-button
                type="warning"
                size="large"
                @click="handleReserve"
                v-else
                round
              >
                预约此书
              </el-button>
              <el-button size="large" @click="openBox" round>再抽一本</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 借阅弹窗 -->
    <el-dialog v-model="borrowDialogVisible" title="借阅图书" width="480px" top="5vh" :close-on-click-modal="false">
      <div v-if="borrowBookInfo" class="borrow-form">
        <el-form :model="borrowForm" label-width="100px">
          <el-form-item label="借阅日期">
            <el-date-picker
              v-model="borrowForm.borrowDate"
              type="date"
              placeholder="选择借阅日期（默认今天）"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
            />
          </el-form-item>
          <el-form-item label="归还日期">
            <el-date-picker
              v-model="borrowForm.dueDate"
              type="date"
              placeholder="选择归还日期（默认30天后）"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDueDate"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBorrow" :loading="borrowSubmitting">确认借阅</el-button>
      </template>
    </el-dialog>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveDialogVisible" title="预约图书" width="480px" top="5vh" :close-on-click-modal="false">
      <p>确定要预约《{{ book?.title }}》吗？</p>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReserve" :loading="reserveSubmitting">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Present, User, Document } from '@element-plus/icons-vue'
import { getRandomBook } from '../api/stats'
import { borrowBook, reserveBook } from '../api/borrow'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const revealed = ref(false)
const book = ref({})

const borrowDialogVisible = ref(false)
const borrowBookInfo = ref(null)
const borrowSubmitting = ref(false)
const borrowForm = ref({ borrowDate: '', dueDate: '' })

const reserveDialogVisible = ref(false)
const reserveSubmitting = ref(false)

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

function sparkleStyle(i) {
  const angle = (i / 8) * 360
  const distance = 80 + Math.random() * 40
  const x = Math.cos(angle * Math.PI / 180) * distance
  const y = Math.sin(angle * Math.PI / 180) * distance
  return {
    '--x': x + 'px',
    '--y': y + 'px',
    animationDelay: (i * 0.1) + 's',
    color: ['#f6d365', '#fda085', '#f093fb', '#f5576c', '#4facfe', '#00f2fe'][i % 6]
  }
}

function disabledDate(time) {
  return time.getTime() > Date.now()
}

function disabledDueDate(time) {
  if (borrowForm.value.borrowDate) {
    return time.getTime() < new Date(borrowForm.value.borrowDate).getTime()
  }
  return false
}

async function openBox() {
  loading.value = true
  revealed.value = false
  // 模拟开盒动画
  await new Promise(r => setTimeout(r, 1800))
  try {
    const res = await getRandomBook()
    book.value = res.data
    revealed.value = true
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

function handleBorrow() {
  borrowBookInfo.value = book.value
  borrowForm.value = { borrowDate: '', dueDate: '' }
  borrowDialogVisible.value = true
}

async function confirmBorrow() {
  borrowSubmitting.value = true
  try {
    const body = {}
    if (borrowForm.value.borrowDate) {
      body.borrowDate = borrowForm.value.borrowDate + 'T00:00:00'
    }
    if (borrowForm.value.dueDate) {
      body.dueDate = borrowForm.value.dueDate + 'T00:00:00'
    }
    await borrowBook(borrowBookInfo.value.id, body)
    borrowDialogVisible.value = false
    ElMessage.success('借阅成功')
  } catch (e) {
    // handled
  } finally {
    borrowSubmitting.value = false
  }
}

function handleReserve() {
  reserveDialogVisible.value = true
}

async function confirmReserve() {
  reserveSubmitting.value = true
  try {
    await reserveBook(book.value.id)
    ElMessage.success('预约成功')
    reserveDialogVisible.value = false
  } catch (e) {
    // handled
  } finally {
    reserveSubmitting.value = false
  }
}
</script>

<style scoped>
.blind-box-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 160px);
}

.blind-box-container {
  text-align: center;
  max-width: 700px;
  width: 100%;
}

.blind-box-title {
  font-size: 28px;
  color: #303133;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0 0 40px;
}

.box-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.gift-box-wrapper {
  position: relative;
  width: 200px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gift-box-wrapper.shaking {
  animation: wrapper-shake 0.3s infinite;
}

@keyframes wrapper-shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-6deg); }
  75% { transform: rotate(6deg); }
}

.gift-box {
  width: 140px;
  height: 140px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
}

.gift-box-lid {
  width: 150px;
  height: 35px;
  background: linear-gradient(135deg, #f6d365, #fda085);
  border-radius: 10px 10px 0 0;
  position: relative;
  top: 3px;
  z-index: 3;
  box-shadow: 0 4px 12px rgba(246, 211, 101, 0.4);
}

.gift-box-body {
  width: 140px;
  height: 105px;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  border-radius: 0 0 16px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(240, 147, 251, 0.35);
}

.question-mark {
  font-size: 56px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.sparkles {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
}

.sparkle {
  position: absolute;
  font-size: 16px;
  animation: sparkle-fly 1.5s ease-out infinite;
}

@keyframes sparkle-fly {
  0% {
    transform: translate(0, 0) scale(0);
    opacity: 1;
  }
  100% {
    transform: translate(var(--x), var(--y)) scale(1.2);
    opacity: 0;
  }
}

.hint-text {
  font-size: 15px;
  color: #c0c4cc;
  margin: 0;
}

.loading-text {
  font-size: 15px;
  color: #e6a23c;
  margin: 0;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.open-btn {
  padding: 12px 40px;
  font-size: 16px;
}

.result-stage {
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.book-reveal {
  display: flex;
  gap: 32px;
  text-align: left;
  background: #fff;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.book-cover {
  width: 180px;
  height: 240px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.cover-char {
  font-size: 72px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.book-details {
  flex: 1;
}

.book-details h3 {
  font-size: 22px;
  color: #303133;
  margin: 0 0 16px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.detail-row .stock {
  color: #67c23a;
  font-weight: 500;
}

.book-desc {
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
  margin: 16px 0;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

@media (max-width: 700px) {
  .book-reveal {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .book-cover {
    width: 140px;
    height: 190px;
  }
  .action-buttons {
    justify-content: center;
  }
}
</style>
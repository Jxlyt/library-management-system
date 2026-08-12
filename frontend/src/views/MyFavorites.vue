<template>
  <div class="favorites-page">
    <el-card shadow="hover" class="favorites-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="20" color="#e6a23c"><StarFilled /></el-icon>
          <span>我的收藏</span>
        </div>
      </template>

      <div v-loading="loading" class="book-grid">
        <el-empty v-if="!loading && favorites.length === 0" description="暂无收藏的图书" />
        <div
          v-for="item in favorites"
          :key="item.id"
          class="book-card"
        >
          <div
            class="book-cover"
            :style="{ background: coverColors[item.book.id % coverColors.length] }"
          >
            <span class="cover-char">{{ item.book.title?.charAt(0) }}</span>
          </div>
          <div class="book-info">
            <h4 class="book-name">{{ item.book.title }}</h4>
            <div class="book-meta">
              <el-icon :size="14"><User /></el-icon>
              <span>{{ item.book.author }}</span>
            </div>
            <div class="book-meta">
              <el-tag size="small" type="primary">{{ item.book.category || '未分类' }}</el-tag>
            </div>
            <div class="book-meta">
              <span class="stock">库存: {{ item.book.availableCopies || 0 }}/{{ item.book.totalCopies || 0 }}</span>
            </div>
            <div class="book-actions">
              <el-button
                v-if="(item.book.availableCopies || 0) > 0"
                type="primary"
                size="small"
                @click="handleBorrow(item.book)"
              >
                借阅
              </el-button>
              <el-button
                v-else
                type="warning"
                size="small"
                @click="handleReserve(item.book)"
              >
                预约
              </el-button>
              <el-button
                type="danger"
                size="small"
                text
                @click="handleRemoveFavorite(item)"
              >
                取消收藏
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 借阅弹窗 -->
    <el-dialog v-model="borrowDialogVisible" title="借阅图书" width="480px" top="5vh" :close-on-click-modal="false">
      <div v-if="borrowBookInfo" class="borrow-form">
        <div class="borrow-book-header">
          <div class="borrow-book-img-placeholder" :style="{ background: coverColors[borrowBookInfo.id % coverColors.length] }">
            <span class="borrow-cover-char">{{ borrowBookInfo.title?.charAt(0) }}</span>
          </div>
          <div class="borrow-book-meta">
            <h4>{{ borrowBookInfo.title }}</h4>
            <p>作者：{{ borrowBookInfo.author }}</p>
            <p>库存：{{ borrowBookInfo.availableCopies || 0 }}/{{ borrowBookInfo.totalCopies || 0 }}</p>
          </div>
        </div>
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
        <el-button type="primary" @click="confirmBorrow" :loading="borrowSubmitting">
          确认借阅
        </el-button>
      </template>
    </el-dialog>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveDialogVisible" title="预约图书" width="480px" top="5vh" :close-on-click-modal="false">
      <div v-if="reserveBookInfo" class="borrow-form">
        <div class="borrow-book-header">
          <div class="borrow-book-img-placeholder" :style="{ background: coverColors[reserveBookInfo.id % coverColors.length] }">
            <span class="borrow-cover-char">{{ reserveBookInfo.title?.charAt(0) }}</span>
          </div>
          <div class="borrow-book-meta">
            <h4>{{ reserveBookInfo.title }}</h4>
            <p>作者：{{ reserveBookInfo.author }}</p>
            <p>库存：0/{{ reserveBookInfo.totalCopies || 0 }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReserve" :loading="reserveSubmitting">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { StarFilled, User } from '@element-plus/icons-vue'
import { getMyFavorites, removeFavorite } from '../api/social'
import { borrowBook, reserveBook } from '../api/borrow'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const favorites = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const borrowDialogVisible = ref(false)
const borrowBookInfo = ref(null)
const borrowSubmitting = ref(false)
const borrowForm = ref({ borrowDate: '', dueDate: '' })

const reserveDialogVisible = ref(false)
const reserveBookInfo = ref(null)
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

function disabledDate(time) {
  return time.getTime() > Date.now()
}

function disabledDueDate(time) {
  if (borrowForm.value.borrowDate) {
    return time.getTime() < new Date(borrowForm.value.borrowDate).getTime()
  }
  return false
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyFavorites({ page: currentPage.value - 1, size: pageSize.value })
    favorites.value = res.data.content
    total.value = res.data.totalElements
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

async function handleRemoveFavorite(item) {
  try {
    await ElMessageBox.confirm(
      `确定要取消收藏《${item.book?.title || '此书'}》吗？`,
      '取消收藏',
      { confirmButtonText: '确认取消', cancelButtonText: '保留', type: 'warning', center: true }
    )
    await removeFavorite(item.book.id)
    ElMessage.success('已取消收藏')
    fetchData()
  } catch (e) {
    // handled
  }
}

function handleBorrow(book) {
  borrowBookInfo.value = book
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
    fetchData()
  } catch (e) {
    // handled
  } finally {
    borrowSubmitting.value = false
  }
}

function handleReserve(book) {
  reserveBookInfo.value = book
  reserveDialogVisible.value = true
}

async function confirmReserve() {
  reserveSubmitting.value = true
  try {
    await reserveBook(reserveBookInfo.value.id)
    ElMessage.success('预约成功')
    reserveDialogVisible.value = false
  } catch (e) {
    // handled
  } finally {
    reserveSubmitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.favorites-page {
  display: flex;
  flex-direction: column;
}

.favorites-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
}

.book-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.book-cover {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.cover-char {
  font-size: 48px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.book-info {
  padding: 14px 16px;
}

.book-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stock {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.book-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.borrow-book-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.borrow-book-img-placeholder {
  width: 80px;
  height: 110px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.borrow-cover-char {
  font-size: 36px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
}

.borrow-book-meta h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.borrow-book-meta p {
  margin: 0 0 4px;
  font-size: 13px;
  color: #909399;
}

@media (max-width: 1200px) {
  .book-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .book-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .book-grid { grid-template-columns: 1fr; }
}
</style>
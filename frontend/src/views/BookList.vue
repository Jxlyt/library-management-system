<template>
  <div class="book-list-container">
    <!-- 搜索工具栏 -->
    <el-card shadow="hover" class="toolbar-card">
      <div class="toolbar">
        <div class="search-wrap">
          <el-input
            v-model="keyword"
            placeholder="搜索书名、作者、ISBN、分类、出版社..."
            clearable
            prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="fetchData"
            class="search-input"
          />
        </div>
        <div class="actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button type="success" @click="handleAdd" v-if="isAdmin">
            <el-icon><Plus /></el-icon>
            新增图书
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 图书列表 -->
    <el-card shadow="hover" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-detail">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="书名" :span="2">{{ row.title }}</el-descriptions-item>
                <el-descriptions-item label="作者">{{ row.author }}</el-descriptions-item>
                <el-descriptions-item label="ISBN">{{ row.isbn }}</el-descriptions-item>
                <el-descriptions-item label="分类">
                  <el-tag size="small" type="primary">{{ row.category || '未分类' }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="出版社">{{ row.publisher || '-' }}</el-descriptions-item>
                <el-descriptions-item label="页数">{{ row.pageCount ? row.pageCount + ' 页' : '-' }}</el-descriptions-item>
                <el-descriptions-item label="出版日期">{{ row.publicationDate || '-' }}</el-descriptions-item>
                <el-descriptions-item label="描述" :span="2">{{ row.description || '暂无描述' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column align="center" prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="书名" min-width="180">
          <template #default="{ row }">
            <span class="book-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.category" size="small" type="primary">{{ row.category }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="publisher" label="出版社" min-width="140">
          <template #default="{ row }">
            <span v-if="row.publisher">{{ row.publisher }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.salePrice" class="price-cell">
              <span v-if="row.discount" class="original-price">¥{{ row.salePrice.toFixed(2) }}</span>
              <span class="current-price">¥{{ (row.salePrice * (row.discount || 1)).toFixed(2) }}</span>
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="saleableCopies" label="可售" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.salePrice">{{ row.saleableCopies || 0 }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="publicationDate" label="出版日期" width="120">
          <template #default="{ row }">
            <span v-if="row.publicationDate">{{ row.publicationDate }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="管理" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除该图书吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDeleteConfirm(row)"
            >
              <template #reference>
                <el-button type="danger" size="small" text>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <el-table-column label="借阅" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleBorrow(row)" v-if="(row.availableCopies || 0) > 0">
              借阅
            </el-button>
            <el-button type="warning" size="small" @click="handleReserve(row)" v-else>
              预约
            </el-button>
            <el-button
              :type="row.isFavorited ? 'warning' : 'default'"
              size="small"
              @click="handleToggleFavorite(row)"
              :icon="row.isFavorited ? 'StarFilled' : 'Star'"
              :loading="row.favLoading"
              style="margin-left: 8px"
            >
              {{ row.isFavorited ? '已收藏' : '收藏' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="fetchData"
          @current-change="fetchData"
          background
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="left">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" placeholder="请输入书名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="请输入ISBN" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" clearable style="width: 100%">
                <el-option label="文学" value="文学" />
                <el-option label="小说" value="小说" />
                <el-option label="科技" value="科技" />
                <el-option label="历史" value="历史" />
                <el-option label="哲学" value="哲学" />
                <el-option label="经济" value="经济" />
                <el-option label="教育" value="教育" />
                <el-option label="艺术" value="艺术" />
                <el-option label="计算机" value="计算机" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出版社" prop="publisher">
              <el-input v-model="form.publisher" placeholder="请输入出版社" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="页数" prop="pageCount">
              <el-input-number v-model="form.pageCount" :min="1" :max="99999" placeholder="页数" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="出版日期" prop="publicationDate">
          <el-date-picker
            v-model="form.publicationDate"
            type="date"
            placeholder="请选择出版日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入图书描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-divider>库存与销售设置</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="总库存">
              <el-input-number v-model="form.totalCopies" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可借库存">
              <el-input-number v-model="form.availableCopies" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可售库存">
              <el-input-number v-model="form.saleableCopies" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="销售价(元)">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" :step="1" placeholder="售价" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="折扣">
              <el-input-number v-model="form.discount" :min="0" :max="1" :precision="2" :step="0.1" placeholder="如0.9=9折" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 借阅弹窗 -->
    <el-dialog v-model="borrowDialogVisible" title="借阅图书" width="480px" top="5vh" :close-on-click-modal="false">
      <el-form :model="borrowForm" label-width="100px">
        <el-form-item label="借阅日期">
          <el-date-picker v-model="borrowForm.borrowDate" type="date" placeholder="默认今天" value-format="YYYY-MM-DD" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="归还日期">
          <el-date-picker v-model="borrowForm.dueDate" type="date" placeholder="默认30天后" value-format="YYYY-MM-DD" :disabled-date="disabledDueDate" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBorrow" :loading="borrowSubmitting">确认借阅</el-button>
      </template>
    </el-dialog>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveDialogVisible" title="预约图书" width="480px" top="5vh" :close-on-click-modal="false">
      <el-alert v-if="earliestReturnDate" :title="`预计最早可取书日期：${earliestReturnDate}`" type="info" :closable="false" show-icon style="margin-bottom:16px" />
      <el-alert v-else title="暂无借出记录" type="warning" :closable="false" show-icon style="margin-bottom:16px" />
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReserve" :loading="reserveSubmitting">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus, Edit, Delete, StarFilled, Star } from '@element-plus/icons-vue'
import { getBooks, createBook, updateBook, deleteBook } from '../api/book'
import { borrowBook, reserveBook, getEarliestReturnDate } from '../api/borrow'
import { addFavorite, removeFavorite, isFavorited } from '../api/social'
import { ElMessage } from 'element-plus'

const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN')

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增图书')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref(null)

const form = reactive({
  title: '',
  author: '',
  isbn: '',
  category: '',
  publisher: '',
  pageCount: null,
  description: '',
  publicationDate: '',
  totalCopies: 0,
  availableCopies: 0,
  saleableCopies: 0,
  salePrice: null,
  discount: null
})

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  isbn: [
    { required: true, message: '请输入ISBN', trigger: 'blur' }
  ]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getBooks({
      keyword: keyword.value,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    const bookList = res.data.content
    const username = localStorage.getItem('username')
    if (username && bookList.length > 0) {
      const favPromises = bookList.map(b => isFavorited(b.id).then(r => ({ id: b.id, fav: r.data })).catch(() => ({ id: b.id, fav: false })))
      const favResults = await Promise.all(favPromises)
      bookList.forEach(b => {
        const fav = favResults.find(f => f.id === b.id)
        b.isFavorited = fav ? fav.fav : false
        b.favLoading = false
      })
    }
    tableData.value = bookList
    total.value = res.data.totalElements
  } catch (e) {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增图书'
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑图书'
  Object.assign(form, {
    title: row.title,
    author: row.author,
    isbn: row.isbn,
    category: row.category || '',
    publisher: row.publisher || '',
    pageCount: row.pageCount || null,
    description: row.description || '',
    publicationDate: row.publicationDate || '',
    totalCopies: row.totalCopies || 0,
    availableCopies: row.availableCopies || 0,
    saleableCopies: row.saleableCopies || 0,
    salePrice: row.salePrice || null,
    discount: row.discount || null
  })
  dialogVisible.value = true
}

async function handleDeleteConfirm(row) {
  try {
    await deleteBook(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // 拦截器已处理
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const payload = { ...form }
    if (isEdit.value) {
      await updateBook(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createBook(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    // 拦截器已处理
  }
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    title: '', author: '', isbn: '', category: '', publisher: '',
    pageCount: null, description: '', publicationDate: '',
    totalCopies: 0, availableCopies: 0, saleableCopies: 0,
    salePrice: null, discount: null
  })
}

onMounted(() => {
  fetchData()
})

// --- 借阅/预约/收藏 ---
const borrowDialogVisible = ref(false)
const borrowBookInfo = ref(null)
const borrowSubmitting = ref(false)
const borrowForm = ref({ borrowDate: '', dueDate: '' })

const reserveDialogVisible = ref(false)
const reserveBookInfo = ref(null)
const reserveSubmitting = ref(false)
const earliestReturnDate = ref(null)

function disabledDate(time) { return time.getTime() > Date.now() }
function disabledDueDate(time) {
  if (borrowForm.value.borrowDate) return time.getTime() < new Date(borrowForm.value.borrowDate).getTime()
  return false
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
    if (borrowForm.value.borrowDate) body.borrowDate = borrowForm.value.borrowDate + 'T00:00:00'
    if (borrowForm.value.dueDate) body.dueDate = borrowForm.value.dueDate + 'T00:00:00'
    const res = await borrowBook(borrowBookInfo.value.id, body)
    ElMessage.success(res.message || '借阅成功')
    borrowDialogVisible.value = false
    fetchData()
  } catch (e) { /* handled */ } finally { borrowSubmitting.value = false }
}

function handleReserve(book) {
  reserveBookInfo.value = book
  earliestReturnDate.value = null
  reserveDialogVisible.value = true
  getEarliestReturnDate(book.id).then(res => { earliestReturnDate.value = res.data }).catch(() => {})
}

async function confirmReserve() {
  reserveSubmitting.value = true
  try {
    await reserveBook(reserveBookInfo.value.id)
    ElMessage.success('预约成功')
    reserveDialogVisible.value = false
  } catch (e) { /* handled */ } finally { reserveSubmitting.value = false }
}

async function handleToggleFavorite(book) {
  book.favLoading = true
  try {
    if (book.isFavorited) {
      await removeFavorite(book.id)
      book.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(book.id)
      book.isFavorited = true
      ElMessage.success('收藏成功')
    }
  } catch (e) { /* handled */ } finally { book.favLoading = false }
}
</script>

<style scoped>
.book-list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-wrap {
  flex: 1;
  max-width: 480px;
}

.search-input {
  width: 100%;
}

.actions {
  display: flex;
  gap: 12px;
}

.table-card {
  border-radius: 12px;
}

.book-title {
  font-weight: 500;
  color: #303133;
}

.text-muted {
  color: #c0c4cc;
}

.price-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.price-cell .original-price {
  text-decoration: line-through;
  color: #c0c4cc;
  font-size: 12px;
}
.price-cell .current-price {
  color: #f56c6c;
  font-weight: 600;
}

.expand-detail {
  padding: 16px 40px;
  background: #fafafa;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 12px;
  }

  .search-wrap {
    width: 100%;
    max-width: none;
  }

  .actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
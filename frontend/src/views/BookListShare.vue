<template>
  <div class="booklist-share-container">
    <el-card shadow="hover" class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="share-tabs">
        <el-tab-pane label="公开书单" name="public">
          <div v-loading="loading" class="tab-content">
            <el-empty v-if="!loading && publicLists.length === 0" description="暂无公开书单" />
            <div v-else class="list-grid">
              <div
                v-for="list in publicLists"
                :key="list.id"
                class="list-card"
              >
                <div class="list-card-cover" :style="{ background: coverColors[list.id % coverColors.length] }">
                  <el-icon :size="40" color="rgba(255,255,255,0.85)"><FolderOpened /></el-icon>
                </div>
                <div class="list-card-body">
                  <h4 class="list-name">{{ list.name }}</h4>
                  <p class="list-desc">{{ list.description || '暂无描述' }}</p>
                  <div class="list-meta">
                    <span class="list-creator">{{ list.creator || '未知用户' }}</span>
                    <span class="list-favorite-count">
                      <el-icon :size="14" color="#e6a23c"><StarFilled /></el-icon>
                      {{ list.favoriteCount || 0 }}
                    </span>
                  </div>
                  <div class="list-card-actions">
                    <el-button type="primary" size="small" @click="handleViewList(list)">
                      查看
                    </el-button>
                    <el-button
                      v-if="!list.isFavorited"
                      type="warning"
                      size="small"
                      plain
                      @click="handleFavorite(list)"
                      :loading="list.favLoading"
                    >
                      <el-icon><Star /></el-icon>
                      收藏
                    </el-button>
                    <el-button
                      v-else
                      type="warning"
                      size="small"
                      @click="handleUnfavorite(list)"
                      :loading="list.favLoading"
                    >
                      <el-icon><StarFilled /></el-icon>
                      已收藏
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            <div class="pagination-wrap" v-if="publicTotal > publicPageSize">
              <el-pagination
                v-model:current-page="publicPage"
                :page-size="publicPageSize"
                :total="publicTotal"
                layout="prev, pager, next"
                @current-change="fetchPublicLists"
                background
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的书单" name="my">
          <div class="my-toolbar">
            <el-button type="primary" @click="handleCreateList">
              <el-icon><Plus /></el-icon>
              创建书单
            </el-button>
          </div>
          <div v-loading="loading" class="tab-content">
            <el-empty v-if="!loading && myLists.length === 0" description="暂无书单，快去创建一个吧" />
            <div v-else class="list-grid">
              <div
                v-for="list in myLists"
                :key="list.id"
                class="list-card"
              >
                <div class="list-card-cover" :style="{ background: coverColors[list.id % coverColors.length] }">
                  <el-icon :size="40" color="rgba(255,255,255,0.85)"><FolderOpened /></el-icon>
                </div>
                <div class="list-card-body">
                  <div class="list-name-row">
                    <h4 class="list-name">{{ list.name }}</h4>
                    <el-tag v-if="list.isPublic" size="small" type="success">公开</el-tag>
                    <el-tag v-else size="small" type="info">私有</el-tag>
                  </div>
                  <p class="list-desc">{{ list.description || '暂无描述' }}</p>
                  <div class="list-meta">
                    <span class="list-favorite-count">
                      <el-icon :size="14" color="#e6a23c"><StarFilled /></el-icon>
                      {{ list.favoriteCount || 0 }}
                    </span>
                  </div>
                  <div class="list-card-actions">
                    <el-button type="primary" size="small" @click="handleViewList(list)">
                      查看
                    </el-button>
                    <el-button type="default" size="small" @click="handleEditList(list)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                    <el-button type="danger" size="small" @click="handleDeleteList(list)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            <div class="pagination-wrap" v-if="myTotal > myPageSize">
              <el-pagination
                v-model:current-page="myPage"
                :page-size="myPageSize"
                :total="myTotal"
                layout="prev, pager, next"
                @current-change="fetchMyLists"
                background
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 创建/编辑书单弹窗 -->
    <el-dialog
      v-model="listDialogVisible"
      :title="isEditMode ? '编辑书单' : '创建书单'"
      width="520px"
      :close-on-click-modal="false"
      @close="resetListForm"
    >
      <el-form ref="listFormRef" :model="listForm" :rules="listRules" label-width="80px" label-position="left">
        <el-form-item label="书单名称" prop="name">
          <el-input v-model="listForm.name" placeholder="请输入书单名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="listForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入书单描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="listForm.isPublic" active-text="公开" inactive-text="私有" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="listDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitList" :loading="submitting">
          {{ isEditMode ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 书单详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="currentList?.name || '书单详情'"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="detail-header" v-if="currentList">
        <p class="detail-desc">{{ currentList.description || '暂无描述' }}</p>
        <div class="detail-meta">
          <span>创建者：{{ currentList.creator || '未知' }}</span>
          <span>收藏数：{{ currentList.favoriteCount || 0 }}</span>
        </div>
      </div>

      <div v-if="isOwner" class="add-book-section">
        <el-select
          v-model="selectedBookId"
          filterable
          remote
          reserve-keyword
          placeholder="搜索并添加图书"
          :remote-method="searchBooks"
          :loading="searchLoading"
          clearable
          style="width: 300px"
        >
          <el-option
            v-for="book in searchResults"
            :key="book.id"
            :label="`${book.title} - ${book.author}`"
            :value="book.id"
          />
        </el-select>
        <el-button type="primary" @click="handleAddBook" :disabled="!selectedBookId" :loading="addingBook">
          <el-icon><Plus /></el-icon>
          添加
        </el-button>
      </div>

      <el-table :data="listItems" border stripe v-loading="itemsLoading" style="width: 100%; margin-top: 16px" row-key="id">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="title" label="书名" min-width="160" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.category" size="small" type="primary">{{ row.category }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isOwner" label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="handleRemoveBook(row)">
              <el-icon><Delete /></el-icon>
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!itemsLoading && listItems.length === 0" description="该书单暂无图书" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Star, StarFilled, FolderOpened } from '@element-plus/icons-vue'
import {
  getPublicBookLists,
  getMyBookLists,
  createBookList,
  updateBookList,
  deleteBookList,
  getBookListItems,
  addBookToList,
  removeBookFromList,
  favoriteBookList,
  unfavoriteBookList
} from '../api/community'
import { getBooks } from '../api/book'

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

const activeTab = ref('public')
const loading = ref(false)
const submitting = ref(false)

// 公开书单
const publicLists = ref([])
const publicPage = ref(1)
const publicPageSize = ref(8)
const publicTotal = ref(0)

// 我的书单
const myLists = ref([])
const myPage = ref(1)
const myPageSize = ref(8)
const myTotal = ref(0)

const currentUser = computed(() => localStorage.getItem('username') || '')

// 创建/编辑书单
const listDialogVisible = ref(false)
const isEditMode = ref(false)
const editListId = ref(null)
const listFormRef = ref(null)
const listForm = reactive({
  name: '',
  description: '',
  isPublic: true
})
const listRules = {
  name: [{ required: true, message: '请输入书单名称', trigger: 'blur' }]
}

// 书单详情
const detailDialogVisible = ref(false)
const currentList = ref(null)
const listItems = ref([])
const itemsLoading = ref(false)

// 添加图书
const selectedBookId = ref(null)
const searchResults = ref([])
const searchLoading = ref(false)
const addingBook = ref(false)

const isOwner = computed(() => {
  if (!currentList.value) return false
  const listOwner = currentList.value.user?.username || currentList.value.creator
  return listOwner === currentUser.value
})

async function fetchPublicLists() {
  loading.value = true
  try {
    const res = await getPublicBookLists({ page: publicPage.value - 1, size: publicPageSize.value })
    const lists = res.data.content || []
    publicLists.value = lists.map(l => ({ ...l, isFavorited: false, favLoading: false }))
    publicTotal.value = res.data.totalElements || 0
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

async function fetchMyLists() {
  loading.value = true
  try {
    const res = await getMyBookLists({ page: myPage.value - 1, size: myPageSize.value })
    myLists.value = res.data.content || []
    myTotal.value = res.data.totalElements || 0
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  if (activeTab.value === 'public') {
    fetchPublicLists()
  } else {
    fetchMyLists()
  }
}

// 创建书单
function handleCreateList() {
  isEditMode.value = false
  editListId.value = null
  resetListForm()
  listDialogVisible.value = true
}

// 编辑书单
function handleEditList(list) {
  isEditMode.value = true
  editListId.value = list.id
  listForm.name = list.name
  listForm.description = list.description || ''
  listForm.isPublic = list.isPublic || false
  listDialogVisible.value = true
}

function resetListForm() {
  listFormRef.value?.resetFields()
  listForm.name = ''
  listForm.description = ''
  listForm.isPublic = true
}

async function handleSubmitList() {
  const valid = await listFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: listForm.name,
      description: listForm.description,
      isPublic: listForm.isPublic
    }
    if (isEditMode.value) {
      await updateBookList(editListId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createBookList(payload)
      ElMessage.success('创建成功')
    }
    listDialogVisible.value = false
    fetchMyLists()
  } catch (e) {
    // handled
  } finally {
    submitting.value = false
  }
}

// 删除书单
async function handleDeleteList(list) {
  try {
    await ElMessageBox.confirm(`确定要删除书单"${list.name}"吗？删除后不可恢复。`, '提示', { type: 'warning' })
    await deleteBookList(list.id)
    ElMessage.success('删除成功')
    fetchMyLists()
  } catch (e) {
    // handled
  }
}

// 收藏/取消收藏
async function handleFavorite(list) {
  list.favLoading = true
  try {
    await favoriteBookList(list.id)
    list.isFavorited = true
    list.favoriteCount = (list.favoriteCount || 0) + 1
    ElMessage.success('收藏成功')
  } catch (e) {
    // handled
  } finally {
    list.favLoading = false
  }
}

async function handleUnfavorite(list) {
  list.favLoading = true
  try {
    await unfavoriteBookList(list.id)
    list.isFavorited = false
    list.favoriteCount = Math.max(0, (list.favoriteCount || 1) - 1)
    ElMessage.success('已取消收藏')
  } catch (e) {
    // handled
  } finally {
    list.favLoading = false
  }
}

// 查看书单
async function handleViewList(list) {
  currentList.value = list
  detailDialogVisible.value = true
  await fetchListItems()
}

async function fetchListItems() {
  itemsLoading.value = true
  try {
    const res = await getBookListItems(currentList.value.id)
    listItems.value = (res.data || []).map(item => item.book || item)
  } catch (e) {
    listItems.value = []
  } finally {
    itemsLoading.value = false
  }
}

// 搜索图书
async function searchBooks(query) {
  if (!query) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    const res = await getBooks({ keyword: query, page: 0, size: 20 })
    searchResults.value = res.data.content || []
  } catch (e) {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

// 添加图书到书单
async function handleAddBook() {
  if (!selectedBookId.value) return
  addingBook.value = true
  try {
    await addBookToList(currentList.value.id, { bookId: selectedBookId.value })
    ElMessage.success('添加成功')
    selectedBookId.value = null
    searchResults.value = []
    await fetchListItems()
  } catch (e) {
    // handled
  } finally {
    addingBook.value = false
  }
}

// 从书单移除图书
async function handleRemoveBook(book) {
  try {
    await ElMessageBox.confirm(`确定要从书单中移除"${book.title}"吗？`, '提示', { type: 'warning' })
    await removeBookFromList(currentList.value.id, book.id)
    ElMessage.success('移除成功')
    await fetchListItems()
  } catch (e) {
    // handled
  }
}

onMounted(() => {
  fetchPublicLists()
})
</script>

<style scoped>
.booklist-share-container {
  display: flex;
  flex-direction: column;
}

.main-card {
  border-radius: 12px;
}

.share-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.my-toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.tab-content {
  min-height: 200px;
}

.list-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.list-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.list-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.list-card-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.list-card-body {
  padding: 14px 16px;
}

.list-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.list-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.list-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  min-height: 39px;
}

.list-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.list-creator {
  color: #606266;
}

.list-favorite-count {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
}

.list-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.detail-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-desc {
  font-size: 14px;
  color: #606266;
  margin: 0 0 8px;
}

.detail-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #909399;
}

.add-book-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.text-muted {
  color: #c0c4cc;
}

@media (max-width: 1200px) {
  .list-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .list-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .list-grid { grid-template-columns: 1fr; }
}
</style>
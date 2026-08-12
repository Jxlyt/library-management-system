<template>
  <div class="user-list-container">
    <!-- 搜索 -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名..."
          clearable
          style="width: 260px"
          @keyup.enter="fetchUsers"
          @clear="fetchUsers"
        />
        <el-button type="primary" @click="fetchUsers">搜索</el-button>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="hover" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%">
        <el-table-column align="center" prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'" effect="dark" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">
            <span v-if="row.createdAt" class="date-cell">
              {{ formatDate(row.createdAt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status !== 'ACTIVE'"
              type="success"
              size="small"
              link
              @click="handleEnable(row.id)"
            >
              启用
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="warning"
              size="small"
              link
              @click="handleDisable(row.id)"
            >
              禁用
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="info"
              size="small"
              link
              @click="handleLost(row.id)"
            >
              挂失
            </el-button>
            <el-popconfirm
              title="确定注销该用户吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small" link>注销</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchUsers"
      />
    </el-card>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户信息" width="450px">
      <el-form :model="editForm" label-width="80px" v-if="editUser">
        <el-form-item label="用户名">
          <el-input :model-value="editUser.username" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit" :loading="editSubmitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const editDialogVisible = ref(false)
const editUser = ref(null)
const editSubmitting = ref(false)
const editForm = ref({ phone: '', email: '' })

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function statusTagType(status) {
  const map = { ACTIVE: 'success', DISABLED: 'warning', LOST: 'info' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { ACTIVE: '正常', DISABLED: '已禁用', LOST: '已挂失' }
  return map[status] || status
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await request.get('/users', {
      params: {
        keyword: keyword.value,
        page: page.value - 1,
        size: size.value
      }
    })
    if (res.data.code === 200) {
      tableData.value = res.data.data.content
      total.value = res.data.data.totalElements
    }
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

function handleEdit(row) {
  editUser.value = row
  editForm.value = { phone: row.phone || '', email: row.email || '' }
  editDialogVisible.value = true
}

async function confirmEdit() {
  editSubmitting.value = true
  try {
    await request.put(`/users/${editUser.value.id}`, editForm.value)
    editDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    // handled
  } finally {
    editSubmitting.value = false
  }
}

async function handleEnable(id) {
  try {
    await request.put(`/users/${id}/enable`)
    fetchUsers()
  } catch (e) {
    // handled
  }
}

async function handleDisable(id) {
  try {
    await request.put(`/users/${id}/disable`)
    fetchUsers()
  } catch (e) {
    // handled
  }
}

async function handleLost(id) {
  try {
    await request.put(`/users/${id}/lost`)
    fetchUsers()
  } catch (e) {
    // handled
  }
}

async function handleDelete(id) {
  try {
    await request.delete(`/users/${id}`)
    fetchUsers()
  } catch (e) {
    // handled
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  border-radius: 12px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.table-card {
  border-radius: 12px;
}

.date-cell {
  color: #909399;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
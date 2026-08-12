<template>
  <div class="reservation-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">预约管理</span>
        </div>
      </template>

      <el-table :data="reservations" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="预约人" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.user?.username }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图书" min-width="180">
          <template #default="{ row }">
            <span class="book-title">{{ row.book?.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="ISBN" width="140">
          <template #default="{ row }">
            <span class="isbn">{{ row.book?.isbn }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预约日期" width="170" align="center">
          <template #default="{ row }">
            {{ formatDate(row.reserveDate) }}
          </template>
        </el-table-column>
        <el-table-column label="过期日期" width="170" align="center">
          <template #default="{ row }">
            {{ formatDate(row.expireDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="statusTag(row.status)"
              size="small"
              effect="dark"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchReservations"
          @current-change="fetchReservations"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllReservations } from '../api/borrow'

const loading = ref(false)
const reservations = ref([])
const page = ref(0)
const size = ref(10)
const total = ref(0)

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

function statusText(status) {
  const map = { PENDING: '待处理', FULFILLED: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}

function statusTag(status) {
  const map = { PENDING: 'warning', FULFILLED: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

async function fetchReservations() {
  loading.value = true
  try {
    const res = await getAllReservations({ page: page.value, size: size.value })
    reservations.value = res.data.content
    total.value = res.data.totalElements
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchReservations()
})
</script>

<style scoped>
.reservation-page {
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.book-title {
  font-weight: 500;
  color: #303133;
}

.isbn {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #909399;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
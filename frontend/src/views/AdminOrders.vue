<template>
  <div class="admin-orders-page">
    <h2 class="page-title">订单管理</h2>
    <el-tabs v-model="statusFilter" @tab-change="fetchOrders" type="card">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="PENDING_PAYMENT" />
      <el-tab-pane label="待发货" name="PAID" />
      <el-tab-pane label="待收货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="RECEIVED" />
      <el-tab-pane label="已取消" name="CANCELLED" />
    </el-tabs>

    <div v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
      <el-table v-else :data="orders" stripe style="width: 100%">
        <el-table-column prop="orderNumber" label="订单号" min-width="200" />
        <el-table-column prop="user.username" label="用户" width="120" />
        <el-table-column label="商品" min-width="250">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.id" class="item-row">
              <span>{{ item.bookTitle }}</span>
              <span class="item-qty">x{{ item.quantity }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="总价" width="120" align="right">
          <template #default="{ row }">
            <span class="price-cell">¥{{ row.payAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="物流信息" min-width="160">
          <template #default="{ row }">
            <span v-if="row.shippingCompany">{{ row.shippingCompany }}：{{ row.trackingNumber }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PAID'" type="primary" size="small" @click="handleShip(row)">发货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > 0" class="pagination" v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="fetchOrders" />
    </div>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="发货" width="420px" :close-on-click-modal="false">
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.shippingCompany" placeholder="选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="邮政EMS" value="邮政EMS" />
            <el-option label="极兔速递" value="极兔速递" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip" :loading="shipping">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllOrders, shipOrder } from '../api/order'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')

const shipVisible = ref(false)
const shipForm = ref({ shippingCompany: '', trackingNumber: '' })
const shipOrderId = ref(null)
const shipping = ref(false)

function formatDate(d) { return d ? d.substring(0, 16).replace('T', ' ') : '-' }

function statusTag(s) {
  const map = { PENDING_PAYMENT: 'warning', PAID: 'primary', SHIPPED: 'success', RECEIVED: 'info', COMPLETED: 'info', CANCELLED: 'danger' }
  return map[s] || 'info'
}

function statusLabel(s) {
  const map = { PENDING_PAYMENT: '待支付', PAID: '待发货', SHIPPED: '待收货', RECEIVED: '已完成', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getAllOrders({ page: page.value - 1, size: size.value, status: statusFilter.value || undefined })
    orders.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch (e) { /* handled */ } finally { loading.value = false }
}

function handleShip(row) {
  shipOrderId.value = row.id
  shipForm.value = { shippingCompany: '', trackingNumber: '' }
  shipVisible.value = true
}

async function confirmShip() {
  if (!shipForm.value.shippingCompany || !shipForm.value.trackingNumber) {
    ElMessage.warning('请填写物流公司和单号')
    return
  }
  shipping.value = true
  try {
    await shipOrder(shipOrderId.value, shipForm.value)
    ElMessage.success('发货成功')
    shipVisible.value = false
    fetchOrders()
  } catch (e) { /* handled */ } finally { shipping.value = false }
}

onMounted(fetchOrders)
</script>

<style scoped>
.admin-orders-page { background: #fff; border-radius: 12px; padding: 24px; min-height: 400px; }
.page-title { margin: 0 0 20px; font-size: 22px; color: #303133; }
.item-row { display: flex; justify-content: space-between; padding: 2px 0; }
.item-qty { color: #909399; font-size: 12px; }
.price-cell { color: #f56c6c; font-weight: 600; }
.text-muted { color: #c0c4cc; }
.pagination { justify-content: center; margin-top: 20px; }
</style>
<template>
  <div class="order-page">
    <h2 class="page-title">我的订单</h2>
    <el-tabs v-model="statusFilter" @tab-change="fetchOrders" type="card">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="PENDING_PAYMENT" />
      <el-tab-pane label="待发货" name="PAID" />
      <el-tab-pane label="待收货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="RECEIVED" />
    </el-tabs>

    <div v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
      <div v-else class="order-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-number">订单号：{{ order.orderNumber }}</span>
            <span class="order-time">{{ formatDate(order.createdAt) }}</span>
            <el-tag :type="statusTag(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
          </div>
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <span class="item-title">{{ item.bookTitle }}</span>
              <span class="item-price">¥{{ item.price?.toFixed(2) }} x{{ item.quantity }}</span>
              <span class="item-subtotal">¥{{ item.subtotal?.toFixed(2) }}</span>
            </div>
          </div>
          <div class="order-footer">
            <span class="order-total">合计：<strong>¥{{ order.payAmount?.toFixed(2) }}</strong></span>
            <span class="order-discount" v-if="order.discountAmount > 0">优惠 ¥{{ order.discountAmount?.toFixed(2) }}</span>
            <div class="order-actions">
              <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" size="small" @click="handleCancel(order)">取消订单</el-button>
              <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" size="small" @click="handlePay(order)">立即支付</el-button>
              <el-button v-if="order.status === 'SHIPPED'" type="success" size="small" @click="handleReceive(order)">确认收货</el-button>
            </div>
          </div>
          <div class="order-shipping" v-if="order.shippingCompany">
            <span>{{ order.shippingCompany }}：{{ order.trackingNumber }}</span>
          </div>
        </div>
      </div>
      <el-pagination v-if="total > 0" class="pagination" v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="fetchOrders" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders, cancelOrder, confirmReceive } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const router = useRouter()
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')

function formatDate(d) { return d ? d.substring(0, 10) : '-' }

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
    const res = await getMyOrders({ page: page.value - 1, size: size.value, status: statusFilter.value || undefined })
    orders.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function handlePay(order) {
  router.push({ path: '/payment', query: { orderId: order.id } })
}

async function handleCancel(order) {
  try {
    await ElMessageBox.confirm(
      `确定取消订单 ${order.orderNumber}？<br/>取消后不可恢复，销售库存将自动恢复。`,
      '取消订单',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '保留订单',
        type: 'warning',
        dangerouslyUseHTMLString: true,
        center: true
      }
    )
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (e) { /* cancelled */ }
}

async function handleReceive(order) {
  try {
    await ElMessageBox.confirm(
      `确认已收到订单 ${order.orderNumber} 的商品？<br/>确认后订单将标记为已完成。`,
      '确认收货',
      {
        confirmButtonText: '确认收货',
        cancelButtonText: '取消',
        type: 'info',
        dangerouslyUseHTMLString: true,
        center: true
      }
    )
    await confirmReceive(order.id)
    ElMessage.success('已确认收货')
    fetchOrders()
  } catch (e) { /* cancelled */ }
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-page { background: #fff; border-radius: 12px; padding: 24px; min-height: 400px; }
.page-title { margin: 0 0 20px; font-size: 22px; color: #303133; }
.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card { border: 1px solid #ebeef5; border-radius: 12px; padding: 16px; }
.order-header { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }
.order-number { color: #606266; font-size: 13px; }
.order-time { color: #909399; font-size: 12px; margin-left: auto; }
.order-items { border-top: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; padding: 8px 0; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 4px 0; }
.item-title { flex: 1; }
.item-price { color: #909399; font-size: 13px; }
.item-subtotal { color: #f56c6c; font-weight: 600; }
.order-footer { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
.order-total strong { color: #f56c6c; font-size: 18px; }
.order-discount { color: #67c23a; font-size: 13px; }
.order-actions { margin-left: auto; display: flex; gap: 8px; }
.order-shipping { margin-top: 8px; color: #909399; font-size: 13px; }
.pagination { justify-content: center; margin-top: 20px; }
</style>
<template>
  <div class="payment-page">
    <div v-loading="loading" class="payment-wrapper">
      <div v-if="order" class="payment-card">
        <div class="payment-icon">💳</div>
        <h2>订单支付</h2>
        <p class="payment-subtitle">请确认以下订单信息后完成支付</p>

        <el-descriptions :column="1" border style="margin: 24px 0">
          <el-descriptions-item label="订单号">{{ order.orderNumber }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDate(order.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ order.shippingName }} {{ order.shippingPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ order.shippingAddress }}</el-descriptions-item>
        </el-descriptions>

        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <span class="item-title">{{ item.bookTitle }}</span>
            <span class="item-price">¥{{ item.price?.toFixed(2) }} x{{ item.quantity }}</span>
            <span class="item-subtotal">¥{{ item.subtotal?.toFixed(2) }}</span>
          </div>
        </div>

        <div class="payment-total">
          <span class="total-label">应付金额</span>
          <span class="total-amount">¥{{ order.payAmount?.toFixed(2) }}</span>
        </div>

        <div v-if="order.discountAmount > 0" class="discount-info">
          <span>已优惠 ¥{{ order.discountAmount?.toFixed(2) }}</span>
        </div>

        <div class="countdown" v-if="countdown > 0">
          <span>请在 {{ countdown }} 秒内完成支付</span>
        </div>

        <el-button type="primary" size="large" class="pay-btn" @click="handlePay" :loading="paying">
          确认支付 ¥{{ order.payAmount?.toFixed(2) }}
        </el-button>
      </div>

      <!-- 支付成功 -->
      <div v-else-if="paySuccess" class="success-card">
        <div class="success-icon">✅</div>
        <h2>支付成功</h2>
        <p>您的订单已支付成功，请等待发货</p>
        <el-button type="primary" @click="$router.push('/orders')">查看订单</el-button>
        <el-button @click="$router.push('/dashboard')">返回首页</el-button>
      </div>

      <el-empty v-else-if="!loading" description="订单不存在">
        <el-button type="primary" @click="$router.push('/orders')">查看订单</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { getMyOrders, payOrder } from '../api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(true)
const order = ref(null)
const paying = ref(false)
const paySuccess = ref(false)
const countdown = ref(300)
let timer = null

function formatDate(d) { return d ? d.substring(0, 16).replace('T', ' ') : '-' }

async function fetchOrder() {
  const orderId = route.query.orderId
  if (!orderId) { loading.value = false; return }
  loading.value = true
  try {
    const res = await getMyOrders({ page: 0, size: 50 })
    const list = res.data?.content || []
    order.value = list.find(o => o.id == orderId)
    if (order.value && order.value.status !== 'PENDING_PAYMENT') {
      ElMessage.warning('该订单已支付或已取消')
      order.value = null
    }
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function handlePay() {
  if (!order.value) return
  paying.value = true
  try {
    await payOrder(order.value.id)
    clearInterval(timer)
    paySuccess.value = true
    order.value = null
  } catch (e) {
    // handled by interceptor
  } finally { paying.value = false }
}

function startCountdown() {
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      ElMessage.warning('支付超时，订单已取消（模拟）')
      order.value = null
    }
  }, 1000)
}

onMounted(() => {
  fetchOrder()
  startCountdown()
})

onBeforeUnmount(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.payment-page { background: #fff; border-radius: 12px; padding: 24px; min-height: 400px; display: flex; justify-content: center; }
.payment-wrapper { width: 100%; max-width: 600px; }
.payment-card { text-align: center; }
.payment-icon { font-size: 64px; margin-bottom: 8px; }
.payment-card h2 { margin: 0 0 8px; font-size: 24px; color: #303133; }
.payment-subtitle { color: #909399; margin: 0 0 8px; }
.order-items { border-top: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; padding: 12px 0; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 6px 0; }
.item-title { flex: 1; text-align: left; }
.item-price { color: #909399; font-size: 13px; }
.item-subtotal { color: #f56c6c; font-weight: 600; }
.payment-total { margin-top: 20px; padding: 16px; background: #fef0f0; border-radius: 8px; }
.total-label { font-size: 16px; color: #606266; }
.total-amount { font-size: 28px; font-weight: 700; color: #f56c6c; margin-left: 16px; }
.discount-info { color: #67c23a; font-size: 13px; margin-top: 8px; }
.countdown { margin-top: 16px; color: #e6a23c; font-size: 14px; }
.pay-btn { width: 100%; margin-top: 24px; height: 48px; font-size: 18px; }
.success-card { text-align: center; padding: 40px 0; }
.success-icon { font-size: 80px; margin-bottom: 16px; }
.success-card h2 { margin: 0 0 8px; font-size: 24px; color: #67c23a; }
.success-card p { color: #909399; margin-bottom: 24px; }
</style>
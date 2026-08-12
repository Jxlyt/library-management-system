<template>
  <div class="cart-page">
    <h2 class="page-title">购物车</h2>
    <div v-loading="loading" class="cart-container">
      <el-empty v-if="!loading && cartItems.length === 0" description="购物车是空的，快去选购吧" />
      <template v-else>
        <div class="cart-items">
          <div v-for="item in cartItems" :key="item.id" class="cart-item">
            <div class="cart-item-cover" :style="{ background: coverColors[(item.book?.id || 0) % coverColors.length] }">
              <span>{{ item.book?.title?.charAt(0) }}</span>
            </div>
            <div class="cart-item-info">
              <h4>{{ item.book?.title }}</h4>
              <p class="cart-item-author">作者：{{ item.book?.author }}</p>
              <div class="cart-item-price">
                <span class="original-price" v-if="item.book?.discount">¥{{ item.book?.salePrice?.toFixed(2) }}</span>
                <span class="current-price">¥{{ getActualPrice(item).toFixed(2) }}</span>
                <el-tag v-if="item.book?.discount" type="danger" size="small" effect="plain">{{ (item.book.discount * 10).toFixed(1) }}折</el-tag>
              </div>
            </div>
            <div class="cart-item-quantity">
              <el-input-number v-model="item.quantity" :min="1" :max="item.book?.saleableCopies || 99" size="small" @change="handleQuantityChange(item)" />
            </div>
            <div class="cart-item-subtotal">
              <span class="subtotal-label">小计</span>
              <span class="subtotal-value">¥{{ (getActualPrice(item) * item.quantity).toFixed(2) }}</span>
            </div>
            <el-button type="danger" size="small" circle @click="handleRemove(item)" :icon="Delete" />
          </div>
        </div>
        <div class="cart-summary" v-if="cartItems.length > 0">
          <div class="summary-left">
            <el-button text type="danger" @click="handleClear">清空购物车</el-button>
          </div>
          <div class="summary-right">
            <span class="summary-total">合计：<strong>¥{{ totalPrice.toFixed(2) }}</strong></span>
            <span class="summary-discount" v-if="totalDiscount > 0">已优惠 ¥{{ totalDiscount.toFixed(2) }}</span>
            <el-button type="primary" size="large" @click="handleCheckout" :loading="checkingOut">去结算</el-button>
          </div>
        </div>
      </template>
    </div>

    <!-- 结算弹窗 -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="600px" top="3vh" :close-on-click-modal="false" center>
      <div class="checkout-body">
        <div class="checkout-section">
          <h4>收货地址</h4>
          <div v-if="addresses.length === 0" class="no-address">
            <el-empty description="暂无收货地址，请先添加" :image-size="80" />
            <el-button type="primary" @click="showAddAddress = true">添加地址</el-button>
          </div>
          <el-radio-group v-else v-model="selectedAddressId" class="address-list">
            <div v-for="addr in addresses" :key="addr.id" class="address-item">
              <el-radio :value="addr.id">
                <span class="addr-name">{{ addr.name }}</span>
                <span class="addr-phone">{{ addr.phone }}</span>
                <span class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</span>
                <el-tag v-if="addr.isDefault" type="primary" size="small" effect="plain">默认</el-tag>
              </el-radio>
            </div>
          </el-radio-group>
          <el-button text type="primary" size="small" @click="showAddAddress = true">+ 新增地址</el-button>
        </div>
        <el-divider />
        <div class="checkout-section">
          <h4>订单信息</h4>
          <div v-for="item in cartItems" :key="item.id" class="checkout-item">
            <span>{{ item.book?.title }} x{{ item.quantity }}</span>
            <span>¥{{ (getActualPrice(item) * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
        <el-divider />
        <div class="checkout-total">
          <span>合计：<strong>¥{{ totalPrice.toFixed(2) }}</strong></span>
          <span v-if="totalDiscount > 0" class="discount-info">已优惠 ¥{{ totalDiscount.toFixed(2) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmOrder" :loading="submitting" :disabled="addresses.length === 0">
          {{ addresses.length === 0 ? '请先添加收货地址' : '提交订单' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加地址弹窗 -->
    <el-dialog v-model="showAddAddress" title="添加地址" width="480px" center>
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="addressForm.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="addressForm.phone" /></el-form-item>
        <el-form-item label="省"><el-input v-model="addressForm.province" /></el-form-item>
        <el-form-item label="市"><el-input v-model="addressForm.city" /></el-form-item>
        <el-form-item label="区"><el-input v-model="addressForm.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="addressForm.detail" /></el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="addressForm.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAddress = false">取消</el-button>
        <el-button type="primary" @click="saveAddress" :loading="savingAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCart, updateCartItem, removeFromCart, clearCart, createOrder } from '../api/order'
import { getMyAddresses, addAddress } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const cartItems = ref([])
const checkingOut = ref(false)
const checkoutVisible = ref(false)
const selectedAddressId = ref(null)
const addresses = ref([])
const showAddAddress = ref(false)
const savingAddress = ref(false)
const submitting = ref(false)

const addressForm = ref({ name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false })

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

function getActualPrice(item) {
  const price = item.book?.salePrice || 0
  const discount = item.book?.discount || 0
  return discount > 0 ? price * discount : price
}

const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + getActualPrice(item) * item.quantity, 0)
})

const totalDiscount = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    const price = item.book?.salePrice || 0
    return sum + (price - getActualPrice(item)) * item.quantity
  }, 0)
})

async function fetchCart() {
  loading.value = true
  try {
    const res = await getCart()
    cartItems.value = res.data || []
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function handleQuantityChange(item) {
  try {
    await updateCartItem(item.id, item.quantity)
  } catch (e) { fetchCart() }
}

async function handleRemove(item) {
  try {
    await removeFromCart(item.id)
    cartItems.value = cartItems.value.filter(i => i.id !== item.id)
    ElMessage.success('已移除')
  } catch (e) { /* handled */ }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定清空购物车？', '提示', { type: 'warning' })
    await clearCart()
    cartItems.value = []
    ElMessage.success('购物车已清空')
  } catch (e) { /* cancelled */ }
}

async function handleCheckout() {
  try {
    const res = await getMyAddresses()
    addresses.value = res.data || []
    const defaultAddr = addresses.value.find(a => a.isDefault)
    selectedAddressId.value = defaultAddr ? defaultAddr.id : (addresses.value[0]?.id || null)
    checkoutVisible.value = true
  } catch (e) { /* handled */ }
}

async function saveAddress() {
  savingAddress.value = true
  try {
    await addAddress(addressForm.value)
    ElMessage.success('地址添加成功')
    showAddAddress.value = false
    addressForm.value = { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
    const res = await getMyAddresses()
    addresses.value = res.data || []
    if (!selectedAddressId.value && addresses.value.length > 0) {
      selectedAddressId.value = addresses.value[0].id
    }
  } catch (e) { /* handled */ } finally { savingAddress.value = false }
}

async function confirmOrder() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const res = await createOrder(selectedAddressId.value)
    ElMessage.success('下单成功')
    checkoutVisible.value = false
    router.push({ path: '/payment', query: { orderId: res.data?.id } })
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

onMounted(fetchCart)
</script>

<style scoped>
.cart-page { background: #fff; border-radius: 12px; padding: 24px; min-height: 400px; }
.page-title { margin: 0 0 20px; font-size: 22px; color: #303133; }
.cart-container { min-height: 200px; }
.cart-item { display: flex; align-items: center; gap: 16px; padding: 16px; border-bottom: 1px solid #ebeef5; }
.cart-item-cover { width: 60px; height: 80px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: rgba(255,255,255,0.85); flex-shrink: 0; }
.cart-item-info { flex: 1; min-width: 0; }
.cart-item-info h4 { margin: 0 0 4px; font-size: 15px; }
.cart-item-author { margin: 0 0 6px; color: #909399; font-size: 13px; }
.cart-item-price { display: flex; align-items: center; gap: 8px; }
.original-price { text-decoration: line-through; color: #c0c4cc; font-size: 13px; }
.current-price { color: #f56c6c; font-weight: 600; font-size: 16px; }
.cart-item-quantity { width: 120px; }
.cart-item-subtotal { text-align: right; min-width: 80px; }
.subtotal-label { display: block; color: #909399; font-size: 12px; }
.subtotal-value { color: #f56c6c; font-weight: 600; }
.cart-summary { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 16px; border-top: 2px solid #ebeef5; }
.summary-right { display: flex; align-items: center; gap: 16px; }
.summary-total { font-size: 16px; }
.summary-total strong { color: #f56c6c; font-size: 22px; }
.summary-discount { color: #67c23a; font-size: 13px; }
.checkout-body { max-height: 60vh; overflow-y: auto; padding-right: 4px; }
.checkout-section h4 { margin: 0 0 12px; font-size: 15px; }
.address-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.address-item { padding: 8px; border: 1px solid #ebeef5; border-radius: 8px; }
.addr-name { font-weight: 600; margin-right: 8px; }
.addr-phone { color: #909399; margin-right: 8px; }
.addr-detail { color: #606266; }
.checkout-item { display: flex; justify-content: space-between; padding: 8px 0; }
.checkout-total { text-align: right; font-size: 16px; }
.checkout-total strong { color: #f56c6c; font-size: 22px; }
.discount-info { color: #67c23a; margin-left: 8px; font-size: 13px; }
.no-address { text-align: center; }
</style>
<template>
  <div class="address-page">
    <div class="page-header">
      <h2>收货地址管理</h2>
      <el-button type="primary" @click="showAddDialog">+ 新增地址</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && addresses.length === 0" description="暂无收货地址">
        <el-button type="primary" @click="showAddDialog">添加地址</el-button>
      </el-empty>
      <div v-else class="address-list">
        <div v-for="addr in addresses" :key="addr.id" class="address-card" :class="{ default: addr.isDefault }">
          <div class="addr-header">
            <span class="addr-name">{{ addr.name }}</span>
            <span class="addr-phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault" type="primary" size="small">默认</el-tag>
          </div>
          <div class="addr-detail">
            {{ addr.province || '' }}{{ addr.city || '' }}{{ addr.district || '' }} {{ addr.detail }}
          </div>
          <div class="addr-actions">
            <el-button text type="primary" size="small" @click="handleEdit(addr)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(addr)">删除</el-button>
            <el-button v-if="!addr.isDefault" text type="primary" size="small" @click="handleSetDefault(addr)">设为默认</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地址' : '新增地址'" width="480px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px" ref="formRef">
        <el-form-item label="收货人"><el-input v-model="form.name" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="省"><el-input v-model="form.province" placeholder="请输入省份" /></el-form-item>
        <el-form-item label="市"><el-input v-model="form.city" placeholder="请输入城市" /></el-form-item>
        <el-form-item label="区"><el-input v-model="form.district" placeholder="请输入区/县" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" placeholder="请输入详细地址" /></el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="form.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const addresses = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)
const form = ref({ name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false })

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await getMyAddresses()
    addresses.value = res.data || []
  } catch (e) { /* handled */ } finally { loading.value = false }
}

function showAddDialog() {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
  dialogVisible.value = true
}

function handleEdit(addr) {
  isEdit.value = true
  editId.value = addr.id
  form.value = { ...addr, isDefault: addr.isDefault || false }
  dialogVisible.value = true
}

async function handleDelete(addr) {
  try {
    await ElMessageBox.confirm(
      `确定删除地址「${addr.name} ${addr.phone}」吗？`,
      '删除地址',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning', center: true }
    )
    await deleteAddress(addr.id)
    ElMessage.success('删除成功')
    fetchAddresses()
  } catch (e) { /* cancelled */ }
}

async function handleSetDefault(addr) {
  try {
    await setDefaultAddress(addr.id)
    ElMessage.success('已设为默认地址')
    fetchAddresses()
  } catch (e) { /* handled */ }
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) {
      await updateAddress(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await addAddress(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchAddresses()
  } catch (e) { /* handled */ } finally { saving.value = false }
}

onMounted(fetchAddresses)
</script>

<style scoped>
.address-page { background: #fff; border-radius: 12px; padding: 24px; min-height: 400px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 22px; color: #303133; }
.address-list { display: flex; flex-direction: column; gap: 12px; }
.address-card { border: 1px solid #ebeef5; border-radius: 12px; padding: 16px; transition: all 0.3s; }
.address-card:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1); }
.address-card.default { border-color: #409eff; background: #ecf5ff; }
.addr-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.addr-name { font-weight: 600; font-size: 16px; color: #303133; }
.addr-phone { color: #909399; }
.addr-detail { color: #606266; font-size: 14px; margin-bottom: 8px; }
.addr-actions { display: flex; gap: 8px; }
</style>
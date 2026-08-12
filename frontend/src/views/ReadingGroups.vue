<template>
  <div class="groups-page">
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="24" color="#409eff"><Grid /></el-icon>
        <span>读书小组</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建小组</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="所有小组" name="all">
        <div class="search-bar">
          <el-input v-model="keyword" placeholder="搜索小组..." clearable @keyup.enter="fetchGroups" @clear="fetchGroups" style="width:300px" />
          <el-button type="primary" @click="fetchGroups">搜索</el-button>
        </div>
        <div v-loading="loading" class="groups-grid">
          <el-empty v-if="!loading && groups.length === 0" description="暂无小组" />
          <el-card v-for="g in groups" :key="g.id" shadow="hover" class="group-card" @click="openGroupDetail(g)">
            <div class="group-banner" :style="{ background: g.coverColor || bannerColors[g.id % bannerColors.length] }">
              <el-icon :size="32" color="rgba(255,255,255,0.9)"><Grid /></el-icon>
            </div>
            <div class="group-body">
              <h3>{{ g.name }}</h3>
              <p class="group-desc">{{ g.description?.substring(0, 60) || '暂无描述' }}</p>
              <div class="group-meta">
                <span><el-icon :size="14"><UserFilled /></el-icon> {{ g.memberCount }} 成员</span>
                <span><el-icon :size="14"><ChatLineSquare /></el-icon> {{ g.postCount }} 帖子</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的小组" name="my">
        <div v-loading="myLoading" class="groups-grid">
          <el-empty v-if="!myLoading && myGroups.length === 0" description="你还没有加入任何小组" />
          <el-card v-for="g in myGroups" :key="g.id" shadow="hover" class="group-card" @click="openGroupDetail(g)">
            <div class="group-banner" :style="{ background: g.coverColor || bannerColors[g.id % bannerColors.length] }">
              <el-icon :size="32" color="rgba(255,255,255,0.9)"><Grid /></el-icon>
            </div>
            <div class="group-body">
              <h3>{{ g.name }}</h3>
              <p class="group-desc">{{ g.description?.substring(0, 60) || '暂无描述' }}</p>
              <div class="group-meta">
                <span><el-icon :size="14"><UserFilled /></el-icon> {{ g.memberCount }} 成员</span>
                <span><el-icon :size="14"><ChatLineSquare /></el-icon> {{ g.postCount }} 帖子</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建小组弹窗 -->
    <el-dialog v-model="createDialogVisible" title="创建小组" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="小组名称" required>
          <el-input v-model="createForm.name" placeholder="输入小组名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="小组描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="描述小组的主题和目的" />
        </el-form-item>
        <el-form-item label="主题色">
          <el-select v-model="createForm.coverColor" placeholder="选择主题色">
            <el-option label="紫色" value="linear-gradient(135deg, #667eea 0%, #764ba2 100%)" />
            <el-option label="粉色" value="linear-gradient(135deg, #f093fb 0%, #f5576c 100%)" />
            <el-option label="蓝色" value="linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)" />
            <el-option label="绿色" value="linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)" />
            <el-option label="橙色" value="linear-gradient(135deg, #fa709a 0%, #fee140 100%)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="createSubmitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- 小组详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" :title="detailGroup?.name" width="700px">
      <div v-if="detailGroup" class="group-detail">
        <div class="detail-banner" :style="{ background: detailGroup.coverColor || bannerColors[0] }">
          <h2>{{ detailGroup.name }}</h2>
          <p>{{ detailGroup.description }}</p>
          <div class="detail-actions">
            <el-button v-if="!isMember" type="primary" @click="handleJoin" :loading="joinLoading">加入小组</el-button>
            <el-button v-else-if="detailGroup.creator?.username !== currentUser" type="warning" @click="handleLeave" :loading="leaveLoading">退出小组</el-button>
          </div>
        </div>

        <div v-if="isMember" class="post-section">
          <div class="post-form">
            <el-input v-model="postTitle" placeholder="帖子标题" style="margin-bottom:8px" />
            <el-input v-model="postContent" type="textarea" :rows="2" placeholder="分享你的想法..." />
            <el-button type="primary" @click="handlePost" :loading="postSubmitting" style="margin-top:8px">发布</el-button>
          </div>
          <el-divider>讨论区</el-divider>
          <div v-loading="postLoading">
            <el-empty v-if="!postLoading && posts.length === 0" description="暂无帖子" />
            <div v-for="p in posts" :key="p.id" class="post-item">
              <div class="post-header">
                <el-avatar :size="28">{{ (p.user?.nickname || p.user?.username || '?').charAt(0) }}</el-avatar>
                <span class="post-author">{{ p.user?.nickname || p.user?.username || '匿名' }}</span>
                <span class="post-time">{{ formatTime(p.createdAt) }}</span>
                <el-button
                  v-if="p.user?.username === currentUser"
                  type="danger"
                  size="small"
                  text
                  @click="handleDeletePost(p)"
                >删除</el-button>
              </div>
              <h4 class="post-title">{{ p.title }}</h4>
              <p class="post-content">{{ p.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getGroups, getMyGroups, createGroup, joinGroup, leaveGroup, checkMembership, getGroupPosts, createGroupPost, deleteGroupPost } from '../api/community'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, Plus, UserFilled, ChatLineSquare } from '@element-plus/icons-vue'

const currentUser = ref(localStorage.getItem('username') || '')
const activeTab = ref('all')
const loading = ref(false)
const myLoading = ref(false)
const groups = ref([])
const myGroups = ref([])
const keyword = ref('')

const bannerColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
]

const createDialogVisible = ref(false)
const createSubmitting = ref(false)
const createForm = ref({ name: '', description: '', coverColor: '' })

const detailDialogVisible = ref(false)
const detailGroup = ref(null)
const isMember = ref(false)
const joinLoading = ref(false)
const leaveLoading = ref(false)
const postLoading = ref(false)
const postSubmitting = ref(false)
const posts = ref([])
const postTitle = ref('')
const postContent = ref('')

async function fetchGroups() {
  loading.value = true
  try {
    const res = await getGroups({ keyword: keyword.value, page: 0, size: 50 })
    groups.value = res.data?.content || []
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function fetchMyGroups() {
  myLoading.value = true
  try {
    const res = await getMyGroups({ page: 0, size: 50 })
    myGroups.value = res.data?.content || []
  } catch (e) { /* handled */ } finally { myLoading.value = false }
}

function handleTabChange(name) {
  if (name === 'all') fetchGroups()
  else if (name === 'my') fetchMyGroups()
}

async function submitCreate() {
  if (!createForm.value.name) { ElMessage.warning('请输入小组名称'); return }
  createSubmitting.value = true
  try {
    await createGroup(createForm.value)
    ElMessage.success('小组创建成功')
    createDialogVisible.value = false
    createForm.value = { name: '', description: '', coverColor: '' }
    fetchGroups()
  } catch (e) { /* handled */ } finally { createSubmitting.value = false }
}

function openCreateDialog() {
  createForm.value = { name: '', description: '', coverColor: '' }
  createDialogVisible.value = true
}

async function openGroupDetail(group) {
  detailGroup.value = group
  detailDialogVisible.value = true
  isMember.value = false
  posts.value = []
  postTitle.value = ''
  postContent.value = ''
  try {
    const res = await checkMembership(group.id)
    isMember.value = res.data === true
    if (isMember.value) {
      await fetchPosts()
    }
  } catch (e) { /* handled */ }
}

async function fetchPosts() {
  postLoading.value = true
  try {
    const res = await getGroupPosts(detailGroup.value.id, { page: 0, size: 50 })
    posts.value = res.data?.content || []
  } catch (e) { /* handled */ } finally { postLoading.value = false }
}

async function handleJoin() {
  joinLoading.value = true
  try {
    await joinGroup(detailGroup.value.id)
    ElMessage.success('加入成功')
    isMember.value = true
    detailGroup.value.memberCount++
    await fetchPosts()
  } catch (e) { /* handled */ } finally { joinLoading.value = false }
}

async function handleLeave() {
  try {
    await ElMessageBox.confirm(
      `确定要退出小组「${detailGroup.value?.name || '此小组'}」吗？退出后需要重新申请加入。`,
      '确认退出',
      { confirmButtonText: '确认退出', cancelButtonText: '取消', type: 'warning', center: true }
    )
    leaveLoading.value = true
    await leaveGroup(detailGroup.value.id)
    ElMessage.success('已退出小组')
    isMember.value = false
    detailGroup.value.memberCount--
    detailDialogVisible.value = false
  } catch (e) { if (e !== 'cancel') { /* handled */ } } finally { leaveLoading.value = false }
}

async function handlePost() {
  if (!postTitle.value.trim() || !postContent.value.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  postSubmitting.value = true
  try {
    await createGroupPost(detailGroup.value.id, { title: postTitle.value, content: postContent.value })
    ElMessage.success('发布成功')
    postTitle.value = ''
    postContent.value = ''
    detailGroup.value.postCount++
    await fetchPosts()
  } catch (e) { /* handled */ } finally { postSubmitting.value = false }
}

async function handleDeletePost(post) {
  try {
    await ElMessageBox.confirm('确定删除此帖子？', '确认', { type: 'warning' })
    await deleteGroupPost(post.id)
    ElMessage.success('已删除')
    detailGroup.value.postCount--
    await fetchPosts()
  } catch (e) { if (e !== 'cancel') { /* handled */ } }
}

function formatTime(date) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => { fetchGroups() })
</script>

<style scoped>
.groups-page { background: #fff; border-radius: 12px; overflow: hidden; }
.page-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #ebeef5; }
.page-title { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 600; }
.search-bar { display: flex; gap: 12px; padding: 16px 24px; }
.groups-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; padding: 0 24px 24px; }
.group-card { cursor: pointer; }
.group-banner { height: 100px; display: flex; align-items: center; justify-content: center; border-radius: 8px; margin: -8px -8px 12px; }
.group-body h3 { font-size: 16px; margin: 0 0 8px; }
.group-desc { font-size: 13px; color: #909399; margin: 0 0 12px; line-height: 1.5; }
.group-meta { display: flex; gap: 16px; font-size: 12px; color: #909399; }
.group-meta span { display: flex; align-items: center; gap: 4px; }

.detail-banner { padding: 24px; border-radius: 12px; color: #fff; margin-bottom: 20px; }
.detail-banner h2 { margin: 0 0 8px; font-size: 20px; }
.detail-banner p { margin: 0 0 12px; font-size: 14px; opacity: 0.9; }
.post-form { padding: 0 0 16px; }
.post-item { padding: 16px 0; border-bottom: 1px solid #f5f7fa; }
.post-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.post-author { font-weight: 500; font-size: 14px; }
.post-time { font-size: 12px; color: #c0c4cc; margin-left: auto; }
.post-title { font-size: 15px; margin: 0 0 6px; color: #303133; }
.post-content { font-size: 14px; color: #606266; margin: 0; line-height: 1.6; }
</style>
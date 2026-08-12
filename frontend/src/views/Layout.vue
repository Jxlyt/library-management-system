<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo" @click="router.push('/')">
        <el-icon :size="28" color="#fff">
          <Reading />
        </el-icon>
        <span v-show="!isCollapse" class="logo-text">图书管理系统</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#bfc9e0"
        active-text-color="#a78bfa"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        <el-menu-item index="/books">
          <el-icon><Reading /></el-icon>
          <span>图书管理</span>
        </el-menu-item>
        <el-menu-item index="/blind-box">
          <el-icon><Present /></el-icon>
          <span>图书盲盒</span>
        </el-menu-item>
        <el-menu-item index="/auctions">
          <el-icon><Histogram /></el-icon>
          <span>图书拍卖</span>
        </el-menu-item>
        <el-menu-item index="/ai-assistant">
          <el-icon><Service /></el-icon>
          <span>AI助手</span>
        </el-menu-item>
        <el-menu-item index="/borrows">
          <el-icon><Collection /></el-icon>
          <span>借阅管理</span>
        </el-menu-item>
        <el-menu-item index="/favorites">
          <el-icon><StarFilled /></el-icon>
          <span>我的收藏</span>
        </el-menu-item>
        <el-menu-item index="/cart">
          <el-icon><ShoppingCart /></el-icon>
          <span>购物车</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="/addresses">
          <el-icon><Location /></el-icon>
          <span>收货地址</span>
        </el-menu-item>
        <el-menu-item index="/mystudy">
          <el-icon><Sunny /></el-icon>
          <span>我的书房</span>
        </el-menu-item>
        <el-menu-item index="/booklists">
          <el-icon><FolderOpened /></el-icon>
          <span>书单分享</span>
        </el-menu-item>
        
        <el-menu-item index="/challenges">
          <el-icon><Flag /></el-icon>
          <span>共读挑战</span>
        </el-menu-item>
        <el-menu-item index="/groups">
          <el-icon><Grid /></el-icon>
          <span>读书小组</span>
        </el-menu-item>
        <el-menu-item index="/messages">
          <el-icon><Message /></el-icon>
          <span>私信</span>
        </el-menu-item>
        <el-menu-item index="/achievements">
          <el-icon><TrophyBase /></el-icon>
          <span>成就徽章</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><UserFilled /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item index="/stats" v-if="userRole === 'ADMIN'">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/users" v-if="userRole === 'ADMIN'">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/announcements" v-if="userRole === 'ADMIN'">
          <el-icon><Bell /></el-icon>
          <span>公告管理</span>
        </el-menu-item>
        <el-menu-item index="/reservations" v-if="userRole === 'ADMIN'">
          <el-icon><Tickets /></el-icon>
          <span>预约管理</span>
        </el-menu-item>
        <el-menu-item index="/admin-orders" v-if="userRole === 'ADMIN'">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <div class="main-area" :class="{ 'night-mode': isNightMode }">
      <!-- 星空背景 -->
      <div class="stars-container" v-if="isNightMode">
        <div v-for="n in 50" :key="n" class="star" :style="{
          left: Math.random() * 100 + '%',
          top: Math.random() * 100 + '%',
          animationDelay: Math.random() * 3 + 's',
          animationDuration: 1.5 + Math.random() * 2 + 's'
        }"></div>
      </div>
      <!-- 顶部导航栏 -->
      <el-header class="topbar">
        <div class="topbar-left">
          <el-icon
            :size="22"
            class="collapse-btn"
            @click="isCollapse = !isCollapse"
          >
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/dashboard'">首页概览</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/books'">图书管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/blind-box'">今天读什么</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/ai-assistant'">AI助手</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/borrows'">借阅管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/favorites'">我的收藏</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/profile'">个人中心</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/stats'">数据统计</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/users'">用户管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/announcements'">公告管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/reservations'">预约管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/achievements'">成就徽章</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/booklists'">书单分享</el-breadcrumb-item>
            
            <el-breadcrumb-item v-if="activeMenu === '/mystudy'">我的书房</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/challenges'">共读挑战</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/groups'">读书小组</el-breadcrumb-item>
            <el-breadcrumb-item v-if="activeMenu === '/messages'">私信</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-tag
            :type="userRole === 'ADMIN' ? 'danger' : 'success'"
            effect="dark"
            size="large"
          >
            <el-icon><UserFilled /></el-icon>
            {{ userName }} ({{ userRole === 'ADMIN' ? '管理员' : '普通用户' }})
          </el-tag>
          <el-button type="danger" text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="content">
        <router-view />
      </el-main>

      <!-- 夜灯开关 -->
      <div class="night-lamp" :class="{ active: isNightMode }" @click="toggleNightMode" :title="isNightMode ? '切换到白天模式' : '打开夜灯模式'">
        <span class="lamp-icon">{{ isNightMode ? '🌙' : '💡' }}</span>
        <span class="lamp-label">{{ isNightMode ? '夜' : '灯' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Reading,
  HomeFilled,
  UserFilled,
  Fold,
  Expand,
  SwitchButton,
  Collection,
  DataAnalysis,
  Tickets,
  StarFilled,
  Bell,
  Present,
  Service,
  Sunny,
  FolderOpened,
  Flag,
  TrophyBase,
  Grid,
  Message,
  Histogram,
  ShoppingCart,
  Location,
  List
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
const userName = ref(localStorage.getItem('username') || '')
const userRole = ref(localStorage.getItem('role') || 'USER')

const activeMenu = computed(() => route.path)

// 夜灯模式
const isNightMode = ref(localStorage.getItem('nightMode') === 'true')
provide('nightMode', isNightMode)

function toggleNightMode() {
  isNightMode.value = !isNightMode.value
  localStorage.setItem('nightMode', isNightMode.value ? 'true' : 'false')
  if (isNightMode.value) {
    playFlipSound()
  }
}

function playFlipSound() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = ctx.createOscillator()
    const gainNode = ctx.createGain()
    oscillator.connect(gainNode)
    gainNode.connect(ctx.destination)
    oscillator.type = 'sine'
    oscillator.frequency.setValueAtTime(800, ctx.currentTime)
    oscillator.frequency.exponentialRampToValueAtTime(300, ctx.currentTime + 0.15)
    gainNode.gain.setValueAtTime(0.05, ctx.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3)
    oscillator.start(ctx.currentTime)
    oscillator.stop(ctx.currentTime + 0.3)
  } catch (e) { /* browser may not support */ }
}

// 点击事件监听：夜灯模式下点图书卡片发出翻书声
function handleClick(e) {
  if (!isNightMode.value) return
  const target = e.target.closest('.book-card, .el-card, .book-cover')
  if (target) {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)()
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(600, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(200, ctx.currentTime + 0.1)
      gain.gain.setValueAtTime(0.03, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2)
      osc.start(ctx.currentTime)
      osc.stop(ctx.currentTime + 0.2)
    } catch (e) { /* ignore */ }
  }
}

onMounted(() => {
  document.addEventListener('click', handleClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClick)
})

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a3e 0%, #2d1b4e 50%, #1a1a3e 100%);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: 60px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(255,255,255,0.5);
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409eff;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content {
  flex: 1;
  background: linear-gradient(180deg, #f8f9fc 0%, #f0eef6 100%);
  padding: 20px;
  overflow-y: auto;
  transition: background 0.8s ease;
}

/* 夜灯模式 */
.night-mode .content {
  background: linear-gradient(180deg, #0a0a2e 0%, #1a1a4e 50%, #0d0d3a 100%);
}

.night-mode .topbar {
  background: #1a1a3e;
  box-shadow: 0 1px 4px rgba(0,0,0,0.3);
  color: #e0e0ff;
  border-bottom: 1px solid #2a2a5e;
}

.night-mode .topbar .el-breadcrumb__item span {
  color: #a0a0d0;
}

.night-mode .topbar .collapse-btn {
  color: #a0a0d0;
}

/* 星空背景 */
.stars-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.star {
  position: absolute;
  width: 2px;
  height: 2px;
  background: #fff;
  border-radius: 50%;
  animation: starTwinkle 2s ease-in-out infinite;
}

@keyframes starTwinkle {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.5); }
}

/* 夜灯开关 */
.night-lamp {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  transition: all 0.5s ease;
  user-select: none;
}

.night-lamp:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(0,0,0,0.2);
}

.night-lamp.active {
  background: #2a2a5e;
  box-shadow: 0 4px 20px rgba(100, 100, 255, 0.4), 0 0 30px rgba(100, 100, 255, 0.2);
}

.lamp-icon {
  font-size: 20px;
  line-height: 1;
}

.lamp-label {
  font-size: 10px;
  color: #909399;
  margin-top: 2px;
}

.night-lamp.active .lamp-label {
  color: #a0a0d0;
}

/* 夜灯模式下图书封面发光 */
.night-mode :deep(.book-card) {
  background: #1e1e4e !important;
  border-color: #2a2a5e !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3);
}

.night-mode :deep(.book-card:hover) {
  box-shadow: 0 8px 32px rgba(80, 80, 200, 0.3);
}

.night-mode :deep(.book-cover) {
  box-shadow: 0 0 20px rgba(100, 120, 255, 0.3), 0 4px 16px rgba(0,0,0,0.3);
  animation: coverGlow 3s ease-in-out infinite;
}

.night-mode :deep(.book-cover:hover) {
  box-shadow: 0 0 40px rgba(100, 120, 255, 0.6), 0 4px 24px rgba(0,0,0,0.4);
}

@keyframes coverGlow {
  0%, 100% { box-shadow: 0 0 20px rgba(100, 120, 255, 0.3), 0 4px 16px rgba(0,0,0,0.3); }
  50% { box-shadow: 0 0 35px rgba(100, 120, 255, 0.5), 0 4px 20px rgba(0,0,0,0.35); }
}

.night-mode :deep(.el-card) {
  background: #1e1e4e !important;
  border-color: #2a2a5e !important;
  color: #e0e0ff;
}

.night-mode :deep(.el-input__wrapper) {
  background: #2a2a5e;
  border-color: #3a3a7e;
  box-shadow: none;
}

.night-mode :deep(.el-input__inner) {
  color: #e0e0ff;
}

.night-mode :deep(.el-table) {
  background: #1a1a3e;
  color: #e0e0ff;
}

.night-mode :deep(.el-table th) {
  background: #1e1e4e;
  color: #a0a0d0;
}

.night-mode :deep(.el-table tr) {
  background: #1a1a3e;
}

.night-mode :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: #1e1e4e;
}

.night-mode :deep(.el-tabs__header) {
  background: #1a1a3e;
  border-color: #2a2a5e;
}

.night-mode :deep(.el-tabs__content) {
  background: #1a1a3e;
}

.night-mode :deep(.el-dialog) {
  background: #1a1a3e;
  border: 1px solid #2a2a5e;
}

.night-mode :deep(.el-dialog__title) {
  color: #e0e0ff;
}

.night-mode :deep(.el-tag) {
  background: #2a2a5e;
  border-color: #3a3a7e;
  color: #e0e0ff;
}
</style>
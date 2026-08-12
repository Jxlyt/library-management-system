<template>
  <div class="stats-dashboard">
    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :span="6" v-for="item in overviewCards" :key="item.label">
        <el-card shadow="hover" class="overview-card">
          <div class="card-content">
            <div class="card-icon" :style="{ background: item.bg }">
              <el-icon :size="28" color="#fff"><component :is="item.icon" /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-num">{{ item.value }}</div>
              <div class="card-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售统计 -->
    <el-row :gutter="20" class="overview-row" style="margin-top: 16px">
      <el-col :span="6" v-for="item in salesCards" :key="item.label">
        <el-card shadow="hover" class="overview-card">
          <div class="card-content">
            <div class="card-icon" :style="{ background: item.bg }">
              <el-icon :size="28" color="#fff"><component :is="item.icon" /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-num">{{ item.value }}</div>
              <div class="card-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="14">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>图书借阅排行榜</span>
            </div>
          </template>
          <v-chart :option="borrowRankingOption" style="height: 380px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>图书分类统计</span>
            </div>
          </template>
          <v-chart :option="categoryOption" style="height: 380px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售排行榜 -->
    <el-row :gutter="20" class="chart-row" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>图书销售排行榜</span>
            </div>
          </template>
          <v-chart :option="salesRankingOption" style="height: 350px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { Reading, UserFilled, Collection, Clock, Money, ShoppingCart, TrendCharts } from '@element-plus/icons-vue'
import axios from 'axios'

use([BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

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

const overview = ref({})

const overviewCards = computed(() => [
  { label: '图书总量', value: overview.value.totalBooks || 0, icon: Reading, bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { label: '用户总数', value: overview.value.totalUsers || 0, icon: UserFilled, bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { label: '今日借阅', value: overview.value.todayBorrows || 0, icon: Collection, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { label: '当前借出', value: overview.value.currentBorrowing || 0, icon: Clock, bg: 'linear-gradient(135deg, #fa709a, #fee140)' },
])

const borrowRanking = ref([])
const categoryStats = ref([])
const salesStats = ref({ todaySales: 0, monthSales: 0, totalOrders: 0, topSelling: [] })

const salesCards = computed(() => [
  { label: '今日销售额', value: '¥' + (salesStats.value.todaySales || 0).toFixed(2), icon: Money, bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' },
  { label: '本月销售额', value: '¥' + (salesStats.value.monthSales || 0).toFixed(2), icon: TrendCharts, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { label: '今日订单', value: salesStats.value.todayOrders || 0, icon: ShoppingCart, bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { label: '总订单数', value: salesStats.value.totalOrders || 0, icon: Collection, bg: 'linear-gradient(135deg, #a18cd1, #fbc2eb)' },
])

const borrowRankingOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#909399' }
  },
  yAxis: {
    type: 'category',
    data: borrowRanking.value.map(i => i.title).reverse(),
    axisLabel: {
      color: '#606266',
      width: 120,
      overflow: 'truncate'
    }
  },
  series: [{
    name: '借阅次数',
    type: 'bar',
    data: borrowRanking.value.map(i => i.count).reverse(),
    itemStyle: {
      borderRadius: [0, 4, 4, 0],
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ]
      }
    },
    barWidth: 20
  }]
}))

const categoryOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    textStyle: { color: '#606266' }
  },
  series: [{
    name: '分类统计',
    type: 'pie',
    radius: ['45%', '75%'],
    center: ['40%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: {
      borderRadius: 6,
      borderColor: '#fff',
      borderWidth: 2
    },
    label: {
      show: true,
      formatter: '{b}\n{d}%'
    },
    data: categoryStats.value.map(i => ({
      name: i.category,
      value: i.count
    }))
  }]
}))
})

const salesRankingOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#909399' }
  },
  yAxis: {
    type: 'category',
    data: (salesStats.value.topSelling || []).map(i => i[1] || '未知').reverse(),
    axisLabel: { color: '#606266', width: 120, overflow: 'truncate' }
  },
  series: [{
    name: '销量',
    type: 'bar',
    data: (salesStats.value.topSelling || []).map(i => i[2] || 0).reverse(),
    itemStyle: {
      borderRadius: [0, 4, 4, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#43e97b' }, { offset: 1, color: '#38f9d7' }] }
    },
    barWidth: 20
  }]
}))

async function fetchOverview() {
  try {
    const res = await request.get('/stats/overview')
    if (res.data.code === 200) {
      overview.value = res.data.data
    }
  } catch (e) {
    // handled
  }
}

async function fetchBorrowRanking() {
  try {
    const res = await request.get('/stats/borrow-ranking')
    if (res.data.code === 200) {
      borrowRanking.value = res.data.data
    }
  } catch (e) {
    // handled
  }
}

async function fetchCategoryStats() {
  try {
    const res = await request.get('/stats/category-stats')
    if (res.data.code === 200) {
      categoryStats.value = res.data.data
    }
  } catch (e) {
    // handled
  }
}

async function fetchSalesStats() {
  try {
    const res = await request.get('/orders/stats')
    if (res.data.code === 200) {
      const data = res.data.data
      // topSelling format: [[bookId, bookTitle, count], ...]
      salesStats.value = {
        todaySales: data.todaySales || 0,
        monthSales: data.monthSales || 0,
        todayOrders: data.todayOrders || 0,
        totalOrders: data.totalOrders || 0,
        topSelling: data.topSelling || []
      }
    }
  } catch (e) {
    // handled
  }
}

onMounted(() => {
  fetchOverview()
  fetchBorrowRanking()
  fetchCategoryStats()
  fetchSalesStats()
})
</script>

<style scoped>
.stats-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.overview-row {
  margin: 0;
}

.overview-card {
  border-radius: 12px;
}

.overview-card :deep(.el-card__body) {
  padding: 20px;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
}

.card-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.card-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.chart-row {
  margin: 0;
}

.chart-card {
  border-radius: 12px;
}

.chart-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
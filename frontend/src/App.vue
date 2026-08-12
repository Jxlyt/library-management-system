<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial,
    sans-serif;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 50%, #f0e6f6 100%);
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
}

/* 全局滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #c4b5e0, #a8d8ea);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #b09cd0, #8cc8e0);
}

/* 全局卡片阴影增强 */
.el-card {
  border-radius: 16px !important;
  border: 1px solid rgba(255,255,255,0.6) !important;
  backdrop-filter: blur(10px);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1) !important;
}
.el-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.12) !important;
}

/* 全局按钮圆角 */
.el-button {
  border-radius: 10px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}
.el-button:hover {
  transform: translateY(-1px);
}

/* 全局输入框 */
.el-input__wrapper {
  border-radius: 10px !important;
  transition: all 0.3s ease !important;
}

/* 全局标签 */
.el-tag {
  border-radius: 8px !important;
}

/* 全局弹窗 */
.el-dialog {
  border-radius: 20px !important;
  overflow: hidden;
}

/* 全局分页 */
.el-pagination .el-pager li {
  border-radius: 8px !important;
}
</style>
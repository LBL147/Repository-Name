<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue';
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">实时资讯</h1>
        <p class="page-subtitle">按关键词查看外部资讯和任务关联线索。</p>
      </div>
    </header>

    <div class="panel">
      <div class="filters">
        <el-input placeholder="关键词" clearable />
        <el-button type="primary">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button>
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="panel panel-body">
      <div class="empty-state">暂无资讯</div>
    </div>
  </section>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
}

.filters :deep(.el-input) {
  width: 280px;
}
</style>

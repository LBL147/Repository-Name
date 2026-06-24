<script setup lang="ts">
import { Download, Grid, List, Plus, Search, RefreshLeft } from '@element-plus/icons-vue';
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">任务列表</h1>
        <p class="page-subtitle">查看任务状态、负责人、优先级和截止日期。</p>
      </div>
      <div class="header-actions">
        <el-button>
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button type="primary">
          <el-icon><Plus /></el-icon>
          新增任务
        </el-button>
      </div>
    </header>

    <div class="panel">
      <div class="filters">
        <el-select placeholder="状态" clearable>
          <el-option label="待办" value="TODO" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="DONE" />
        </el-select>
        <el-input placeholder="关键词" clearable />
        <el-date-picker type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-button type="primary">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button>
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
        <el-segmented :options="['表格', '卡片']">
          <template #default="{ item }">
            <el-icon v-if="item === '表格'"><List /></el-icon>
            <el-icon v-else><Grid /></el-icon>
            <span>{{ item }}</span>
          </template>
        </el-segmented>
      </div>
    </div>

    <div class="panel">
      <el-table :data="[]" class="task-table">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="priority" label="优先级" width="120" />
        <el-table-column prop="assigneeId" label="负责人" width="120" />
        <el-table-column prop="dueDate" label="截止日期" width="140" />
        <el-table-column label="操作" width="220" fixed="right" />
      </el-table>
      <div class="empty-state">暂无任务</div>
    </div>
  </section>
</template>

<style scoped>
.header-actions,
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.filters {
  padding: 16px;
}

.filters :deep(.el-select),
.filters :deep(.el-input) {
  width: 180px;
}

.task-table {
  border-radius: 8px 8px 0 0;
}
</style>

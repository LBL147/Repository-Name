<script setup lang="ts">
import MetricCard from '@/components/MetricCard.vue';
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">仪表盘</h1>
        <p class="page-subtitle">关注待办、进行中、完成率和临期任务。</p>
      </div>
    </header>

    <div class="metric-grid">
      <MetricCard label="待办" value="0" />
      <MetricCard label="进行中" value="0" tone="warning" />
      <MetricCard label="已完成" value="0" tone="success" />
      <MetricCard label="完成率" value="0%" tone="primary" />
    </div>

    <div class="dashboard-grid">
      <div class="panel panel-body chart-panel">
        <h2>状态分布</h2>
        <div class="empty-state">暂无统计数据</div>
      </div>
      <div class="panel panel-body">
        <h2>临期与逾期</h2>
        <div class="empty-state">暂无提醒</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.panel h2 {
  margin: 0 0 16px;
  color: #101828;
  font-size: 18px;
}

.chart-panel {
  min-height: 360px;
}

@media (max-width: 1024px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>

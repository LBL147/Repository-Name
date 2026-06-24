<script setup lang="ts">
import * as echarts from 'echarts';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchDashboardOverdueTasks,
  fetchDashboardStatusChart,
  fetchDashboardSummary,
  fetchDashboardUpcomingTasks,
} from '@/api/dashboard';
import { fetchTasks } from '@/api/tasks';
import MetricCard from '@/components/MetricCard.vue';
import { useAuthStore } from '@/stores/auth';
import type { TaskStatus } from '@/types/api';
import type { DashboardStatusChartResponse, DashboardSummaryResponse } from '@/types/dashboard';
import type { TaskListItemResponse } from '@/types/task';
import { priorityLabels, priorityTagTypes, statusLabels, statusTagTypes } from '@/utils/labels';

const emptySummary: DashboardSummaryResponse = {
  todoCount: 0,
  inProgressCount: 0,
  doneCount: 0,
  totalCount: 0,
  completionRate: 0,
};

const emptyStatusChart: DashboardStatusChartResponse = {
  legendData: [],
  seriesData: [],
};

const statusColors: Record<TaskStatus, string> = {
  TODO: '#64748b',
  IN_PROGRESS: '#f97316',
  DONE: '#059669',
};

const auth = useAuthStore();
const loading = ref(false);
const reminderLoading = ref(false);
const loadError = ref('');
const summary = ref<DashboardSummaryResponse>({ ...emptySummary });
const statusChart = ref<DashboardStatusChartResponse>({ ...emptyStatusChart });
const upcomingTasks = ref<TaskListItemResponse[]>([]);
const overdueTasks = ref<TaskListItemResponse[]>([]);
const activeReminderTab = ref<'overdue' | 'upcoming'>('overdue');
const chartRef = ref<HTMLDivElement>();
let chartInstance: echarts.ECharts | null = null;
let chartResizeObserver: ResizeObserver | null = null;

const scopeText = computed(() =>
  auth.isMentor ? '导师视图：统计全部可见任务' : '实习生视图：仅统计分配给你的任务',
);

const metricCards = computed(() => [
  { label: '待办', value: summary.value.todoCount, tone: 'primary' as const },
  { label: '进行中', value: summary.value.inProgressCount, tone: 'warning' as const },
  { label: '已完成', value: summary.value.doneCount, tone: 'success' as const },
  { label: '总数', value: summary.value.totalCount, tone: 'primary' as const },
  { label: '完成率', value: formatRate(summary.value.completionRate), tone: 'success' as const },
]);

const hasChartData = computed(() => statusChart.value.seriesData.some((item) => item.value > 0));

function todayText() {
  return new Date().toISOString().slice(0, 10);
}

function addDaysText(days: number) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}

function formatRate(value: number) {
  if (!Number.isFinite(value)) {
    return '0%';
  }
  return `${Number.isInteger(value) ? value : value.toFixed(2)}%`;
}

function statusLabel(status: TaskStatus) {
  return statusLabels[status];
}

function priorityLabel(priority: TaskListItemResponse['priority']) {
  return priorityLabels[priority];
}

function formatUserId(id: number) {
  if (auth.user?.id === id) {
    return `${auth.user.displayName || auth.user.username}（ID ${id}）`;
  }
  return `用户 ID ${id}`;
}

function formatDueDate(value?: string) {
  return value || '未设置';
}

function compareByDueDate(a: TaskListItemResponse, b: TaskListItemResponse) {
  return (a.dueDate || '').localeCompare(b.dueDate || '') || b.id - a.id;
}

function isOverdueTask(task: TaskListItemResponse) {
  return Boolean(task.dueDate && task.dueDate < todayText() && task.status !== 'DONE');
}

function isUpcomingTask(task: TaskListItemResponse) {
  const today = todayText();
  const end = addDaysText(7);
  return Boolean(task.dueDate && task.dueDate >= today && task.dueDate <= end && task.status !== 'DONE');
}

function daysUntilDue(task: TaskListItemResponse) {
  if (!task.dueDate) {
    return '';
  }

  const due = new Date(`${task.dueDate}T00:00:00`);
  const today = new Date(`${todayText()}T00:00:00`);
  const diff = Math.round((due.getTime() - today.getTime()) / 86_400_000);

  if (diff < 0) {
    return `已逾期 ${Math.abs(diff)} 天`;
  }
  if (diff === 0) {
    return '今天截止';
  }
  return `${diff} 天后截止`;
}

function chartSeriesData() {
  return statusChart.value.seriesData.map((item) => ({
    name: item.name || statusLabel(item.status),
    value: item.value,
    itemStyle: {
      color: statusColors[item.status],
    },
  }));
}

function initChart() {
  if (!chartRef.value) {
    return;
  }

  chartInstance ??= echarts.init(chartRef.value);
}

function renderChart() {
  if (!chartRef.value || !hasChartData.value) {
    return;
  }

  initChart();
  chartInstance?.setOption({
    color: Object.values(statusColors),
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: 0,
      left: 'center',
      data: statusChart.value.legendData,
      itemGap: 18,
    },
    series: [
      {
        name: '状态分布',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: {
          formatter: '{b}\n{c}',
          color: '#344054',
          fontSize: 12,
        },
        labelLine: {
          length: 14,
          length2: 8,
        },
        data: chartSeriesData(),
      },
    ],
  });
}

function resizeChart() {
  chartInstance?.resize();
}

async function loadFallbackReminderTasks() {
  const [upcomingPage, todoOverduePage, progressOverduePage] = await Promise.all([
    fetchTasks({
      dueDateStart: todayText(),
      dueDateEnd: addDaysText(7),
      page: 1,
      size: 1000,
    }),
    fetchTasks({
      status: 'TODO',
      dueDateEnd: addDaysText(-1),
      page: 1,
      size: 1000,
    }),
    fetchTasks({
      status: 'IN_PROGRESS',
      dueDateEnd: addDaysText(-1),
      page: 1,
      size: 1000,
    }),
  ]);

  upcomingTasks.value = upcomingPage.records.filter(isUpcomingTask).sort(compareByDueDate);
  overdueTasks.value = [...todoOverduePage.records, ...progressOverduePage.records].filter(isOverdueTask).sort(compareByDueDate);
}

async function loadReminderTasks() {
  reminderLoading.value = true;
  try {
    const [upcoming, overdue] = await Promise.all([
      fetchDashboardUpcomingTasks(),
      fetchDashboardOverdueTasks(),
    ]);
    upcomingTasks.value = upcoming.filter(isUpcomingTask).sort(compareByDueDate);
    overdueTasks.value = overdue.filter(isOverdueTask).sort(compareByDueDate);
  } catch {
    await loadFallbackReminderTasks();
  } finally {
    reminderLoading.value = false;
  }
}

async function loadDashboard() {
  loading.value = true;
  loadError.value = '';
  try {
    const [summaryResponse, chartResponse] = await Promise.all([
      fetchDashboardSummary(),
      fetchDashboardStatusChart(),
    ]);
    summary.value = summaryResponse;
    statusChart.value = chartResponse;
    await loadReminderTasks();
    await nextTick();
    renderChart();
  } catch (error) {
    summary.value = { ...emptySummary };
    statusChart.value = { ...emptyStatusChart };
    upcomingTasks.value = [];
    overdueTasks.value = [];
    loadError.value = error instanceof Error ? error.message : '仪表盘加载失败';
    ElMessage.error(loadError.value);
  } finally {
    loading.value = false;
  }
}

watch(
  () => statusChart.value.seriesData,
  () => {
    void nextTick(renderChart);
  },
  { deep: true },
);

onMounted(async () => {
  await loadDashboard();
  if (chartRef.value) {
    chartResizeObserver = new ResizeObserver(resizeChart);
    chartResizeObserver.observe(chartRef.value);
  }
});

onBeforeUnmount(() => {
  chartResizeObserver?.disconnect();
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">仪表盘</h1>
        <p class="page-subtitle">{{ scopeText }}，关注待办、进行中、完成率和临期任务。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
    </header>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />

    <div class="metric-grid" v-loading="loading">
      <MetricCard
        v-for="card in metricCards"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :tone="card.tone"
      />
    </div>

    <div class="dashboard-grid">
      <div class="panel panel-body chart-panel" v-loading="loading">
        <div class="panel-head">
          <div>
            <h2>状态分布</h2>
            <p>待办、进行中、已完成任务占比</p>
          </div>
        </div>
        <div v-show="hasChartData" ref="chartRef" class="status-chart" aria-label="任务状态分布图"></div>
        <div v-if="!hasChartData" class="empty-state">暂无统计数据</div>
      </div>

      <div class="panel panel-body reminder-panel" v-loading="reminderLoading">
        <div class="panel-head">
          <div>
            <h2>临期与逾期</h2>
            <p>临期范围为今天至未来 7 天</p>
          </div>
        </div>

        <el-tabs v-model="activeReminderTab" class="reminder-tabs">
          <el-tab-pane :label="`逾期 ${overdueTasks.length}`" name="overdue">
            <div v-if="overdueTasks.length" class="reminder-list">
              <article v-for="task in overdueTasks" :key="task.id" class="reminder-item danger">
                <div class="reminder-title-row">
                  <strong>{{ task.title }}</strong>
                  <el-tag type="danger" effect="light">{{ daysUntilDue(task) }}</el-tag>
                </div>
                <div class="reminder-meta">
                  <span>{{ formatDueDate(task.dueDate) }}</span>
                  <span>{{ formatUserId(task.assigneeId) }}</span>
                </div>
                <div class="reminder-tags">
                  <el-tag :type="statusTagTypes[task.status]" effect="light">{{ statusLabel(task.status) }}</el-tag>
                  <el-tag :type="priorityTagTypes[task.priority]" effect="plain">{{ priorityLabel(task.priority) }}</el-tag>
                </div>
              </article>
            </div>
            <div v-else class="empty-state compact">当前没有逾期任务</div>
          </el-tab-pane>

          <el-tab-pane :label="`临期 ${upcomingTasks.length}`" name="upcoming">
            <div v-if="upcomingTasks.length" class="reminder-list">
              <article v-for="task in upcomingTasks" :key="task.id" class="reminder-item warning">
                <div class="reminder-title-row">
                  <strong>{{ task.title }}</strong>
                  <el-tag type="warning" effect="light">{{ daysUntilDue(task) }}</el-tag>
                </div>
                <div class="reminder-meta">
                  <span>{{ formatDueDate(task.dueDate) }}</span>
                  <span>{{ formatUserId(task.assigneeId) }}</span>
                </div>
                <div class="reminder-tags">
                  <el-tag :type="statusTagTypes[task.status]" effect="light">{{ statusLabel(task.status) }}</el-tag>
                  <el-tag :type="priorityTagTypes[task.priority]" effect="plain">{{ priorityLabel(task.priority) }}</el-tag>
                </div>
              </article>
            </div>
            <div v-else class="empty-state compact">未来 7 天没有临期任务</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </section>
</template>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0 0 16px;
  color: #101828;
  font-size: 18px;
}

.panel-head p {
  margin: -8px 0 0;
  color: var(--tm-muted);
  font-size: 13px;
  line-height: 1.5;
}

.chart-panel {
  min-height: 360px;
}

.status-chart {
  width: 100%;
  height: 340px;
  min-height: 340px;
}

.reminder-panel {
  min-width: 0;
}

.reminder-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.reminder-list {
  display: flex;
  max-height: 340px;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.reminder-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
  border: 1px solid var(--tm-border);
  border-left-width: 4px;
  border-radius: 8px;
  padding: 12px;
  background: #ffffff;
}

.reminder-item.danger {
  border-left-color: var(--tm-danger);
}

.reminder-item.warning {
  border-left-color: var(--tm-warning);
}

.reminder-title-row,
.reminder-meta,
.reminder-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reminder-title-row {
  justify-content: space-between;
}

.reminder-title-row strong {
  min-width: 0;
  color: #101828;
  font-size: 15px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.reminder-meta {
  flex-wrap: wrap;
  color: var(--tm-muted);
  font-size: 12px;
}

.reminder-tags {
  flex-wrap: wrap;
}

.empty-state.compact {
  min-height: 180px;
}

@media (max-width: 1024px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .status-chart {
    height: 300px;
    min-height: 300px;
  }

  .reminder-title-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

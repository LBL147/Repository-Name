<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Grid, List, Plus, Refresh, Right, Search, Select, View } from '@element-plus/icons-vue';
import { createTask, deleteTask, fetchTask, fetchTasks, updateTask, updateTaskStatus } from '@/api/tasks';
import { useAuthStore } from '@/stores/auth';
import type { TaskPriority, TaskStatus } from '@/types/api';
import type { TaskListItemResponse, TaskQuery, TaskResponse } from '@/types/task';
import { priorityLabels, priorityTagTypes, statusLabels, statusTagTypes } from '@/utils/labels';

type ViewMode = 'table' | 'cards';
type DialogMode = 'create' | 'edit';

interface TaskFormState {
  title: string;
  description: string;
  assigneeId?: number;
  priority: TaskPriority;
  dueDate: string;
  status: TaskStatus;
}

interface TaskFilterState {
  status: TaskStatus | '';
  assigneeId?: number;
  dueDateRange: [string, string] | null;
  keyword: string;
}

const auth = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const detailLoading = ref(false);
const loadError = ref('');
const tasks = ref<TaskListItemResponse[]>([]);
const statusUpdatingIds = ref(new Set<number>());
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const viewMode = ref<ViewMode>('table');
const dialogVisible = ref(false);
const dialogMode = ref<DialogMode>('create');
const currentTaskId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const form = reactive<TaskFormState>({
  title: '',
  description: '',
  assigneeId: undefined,
  priority: 'MEDIUM',
  dueDate: '',
  status: 'TODO',
});

const filters = reactive<TaskFilterState>({
  status: '',
  // TODO: replace numeric assignee fields with a user selector once a user list API is available.
  assigneeId: undefined,
  dueDateRange: null,
  keyword: '',
});

const rules: FormRules<TaskFormState> = {
  title: [
    { required: true, message: '请输入任务标题', trigger: 'blur' },
    { max: 128, message: '标题不能超过 128 个字符', trigger: 'blur' },
  ],
  description: [{ max: 2000, message: '描述不能超过 2000 个字符', trigger: 'blur' }],
  assigneeId: [{ required: true, message: '请输入负责人 ID', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const viewOptions = [
  { label: '表格', value: 'table' },
  { label: '卡片', value: 'cards' },
];

const statusOptions: Array<{ label: string; value: TaskStatus }> = [
  { label: statusLabels.TODO, value: 'TODO' },
  { label: statusLabels.IN_PROGRESS, value: 'IN_PROGRESS' },
  { label: statusLabels.DONE, value: 'DONE' },
];

const priorityOptions: Array<{ label: string; value: TaskPriority }> = [
  { label: priorityLabels.HIGH, value: 'HIGH' },
  { label: priorityLabels.MEDIUM, value: 'MEDIUM' },
  { label: priorityLabels.LOW, value: 'LOW' },
];

const canCreate = computed(() => auth.isMentor);
const canDelete = computed(() => auth.isMentor);
const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增任务' : '任务详情 / 编辑'));
const emptyText = computed(() => (loadError.value ? '任务加载失败' : '暂无任务'));
const canUpdateCurrentTaskStatus = computed(
  () => dialogMode.value === 'edit' && currentTaskId.value !== null && canUpdateStatus({ assigneeId: form.assigneeId }),
);

function formatUserId(id: number) {
  if (auth.user?.id === id) {
    return `${auth.user.displayName || auth.user.username}（ID ${id}）`;
  }
  return `用户 ID ${id}`;
}

function normalizeDate(value?: string) {
  return value || '未设置';
}

function statusLabel(status: TaskStatus) {
  return statusLabels[status];
}

function statusTagType(status: TaskStatus) {
  return statusTagTypes[status];
}

function priorityLabel(priority: TaskPriority) {
  return priorityLabels[priority];
}

function priorityTagType(priority: TaskPriority) {
  return priorityTagTypes[priority];
}

function canUpdateStatus(task: Pick<TaskListItemResponse, 'assigneeId'> | { assigneeId?: number }) {
  return auth.isMentor || (task.assigneeId !== undefined && auth.user?.id === task.assigneeId);
}

function nextStatus(status: TaskStatus): TaskStatus | null {
  if (status === 'TODO') {
    return 'IN_PROGRESS';
  }
  if (status === 'IN_PROGRESS') {
    return 'DONE';
  }
  return null;
}

function nextStatusButtonText(status: TaskStatus) {
  if (status === 'TODO') {
    return '开始处理';
  }
  if (status === 'IN_PROGRESS') {
    return '标记完成';
  }
  return '已完成';
}

function nextStatusButtonIcon(status: TaskStatus) {
  return status === 'IN_PROGRESS' ? Select : Right;
}

function isStatusUpdating(id: number) {
  return statusUpdatingIds.value.has(id);
}

function setStatusUpdating(id: number, updating: boolean) {
  const nextIds = new Set(statusUpdatingIds.value);
  if (updating) {
    nextIds.add(id);
  } else {
    nextIds.delete(id);
  }
  statusUpdatingIds.value = nextIds;
}

function buildTaskQuery(): TaskQuery {
  const query: TaskQuery = {
    page: currentPage.value,
    size: pageSize.value,
  };

  if (filters.status) {
    query.status = filters.status;
  }
  if (filters.assigneeId) {
    query.assigneeId = filters.assigneeId;
  }
  if (filters.dueDateRange) {
    query.dueDateStart = filters.dueDateRange[0];
    query.dueDateEnd = filters.dueDateRange[1];
  }
  const keyword = filters.keyword.trim();
  if (keyword) {
    query.keyword = keyword;
  }

  return query;
}

function resetForm() {
  form.title = '';
  form.description = '';
  form.assigneeId = auth.isMentor ? undefined : auth.user?.id;
  form.priority = 'MEDIUM';
  form.dueDate = '';
  form.status = 'TODO';
  currentTaskId.value = null;
  formRef.value?.clearValidate();
}

function applyTaskToForm(task: TaskResponse) {
  form.title = task.title;
  form.description = task.description || '';
  form.assigneeId = task.assigneeId;
  form.priority = task.priority;
  form.dueDate = task.dueDate || '';
  form.status = task.status;
  currentTaskId.value = task.id;
}

async function loadTasks() {
  loading.value = true;
  loadError.value = '';
  try {
    const page = await fetchTasks(buildTaskQuery());
    tasks.value = page.records;
    total.value = page.total;
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '任务列表加载失败';
    ElMessage.error(loadError.value);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadTasks();
}

function resetFilters() {
  filters.status = '';
  filters.assigneeId = undefined;
  filters.dueDateRange = null;
  filters.keyword = '';
  currentPage.value = 1;
  loadTasks();
}

function openCreateDialog() {
  if (!canCreate.value) {
    ElMessage.warning('实习生不能新增任务');
    return;
  }
  dialogMode.value = 'create';
  dialogVisible.value = true;
  resetForm();
}

async function openEditDialog(task: TaskListItemResponse) {
  dialogMode.value = 'edit';
  dialogVisible.value = true;
  resetForm();
  detailLoading.value = true;
  try {
    applyTaskToForm(await fetchTask(task.id));
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务详情加载失败');
    dialogVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !form.assigneeId) {
    return;
  }

  saving.value = true;
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      assigneeId: form.assigneeId,
      priority: form.priority,
      dueDate: form.dueDate || undefined,
      status: form.status,
    };

    if (dialogMode.value === 'create') {
      await createTask(payload);
      ElMessage.success('任务已新增');
    } else if (currentTaskId.value !== null) {
      await updateTask(currentTaskId.value, payload);
      ElMessage.success('任务已保存');
    }

    dialogVisible.value = false;
    await loadTasks();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务保存失败');
  } finally {
    saving.value = false;
  }
}

async function changeTaskStatus(task: TaskListItemResponse, status: TaskStatus) {
  if (!canUpdateStatus(task)) {
    ElMessage.warning('当前账号无权更新该任务状态');
    return;
  }
  if (task.status === status || isStatusUpdating(task.id)) {
    return;
  }

  setStatusUpdating(task.id, true);
  try {
    const updatedTask = await updateTaskStatus(task.id, { status });
    ElMessage.success(`任务状态已更新为${statusLabel(status)}`);
    if (currentTaskId.value === task.id) {
      applyTaskToForm(updatedTask);
    }
    await loadTasks();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '状态更新失败，请稍后重试');
  } finally {
    setStatusUpdating(task.id, false);
  }
}

function changeTaskToNextStatus(task: TaskListItemResponse) {
  const targetStatus = nextStatus(task.status);
  if (targetStatus) {
    changeTaskStatus(task, targetStatus);
  }
}

async function changeCurrentTaskStatus(status: TaskStatus) {
  if (currentTaskId.value === null || !canUpdateCurrentTaskStatus.value || form.status === status) {
    return;
  }

  const taskId = currentTaskId.value;
  setStatusUpdating(taskId, true);
  try {
    const updatedTask = await updateTaskStatus(taskId, { status });
    applyTaskToForm(updatedTask);
    ElMessage.success(`任务状态已更新为${statusLabel(status)}`);
    await loadTasks();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '状态更新失败，请稍后重试');
  } finally {
    setStatusUpdating(taskId, false);
  }
}

function changeCurrentTaskToNextStatus() {
  const targetStatus = nextStatus(form.status);
  if (targetStatus) {
    changeCurrentTaskStatus(targetStatus);
  }
}

async function confirmDelete(task: TaskListItemResponse) {
  if (!canDelete.value) {
    return;
  }

  try {
    await ElMessageBox.confirm(`确认删除任务“${task.title}”？`, '删除任务', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    });
    await deleteTask(task.id);
    ElMessage.success('任务已删除');
    if (currentTaskId.value === task.id) {
      dialogVisible.value = false;
    }
    await loadTasks();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '任务删除失败');
    }
  }
}

async function deleteCurrentTask() {
  const task = tasks.value.find((item) => item.id === currentTaskId.value);
  if (task) {
    await confirmDelete(task);
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  loadTasks();
}

function handleSizeChange(size: number) {
  pageSize.value = size;
  currentPage.value = 1;
  loadTasks();
}

onMounted(loadTasks);
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">任务列表</h1>
        <p class="page-subtitle">查看任务状态、负责人、优先级和截止日期，并完成基础 CRUD 操作。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadTasks">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button v-if="canCreate" type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增任务
        </el-button>
      </div>
    </header>

    <div class="panel toolbar-panel">
      <el-form class="filter-form" :model="filters" label-position="top">
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人 ID">
          <el-input-number
            v-model="filters.assigneeId"
            :min="1"
            :precision="0"
            clearable
            controls-position="right"
            placeholder="全部负责人"
          />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="filters.dueDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="至"
            clearable
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索标题或描述"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <div class="filter-actions">
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" :disabled="loading" @click="resetFilters">重置</el-button>
        </div>
      </el-form>

      <div class="toolbar">
        <div class="list-summary">
          <strong>{{ total }}</strong>
          <span>个任务</span>
        </div>
        <el-segmented v-model="viewMode" :options="viewOptions">
          <template #default="{ item }">
            <el-icon v-if="item.value === 'table'"><List /></el-icon>
            <el-icon v-else><Grid /></el-icon>
            <span>{{ item.label }}</span>
          </template>
        </el-segmented>
      </div>
    </div>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />

    <div class="panel list-panel" v-loading="loading">
      <template v-if="tasks.length">
        <el-table v-if="viewMode === 'table'" :data="tasks" class="task-table" row-key="id">
          <el-table-column prop="title" label="标题" min-width="240">
            <template #default="{ row }">
              <button class="link-button" type="button" @click="openEditDialog(row)">
                {{ row.title }}
              </button>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="120">
            <template #default="{ row }">
              <el-tag :type="priorityTagType(row.priority)" effect="plain">
                {{ priorityLabel(row.priority) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="负责人" width="160">
            <template #default="{ row }">
              {{ formatUserId(row.assigneeId) }}
            </template>
          </el-table-column>
          <el-table-column label="截止日期" width="140">
            <template #default="{ row }">
              {{ normalizeDate(row.dueDate) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="290" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button
                  v-if="nextStatus(row.status) && canUpdateStatus(row)"
                  size="small"
                  type="primary"
                  plain
                  :icon="nextStatusButtonIcon(row.status)"
                  :loading="isStatusUpdating(row.id)"
                  @click="changeTaskToNextStatus(row)"
                >
                  {{ nextStatusButtonText(row.status) }}
                </el-button>
                <el-button size="small" :icon="View" @click="openEditDialog(row)">详情</el-button>
                <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
                <el-button
                  v-if="canDelete"
                  size="small"
                  type="danger"
                  :icon="Delete"
                  @click="confirmDelete(row)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="task-card-grid">
          <article v-for="task in tasks" :key="task.id" class="task-card">
            <div class="card-head">
              <button class="card-title" type="button" @click="openEditDialog(task)">
                {{ task.title }}
              </button>
              <el-tag :type="priorityTagType(task.priority)" effect="plain">
                {{ priorityLabel(task.priority) }}
              </el-tag>
            </div>
            <div class="card-meta">
              <span>状态</span>
              <el-tag :type="statusTagType(task.status)" effect="light">
                {{ statusLabel(task.status) }}
              </el-tag>
            </div>
            <div class="card-meta">
              <span>负责人</span>
              <strong>{{ formatUserId(task.assigneeId) }}</strong>
            </div>
            <div class="card-meta">
              <span>截止日期</span>
              <strong>{{ normalizeDate(task.dueDate) }}</strong>
            </div>
            <div class="card-actions">
              <el-button
                v-if="nextStatus(task.status) && canUpdateStatus(task)"
                type="primary"
                plain
                :icon="nextStatusButtonIcon(task.status)"
                :loading="isStatusUpdating(task.id)"
                @click="changeTaskToNextStatus(task)"
              >
                {{ nextStatusButtonText(task.status) }}
              </el-button>
              <el-button :icon="View" @click="openEditDialog(task)">详情 / 编辑</el-button>
              <el-button v-if="canDelete" type="danger" :icon="Delete" @click="confirmDelete(task)">删除</el-button>
            </div>
          </article>
        </div>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </template>

      <div v-else class="empty-state">
        <div>
          <strong>{{ emptyText }}</strong>
          <p>{{ loadError ? '请稍后重试或刷新页面。' : '当前列表没有任务。' }}</p>
          <el-button v-if="!loadError && canCreate" type="primary" :icon="Plus" @click="openCreateDialog">
            新增任务
          </el-button>
          <el-button v-if="loadError" :icon="Refresh" @click="loadTasks">重新加载</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <div v-loading="detailLoading" class="dialog-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="请输入任务标题" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="请输入任务描述"
            />
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="负责人 ID" prop="assigneeId">
              <el-input-number
                v-model="form.assigneeId"
                :min="1"
                :precision="0"
                :disabled="!auth.isMentor"
                controls-position="right"
              />
            </el-form-item>
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority">
                <el-option
                  v-for="option in priorityOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="截止日期" prop="dueDate">
              <el-date-picker
                v-model="form.dueDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择截止日期"
                clearable
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" :disabled="dialogMode === 'edit' && !canUpdateCurrentTaskStatus">
                <el-option
                  v-for="option in statusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>

        <div v-if="dialogMode === 'edit'" class="status-flow-panel">
          <div>
            <span>当前状态</span>
            <el-tag :type="statusTagType(form.status)" effect="light">
              {{ statusLabel(form.status) }}
            </el-tag>
          </div>
          <el-button
            v-if="nextStatus(form.status) && canUpdateCurrentTaskStatus"
            type="primary"
            plain
            :icon="nextStatusButtonIcon(form.status)"
            :loading="currentTaskId !== null && isStatusUpdating(currentTaskId)"
            @click="changeCurrentTaskToNextStatus"
          >
            {{ nextStatusButtonText(form.status) }}
          </el-button>
          <span v-else-if="form.status === 'DONE'" class="done-text">已完成</span>
          <span v-else class="muted-text">当前账号无权更新状态</span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            v-if="dialogMode === 'edit' && canDelete"
            type="danger"
            :icon="Delete"
            :disabled="currentTaskId === null"
            @click="deleteCurrentTask"
          >
            删除
          </el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.header-actions,
.toolbar,
.row-actions,
.card-actions,
.dialog-footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-panel {
  overflow: hidden;
}

.filter-form {
  display: grid;
  grid-template-columns: minmax(140px, 0.8fr) minmax(150px, 0.8fr) minmax(260px, 1.25fr) minmax(220px, 1fr) auto;
  align-items: end;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--tm-border);
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-form :deep(.el-select),
.filter-form :deep(.el-date-editor),
.filter-form :deep(.el-input-number) {
  width: 100%;
}

.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.toolbar {
  justify-content: space-between;
  padding: 14px 16px;
}

.list-summary {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--tm-muted);
}

.list-summary strong {
  color: #101828;
  font-size: 20px;
}

.list-panel {
  min-height: 280px;
  overflow: hidden;
}

.task-table {
  width: 100%;
}

.link-button,
.card-title {
  border: 0;
  padding: 0;
  color: #1d4ed8;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-weight: 650;
  text-align: left;
  transition: color 180ms ease;
}

.link-button:hover,
.link-button:focus-visible,
.card-title:hover,
.card-title:focus-visible {
  color: var(--tm-primary);
  outline: none;
}

.row-actions {
  flex-wrap: wrap;
}

.task-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 16px;
}

.task-card {
  display: flex;
  min-height: 220px;
  flex-direction: column;
  gap: 14px;
  border: 1px solid var(--tm-border);
  border-radius: 8px;
  padding: 16px;
  background: #ffffff;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.task-card:hover {
  border-color: #b8c0d4;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  color: #101828;
  font-size: 16px;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--tm-muted);
  font-size: 13px;
}

.card-meta strong {
  color: #344054;
  font-weight: 600;
  text-align: right;
}

.card-actions {
  margin-top: auto;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 16px;
  border-top: 1px solid var(--tm-border);
}

.empty-state p {
  margin: 8px 0 16px;
  color: var(--tm-muted);
}

.dialog-body {
  min-height: 240px;
}

.status-flow-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--tm-border);
  border-radius: 8px;
  padding: 12px 14px;
  background: #f8fafc;
}

.status-flow-panel > div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--tm-muted);
}

.done-text,
.muted-text {
  color: var(--tm-muted);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-date-editor),
.form-grid :deep(.el-input-number) {
  width: 100%;
}

.dialog-footer {
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .task-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-form {
    grid-template-columns: 1fr;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .task-card-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .status-flow-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

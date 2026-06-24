<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Link, Refresh, Search } from '@element-plus/icons-vue';
import { fetchNews, refreshNews } from '@/api/news';
import type { NewsItemResponse } from '@/types/news';

const loading = ref(false);
const refreshing = ref(false);
const loadError = ref('');
const news = ref<NewsItemResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const filters = reactive({
  keyword: '',
});

const emptyTitle = computed(() => (loadError.value ? '资讯加载失败' : '暂无资讯'));
const emptyDescription = computed(() =>
  loadError.value ? '请稍后重试，已有资讯会继续保留。' : '可以输入关键词搜索，或手动刷新获取最新资讯。',
);

function formatDate(value?: string) {
  if (!value) {
    return '未发布';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function buildQuery() {
  const keyword = filters.keyword.trim();
  return {
    page: currentPage.value,
    size: pageSize.value,
    ...(keyword ? { keyword } : {}),
  };
}

async function loadNews() {
  loading.value = true;
  loadError.value = '';
  try {
    const page = await fetchNews(buildQuery());
    news.value = page.records;
    total.value = page.total;
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '资讯加载失败';
    ElMessage.error(loadError.value);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadNews();
}

async function handleRefresh() {
  const keyword = filters.keyword.trim();
  if (!keyword) {
    ElMessage.warning('请输入关键词后再刷新资讯');
    return;
  }

  refreshing.value = true;
  try {
    const response = await refreshNews({ keyword });
    news.value = response.records;
    total.value = response.records.length;
    currentPage.value = 1;
    loadError.value = '';
    ElMessage.success(response.message || `已刷新 ${response.records.length} 条资讯`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资讯刷新失败，已保留当前列表');
  } finally {
    refreshing.value = false;
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  loadNews();
}

function handleSizeChange(size: number) {
  pageSize.value = size;
  currentPage.value = 1;
  loadNews();
}

onMounted(loadNews);
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">实时资讯</h1>
        <p class="page-subtitle">按关键词查询外部资讯，手动刷新后保留可追溯的任务线索。</p>
      </div>
    </header>

    <div class="panel toolbar-panel">
      <div class="filters">
        <el-input
          v-model="filters.keyword"
          placeholder="输入关键词"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" :loading="refreshing" :disabled="loading" @click="handleRefresh">刷新资讯</el-button>
      </div>
    </div>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />

    <div class="panel news-panel" v-loading="loading">
      <template v-if="news.length">
        <div class="news-list">
          <article v-for="item in news" :key="item.id" class="news-item">
            <div class="news-main">
              <a class="news-title" :href="item.url" target="_blank" rel="noopener noreferrer">
                {{ item.title }}
              </a>
              <div class="news-meta">
                <span>{{ item.source || '未知来源' }}</span>
                <span>{{ formatDate(item.publishedAt) }}</span>
                <el-tag v-if="item.keyword" size="small" effect="plain">{{ item.keyword }}</el-tag>
              </div>
            </div>
            <el-button class="open-link" :icon="Link" tag="a" :href="item.url" target="_blank" rel="noopener noreferrer">
              打开
            </el-button>
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
          <strong>{{ emptyTitle }}</strong>
          <p>{{ emptyDescription }}</p>
          <el-button v-if="loadError" :icon="Refresh" @click="loadNews">重新加载</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.toolbar-panel {
  overflow: hidden;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
}

.filters :deep(.el-input) {
  width: 320px;
}

.news-panel {
  min-height: 360px;
  overflow: hidden;
}

.news-list {
  display: flex;
  min-height: 280px;
  flex-direction: column;
}

.news-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  min-height: 92px;
  padding: 16px;
  border-bottom: 1px solid var(--tm-border);
  background: #ffffff;
}

.news-item:last-child {
  border-bottom: 0;
}

.news-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.news-title {
  color: #101828;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.5;
  transition: color 180ms ease;
}

.news-title:hover,
.news-title:focus-visible {
  color: var(--tm-primary);
  outline: none;
}

.news-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: var(--tm-muted);
  font-size: 13px;
}

.open-link {
  flex: 0 0 auto;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  min-height: 64px;
  padding: 14px 16px 16px;
  border-top: 1px solid var(--tm-border);
}

.empty-state p {
  margin: 8px 0 16px;
  color: var(--tm-muted);
}

@media (max-width: 768px) {
  .filters :deep(.el-input) {
    width: 100%;
  }

  .filters :deep(.el-button) {
    flex: 1 1 140px;
  }

  .news-item {
    flex-direction: column;
  }

  .open-link {
    width: 100%;
  }

  .pagination-row {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>

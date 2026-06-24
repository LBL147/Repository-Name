<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { DataAnalysis, Document, Fold, TrendCharts } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const activeMenu = computed(() => route.path);

function logout() {
  auth.clearSession();
  router.push({ name: 'login' });
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <div class="brand-mark">IC</div>
        <div>
          <strong>任务管理</strong>
          <span>内部协作系统</span>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="nav-menu">
        <el-menu-item index="/tasks">
          <el-icon><Document /></el-icon>
          <span>任务列表</span>
        </el-menu-item>
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/news">
          <el-icon><TrendCharts /></el-icon>
          <span>实时资讯</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-title">
          <el-icon><Fold /></el-icon>
          <span>任务跟踪工作台</span>
        </div>
        <div class="account">
          <span class="account-name">{{ auth.user?.displayName || auth.user?.username }}</span>
          <el-tag size="small" effect="plain">{{ auth.user?.role === 'MENTOR' ? '导师' : '实习生' }}</el-tag>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f6f7fb;
}

.sidebar {
  border-right: 1px solid var(--tm-border);
  background: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 18px;
  border-bottom: 1px solid var(--tm-border);
}

.brand-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: #4f46e5;
  font-weight: 800;
}

.brand strong,
.brand span {
  display: block;
}

.brand span {
  margin-top: 2px;
  color: var(--tm-muted);
  font-size: 12px;
}

.nav-menu {
  border-right: 0;
  padding: 10px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--tm-border);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.topbar-title,
.account {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topbar-title {
  color: #344054;
  font-weight: 650;
}

.account-name {
  color: #101828;
  font-weight: 600;
}

.main {
  padding: 24px;
}

@media (max-width: 768px) {
  .app-shell {
    display: block;
  }

  .sidebar {
    width: 100% !important;
  }

  .nav-menu {
    display: flex;
    overflow-x: auto;
  }

  .topbar {
    min-height: 64px;
  }

  .main {
    padding: 16px;
  }
}
</style>

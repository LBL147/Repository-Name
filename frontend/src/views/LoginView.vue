<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import type { UserRole } from '@/types/api';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);
const errorMessage = ref('');

const form = reactive({
  username: '',
  password: '',
});

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

async function afterLogin() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/tasks';
  await router.push(redirect);
}

async function submitLogin() {
  errorMessage.value = '';
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  loading.value = true;
  try {
    await auth.signIn({
      username: form.username.trim(),
      password: form.password,
    });
    ElMessage.success('登录成功');
    await afterLogin();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败';
    ElMessage.error(errorMessage.value);
  } finally {
    loading.value = false;
  }
}

async function submitMockLogin(role: UserRole) {
  errorMessage.value = '';
  loading.value = true;
  try {
    await auth.signInMock({ role });
    ElMessage.success('登录成功');
    await afterLogin();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Mock 登录失败';
    ElMessage.error(errorMessage.value);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-copy">
      <span class="eyebrow">ICINFO TASKS</span>
      <h1>内部任务管理系统</h1>
      <p>导师分配、实习生推进、任务状态和资讯线索汇总在同一个工作台。</p>
    </section>

    <section class="login-panel">
      <h2>登录</h2>
      <el-alert v-if="errorMessage" class="login-error" type="error" :title="errorMessage" show-icon :closable="false" />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" size="large" autocomplete="username">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            autocomplete="current-password"
            @keyup.enter="submitLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button class="primary-action" type="primary" size="large" native-type="submit" :icon="Lock" :loading="loading">
          登录
        </el-button>
      </el-form>

      <div class="mock-actions">
        <el-button :icon="User" :loading="loading" @click="submitMockLogin('MENTOR')">导师 Mock 登录</el-button>
        <el-button :icon="User" :loading="loading" @click="submitMockLogin('INTERN')">实习生 Mock 登录</el-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 48px;
  align-items: center;
  padding: 56px max(32px, calc((100vw - 1180px) / 2));
  background:
    linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(249, 115, 22, 0.08)),
    #f6f7fb;
}

.login-copy {
  max-width: 640px;
}

.eyebrow {
  color: var(--tm-primary);
  font-size: 13px;
  font-weight: 800;
}

.login-copy h1 {
  margin: 14px 0 18px;
  color: #101828;
  font-size: 48px;
  line-height: 1.08;
  letter-spacing: 0;
}

.login-copy p {
  max-width: 520px;
  margin: 0;
  color: #475467;
  font-size: 17px;
  line-height: 1.8;
}

.login-panel {
  border: 1px solid var(--tm-border);
  border-radius: 8px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.login-panel h2 {
  margin: 0 0 22px;
  color: #101828;
  font-size: 24px;
}

.login-error {
  margin-bottom: 18px;
}

.primary-action {
  width: 100%;
}

.mock-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 32px 18px;
  }

  .login-copy h1 {
    font-size: 34px;
  }
}

@media (max-width: 480px) {
  .mock-actions {
    grid-template-columns: 1fr;
  }
}
</style>

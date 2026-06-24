<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
  tone?: 'primary' | 'success' | 'warning' | 'danger';
}>();
</script>

<template>
  <div class="metric-card" :class="tone || 'primary'">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
  </div>
</template>

<style scoped>
.metric-card {
  display: flex;
  min-height: 112px;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid var(--tm-border);
  border-radius: 8px;
  padding: 18px;
  background: #ffffff;
}

.metric-card span {
  color: var(--tm-muted);
  font-size: 14px;
}

.metric-card strong {
  color: #101828;
  font-size: 30px;
  font-weight: 750;
  letter-spacing: 0;
}

.metric-card.primary {
  border-top: 4px solid var(--tm-primary);
}

.metric-card.success {
  border-top: 4px solid var(--tm-success);
}

.metric-card.warning {
  border-top: 4px solid var(--tm-warning);
}

.metric-card.danger {
  border-top: 4px solid var(--tm-danger);
}
</style>

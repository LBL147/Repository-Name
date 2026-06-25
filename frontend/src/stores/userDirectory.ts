import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchInterns } from '@/api/users';
import type { User } from '@/types/auth';

function userDisplayName(user: User) {
  return user.displayName || user.username;
}

export const useUserDirectoryStore = defineStore('userDirectory', () => {
  const interns = ref<User[]>([]);
  const loading = ref(false);
  const loaded = ref(false);
  const loadError = ref('');

  const nameCounts = computed(() => {
    const counts = new Map<string, number>();
    for (const user of interns.value) {
      const name = userDisplayName(user);
      counts.set(name, (counts.get(name) || 0) + 1);
    }
    return counts;
  });

  const assigneeOptions = computed(() =>
    interns.value.map((user) => {
      const label = userDisplayName(user);
      return {
        value: user.id,
        label,
        username: user.username,
        showUsername: (nameCounts.value.get(label) || 0) > 1,
      };
    }),
  );

  function assigneeName(id?: number) {
    if (id === undefined) {
      return '未知负责人';
    }
    const user = interns.value.find((item) => item.id === id);
    return user ? userDisplayName(user) : '未知负责人';
  }

  async function loadInterns(force = false) {
    if (loading.value || (loaded.value && !force)) {
      return;
    }

    loading.value = true;
    loadError.value = '';
    try {
      interns.value = await fetchInterns();
      loaded.value = true;
    } catch (error) {
      loadError.value = error instanceof Error ? error.message : '负责人列表加载失败';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  return {
    interns,
    loading,
    loaded,
    loadError,
    assigneeOptions,
    assigneeName,
    loadInterns,
  };
});

<script setup lang="ts">
defineProps<{ modelValue: string; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [string]; search: [] }>()
</script>

<template>
  <div class="searchbar">
    <span class="icon">🔍</span>
    <input
      class="search-input"
      :value="modelValue"
      :placeholder="placeholder || '搜索…'"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @keyup.enter="emit('search')"
    />
  </div>
</template>

<style scoped>
.searchbar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 0 14px;
}
.searchbar:focus-within { border-color: var(--primary); }
.icon { opacity: 0.6; }
.search-input {
  flex: 1;
  border: none;
  background: none;
  padding: 12px 0;
  color: var(--text);
  font-size: 15px;
  outline: none;
}
</style>

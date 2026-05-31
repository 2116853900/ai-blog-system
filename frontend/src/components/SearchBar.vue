<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  debounce?: number
}>(), {
  debounce: 250
})
const emit = defineEmits<{ 'update:modelValue': [string]; search: [] }>()

let timer: number | undefined

function onInput(e: Event) {
  const val = (e.target as HTMLInputElement).value
  emit('update:modelValue', val)
  window.clearTimeout(timer)
  timer = window.setTimeout(() => emit('search'), props.debounce)
}

function clear() {
  emit('update:modelValue', '')
  emit('search')
}
</script>

<template>
  <div class="searchbar" role="search">
    <svg class="icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="M21 21l-4.3-4.3" />
    </svg>
    <input
      class="search-input"
      type="search"
      :value="modelValue"
      :placeholder="placeholder || '搜索…'"
      :aria-label="placeholder || '搜索'"
      @input="onInput"
      @keyup.enter="emit('search')"
    />
    <button v-if="modelValue" class="clear" @click="clear" aria-label="清空搜索">✕</button>
  </div>
</template>

<style scoped>
.searchbar {
  display: flex;
  align-items: center;
  gap: 9px;
  background: var(--bg-inset);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.searchbar:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.icon { color: var(--text-dim); flex-shrink: 0; }
.search-input {
  flex: 1;
  border: none;
  background: none;
  padding: 11px 0;
  color: var(--text);
  font-size: 14px;
  font-family: var(--font-sans);
  outline: none;
}
.search-input::-webkit-search-cancel-button { display: none; }
.clear {
  background: none;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 13px;
  padding: 4px;
}
.clear:hover { color: var(--text); }
</style>

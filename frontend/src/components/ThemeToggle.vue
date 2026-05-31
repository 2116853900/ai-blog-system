<script setup lang="ts">
import { useThemeStore } from '../stores/theme'
const theme = useThemeStore()
</script>

<template>
  <button
    class="toggle"
    @click="theme.toggle()"
    :aria-pressed="!theme.isDark"
    :aria-label="theme.isDark ? '切换到浅色主题' : '切换到深色主题'"
    :title="theme.isDark ? '浅色' : '深色'"
  >
    <!-- 深色时显示月亮，浅色时显示太阳 -->
    <svg v-if="theme.isDark" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>
    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
    </svg>
  </button>
</template>

<style scoped>
.toggle {
  background: var(--bg-soft);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  width: 38px;
  height: 36px;
  display: grid;
  place-items: center;
  color: var(--text-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}
.toggle:hover { border-color: var(--primary); color: var(--primary); }
</style>

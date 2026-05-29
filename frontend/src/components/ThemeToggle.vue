<script setup lang="ts">
import { useThemeStore } from '../stores/theme'
const theme = useThemeStore()
</script>

<template>
  <button class="toggle" @click="theme.toggle()" :title="theme.isDark ? '切换到浅色' : '切换到深色'">
    {{ theme.isDark ? '☀️' : '🌙' }}
  </button>
</template>

<style scoped>
.toggle {
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 8px;
  width: 38px;
  height: 36px;
  cursor: pointer;
  font-size: 16px;
}
.toggle:hover { border-color: var(--primary); }
</style>

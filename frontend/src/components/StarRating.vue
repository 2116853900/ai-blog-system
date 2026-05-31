<script setup lang="ts">
defineProps<{ level?: number }>()
</script>

<template>
  <span class="stars" role="img" :aria-label="`推荐指数 ${level || 0} / 5`">
    <span v-for="i in 5" :key="i" :class="{ on: i <= (level || 0) }" aria-hidden="true">★</span>
  </span>
</template>

<style scoped>
.stars { display: inline-flex; gap: 1px; letter-spacing: 1px; }
.stars span { color: var(--border-strong); font-size: 13px; }
.stars span.on { color: var(--primary); }
</style>

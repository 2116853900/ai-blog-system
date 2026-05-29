<script setup lang="ts">
defineProps<{ level?: number }>()
</script>

<template>
  <span class="stars" :title="`推荐指数 ${level || 0}/5`">
    <span v-for="i in 5" :key="i" :class="{ on: i <= (level || 0) }">★</span>
  </span>
</template>

<style scoped>
.stars span { color: var(--border); font-size: 14px; }
.stars span.on { color: var(--warning); }
</style>

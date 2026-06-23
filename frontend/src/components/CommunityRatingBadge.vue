<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  averageRating?: number
  reviewCount?: number
}>()

const hasReviews = computed(() => (props.reviewCount ?? 0) > 0)
const label = computed(() => {
  if (!hasReviews.value) return '暂无评价'
  const avg = props.averageRating ?? 0
  const count = props.reviewCount ?? 0
  return `${avg.toFixed(1)} · ${count} 评`
})
</script>

<template>
  <span class="community-rating mono" :class="{ empty: !hasReviews }" :title="hasReviews ? '社区评分' : '尚无社区评价'">
    <span aria-hidden="true">★</span> {{ label }}
  </span>
</template>

<style scoped>
.community-rating {
  font-size: 0.78rem;
  color: var(--accent);
  white-space: nowrap;
}
.community-rating.empty {
  color: var(--muted);
  font-weight: 400;
}
</style>
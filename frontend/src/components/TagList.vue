<script setup lang="ts">
defineProps<{ tags?: string; active?: string }>()
const emit = defineEmits<{ select: [tag: string] }>()

function split(tags?: string): string[] {
  if (!tags) return []
  return tags.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}
</script>

<template>
  <span>
    <span
      v-for="t in split(tags)"
      :key="t"
      class="tag"
      :class="{ 'tag-active': t === active }"
      @click.stop.prevent="emit('select', t)"
    >{{ t }}</span>
  </span>
</template>

<style scoped>
.tag-active { background: var(--primary); color: #fff; }
</style>

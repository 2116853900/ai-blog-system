<script setup lang="ts">
defineProps<{ tags?: string; active?: string }>()
const emit = defineEmits<{ select: [tag: string] }>()

function split(tags?: string): string[] {
  if (!tags) return []
  return tags.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}
</script>

<template>
  <span class="taglist">
    <button
      v-for="t in split(tags)"
      :key="t"
      type="button"
      class="tag"
      :class="{ 'tag-active': t === active }"
      :aria-pressed="t === active"
      @click.stop.prevent="emit('select', t)"
    >{{ t }}</button>
  </span>
</template>

<style scoped>
.taglist { display: inline-flex; flex-wrap: wrap; align-items: center; }
.tag { font: inherit; font-family: var(--font-mono); }
</style>

<script setup lang="ts">
withDefaults(defineProps<{
  width?: string
  height?: string
  radius?: string
  block?: boolean
}>(), {
  width: '100%',
  height: '1em',
  radius: 'var(--radius-sm)'
})
</script>

<template>
  <span
    class="skeleton"
    :class="{ block }"
    :style="{ width, height, borderRadius: radius }"
    aria-hidden="true"
  />
</template>

<style scoped>
.skeleton { display: inline-block; }
.skeleton.block { display: block; }
</style>

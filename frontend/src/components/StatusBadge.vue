<script setup lang="ts">
import type { ApiStatus } from '../api/types'
defineProps<{ status: ApiStatus; latencyMs?: number }>()

const labels: Record<ApiStatus, string> = {
  UP: '在线',
  DOWN: '离线',
  UNKNOWN: '未检测'
}
</script>

<template>
  <span
    class="badge"
    :class="{
      'badge-up': status === 'UP',
      'badge-down': status === 'DOWN',
      'badge-unknown': status === 'UNKNOWN'
    }"
  >
    <span class="dot"></span>
    {{ labels[status] }}
    <template v-if="status === 'UP' && latencyMs != null"> · {{ latencyMs }}ms</template>
  </span>
</template>

<style scoped>
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  display: inline-block;
}
</style>

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
    :title="`状态：${labels[status]}`"
  >
    <span class="dot" :class="{ pulse: status === 'UP' }" aria-hidden="true"></span>
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
  position: relative;
}
.dot.pulse::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: currentColor;
  animation: dot-pulse 1.8s var(--ease) infinite;
}
@keyframes dot-pulse {
  0% { transform: scale(1); opacity: 0.7; }
  70%, 100% { transform: scale(2.6); opacity: 0; }
}
</style>

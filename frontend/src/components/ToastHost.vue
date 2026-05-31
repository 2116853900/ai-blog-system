<script setup lang="ts">
import { useToast } from '../composables/useToast'

const { state, dismiss } = useToast()

const icons: Record<string, string> = {
  success: '✓',
  error: '✕',
  info: 'ℹ'
}
</script>

<template>
  <div class="toast-host" aria-live="polite" aria-atomic="false">
    <TransitionGroup name="toast">
      <div
        v-for="t in state.items"
        :key="t.id"
        class="toast"
        :class="`toast-${t.kind}`"
        role="status"
      >
        <span class="toast-icon" aria-hidden="true">{{ icons[t.kind] }}</span>
        <span class="toast-msg">{{ t.message }}</span>
        <button class="toast-close" @click="dismiss(t.id)" aria-label="关闭通知">✕</button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  z-index: 200;
  right: 20px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: min(380px, calc(100vw - 40px));
}
.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 13px;
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  border: 1px solid var(--border-strong);
  box-shadow: var(--shadow-lg);
  font-family: var(--font-mono);
  font-size: 13px;
}
.toast-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
}
.toast-success { border-left: 3px solid var(--accent); }
.toast-success .toast-icon { background: color-mix(in srgb, var(--accent) 18%, transparent); color: var(--accent); }
.toast-error { border-left: 3px solid var(--danger); }
.toast-error .toast-icon { background: color-mix(in srgb, var(--danger) 18%, transparent); color: var(--danger); }
.toast-info { border-left: 3px solid var(--info); }
.toast-info .toast-icon { background: color-mix(in srgb, var(--info) 18%, transparent); color: var(--info); }
.toast-msg { flex: 1; color: var(--text); line-height: 1.4; }
.toast-close {
  background: none;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 12px;
  padding: 2px;
}
.toast-close:hover { color: var(--text); }

.toast-enter-active, .toast-leave-active { transition: all 0.3s var(--ease); }
.toast-enter-from { opacity: 0; transform: translateX(20px); }
.toast-leave-to { opacity: 0; transform: translateX(20px); }
.toast-move { transition: transform 0.3s var(--ease); }
</style>

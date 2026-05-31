<script setup lang="ts">
import { ref } from 'vue'
import { toast } from '../composables/useToast'

const props = withDefaults(defineProps<{
  text: string
  label?: string
  successMsg?: string
}>(), {
  label: '复制'
})

const done = ref(false)

async function copy() {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(props.text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = props.text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    done.value = true
    toast.success(props.successMsg || '已复制到剪贴板')
    setTimeout(() => (done.value = false), 1600)
  } catch {
    toast.error('复制失败，请手动复制')
  }
}
</script>

<template>
  <button
    type="button"
    class="copy-btn"
    :class="{ done }"
    @click.stop.prevent="copy"
    :aria-label="`${label}：${text}`"
  >
    <span aria-hidden="true">{{ done ? '✓' : '⧉' }}</span>
    <span class="copy-label">{{ done ? '已复制' : label }}</span>
  </button>
</template>

<style scoped>
.copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 9px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-strong);
  background: var(--bg-elevated);
  color: var(--text-soft);
  font-family: var(--font-mono);
  font-size: 11.5px;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--dur) var(--ease);
}
.copy-btn:hover { border-color: var(--primary); color: var(--primary); }
.copy-btn.done { border-color: var(--accent); color: var(--accent); }
</style>

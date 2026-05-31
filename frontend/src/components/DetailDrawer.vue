<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'

const props = defineProps<{ open: boolean; title?: string }>()
const emit = defineEmits<{ close: [] }>()

const panel = ref<HTMLElement | null>(null)
let lastFocused: HTMLElement | null = null

function close() { emit('close') }

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') { close(); return }
  if (e.key === 'Tab') trapFocus(e)
}

function focusables(): HTMLElement[] {
  if (!panel.value) return []
  return Array.from(
    panel.value.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), input, textarea, select, [tabindex]:not([tabindex="-1"])'
    )
  ).filter(el => el.offsetParent !== null)
}

function trapFocus(e: KeyboardEvent) {
  const els = focusables()
  if (!els.length) return
  const first = els[0]
  const last = els[els.length - 1]
  const active = document.activeElement as HTMLElement
  if (e.shiftKey && active === first) { e.preventDefault(); last.focus() }
  else if (!e.shiftKey && active === last) { e.preventDefault(); first.focus() }
}

watch(() => props.open, async (open) => {
  if (open) {
    lastFocused = document.activeElement as HTMLElement
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', onKeydown)
    await nextTick()
    focusables()[0]?.focus()
  } else {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', onKeydown)
    lastFocused?.focus()
  }
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="open" class="drawer-overlay" @click.self="close">
        <aside
          ref="panel"
          class="drawer-panel"
          role="dialog"
          aria-modal="true"
          :aria-label="title || '详情'"
        >
          <header class="drawer-head">
            <h2 class="drawer-title mono">{{ title }}</h2>
            <button class="btn btn-ghost btn-sm" @click="close" aria-label="关闭">esc ✕</button>
          </header>
          <div class="drawer-body">
            <slot />
          </div>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 150;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(3px);
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: min(560px, 100vw);
  height: 100%;
  background: var(--bg-elevated);
  border-left: 1px solid var(--border-strong);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.drawer-title { margin: 0; font-size: 19px; font-weight: 800; }
.drawer-body { padding: 22px; overflow-y: auto; flex: 1; }

.drawer-enter-active, .drawer-leave-active { transition: opacity 0.3s var(--ease); }
.drawer-enter-active .drawer-panel, .drawer-leave-active .drawer-panel { transition: transform 0.32s var(--ease); }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from .drawer-panel, .drawer-leave-to .drawer-panel { transform: translateX(100%); }

@media (max-width: 600px) {
  .drawer-panel { width: 100vw; }
}
</style>

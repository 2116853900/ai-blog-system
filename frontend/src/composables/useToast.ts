import { reactive } from 'vue'

export type ToastKind = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  kind: ToastKind
  message: string
}

// 模块级单例：可在组件外（如 axios 拦截器）调用
const state = reactive<{ items: Toast[] }>({ items: [] })
let seq = 0

function push(message: string, kind: ToastKind = 'info', timeout = 3200) {
  const id = ++seq
  state.items.push({ id, kind, message })
  if (timeout > 0) {
    window.setTimeout(() => dismiss(id), timeout)
  }
  return id
}

function dismiss(id: number) {
  const i = state.items.findIndex(t => t.id === id)
  if (i !== -1) state.items.splice(i, 1)
}

export const toast = {
  success: (m: string) => push(m, 'success'),
  error: (m: string) => push(m, 'error'),
  info: (m: string) => push(m, 'info')
}

export function useToast() {
  return { state, toast, dismiss }
}

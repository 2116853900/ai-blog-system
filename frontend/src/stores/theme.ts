import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(true)

  function apply() {
    // 深色为默认主题，浅色通过 .light 类覆盖
    document.documentElement.classList.toggle('light', !isDark.value)
  }

  function init() {
    const saved = localStorage.getItem('theme')
    if (saved) {
      isDark.value = saved === 'dark'
    } else {
      isDark.value = !window.matchMedia('(prefers-color-scheme: light)').matches
    }
    apply()
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    apply()
  }

  return { isDark, init, toggle }
})

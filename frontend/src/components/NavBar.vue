<script setup lang="ts">
import { RouterLink } from 'vue-router'
import ThemeToggle from './ThemeToggle.vue'
import { ref } from 'vue'

const open = ref(false)
const links = [
  { to: '/', label: '首页' },
  { to: '/skills', label: 'AI Skill' },
  { to: '/mcps', label: 'MCP' },
  { to: '/tutorials', label: '教程' },
  { to: '/api-stations', label: '公益 API' },
  { to: '/submit', label: '投稿' }
]
</script>

<template>
  <header class="nav">
    <div class="container nav-inner">
      <RouterLink to="/" class="brand">🤖 AI 信息站</RouterLink>

      <button class="menu-btn" @click="open = !open" aria-label="菜单">☰</button>

      <nav class="links" :class="{ open }" @click="open = false">
        <RouterLink v-for="l in links" :key="l.to" :to="l.to" class="nav-link">
          {{ l.label }}
        </RouterLink>
        <ThemeToggle />
      </nav>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: color-mix(in srgb, var(--bg-elevated) 92%, transparent);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}
.brand {
  font-weight: 700;
  font-size: 20px;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 8px;
}
.brand:hover { text-decoration: none; opacity: 0.8; }
.links { display: flex; align-items: center; gap: 4px; }
.nav-link {
  padding: 8px 14px;
  border-radius: 8px;
  color: var(--text-soft);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
}
.nav-link:hover {
  background: var(--bg-soft);
  color: var(--text);
  text-decoration: none;
}
.router-link-exact-active.nav-link {
  color: var(--primary);
  background: var(--primary-soft);
  font-weight: 600;
}
.menu-btn {
  display: none;
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text);
  cursor: pointer;
  padding: 8px;
}

@media (max-width: 720px) {
  .menu-btn { display: block; }
  .links {
    position: absolute;
    top: 64px;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border);
    padding: 12px 20px 16px;
    gap: 4px;
    display: none;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }
  .links.open { display: flex; }
  .nav-link { padding: 10px 14px; }
}
</style>

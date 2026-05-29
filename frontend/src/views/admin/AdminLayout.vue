<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import ThemeToggle from '../../components/ThemeToggle.vue'

const auth = useAuthStore()
const router = useRouter()

const menu = [
  { to: '/admin/posts', label: '📚 教程' },
  { to: '/admin/skills', label: '✨ Skill' },
  { to: '/admin/mcps', label: '🔌 MCP' },
  { to: '/admin/api-stations', label: '🌐 API 站点' },
  { to: '/admin/comments', label: '💬 评论审核' },
  { to: '/admin/submissions', label: '📥 投稿审核' }
]

function logout() {
  auth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <RouterLink to="/" class="brand">🤖 AI 信息站</RouterLink>
      <nav class="menu">
        <RouterLink v-for="m in menu" :key="m.to" :to="m.to" class="menu-item">{{ m.label }}</RouterLink>
      </nav>
      <div class="side-foot">
        <ThemeToggle />
        <span class="muted user">{{ auth.username }}</span>
        <button class="btn btn-sm" @click="logout">退出</button>
      </div>
    </aside>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin { display: flex; min-height: 100vh; }
.sidebar {
  width: 220px; flex-shrink: 0; border-right: 1px solid var(--border);
  background: var(--bg-elevated); padding: 20px 14px; display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh;
}
.brand { font-weight: 800; font-size: 17px; color: var(--text); margin-bottom: 20px; }
.brand:hover { text-decoration: none; }
.menu { display: flex; flex-direction: column; gap: 4px; flex: 1; }
.menu-item { padding: 10px 12px; border-radius: 8px; color: var(--text-soft); }
.menu-item:hover { background: var(--bg-soft); color: var(--text); text-decoration: none; }
.router-link-active.menu-item { background: var(--primary-soft); color: var(--primary); }
.side-foot { display: flex; align-items: center; gap: 8px; padding-top: 14px; border-top: 1px solid var(--border); }
.user { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content { flex: 1; padding: 28px 32px; max-width: 100%; overflow-x: hidden; }
@media (max-width: 640px) {
  .admin { flex-direction: column; }
  .sidebar { width: 100%; height: auto; position: relative; flex-direction: row; flex-wrap: wrap; align-items: center; gap: 8px; }
  .menu { flex-direction: row; flex-wrap: wrap; }
  .content { padding: 20px; }
}
</style>

<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ThemeToggle from './ThemeToggle.vue'
import { ref, watch } from 'vue'
import { useAuthStore } from '../stores/auth'

const open = ref(false)
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const searchQuery = ref(typeof route.query.q === 'string' ? route.query.q : '')
const links = [
  { to: '/', label: 'home' },
  { to: '/skills', label: 'skills' },
  { to: '/mcps', label: 'mcp' },
  { to: '/tutorials', label: 'docs' },
  { to: '/api-stations', label: 'api' },
  { to: '/forum', label: 'forum' },
  { to: '/submit', label: 'submit' }
]

function logout() {
  auth.logout()
  open.value = false
}

function submitSearch() {
  const q = searchQuery.value.trim()
  if (!q) return
  open.value = false
  router.push({ name: 'search', query: { q } })
}

watch(
  () => route.query.q,
  value => {
    if (route.name === 'search') {
      searchQuery.value = typeof value === 'string' ? value : ''
    }
  }
)
</script>

<template>
  <header class="nav">
    <div class="container nav-inner">
      <RouterLink to="/" class="brand">
        <span class="brand-mark" aria-hidden="true">&gt;_</span>
        <span class="brand-text">AI 信息站</span>
      </RouterLink>

      <button
        class="menu-btn"
        @click="open = !open"
        :aria-expanded="open"
        aria-controls="nav-links"
        aria-label="切换导航菜单"
      >
        {{ open ? '✕' : '☰' }}
      </button>

      <nav id="nav-links" class="links" :class="{ open }" aria-label="主导航">
        <RouterLink v-for="l in links" :key="l.to" :to="l.to" class="nav-link" @click="open = false">
          {{ l.label }}
        </RouterLink>
        <form class="nav-search" role="search" @submit.prevent="submitSearch" @click.stop>
          <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.3-4.3" />
          </svg>
          <input
            v-model="searchQuery"
            class="nav-search-input"
            type="search"
            placeholder="全站搜索"
            aria-label="全站搜索"
          />
        </form>
        <RouterLink v-if="!auth.isLoggedIn()" to="/login" class="nav-link" @click="open = false">login</RouterLink>
        <div v-else class="user-box">
          <RouterLink to="/account" class="user-link mono" @click="open = false">{{ auth.displayName }}</RouterLink>
          <button class="btn btn-sm btn-ghost" @click="logout">退出</button>
        </div>
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
  background: color-mix(in srgb, var(--bg) 80%, transparent);
  backdrop-filter: blur(12px) saturate(1.4);
  border-bottom: 1px solid var(--border);
}
.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 62px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-family: var(--font-mono);
  font-weight: 800;
  font-size: 17px;
  color: var(--text);
}
.brand:hover { text-decoration: none; }
.brand-mark {
  color: var(--primary);
  background: var(--primary-soft);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  padding: 2px 7px;
  font-weight: 700;
}
.links { display: flex; align-items: center; gap: 2px; }
.nav-link {
  padding: 7px 13px;
  border-radius: var(--radius-sm);
  color: var(--text-soft);
  font-family: var(--font-mono);
  font-size: 13.5px;
  font-weight: 500;
  transition: all var(--dur) var(--ease);
}
.nav-link::before { content: '/'; color: var(--text-dim); margin-right: 1px; }
.nav-link:hover { background: var(--bg-soft); color: var(--text); text-decoration: none; }
.router-link-exact-active.nav-link {
  color: var(--primary);
  background: var(--primary-soft);
}
.router-link-exact-active.nav-link::before { color: var(--primary-dim); }
.nav-search {
  width: 178px;
  height: 34px;
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 6px;
  padding: 0 10px;
  background: var(--bg-inset);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.nav-search:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}
.search-icon {
  flex-shrink: 0;
  color: var(--text-dim);
}
.nav-search-input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 12.5px;
}
.nav-search-input::placeholder { color: var(--text-dim); }
.nav-search-input::-webkit-search-cancel-button { display: none; }
.user-box { display: flex; align-items: center; gap: 8px; margin-left: 4px; }
.user-link {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 6px 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-soft);
  font-size: 12px;
}
.user-link:hover { color: var(--primary); border-color: var(--primary-dim); text-decoration: none; }
.menu-btn {
  display: none;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 18px;
  width: 40px;
  height: 38px;
  color: var(--text);
  cursor: pointer;
}

@media (max-width: 720px) {
  .menu-btn { display: block; }
  .links {
    position: absolute;
    top: 62px;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-strong);
    padding: 12px 20px 16px;
    gap: 4px;
    display: none;
    box-shadow: var(--shadow-lg);
  }
  .links.open { display: flex; }
  .nav-link { padding: 11px 14px; font-size: 15px; }
  .nav-search {
    width: 100%;
    height: 42px;
    margin: 2px 0 8px;
  }
  .nav-search-input { font-size: 14px; }
  .user-box { margin-left: 0; align-items: stretch; }
  .user-link { max-width: none; }
}
</style>

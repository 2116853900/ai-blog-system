<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import ToastHost from './components/ToastHost.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
</script>

<template>
  <a href="#main" class="skip-link">跳到主内容</a>
  <NavBar v-if="!isAdmin" />
  <main id="main">
    <router-view />
  </main>
  <footer v-if="!isAdmin" class="site-footer">
    <div class="container">
      <p class="muted">
        <span class="prompt mono">AI 信息站</span> ·
        收录 AI Skill / MCP / 教程 / 公益 API 中转站 ·
        内容仅供学习交流，公益 API 请遵守各站点规则
      </p>
    </div>
  </footer>
  <ToastHost />
</template>

<style scoped>
.skip-link {
  position: absolute;
  left: 12px;
  top: -48px;
  z-index: 300;
  padding: 8px 14px;
  background: var(--primary);
  color: var(--bg);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 13px;
  transition: top 0.2s var(--ease);
}
.skip-link:focus { top: 12px; }
.site-footer {
  margin-top: 80px;
  padding: 40px 0;
  border-top: 1px solid var(--border);
  text-align: center;
  font-size: 14px;
  background: var(--bg-soft);
}
.site-footer .muted {
  line-height: 1.8;
  max-width: 800px;
  margin: 0 auto;
}
</style>

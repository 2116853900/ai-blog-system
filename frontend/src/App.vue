<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
</script>

<template>
  <NavBar v-if="!isAdmin" />
  <router-view />
  <footer v-if="!isAdmin" class="site-footer">
    <div class="container">
      <p class="muted">
        AI 信息站 · 收录 AI Skill / MCP / 教程 / 公益 API 中转站 ·
        内容仅供学习交流，公益 API 请遵守各站点规则
      </p>
    </div>
  </footer>
</template>

<style scoped>
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

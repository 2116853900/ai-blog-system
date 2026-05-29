<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { Post } from '../api/types'

const latestPosts = ref<Post[]>([])

const sections = [
  { to: '/skills', icon: '✨', title: 'AI Skill 推荐', desc: '精选实用的 AI 能力与技巧，提升你的生产力' },
  { to: '/mcps', icon: '🔌', title: 'MCP 推荐', desc: '模型上下文协议服务器，让 AI 连接万物' },
  { to: '/tutorials', icon: '📚', title: '相关教程', desc: '从入门到进阶的 AI 实战教程' },
  { to: '/api-stations', icon: '🌐', title: '公益 API 中转站', desc: '社区分享的免费/公益 API，实时在线状态' }
]

onMounted(async () => {
  try {
    latestPosts.value = (await publicApi.posts()).slice(0, 4)
  } catch (e) { /* ignore */ }
})
</script>

<template>
  <div class="container">
    <section class="hero">
      <h1>探索 AI 的实用信息</h1>
      <p class="muted">
        收录优质 <b>AI Skill</b>、<b>MCP 服务器</b>、<b>实战教程</b> 与 <b>公益 API 中转站</b>，
        帮你更快用上 AI。
      </p>
    </section>

    <section class="grid entry-grid">
      <RouterLink v-for="s in sections" :key="s.to" :to="s.to" class="card entry">
        <div class="entry-icon">{{ s.icon }}</div>
        <h3>{{ s.title }}</h3>
        <p class="muted">{{ s.desc }}</p>
      </RouterLink>
    </section>

    <section v-if="latestPosts.length" class="latest">
      <h2 class="section-title">最新教程</h2>
      <div class="grid">
        <RouterLink
          v-for="p in latestPosts"
          :key="p.id"
          :to="`/tutorials/${p.slug}`"
          class="card post-card"
        >
          <span v-if="p.category" class="tag">{{ p.category }}</span>
          <h3>{{ p.title }}</h3>
          <p class="muted">{{ p.summary }}</p>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  text-align: center;
  padding: 80px 0 60px;
  background: linear-gradient(135deg, var(--primary-soft) 0%, var(--bg) 100%);
  margin: -20px -20px 40px;
  border-radius: 0 0 24px 24px;
}
.hero h1 {
  font-size: 48px;
  font-weight: 800;
  margin: 0 0 16px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero p {
  font-size: 18px;
  max-width: 680px;
  margin: 0 auto;
  line-height: 1.8;
}
.entry-grid { margin-bottom: 60px; }
.entry {
  padding: 32px;
  text-decoration: none;
  color: var(--text);
  text-align: center;
  position: relative;
}
.entry:hover { text-decoration: none; }
.entry-icon {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
}
.entry h3 {
  margin: 12px 0 8px;
  font-size: 20px;
  font-weight: 600;
}
.entry p {
  font-size: 14px;
  line-height: 1.6;
}
.latest {
  margin-top: 60px;
}
.post-card {
  padding: 24px;
  color: var(--text);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.post-card:hover { text-decoration: none; }
.post-card h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}
.post-card p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}
</style>

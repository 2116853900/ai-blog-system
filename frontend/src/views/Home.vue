<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { Post } from '../api/types'

const latestPosts = ref<Post[]>([])

const sections = [
  { to: '/skills', no: '01', cmd: 'skills', title: 'AI Skill 推荐', desc: '精选实用的 AI 能力与技巧，提升你的生产力' },
  { to: '/mcps', no: '02', cmd: 'mcp', title: 'MCP 推荐', desc: '模型上下文协议服务器，让 AI 连接万物' },
  { to: '/tutorials', no: '03', cmd: 'docs', title: '相关教程', desc: '从入门到进阶的 AI 实战教程' },
  { to: '/api-stations', no: '04', cmd: 'api', title: '公益 API 中转站', desc: '社区分享的公益 API，实时在线状态' }
]

onMounted(async () => {
  try {
    latestPosts.value = (await publicApi.posts()).slice(0, 4)
  } catch { /* ignore */ }
})
</script>

<template>
  <div class="container">
    <section class="hero">
      <div class="hero-card card">
        <div class="hero-bar" aria-hidden="true">
          <span class="dot d1"></span><span class="dot d2"></span><span class="dot d3"></span>
          <span class="hero-bar-title mono">ai-info-station — zsh</span>
        </div>
        <div class="hero-body">
          <p class="hero-line mono"><span class="prompt"></span>whoami</p>
          <h1 class="hero-title">探索 AI 的<span class="hl">实用信息</span></h1>
          <p class="hero-desc">
            收录优质 <b>AI Skill</b>、<b>MCP 服务器</b>、<b>实战教程</b> 与 <b>公益 API 中转站</b>，
            帮你更快用上 AI。
          </p>
          <p class="hero-line mono out"><span class="prompt"></span>ls ./sections<span class="cursor"></span></p>
        </div>
      </div>
    </section>

    <section class="entry-grid" aria-label="内容分区">
      <RouterLink
        v-for="(s, i) in sections" :key="s.to" :to="s.to"
        class="card entry rise" :style="{ animationDelay: `${i * 0.06}s` }"
      >
        <div class="entry-top">
          <span class="entry-no mono">{{ s.no }}</span>
          <span class="entry-cmd mono">./{{ s.cmd }}</span>
        </div>
        <h3 class="entry-title mono">{{ s.title }}</h3>
        <p class="muted entry-desc">{{ s.desc }}</p>
        <span class="entry-go mono">cd → </span>
      </RouterLink>
    </section>

    <section v-if="latestPosts.length" class="latest">
      <h2 class="section-title prompt">最新教程</h2>
      <div class="grid">
        <RouterLink
          v-for="p in latestPosts" :key="p.id"
          :to="`/tutorials/${p.slug}`"
          class="card post-card"
        >
          <span v-if="p.category" class="chip cat">{{ p.category }}</span>
          <h3 class="mono">{{ p.title }}</h3>
          <p class="muted">{{ p.summary }}</p>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero { padding: 48px 0 40px; }
.hero-card { overflow: hidden; padding: 0; }
.hero-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 16px;
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}
.dot { width: 11px; height: 11px; border-radius: 50%; }
.d1 { background: #ff5f56; } .d2 { background: #ffbd2e; } .d3 { background: #27c93f; }
.hero-bar-title { margin-left: 10px; font-size: 12px; color: var(--text-dim); }
.hero-body { padding: 30px 32px 34px; }
.hero-line { font-size: 14px; color: var(--text-soft); margin: 0; }
.hero-line.out { margin-top: 22px; }
.hero-title {
  font-family: var(--font-mono);
  font-size: clamp(30px, 5vw, 48px);
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.15;
  margin: 16px 0 14px;
}
.hl { color: var(--primary); }
.hero-desc { font-size: 17px; line-height: 1.8; max-width: 640px; margin: 0; }
.hero-desc b { color: var(--text); font-weight: 600; }

.entry-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  margin-bottom: 64px;
}
.entry {
  padding: 24px;
  color: var(--text);
  display: flex;
  flex-direction: column;
  position: relative;
}
.entry:hover { text-decoration: none; }
.entry-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.entry-no { font-size: 13px; color: var(--primary); font-weight: 700; }
.entry-cmd { font-size: 12px; color: var(--text-dim); }
.entry-title { margin: 0 0 8px; font-size: 17px; font-weight: 700; }
.entry-desc { font-size: 13.5px; line-height: 1.6; flex: 1; margin: 0; }
.entry-go {
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-dim);
  transition: color var(--dur) var(--ease), transform var(--dur) var(--ease);
}
.entry:hover .entry-go { color: var(--primary); transform: translateX(4px); }

.latest { margin-top: 56px; }
.post-card { padding: 22px; color: var(--text); display: flex; flex-direction: column; gap: 10px; }
.post-card:hover { text-decoration: none; }
.cat { align-self: flex-start; pointer-events: none; }
.post-card h3 { margin: 0; font-size: 16px; font-weight: 700; line-height: 1.4; }
.post-card p { margin: 0; font-size: 14px; line-height: 1.6; }
</style>

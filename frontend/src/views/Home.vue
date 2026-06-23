<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { computed, onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { GlobalSearchType, Post, PublicStats } from '../api/types'

const latestPosts = ref<Post[]>([])
const stats = ref<PublicStats | null>(null)
const statsLoading = ref(true)

const typeLabels: Record<GlobalSearchType, string> = {
  POST: '教程',
  SKILL: 'Skill',
  MCP: 'MCP',
  API: 'API',
  FORUM_THREAD: '讨论'
}

const apiSegments = computed(() => {
  const health = stats.value?.apiHealth
  if (!health || health.total === 0) return []
  return [
    { key: 'up', label: '在线', value: health.up, width: `${health.up * 100 / health.total}%` },
    { key: 'down', label: '离线', value: health.down, width: `${health.down * 100 / health.total}%` },
    { key: 'unknown', label: '未知', value: health.unknown, width: `${health.unknown * 100 / health.total}%` }
  ].filter(segment => segment.value > 0)
})

const sections = [
  { to: '/skills', no: '01', cmd: 'skills', title: 'AI Skill 推荐', desc: '精选实用的 AI 能力与技巧，提升你的生产力' },
  { to: '/mcps', no: '02', cmd: 'mcp', title: 'MCP 推荐', desc: '模型上下文协议服务器，让 AI 连接万物' },
  { to: '/tutorials', no: '03', cmd: 'docs', title: '相关教程', desc: '从入门到进阶的 AI 实战教程' },
  { to: '/api-stations', no: '04', cmd: 'api', title: '公益 API 中转站', desc: '社区分享的公益 API，实时在线状态' }
]

function fmtDate(value?: string) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

onMounted(async () => {
  try {
    latestPosts.value = (await publicApi.posts()).slice(0, 4)
  } catch { /* ignore */ }
  statsLoading.value = true
  try {
    stats.value = await publicApi.stats()
  } catch {
    stats.value = null
  } finally {
    statsLoading.value = false
  }
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

    <section v-if="statsLoading || stats" class="platform-dynamics">
      <div class="dynamics-head">
        <h2 class="section-title prompt">平台动态</h2>
        <RouterLink to="/stats" class="muted mono dynamics-link">完整洞察 →</RouterLink>
      </div>

      <div v-if="statsLoading" class="dynamics-loading muted mono">加载中…</div>

      <template v-else-if="stats">
        <div class="dynamics-api card">
          <div class="api-head">
            <span class="mono dim">API 健康</span>
            <strong class="mono">{{ stats.apiHealth.uptimeRate.toFixed(1) }}% 可用</strong>
          </div>
          <div v-if="apiSegments.length" class="status-rail" aria-label="API 状态分布">
            <span
              v-for="segment in apiSegments"
              :key="segment.key"
              :class="['rail-segment', segment.key]"
              :style="{ width: segment.width }"
              :title="`${segment.label}: ${segment.value}`"
            ></span>
          </div>
          <p class="muted api-meta mono">
            {{ stats.apiHealth.up }} 在线 / {{ stats.apiHealth.down }} 离线 / {{ stats.apiHealth.unknown }} 未知
          </p>
        </div>

        <div class="dynamics-grid">
          <div v-if="stats.recentItems.length" class="card dynamics-panel">
            <h3 class="mono panel-title">最新收录</h3>
            <RouterLink
              v-for="item in stats.recentItems.slice(0, 6)"
              :key="`${item.type}-${item.url}`"
              :to="item.url"
              class="dynamics-row"
            >
              <span class="mono row-type">{{ typeLabels[item.type] }}</span>
              <span class="row-main">
                <strong>{{ item.title }}</strong>
                <small v-if="item.description" class="muted">{{ item.description }}</small>
              </span>
              <span class="mono dim row-meta">{{ fmtDate(item.createdAt) }}</span>
            </RouterLink>
          </div>

          <div v-if="stats.hotThreads.length" class="card dynamics-panel">
            <h3 class="mono panel-title">热门讨论</h3>
            <RouterLink
              v-for="thread in stats.hotThreads.slice(0, 5)"
              :key="thread.id"
              :to="thread.url"
              class="dynamics-row thread-row"
            >
              <span class="row-main">
                <strong>{{ thread.title }}</strong>
                <small class="muted mono">{{ thread.replyCount }} 回复 · {{ thread.viewCount }} 浏览</small>
              </span>
              <span v-if="thread.solved" class="chip chip-active solved">已解决</span>
            </RouterLink>
          </div>
        </div>
      </template>
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
  margin-bottom: 48px;
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

.platform-dynamics { margin-bottom: 56px; }
.dynamics-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.dynamics-link { font-size: 13px; }
.dynamics-loading { padding: 20px; }
.dynamics-api { padding: 18px 20px; margin-bottom: 16px; }
.api-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.api-head strong { color: var(--primary); font-size: 18px; }
.status-rail {
  height: 10px;
  display: flex;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  margin-bottom: 10px;
}
.rail-segment.up { background: var(--accent); }
.rail-segment.down { background: var(--danger); }
.rail-segment.unknown { background: var(--text-dim); }
.api-meta { margin: 0; font-size: 12px; }
.dynamics-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}
.dynamics-panel { padding: 18px 20px; }
.panel-title { margin: 0 0 12px; font-size: 15px; }
.dynamics-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--border);
  color: var(--text);
}
.dynamics-row:first-of-type { border-top: 0; padding-top: 0; }
.dynamics-row:hover { text-decoration: none; }
.thread-row { grid-template-columns: minmax(0, 1fr) auto; }
.row-type { font-size: 11px; color: var(--primary); }
.row-main { min-width: 0; display: grid; gap: 2px; }
.row-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.row-main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.row-meta { font-size: 11px; white-space: nowrap; }
.solved { font-size: 11px; }

.latest { margin-top: 56px; }
.post-card { padding: 22px; color: var(--text); display: flex; flex-direction: column; gap: 10px; }
.post-card:hover { text-decoration: none; }
.cat { align-self: flex-start; pointer-events: none; }
.post-card h3 { margin: 0; font-size: 16px; font-weight: 700; line-height: 1.4; }
.post-card p { margin: 0; font-size: 14px; line-height: 1.6; }

@media (max-width: 640px) {
  .dynamics-row { grid-template-columns: 1fr; }
  .row-meta { justify-self: start; }
}
</style>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { PublicStats, GlobalSearchType } from '../api/types'

const stats = ref<PublicStats | null>(null)
const loading = ref(true)
const error = ref('')

const typeLabels: Record<GlobalSearchType, string> = {
  POST: 'DOC',
  SKILL: 'SKILL',
  MCP: 'MCP',
  API: 'API',
  FORUM_THREAD: 'THREAD'
}

const apiSegments = computed(() => {
  const health = stats.value?.apiHealth
  if (!health || health.total === 0) return []
  return [
    { key: 'up', label: 'UP', value: health.up, width: `${health.up * 100 / health.total}%` },
    { key: 'down', label: 'DOWN', value: health.down, width: `${health.down * 100 / health.total}%` },
    { key: 'unknown', label: 'UNKNOWN', value: health.unknown, width: `${health.unknown * 100 / health.total}%` }
  ].filter(segment => segment.value > 0)
})

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    stats.value = await publicApi.stats()
  } catch {
    error.value = '统计数据加载失败'
  } finally {
    loading.value = false
  }
})

function fmt(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function fmtDate(value?: string) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function tagList(tags?: string) {
  if (!tags) return []
  return tags.split(/[,，]/).map(tag => tag.trim()).filter(Boolean).slice(0, 3)
}
</script>

<template>
  <main class="container stats-page">
    <section class="stats-hero">
      <div>
        <p class="mono eyebrow"><span class="prompt"></span>platform telemetry</p>
        <h1 class="mono">平台洞察</h1>
        <p class="muted">
          汇总内容资产、社区讨论、API 中转站健康状态和近期活跃信号。
        </p>
      </div>
      <div class="hero-terminal mono">
        <span>snapshot</span>
        <strong>{{ stats ? fmtDate(stats.generatedAt) : 'pending' }}</strong>
      </div>
    </section>

    <section v-if="loading" class="loading-grid" aria-label="统计加载中">
      <div v-for="i in 8" :key="i" class="skeleton load-cell"></div>
    </section>

    <section v-else-if="error" class="state-panel">
      <p class="mono">{{ error }}</p>
      <button class="btn" @click="$router.go(0)">重新加载</button>
    </section>

    <template v-else-if="stats">
      <section class="metric-strip" aria-label="核心统计">
        <article class="metric-card">
          <span class="mono metric-label">resources</span>
          <strong class="mono">{{ fmt(stats.content.totalResources) }}</strong>
          <p>{{ fmt(stats.content.posts) }} docs / {{ fmt(stats.content.skills) }} skills / {{ fmt(stats.content.mcps) }} mcps</p>
        </article>
        <article class="metric-card">
          <span class="mono metric-label">api uptime</span>
          <strong class="mono">{{ stats.apiHealth.uptimeRate.toFixed(1) }}%</strong>
          <p>{{ fmt(stats.apiHealth.up) }} up / {{ fmt(stats.apiHealth.down) }} down / {{ fmt(stats.apiHealth.unknown) }} unknown</p>
        </article>
        <article class="metric-card">
          <span class="mono metric-label">community</span>
          <strong class="mono">{{ fmt(stats.community.threads) }}</strong>
          <p>{{ fmt(stats.community.replies) }} replies / {{ fmt(stats.community.solvedThreads) }} solved</p>
        </article>
        <article class="metric-card">
          <span class="mono metric-label">attention</span>
          <strong class="mono">{{ fmt(stats.community.totalViews) }}</strong>
          <p>{{ fmt(stats.community.totalLikes) }} likes / {{ fmt(stats.community.totalFavorites) }} favorites</p>
        </article>
      </section>

      <section class="ops-grid">
        <div class="panel api-panel">
          <div class="panel-head">
            <h2 class="mono">API 健康</h2>
            <span class="mono">{{ stats.apiHealth.averageLatencyMs ?? '—' }} ms avg</span>
          </div>
          <div class="status-rail" aria-label="API 状态分布">
            <span
              v-for="segment in apiSegments"
              :key="segment.key"
              :class="['rail-segment', segment.key]"
              :style="{ width: segment.width }"
              :title="`${segment.label}: ${segment.value}`"
            ></span>
          </div>
          <div class="status-cells">
            <div><b>{{ fmt(stats.apiHealth.up) }}</b><span>online</span></div>
            <div><b>{{ fmt(stats.apiHealth.down) }}</b><span>offline</span></div>
            <div><b>{{ fmt(stats.apiHealth.unknown) }}</b><span>unknown</span></div>
          </div>
        </div>

        <div class="panel tags-panel">
          <div class="panel-head">
            <h2 class="mono">热门标签</h2>
            <span class="mono">{{ stats.popularTags.length }} signals</span>
          </div>
          <div class="tag-cloud">
            <RouterLink
              v-for="tag in stats.popularTags"
              :key="tag.tag"
              :to="tag.url"
              class="signal-tag"
            >
              <span>{{ tag.tag }}</span>
              <b class="mono">{{ tag.count }}</b>
            </RouterLink>
          </div>
        </div>
      </section>

      <section class="activity-grid">
        <div class="panel feed-panel">
          <div class="panel-head">
            <h2 class="mono">最新动态</h2>
            <span class="mono">{{ stats.recentItems.length }} items</span>
          </div>
          <RouterLink v-for="item in stats.recentItems" :key="`${item.type}-${item.url}`" :to="item.url" class="feed-row">
            <span class="mono item-type">{{ typeLabels[item.type] }}</span>
            <span class="feed-main">
              <strong>{{ item.title }}</strong>
              <small class="muted">{{ item.description }}</small>
            </span>
            <span class="mono feed-meta">{{ item.metric || fmtDate(item.createdAt) }}</span>
          </RouterLink>
        </div>

        <div class="panel thread-panel">
          <div class="panel-head">
            <h2 class="mono">热门讨论</h2>
            <span class="mono">{{ stats.hotThreads.length }} threads</span>
          </div>
          <RouterLink v-for="thread in stats.hotThreads" :key="thread.id" :to="thread.url" class="thread-row">
            <div class="thread-copy">
              <strong>{{ thread.title }}</strong>
              <span class="thread-tags">
                <span v-for="tag in tagList(thread.tags)" :key="tag">#{{ tag }}</span>
              </span>
            </div>
            <div class="thread-metrics mono">
              <span>{{ fmt(thread.viewCount) }} views</span>
              <span>{{ fmt(thread.replyCount) }} replies</span>
              <span v-if="thread.solved" class="solved">solved</span>
            </div>
          </RouterLink>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.stats-page { padding: 40px 22px 70px; }
.stats-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 22px;
  align-items: end;
  margin-bottom: 26px;
}
.eyebrow { margin: 0 0 8px; color: var(--primary); font-size: 13px; }
.stats-hero h1 {
  margin: 0 0 10px;
  font-size: 52px;
  line-height: 1;
  letter-spacing: 0;
}
.stats-hero p { max-width: 620px; margin: 0; }
.hero-terminal {
  min-width: 210px;
  padding: 14px 16px;
  border: 1px solid var(--border-strong);
  background: var(--bg-inset);
  border-radius: var(--radius-sm);
  display: grid;
  gap: 4px;
}
.hero-terminal span { color: var(--text-dim); font-size: 12px; }
.hero-terminal strong { color: var(--primary); font-size: 15px; }
.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}
.load-cell { height: 136px; }
.state-panel {
  border: 1px dashed var(--border-strong);
  background: var(--bg-elevated);
  border-radius: var(--radius);
  padding: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid var(--border);
  background: var(--bg-elevated);
  border-radius: var(--radius);
  overflow: hidden;
}
.metric-card {
  padding: 20px;
  border-right: 1px solid var(--border);
}
.metric-card:last-child { border-right: 0; }
.metric-label {
  display: block;
  color: var(--text-dim);
  font-size: 12px;
  margin-bottom: 12px;
}
.metric-card strong {
  display: block;
  color: var(--text);
  font-size: 32px;
  line-height: 1;
}
.metric-card p {
  margin: 12px 0 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.5;
}
.ops-grid,
.activity-grid {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}
.ops-grid { grid-template-columns: minmax(280px, 0.9fr) minmax(0, 1.1fr); }
.activity-grid { grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr); }
.panel {
  border: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-elevated) 86%, transparent);
  border-radius: var(--radius);
  padding: 20px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}
.panel-head h2 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0;
}
.panel-head span {
  color: var(--text-dim);
  font-size: 12px;
  white-space: nowrap;
}
.status-rail {
  height: 18px;
  display: flex;
  overflow: hidden;
  border: 1px solid var(--border-strong);
  background: var(--bg-inset);
  border-radius: var(--radius-sm);
}
.rail-segment.up { background: var(--accent); }
.rail-segment.down { background: var(--danger); }
.rail-segment.unknown { background: var(--text-dim); }
.status-cells {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
}
.status-cells div {
  padding: 12px;
  border: 1px solid var(--border);
  background: var(--bg-inset);
  border-radius: var(--radius-sm);
}
.status-cells b { display: block; font-size: 22px; line-height: 1; }
.status-cells span { color: var(--text-dim); font-size: 12px; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 9px; }
.signal-tag {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-soft);
  background: var(--bg-inset);
}
.signal-tag::before { content: '#'; color: var(--primary-dim); }
.signal-tag:hover {
  color: var(--primary);
  border-color: var(--primary-dim);
  text-decoration: none;
}
.signal-tag b { color: var(--text-dim); font-size: 12px; }
.feed-row,
.thread-row {
  display: grid;
  gap: 12px;
  align-items: center;
  padding: 13px 0;
  border-top: 1px solid var(--border);
  color: var(--text);
}
.feed-row { grid-template-columns: 86px minmax(0, 1fr) auto; }
.thread-row { grid-template-columns: minmax(0, 1fr) auto; }
.feed-row:hover,
.thread-row:hover { text-decoration: none; }
.item-type {
  color: var(--primary);
  font-size: 12px;
}
.feed-main {
  min-width: 0;
  display: grid;
  gap: 2px;
}
.feed-main strong,
.thread-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.feed-main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.feed-meta {
  color: var(--text-dim);
  font-size: 12px;
  white-space: nowrap;
}
.thread-copy { min-width: 0; display: grid; gap: 6px; }
.thread-tags { display: flex; flex-wrap: wrap; gap: 6px; color: var(--text-dim); font-size: 12px; }
.thread-metrics {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: var(--text-dim);
  font-size: 12px;
}
.solved { color: var(--accent); }

@media (max-width: 920px) {
  .stats-hero,
  .ops-grid,
  .activity-grid { grid-template-columns: 1fr; }
  .metric-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metric-card:nth-child(2) { border-right: 0; }
  .metric-card:nth-child(-n + 2) { border-bottom: 1px solid var(--border); }
}

@media (max-width: 640px) {
  .stats-page { padding-top: 26px; }
  .stats-hero h1 { font-size: 36px; }
  .metric-strip,
  .status-cells { grid-template-columns: 1fr; }
  .metric-card {
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }
  .metric-card:last-child { border-bottom: 0; }
  .feed-row,
  .thread-row {
    grid-template-columns: 1fr;
    align-items: start;
  }
  .thread-metrics { align-items: flex-start; flex-direction: row; flex-wrap: wrap; }
}
</style>

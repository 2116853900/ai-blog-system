<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { publicApi } from '../api'
import type { ApiStation, ApiStationStatusCheck, ApiStationStatusSummary } from '../api/types'
import CommentSection from '../components/CommentSection.vue'
import CopyButton from '../components/CopyButton.vue'
import LinkedDiscussions from '../components/LinkedDiscussions.vue'
import RelatedResources from '../components/RelatedResources.vue'
import ResourceFavoriteButton from '../components/ResourceFavoriteButton.vue'
import ResourceReviewPanel from '../components/ResourceReviewPanel.vue'
import Skeleton from '../components/Skeleton.vue'
import StatusBadge from '../components/StatusBadge.vue'
import TagList from '../components/TagList.vue'

const route = useRoute()
const station = ref<ApiStation | null>(null)
const checks = ref<ApiStationStatusCheck[]>([])
const summary = ref<ApiStationStatusSummary | null>(null)
const loading = ref(true)
const notFound = ref(false)

const trendChecks = computed(() => checks.value.slice().reverse())

function models(s?: string): string[] {
  return (s || '').split(/[,，]/).map(model => model.trim()).filter(Boolean)
}

function fmtTime(d?: string): string {
  return d ? new Date(d).toLocaleString('zh-CN') : ''
}

function fmtPercent(value?: number): string {
  return typeof value === 'number' ? `${Math.round(value * 100)}%` : '--'
}

function fmtLatency(value?: number): string {
  return typeof value === 'number' ? `${value} ms` : '无数据'
}

function statusTitle(check: ApiStationStatusCheck): string {
  const latency = typeof check.latencyMs === 'number' ? ` / ${check.latencyMs} ms` : ''
  const error = check.errorMessage ? ` / ${check.errorMessage}` : ''
  return `${fmtTime(check.checkedAt)} / ${check.status}${latency}${error}`
}

async function load() {
  loading.value = true
  notFound.value = false
  station.value = null
  checks.value = []
  summary.value = null
  try {
    const id = Number(route.params.id)
    const [stationResult, checksResult, summaryResult] = await Promise.allSettled([
      publicApi.apiStation(id),
      publicApi.apiStationChecks(id, { limit: 12 }),
      publicApi.apiStationCheckSummary(id, { limit: 30 })
    ])
    if (stationResult.status === 'rejected') {
      notFound.value = true
      return
    }
    station.value = stationResult.value
    checks.value = checksResult.status === 'fulfilled' ? checksResult.value : []
    summary.value = summaryResult.status === 'fulfilled' ? summaryResult.value : null
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div class="container detail-page">
    <RouterLink to="/api-stations" class="back mono">← 返回 API 站点列表</RouterLink>

    <div v-if="loading" class="loading">
      <Skeleton block height="34px" width="58%" />
      <Skeleton block height="18px" width="160px" radius="6px" />
      <Skeleton block height="40px" />
      <Skeleton block height="14px" width="80%" />
    </div>

    <div v-else-if="notFound" class="notfound">
      <span class="nf-mark mono" aria-hidden="true">404</span>
      <p class="muted">API 站点不存在。</p>
      <RouterLink to="/api-stations" class="btn">返回列表</RouterLink>
    </div>

    <article v-else-if="station" class="detail-card card">
      <header class="detail-head">
        <div>
          <StatusBadge :status="station.status" :latency-ms="station.latencyMs" />
          <h1 class="mono">{{ station.name }}</h1>
          <p v-if="station.lastCheckedAt" class="muted mono checked">检测于 {{ fmtTime(station.lastCheckedAt) }}</p>
        </div>
        <div class="head-actions">
          <ResourceFavoriteButton ref-type="API" :ref-id="station.id" />
        </div>
      </header>

      <p v-if="station.description" class="desc">{{ station.description }}</p>

      <div class="field">
        <span class="label mono">Base URL</span>
        <div class="codeline">
          <code>{{ station.baseUrl }}</code>
          <CopyButton :text="station.baseUrl" success-msg="API 地址已复制" />
        </div>
      </div>

      <div v-if="models(station.supportedModels).length" class="field">
        <span class="label mono">支持模型</span>
        <div class="models">
          <span v-for="model in models(station.supportedModels)" :key="model" class="model-pill mono">{{ model }}</span>
        </div>
      </div>

      <TagList :tags="station.tags" />

      <hr class="sep" />
      <section class="history">
        <div class="section-head">
          <div>
            <h2 class="mono">可用性监控</h2>
            <p v-if="summary?.sampleSize" class="muted mono window">
              统计 {{ summary.sampleSize }} 次样本 · {{ fmtTime(summary.firstCheckedAt) }} - {{ fmtTime(summary.lastCheckedAt) }}
            </p>
          </div>
          <span v-if="checks.length" class="muted mono">最近 {{ checks.length }} 条</span>
        </div>
        <div v-if="summary?.sampleSize" class="availability">
          <div class="metric-cell">
            <span class="metric-label mono">可用率</span>
            <strong class="metric-value mono">{{ fmtPercent(summary.uptimeRate) }}</strong>
            <span class="metric-note muted">UP {{ summary.upCount }} / DOWN {{ summary.downCount }} / UNKNOWN {{ summary.unknownCount }}</span>
          </div>
          <div class="metric-cell">
            <span class="metric-label mono">平均延迟</span>
            <strong class="metric-value mono">{{ fmtLatency(summary.averageLatencyMs) }}</strong>
            <span class="metric-note muted">最快 {{ fmtLatency(summary.fastestLatencyMs) }} · 最慢 {{ fmtLatency(summary.slowestLatencyMs) }}</span>
          </div>
          <div class="metric-cell">
            <span class="metric-label mono">最长故障</span>
            <strong class="metric-value mono">{{ summary.longestFailureStreak }} 次</strong>
            <span class="metric-note muted">当前状态 {{ summary.currentStatus }}</span>
          </div>
          <div class="trend-panel" aria-label="最近检测趋势">
            <span
              v-for="check in trendChecks"
              :key="check.id"
              class="trend-bar"
              :class="`trend-${check.status.toLowerCase()}`"
              :title="statusTitle(check)"
            />
          </div>
        </div>
        <p v-else class="muted empty-history">暂无足够样本生成可用性分析。</p>
        <div v-if="checks.length" class="check-list">
          <div v-for="check in checks" :key="check.id" class="check-row">
            <StatusBadge :status="check.status" :latency-ms="check.latencyMs" />
            <div class="check-meta">
              <span class="mono">{{ fmtTime(check.checkedAt) }}</span>
              <span v-if="check.errorMessage" class="muted">{{ check.errorMessage }}</span>
            </div>
          </div>
        </div>
        <p v-else class="muted empty-history">暂无检测历史。</p>
      </section>

      <hr class="sep" />
      <RelatedResources ref-type="API" :ref-id="station.id" />

      <hr class="sep" />
      <LinkedDiscussions ref-type="API" :ref-id="station.id" :source-title="station.name" />

      <hr class="sep" />
      <CommentSection ref-type="API" :ref-id="station.id" />
    </article>

    <ResourceReviewPanel v-if="station?.id" ref-type="API" :ref-id="station.id" />
  </div>
</template>

<style scoped>
.detail-page { padding: 24px 0 70px; max-width: 820px; }
.back { display: inline-block; margin-bottom: 20px; font-size: 13px; }
.loading { display: flex; flex-direction: column; gap: 14px; }
.notfound { display: grid; place-items: center; gap: 14px; padding: 60px 0; text-align: center; }
.nf-mark { font-size: 56px; font-weight: 800; color: var(--primary-dim); }
.detail-card { padding: 26px; }
.detail-head { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; margin-bottom: 16px; }
.detail-head h1 { margin: 12px 0 4px; font-size: 30px; line-height: 1.25; }
.head-actions { display: flex; justify-content: flex-end; align-items: center; flex-wrap: wrap; gap: 10px; }
.checked { margin: 0; font-size: 12px; }
.desc { margin: 0 0 18px; line-height: 1.85; color: var(--text-soft); }
.field { margin-bottom: 18px; }
.label { display: block; font-size: 12px; color: var(--text-soft); margin-bottom: 6px; }
.models { display: flex; flex-wrap: wrap; gap: 6px; }
.model-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  color: var(--text-soft);
}
.history { display: flex; flex-direction: column; gap: 12px; }
.section-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.section-head h2 { margin: 0; font-size: 18px; line-height: 1.4; }
.window { margin: 3px 0 0; font-size: 11px; overflow-wrap: anywhere; }
.availability {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--border);
}
.metric-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 13px;
  background: linear-gradient(180deg, var(--bg-soft), var(--bg-inset));
}
.metric-label { font-size: 11px; color: var(--text-soft); }
.metric-value { font-size: 23px; line-height: 1.1; color: var(--primary); }
.metric-note { font-size: 11px; line-height: 1.4; overflow-wrap: anywhere; }
.trend-panel {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10px, 1fr));
  gap: 4px;
  min-height: 34px;
  padding: 10px;
  background: var(--bg-inset);
}
.trend-bar {
  min-width: 10px;
  height: 14px;
  align-self: center;
  border-radius: 3px;
  border: 1px solid var(--border-strong);
  background: var(--text-dim);
}
.trend-up {
  background: color-mix(in srgb, var(--accent) 72%, var(--bg));
  border-color: color-mix(in srgb, var(--accent) 55%, var(--border));
  box-shadow: 0 0 14px -8px var(--accent);
}
.trend-down {
  height: 24px;
  background: color-mix(in srgb, var(--danger) 78%, var(--bg));
  border-color: color-mix(in srgb, var(--danger) 55%, var(--border));
}
.trend-unknown {
  height: 18px;
  background: var(--bg-soft);
}
.check-list { display: flex; flex-direction: column; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }
.check-row {
  display: grid;
  grid-template-columns: minmax(110px, max-content) 1fr;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}
.check-row:last-child { border-bottom: none; }
.check-meta { min-width: 0; display: flex; flex-direction: column; gap: 3px; font-size: 12px; }
.check-meta span { overflow-wrap: anywhere; }
.empty-history { margin: 0; font-size: 13px; }
.sep { border: none; border-top: 1px dashed var(--border-strong); margin: 28px 0; }
@media (max-width: 640px) {
  .detail-head { flex-direction: column; }
  .head-actions { justify-content: flex-start; }
  .detail-card { padding: 20px; }
  .section-head { flex-direction: column; }
  .availability { grid-template-columns: 1fr; }
  .check-row { grid-template-columns: 1fr; align-items: flex-start; }
}
</style>

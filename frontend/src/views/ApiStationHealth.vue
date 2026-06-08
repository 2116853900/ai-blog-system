<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { ApiStationHealthDashboard, ApiStationHealthLevel, ApiStationHealthTrendResponse } from '../api/types'
import Skeleton from '../components/Skeleton.vue'
import StateBlock from '../components/StateBlock.vue'
import StatusBadge from '../components/StatusBadge.vue'

type LevelFilter = 'ALL' | ApiStationHealthLevel

type IncidentDisplayItem = {
  stationId: number
  stationName: string
  startedAt: string
  durationMinutes: number
  failureCount: number
  latestErrorMessage?: string
  resolved: boolean
}

const dashboard = ref<ApiStationHealthDashboard | null>(null)
const trends = ref<ApiStationHealthTrendResponse | null>(null)
const loading = ref(true)
const error = ref('')
const activeLevel = ref<LevelFilter>('ALL')

const levelOptions: Array<{ value: LevelFilter; label: string }> = [
  { value: 'ALL', label: '全部' },
  { value: 'down', label: '故障' },
  { value: 'degraded', label: '波动' },
  { value: 'unknown', label: '未检测' },
  { value: 'healthy', label: '健康' }
]

const levelLabels: Record<ApiStationHealthLevel, string> = {
  healthy: '健康',
  degraded: '波动',
  down: '故障',
  unknown: '未检测'
}

const filteredStations = computed(() => {
  if (!dashboard.value) return []
  if (activeLevel.value === 'ALL') return dashboard.value.stations
  return dashboard.value.stations.filter(station => station.healthLevel === activeLevel.value)
})
const hasData = computed(() => !!dashboard.value && dashboard.value.stationCount > 0)
const uptimePercent = computed(() => formatPercent(dashboard.value?.uptimeRate ?? 0))
const incidentItems = computed<IncidentDisplayItem[]>(() => {
  if (trends.value) {
    return trends.value.incidents
  }
  return (dashboard.value?.recentFailures ?? []).map(failure => ({
    stationId: failure.stationId,
    stationName: failure.stationName,
    startedAt: failure.checkedAt,
    durationMinutes: 0,
    failureCount: 1,
    latestErrorMessage: failure.errorMessage,
    resolved: false
  }))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [dashboardResponse, trendResponse] = await Promise.all([
      publicApi.apiStationHealthDashboard({ sampleLimit: 30, failureLimit: 8 }),
      publicApi.apiStationHealthTrends({ days: 7, incidentLimit: 8 })
    ])
    dashboard.value = dashboardResponse
    trends.value = trendResponse
  } catch {
    error.value = '状态大盘加载失败'
  } finally {
    loading.value = false
  }
}

function formatPercent(value: number): string {
  return `${Math.round(value * 1000) / 10}%`
}

function fmtTime(value?: string): string {
  return value ? new Date(value).toLocaleString('zh-CN') : '未检测'
}

function latency(value?: number): string {
  return value == null ? 'N/A' : `${value}ms`
}

function durationText(minutes: number): string {
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}

function barHeight(rate: number): string {
  if (rate <= 0) return '4%'
  return `${Math.max(Math.round(rate * 100), 4)}%`
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <div>
        <p class="eyebrow mono">API RELAY HEALTH</p>
        <h1 class="section-title prompt">状态大盘</h1>
        <p class="muted">集中查看公益 API 中转站的当前在线情况、最近检测质量和故障记录。</p>
      </div>
      <RouterLink to="/api-stations" class="btn btn-sm">返回站点列表</RouterLink>
    </header>

    <StateBlock :loading="loading" :empty="!hasData && !error" empty-text="暂无 API 站点状态数据。">
      <template #skeleton>
        <div class="metric-grid">
          <div v-for="i in 4" :key="i" class="card metric-card">
            <Skeleton block height="13px" width="42%" />
            <Skeleton block height="30px" width="58%" />
          </div>
        </div>
        <div class="card board-skeleton">
          <Skeleton v-for="i in 6" :key="i" block height="18px" radius="6px" />
        </div>
      </template>

      <div v-if="error" class="error-panel card">
        <div>
          <strong>{{ error }}</strong>
          <p class="muted">请检查后端服务是否运行，再重试。</p>
        </div>
        <button class="btn btn-primary" @click="load">重试</button>
      </div>

      <template v-else-if="dashboard">
        <section class="metric-grid" aria-label="API 状态指标">
          <div class="card metric-card primary">
            <span class="metric-label mono">整体可用率</span>
            <strong>{{ uptimePercent }}</strong>
            <small class="muted">基于当前站点状态</small>
          </div>
          <div class="card metric-card">
            <span class="metric-label mono">在线 / 总数</span>
            <strong>{{ dashboard.upCount }} / {{ dashboard.stationCount }}</strong>
            <small class="muted">离线 {{ dashboard.downCount }} · 未检测 {{ dashboard.unknownCount }}</small>
          </div>
          <div class="card metric-card">
            <span class="metric-label mono">平均延迟</span>
            <strong>{{ latency(dashboard.averageLatencyMs) }}</strong>
            <small class="muted">仅统计有延迟数据的站点</small>
          </div>
          <div class="card metric-card">
            <span class="metric-label mono">最近更新</span>
            <strong>{{ fmtTime(dashboard.generatedAt) }}</strong>
            <small class="muted">实时读取检测历史</small>
          </div>
        </section>

        <section v-if="trends" class="trend-panel card">
          <div class="panel-head">
            <div>
              <h2 class="mono">7 日趋势</h2>
              <p class="muted">按检测日期聚合可用率、样本量和平均延迟。</p>
            </div>
            <span class="range mono">{{ fmtTime(trends.startAt) }} / {{ fmtTime(trends.endAt) }}</span>
          </div>
          <div class="trend-bars" aria-label="最近 7 日可用率趋势">
            <div v-for="bucket in trends.buckets" :key="bucket.date" class="trend-day">
              <div class="bar-shell">
                <span class="bar-fill" :style="{ height: barHeight(bucket.uptimeRate) }"></span>
              </div>
              <strong class="mono">{{ formatPercent(bucket.uptimeRate) }}</strong>
              <small>{{ bucket.date.slice(5) }} · {{ bucket.sampleSize }} 次</small>
              <small class="dim">{{ latency(bucket.averageLatencyMs) }}</small>
            </div>
          </div>
        </section>

        <section class="health-layout">
          <div class="card health-board">
            <div class="board-head">
              <div>
                <h2 class="mono">站点健康排行</h2>
                <p class="muted">优先显示故障和波动站点，便于快速处理。</p>
              </div>
              <div class="level-filter" role="group" aria-label="健康等级筛选">
                <button
                  v-for="option in levelOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: activeLevel === option.value }"
                  @click="activeLevel = option.value"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>

            <div class="station-list">
              <RouterLink
                v-for="station in filteredStations"
                :key="station.id"
                class="station-row"
                :class="`level-${station.healthLevel}`"
                :to="`/api-stations/${station.id}`"
              >
                <div class="station-main">
                  <span class="level-dot" aria-hidden="true"></span>
                  <div>
                    <strong>{{ station.name }}</strong>
                    <code>{{ station.baseUrl }}</code>
                  </div>
                </div>
                <StatusBadge :status="station.status" :latency-ms="station.latencyMs" />
                <div class="station-metrics mono">
                  <span>{{ formatPercent(station.uptimeRate) }}</span>
                  <small>{{ station.sampleSize }} 次样本</small>
                </div>
                <div class="station-metrics mono">
                  <span>{{ latency(station.averageLatencyMs) }}</span>
                  <small>平均延迟</small>
                </div>
                <div class="station-level mono">{{ levelLabels[station.healthLevel] }}</div>
              </RouterLink>
            </div>
          </div>

          <aside class="card failure-panel">
            <div class="panel-head">
              <h2 class="mono">故障事件</h2>
              <span class="count mono">{{ incidentItems.length }}</span>
            </div>
            <div v-if="!incidentItems.length" class="quiet-state">
              <span aria-hidden="true">OK</span>
              <p class="muted">最近趋势窗口内没有故障事件。</p>
            </div>
            <RouterLink
              v-for="incident in incidentItems"
              v-else
              :key="`${incident.stationId}-${incident.startedAt}`"
              class="failure-item"
              :class="{ resolved: incident.resolved }"
              :to="`/api-stations/${incident.stationId}`"
            >
              <div>
                <div class="failure-title">
                  <strong>{{ incident.stationName }}</strong>
                  <span class="incident-state mono">{{ incident.resolved ? '已恢复' : '进行中' }}</span>
                </div>
                <p>{{ incident.latestErrorMessage || '状态异常，未返回错误信息' }}</p>
              </div>
              <time class="mono">
                {{ fmtTime(incident.startedAt) }} · {{ durationText(incident.durationMinutes) }} · {{ incident.failureCount }} 次
              </time>
            </RouterLink>
          </aside>
        </section>
      </template>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 64px; }
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}
.eyebrow {
  margin: 0;
  color: var(--primary-dim);
  font-size: 12px;
  letter-spacing: 0;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 18px 0;
}
.metric-card {
  padding: 18px;
  min-height: 126px;
  display: grid;
  align-content: space-between;
  overflow: hidden;
}
.metric-card.primary {
  border-color: var(--primary-dim);
  box-shadow: var(--glow);
}
.metric-label {
  color: var(--text-soft);
  font-size: 12px;
}
.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  font-family: var(--font-mono);
  font-size: 25px;
  line-height: 1.2;
}
.metric-card small { font-size: 12px; }
.health-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 16px;
  align-items: start;
}
.health-board,
.failure-panel,
.trend-panel,
.board-skeleton,
.error-panel { padding: 18px; }
.board-skeleton {
  display: grid;
  gap: 12px;
}
.error-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.error-panel strong { color: var(--danger); }
.error-panel p { margin: 4px 0 0; }
.board-head,
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}
.board-head h2,
.panel-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
}
.board-head p { margin: 0; font-size: 13px; }
.trend-panel {
  margin: 0 0 16px;
}
.trend-panel .panel-head { margin-bottom: 14px; }
.range {
  max-width: 260px;
  color: var(--text-dim);
  font-size: 11px;
  text-align: right;
}
.trend-bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}
.trend-day {
  min-width: 0;
  display: grid;
  gap: 6px;
  justify-items: center;
  padding: 10px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.bar-shell {
  position: relative;
  width: 100%;
  height: 82px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(to top, transparent 24%, var(--border) 25%, transparent 26%),
    var(--bg-soft);
}
.bar-fill {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: block;
  min-height: 4%;
  background: linear-gradient(to top, var(--primary), var(--accent));
  box-shadow: 0 0 18px -8px var(--primary);
}
.trend-day strong {
  color: var(--text);
  font-size: 13px;
  line-height: 1;
}
.trend-day small {
  max-width: 100%;
  overflow: hidden;
  color: var(--text-soft);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.level-filter {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: var(--bg-inset);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow-x: auto;
}
.level-filter button {
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 7px 10px;
  white-space: nowrap;
}
.level-filter button:hover,
.level-filter button.active {
  background: var(--primary-soft);
  color: var(--primary);
}
.station-list {
  display: grid;
  gap: 8px;
}
.station-row {
  display: grid;
  grid-template-columns: minmax(210px, 1fr) auto 92px 92px 72px;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text);
}
.station-row:hover {
  border-color: var(--primary-dim);
  background: var(--bg-soft);
  text-decoration: none;
}
.station-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
.station-main strong {
  display: block;
  margin-bottom: 2px;
}
.station-main code {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: var(--text-dim);
  text-overflow: ellipsis;
  white-space: nowrap;
  background: none;
}
.level-dot {
  width: 9px;
  height: 30px;
  border-radius: 999px;
  background: var(--text-dim);
  flex-shrink: 0;
}
.level-healthy .level-dot { background: var(--accent); }
.level-degraded .level-dot { background: var(--warning); }
.level-down .level-dot { background: var(--danger); }
.level-unknown .level-dot { background: var(--text-dim); }
.station-metrics span {
  display: block;
  color: var(--text);
  font-size: 13px;
}
.station-metrics small {
  display: block;
  color: var(--text-dim);
  font-size: 11px;
}
.station-level {
  justify-self: end;
  color: var(--text-soft);
  font-size: 12px;
}
.count {
  min-width: 28px;
  height: 28px;
  display: inline-grid;
  place-items: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  font-size: 12px;
}
.failure-panel {
  display: grid;
  gap: 10px;
}
.failure-item {
  display: block;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text);
}
.failure-item:hover {
  border-color: var(--danger);
  text-decoration: none;
}
.failure-item.resolved:hover { border-color: var(--accent); }
.failure-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.incident-state {
  flex-shrink: 0;
  padding: 2px 7px;
  border: 1px solid color-mix(in srgb, var(--danger) 35%, var(--border));
  border-radius: var(--radius-sm);
  color: var(--danger);
  font-size: 10px;
}
.failure-item.resolved .incident-state {
  border-color: color-mix(in srgb, var(--accent) 35%, var(--border));
  color: var(--accent);
}
.failure-item p {
  margin: 4px 0 8px;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.5;
}
.failure-item time {
  color: var(--text-dim);
  font-size: 11px;
}
.quiet-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 180px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.quiet-state span {
  font-family: var(--font-mono);
  color: var(--accent);
  font-size: 24px;
}
.quiet-state p { margin: 0; }

@media (max-width: 980px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .health-layout { grid-template-columns: 1fr; }
  .trend-bars { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}

@media (max-width: 720px) {
  .page-head { display: grid; }
  .metric-grid { grid-template-columns: 1fr; }
  .trend-bars { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .range { max-width: none; text-align: left; }
  .board-head { display: grid; }
  .station-row {
    grid-template-columns: 1fr;
    align-items: start;
  }
  .station-level { justify-self: start; }
  .error-panel { display: grid; }
}
</style>

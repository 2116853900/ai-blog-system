<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { ApiStation, ApiStatus, ResourceTagSummary } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import StatusBadge from '../components/StatusBadge.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import DetailDrawer from '../components/DetailDrawer.vue'
import CommentSection from '../components/CommentSection.vue'
import CopyButton from '../components/CopyButton.vue'
import ResourceFavoriteButton from '../components/ResourceFavoriteButton.vue'
import ResourceReviewPanel from '../components/ResourceReviewPanel.vue'
import { useListView } from '../composables/useListView'

type StatusFilter = 'ALL' | ApiStatus

const statusOptions: Array<{ value: StatusFilter; label: string; tone: string }> = [
  { value: 'ALL', label: '全部状态', tone: 'all' },
  { value: 'UP', label: '在线', tone: 'up' },
  { value: 'DOWN', label: '离线', tone: 'down' },
  { value: 'UNKNOWN', label: '未检测', tone: 'unknown' }
]
const statusFilter = ref<StatusFilter>('ALL')

function statusParam() {
  return statusFilter.value === 'ALL' ? undefined : statusFilter.value
}

const {
  items, loading, q, activeTag, isEmpty, hasFilter, load, toggleTag, reset
} = useListView<ApiStation>((params) => publicApi.apiStations({
  ...params,
  status: statusParam()
}))

const selected = ref<ApiStation | null>(null)
const popularTags = ref<ResourceTagSummary[]>([])
const hasAnyFilter = computed(() => hasFilter.value || statusFilter.value !== 'ALL')

function open(a: ApiStation) { selected.value = a }
function close() { selected.value = null }

function selectStatus(value: StatusFilter) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  load()
}

function resetFilters() {
  statusFilter.value = 'ALL'
  reset()
}

async function loadPopularTags() {
  try {
    popularTags.value = await publicApi.apiStationPopularTags({ limit: 12 })
  } catch {
    popularTags.value = []
  }
}

function models(s?: string): string[] {
  if (!s) return []
  return s.split(/[,，]/).map(m => m.trim()).filter(Boolean)
}

function fmtTime(d?: string): string {
  return d ? new Date(d).toLocaleString('zh-CN') : ''
}

onMounted(loadPopularTags)
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <div>
        <h1 class="section-title prompt">公益 API 中转站</h1>
        <p class="muted">社区维护的 API 中转，实时状态监测。请遵守各站点规则，理性使用。</p>
      </div>
      <RouterLink to="/api-stations/health" class="btn btn-sm">状态大盘</RouterLink>
    </header>

    <div class="toolbar">
      <SearchBar class="grow" v-model="q" placeholder="搜索站点名称、模型、标签…" @search="load" />
      <div class="status-filter" aria-label="API 状态筛选">
        <button
          v-for="option in statusOptions"
          :key="option.value"
          type="button"
          :class="['status-option', `tone-${option.tone}`, { active: statusFilter === option.value }]"
          @click="selectStatus(option.value)"
        >
          <span aria-hidden="true"></span>
          {{ option.label }}
        </button>
      </div>
      <button v-if="hasAnyFilter" class="btn btn-sm" @click="resetFilters">清除筛选 ✕</button>
    </div>

    <div v-if="popularTags.length" class="popular-tags" role="group" aria-label="热门标签">
      <span class="popular-label mono">热门标签</span>
      <button
        v-for="tag in popularTags"
        :key="tag.tag"
        class="tag-chip"
        :class="{ active: activeTag === tag.tag }"
        :aria-pressed="activeTag === tag.tag"
        @click="toggleTag(tag.tag)"
      >
        {{ tag.tag }}<span>{{ tag.count }}</span>
      </button>
    </div>

    <StateBlock :loading="loading" :empty="isEmpty" :empty-text="hasAnyFilter ? '没有匹配的 API 站点。' : '暂无 API 站点。'" class="block-area">
      <template #skeleton>
        <div class="grid">
          <div v-for="i in 4" :key="i" class="card item">
            <Skeleton block height="20px" width="50%" />
            <Skeleton block height="34px" radius="6px" />
            <Skeleton block height="14px" width="70%" radius="6px" />
          </div>
        </div>
      </template>

      <div class="grid">
        <button
          v-for="(a, i) in items" :key="a.id"
          class="card item is-interactive rise"
          :style="{ animationDelay: `${Math.min(i * 0.04, 0.4)}s` }"
          @click="open(a)"
        >
          <div class="item-head">
            <h3 class="mono">{{ a.name }}</h3>
            <div class="item-ratings">
              <CommunityRatingBadge :average-rating="a.averageRating" :review-count="a.reviewCount" />
              <StatusBadge :status="a.status" :latency-ms="a.latencyMs" />
            </div>
          </div>
          <p v-if="a.description" class="muted desc">{{ a.description }}</p>
          <div class="codeline" @click.stop>
            <code>{{ a.baseUrl }}</code>
            <CopyButton :text="a.baseUrl" label="复制" success-msg="API 地址已复制" />
          </div>
          <div v-if="models(a.supportedModels).length" class="models">
            <span v-for="m in models(a.supportedModels).slice(0, 4)" :key="m" class="model-pill mono">{{ m }}</span>
            <span v-if="models(a.supportedModels).length > 4" class="model-pill mono dim">+{{ models(a.supportedModels).length - 4 }}</span>
          </div>
          <div class="item-foot">
            <TagList :tags="a.tags" :active="activeTag" @select="toggleTag" />
            <span class="open-hint mono">详情 →</span>
          </div>
        </button>
      </div>
    </StateBlock>

    <DetailDrawer :open="!!selected" :title="selected?.name" @close="close">
      <template v-if="selected">
        <div class="d-meta">
          <StatusBadge :status="selected.status" :latency-ms="selected.latencyMs" />
          <span v-if="selected.lastCheckedAt" class="dim mono check-time">检测于 {{ fmtTime(selected.lastCheckedAt) }}</span>
        </div>
        <p v-if="selected.description" class="d-desc">{{ selected.description }}</p>

        <div class="d-field">
          <span class="d-label mono">Base URL</span>
          <div class="codeline">
            <code>{{ selected.baseUrl }}</code>
            <CopyButton :text="selected.baseUrl" success-msg="API 地址已复制" />
          </div>
        </div>

        <div v-if="models(selected.supportedModels).length" class="d-field">
          <span class="d-label mono">支持模型</span>
          <div class="models">
            <span v-for="m in models(selected.supportedModels)" :key="m" class="model-pill mono">{{ m }}</span>
          </div>
        </div>

        <TagList :tags="selected.tags" @select="(t) => { toggleTag(t); close() }" />
        <div class="resource-actions">
          <ResourceFavoriteButton ref-type="API" :ref-id="selected.id" />
          <ResourceReviewPanel ref-type="API" :ref-id="selected.id" />
        </div>
        <hr class="d-sep" />
        <CommentSection ref-type="API" :ref-id="selected.id" />
      </template>
    </DetailDrawer>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 16px; }
.block-area { margin-top: 20px; }
.popular-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
.popular-label { color: var(--text-dim); font-size: 12px; }
.tag-chip {
  border: 1px solid var(--border); background: var(--bg-soft); color: var(--text-soft);
  border-radius: 999px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;
  font: inherit; font-size: 12px; padding: 5px 10px;
}
.tag-chip span { color: var(--text-dim); font-family: var(--font-mono); font-size: 11px; }
.tag-chip:hover, .tag-chip.active { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.status-filter {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  overflow-x: auto;
}
.status-option {
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 7px 10px;
  white-space: nowrap;
}
.status-option span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}
.status-option:hover,
.status-option.active {
  background: var(--primary-soft);
  color: var(--primary);
}
.status-option.tone-up.active { color: var(--accent); }
.status-option.tone-down.active { color: var(--danger); }
.status-option.tone-unknown.active { color: var(--text-soft); }
.item {
  padding: 20px; display: flex; flex-direction: column; gap: 10px;
  text-align: left; width: 100%; cursor: pointer; font: inherit; color: inherit;
}
.item-ratings { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; flex-shrink: 0; }
.item-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.item-head h3 { margin: 0; font-size: 16px; font-weight: 700; }
.desc { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.models { display: flex; flex-wrap: wrap; gap: 6px; }
.model-pill { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: var(--bg-soft); border: 1px solid var(--border); color: var(--text-soft); }
.item-foot { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; }
.open-hint { font-size: 12px; color: var(--text-dim); flex-shrink: 0; }
.item:hover .open-hint { color: var(--primary); }
.d-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.check-time { font-size: 11px; }
.d-desc { line-height: 1.8; margin: 0 0 16px; }
.d-field { margin-bottom: 16px; }
.d-label { display: block; font-size: 12px; color: var(--text-soft); margin-bottom: 6px; }
.resource-actions { display: grid; gap: 18px; margin-top: 20px; }
.d-sep { border: none; border-top: 1px dashed var(--border-strong); margin: 24px 0; }
@media (max-width: 720px) {
  .page-head { display: grid; }
}
</style>

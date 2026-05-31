<script setup lang="ts">
import { ref } from 'vue'
import { publicApi } from '../api'
import type { ApiStation } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import StatusBadge from '../components/StatusBadge.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import DetailDrawer from '../components/DetailDrawer.vue'
import CommentSection from '../components/CommentSection.vue'
import CopyButton from '../components/CopyButton.vue'
import { useListView } from '../composables/useListView'

const {
  items, loading, q, activeTag, isEmpty, hasFilter, load, toggleTag, reset
} = useListView<ApiStation>(publicApi.apiStations)

const selected = ref<ApiStation | null>(null)
function open(a: ApiStation) { selected.value = a }
function close() { selected.value = null }

function models(s?: string): string[] {
  if (!s) return []
  return s.split(/[,，]/).map(m => m.trim()).filter(Boolean)
}

function fmtTime(d?: string): string {
  return d ? new Date(d).toLocaleString('zh-CN') : ''
}
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">公益 API 中转站</h1>
      <p class="muted">社区维护的 API 中转，实时状态监测。请遵守各站点规则，理性使用。</p>
    </header>

    <div class="toolbar">
      <SearchBar class="grow" v-model="q" placeholder="搜索站点名称、模型、标签…" @search="load" />
      <button v-if="hasFilter" class="btn btn-sm" @click="reset">清除筛选 ✕</button>
    </div>

    <StateBlock :loading="loading" :empty="isEmpty" empty-text="暂无 API 站点。" class="block-area">
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
            <StatusBadge :status="a.status" :latency-ms="a.latencyMs" />
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
        <hr class="d-sep" />
        <CommentSection ref-type="API" :ref-id="selected.id" />
      </template>
    </DetailDrawer>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.page-head { margin-bottom: 16px; }
.block-area { margin-top: 20px; }
.item {
  padding: 20px; display: flex; flex-direction: column; gap: 10px;
  text-align: left; width: 100%; cursor: pointer; font: inherit; color: inherit;
}
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
.d-sep { border: none; border-top: 1px dashed var(--border-strong); margin: 24px 0; }
</style>

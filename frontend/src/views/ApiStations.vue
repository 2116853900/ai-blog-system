<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { ApiStation } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import StatusBadge from '../components/StatusBadge.vue'

const items = ref<ApiStation[]>([])
const loading = ref(false)
const q = ref('')
const activeTag = ref('')

async function load() {
  loading.value = true
  try {
    items.value = await publicApi.apiStations({
      q: q.value || undefined,
      tag: activeTag.value || undefined
    })
  } finally {
    loading.value = false
  }
}

function toggleTag(t: string) {
  activeTag.value = activeTag.value === t ? '' : t
  load()
}

function fmt(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN') : '尚未检测'
}

function models(s?: string): string[] {
  if (!s) return []
  return s.split(/[,，]/).map(x => x.trim()).filter(Boolean)
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <h1 class="section-title">🌐 公益 API 中转站</h1>
    <p class="muted">
      社区分享的公益/免费 API 中转站，状态每 10 分钟自动检测。请遵守各站点使用规则，理性使用。
    </p>

    <div class="toolbar">
      <SearchBar v-model="q" placeholder="搜索站点名称、模型、标签…" @search="load" />
      <button class="btn btn-primary" @click="load">搜索</button>
      <button v-if="activeTag" class="btn btn-sm" @click="toggleTag(activeTag)">清除筛选: {{ activeTag }}</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有找到站点。</p>

    <div class="grid">
      <div v-for="s in items" :key="s.id" class="card station">
        <div class="station-head">
          <h3>{{ s.name }}</h3>
          <StatusBadge :status="s.status" :latency-ms="s.latencyMs" />
        </div>
        <p class="muted desc">{{ s.description }}</p>

        <div class="url-row">
          <code>{{ s.baseUrl }}</code>
        </div>

        <div v-if="models(s.supportedModels).length" class="models">
          <span class="muted models-label">支持模型：</span>
          <span v-for="m in models(s.supportedModels)" :key="m" class="model-chip">{{ m }}</span>
        </div>

        <div class="station-foot">
          <TagList :tags="s.tags" :active="activeTag" @select="toggleTag" />
          <span class="muted checked">检测于 {{ fmt(s.lastCheckedAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin: 18px 0 24px; flex-wrap: wrap; }
.toolbar > :first-child { flex: 1; min-width: 220px; }
.station { padding: 20px; display: flex; flex-direction: column; gap: 8px; }
.station-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.station-head h3 { margin: 0; }
.desc { margin: 0; }
.url-row code {
  display: block; background: var(--bg-soft); border-radius: 8px; padding: 8px 10px;
  font-size: 13px; overflow-x: auto; white-space: nowrap;
}
.models { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.models-label { font-size: 13px; }
.model-chip {
  background: var(--bg-soft); border-radius: 6px; padding: 2px 8px; font-size: 12px;
}
.station-foot { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 4px; flex-wrap: wrap; }
.checked { font-size: 12px; white-space: nowrap; }
</style>

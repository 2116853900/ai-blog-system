<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { publicApi } from '../api'
import type { ApiStation } from '../api/types'
import CommentSection from '../components/CommentSection.vue'
import CopyButton from '../components/CopyButton.vue'
import LinkedDiscussions from '../components/LinkedDiscussions.vue'
import Skeleton from '../components/Skeleton.vue'
import StatusBadge from '../components/StatusBadge.vue'
import TagList from '../components/TagList.vue'

const route = useRoute()
const station = ref<ApiStation | null>(null)
const loading = ref(true)
const notFound = ref(false)

function models(s?: string): string[] {
  return (s || '').split(/[,，]/).map(model => model.trim()).filter(Boolean)
}

function fmtTime(d?: string): string {
  return d ? new Date(d).toLocaleString('zh-CN') : ''
}

async function load() {
  loading.value = true
  notFound.value = false
  station.value = null
  try {
    station.value = await publicApi.apiStation(Number(route.params.id))
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
      <LinkedDiscussions ref-type="API" :ref-id="station.id" :source-title="station.name" />

      <hr class="sep" />
      <CommentSection ref-type="API" :ref-id="station.id" />
    </article>
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
.sep { border: none; border-top: 1px dashed var(--border-strong); margin: 28px 0; }
@media (max-width: 640px) {
  .detail-card { padding: 20px; }
}
</style>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { publicApi } from '../api'
import type { Mcp } from '../api/types'
import CommentSection from '../components/CommentSection.vue'
import CopyButton from '../components/CopyButton.vue'
import LinkedDiscussions from '../components/LinkedDiscussions.vue'
import RelatedResources from '../components/RelatedResources.vue'
import ResourceFavoriteButton from '../components/ResourceFavoriteButton.vue'
import ResourceReviewPanel from '../components/ResourceReviewPanel.vue'
import Skeleton from '../components/Skeleton.vue'
import StarRating from '../components/StarRating.vue'
import TagList from '../components/TagList.vue'

const route = useRoute()
const mcp = ref<Mcp | null>(null)
const loading = ref(true)
const notFound = ref(false)

async function load() {
  loading.value = true
  notFound.value = false
  mcp.value = null
  try {
    mcp.value = await publicApi.mcp(Number(route.params.id))
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
    <RouterLink to="/mcps" class="back mono">← 返回 MCP 列表</RouterLink>

    <div v-if="loading" class="loading">
      <Skeleton block height="34px" width="58%" />
      <Skeleton block height="18px" width="180px" radius="6px" />
      <Skeleton block height="14px" />
      <Skeleton block height="40px" />
    </div>

    <div v-else-if="notFound" class="notfound">
      <span class="nf-mark mono" aria-hidden="true">404</span>
      <p class="muted">MCP 不存在。</p>
      <RouterLink to="/mcps" class="btn">返回列表</RouterLink>
    </div>

    <article v-else-if="mcp" class="detail-card card">
      <header class="detail-head">
        <div>
          <span v-if="mcp.category" class="chip chip-active">{{ mcp.category }}</span>
          <h1 class="mono">{{ mcp.name }}</h1>
        </div>
        <div class="head-actions">
          <StarRating :level="mcp.recommendLevel" />
          <ResourceFavoriteButton ref-type="MCP" :ref-id="mcp.id" />
        </div>
      </header>

      <p class="desc">{{ mcp.description || '暂无描述。' }}</p>

      <div v-if="mcp.installCmd" class="field">
        <span class="label mono">安装命令</span>
        <div class="codeline">
          <code>{{ mcp.installCmd }}</code>
          <CopyButton :text="mcp.installCmd" success-msg="安装命令已复制" />
        </div>
      </div>

      <TagList :tags="mcp.tags" />
      <a v-if="mcp.repoUrl" :href="mcp.repoUrl" target="_blank" rel="noopener" class="btn btn-primary action">
        查看仓库 ↗
      </a>

      <hr class="sep" />
      <RelatedResources ref-type="MCP" :ref-id="mcp.id" />

      <hr class="sep" />
      <LinkedDiscussions ref-type="MCP" :ref-id="mcp.id" :source-title="mcp.name" />

      <hr class="sep" />
      <CommentSection ref-type="MCP" :ref-id="mcp.id" />
    </article>

    <ResourceReviewPanel v-if="mcp?.id" ref-type="MCP" :ref-id="mcp.id" />
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
.detail-head h1 { margin: 10px 0 0; font-size: 30px; line-height: 1.25; }
.head-actions { display: flex; justify-content: flex-end; align-items: center; flex-wrap: wrap; gap: 10px; }
.desc { margin: 0 0 18px; line-height: 1.85; color: var(--text-soft); }
.field { margin-bottom: 18px; }
.label { display: block; font-size: 12px; color: var(--text-soft); margin-bottom: 6px; }
.action { margin-top: 20px; }
.sep { border: none; border-top: 1px dashed var(--border-strong); margin: 28px 0; }
@media (max-width: 640px) {
  .detail-head { flex-direction: column; }
  .head-actions { justify-content: flex-start; }
  .detail-card { padding: 20px; }
}
</style>

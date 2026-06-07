<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { RelatedResource, ResourceFavoriteRefType } from '../api/types'
import Skeleton from './Skeleton.vue'

const props = withDefaults(defineProps<{
  refType: ResourceFavoriteRefType
  refId: number
  limit?: number
}>(), {
  limit: 4
})

const items = ref<RelatedResource[]>([])
const loading = ref(false)
const failed = ref(false)

const labels: Record<ResourceFavoriteRefType, string> = {
  POST: '教程',
  SKILL: 'Skill',
  MCP: 'MCP',
  API: 'API'
}

function splitTags(tags?: string): string[] {
  return (tags || '').split(/[,，]/).map(tag => tag.trim()).filter(Boolean).slice(0, 3)
}

async function load() {
  if (!props.refId) return
  loading.value = true
  failed.value = false
  try {
    items.value = await publicApi.relatedResources(props.refType, props.refId, { limit: props.limit })
  } catch {
    items.value = []
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => [props.refType, props.refId, props.limit], load)
</script>

<template>
  <section v-if="loading || items.length" class="related" aria-labelledby="related-title">
    <div class="related-head">
      <div>
        <h2 id="related-title" class="mono">延伸阅读</h2>
        <p class="muted mono">按标签、分类与关键词匹配</p>
      </div>
      <span v-if="items.length" class="count mono">{{ items.length }} 条</span>
    </div>

    <div v-if="loading" class="related-list">
      <div v-for="idx in 3" :key="idx" class="related-skeleton">
        <Skeleton width="58px" height="20px" radius="999px" />
        <Skeleton block height="16px" width="70%" />
        <Skeleton block height="13px" width="90%" />
      </div>
    </div>

    <div v-else class="related-list">
      <RouterLink
        v-for="item in items"
        :key="`${item.type}-${item.id}`"
        :to="item.url"
        class="related-item"
      >
        <div class="item-top">
          <span class="type-badge mono">{{ labels[item.type] }}</span>
          <span class="reason mono">{{ item.reason }}</span>
        </div>
        <h3 class="mono">{{ item.title }}</h3>
        <p v-if="item.description" class="desc">{{ item.description }}</p>
        <div v-if="item.category || splitTags(item.tags).length" class="meta-line">
          <span v-if="item.category" class="category mono">{{ item.category }}</span>
          <span v-for="tag in splitTags(item.tags)" :key="tag" class="mini-tag mono">#{{ tag }}</span>
        </div>
      </RouterLink>
    </div>
  </section>
  <p v-else-if="failed" class="sr-only">相关资源加载失败</p>
</template>

<style scoped>
.related {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.related-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}
.related-head h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.35;
}
.related-head p {
  margin: 2px 0 0;
  font-size: 11px;
}
.count {
  flex: 0 0 auto;
  padding: 2px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-soft);
  background: var(--bg-inset);
  font-size: 11px;
}
.related-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.related-item,
.related-skeleton {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 146px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--primary) 8%, transparent), transparent 36%),
    var(--bg-soft);
}
.related-item {
  color: var(--text);
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease), background var(--dur) var(--ease);
}
.related-item:hover {
  transform: translateY(-2px);
  border-color: var(--primary-dim);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--primary) 12%, transparent), transparent 42%),
    var(--bg-elevated);
}
.item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.type-badge {
  flex: 0 0 auto;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--primary) 38%, var(--border));
  color: var(--primary);
  background: var(--primary-soft);
  font-size: 11px;
}
.reason {
  min-width: 0;
  color: var(--text-soft);
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.related-item h3 {
  margin: 0;
  font-size: 15px;
  line-height: 1.42;
  overflow-wrap: anywhere;
}
.desc {
  margin: 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: auto;
}
.category,
.mini-tag {
  font-size: 10.5px;
  color: var(--text-soft);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 1px 7px;
  background: var(--bg-inset);
}
@media (max-width: 640px) {
  .related-list { grid-template-columns: 1fr; }
  .related-head { flex-direction: column; }
}
</style>

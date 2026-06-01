<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { publicApi } from '../api'
import type { GlobalSearchResponse } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import Skeleton from '../components/Skeleton.vue'
import StateBlock from '../components/StateBlock.vue'

const route = useRoute()
const router = useRouter()
const q = ref(typeof route.query.q === 'string' ? route.query.q : '')
const result = ref<GlobalSearchResponse | null>(null)
const loading = ref(false)
const searched = ref(false)

const hasQuery = computed(() => q.value.trim().length > 0)
const empty = computed(() => searched.value && !loading.value && (result.value?.totalCount || 0) === 0)

function tagsOf(tags?: string) {
  return (tags || '').split(',').map(tag => tag.trim()).filter(Boolean)
}

function typeClass(type: string) {
  return `type-${type.toLowerCase().replace('_', '-')}`
}

async function syncQuery() {
  const query = q.value.trim() ? { q: q.value.trim() } : {}
  await router.replace({ query })
}

async function search() {
  await syncQuery()
  const keyword = q.value.trim()
  searched.value = true
  if (!keyword) {
    result.value = { query: '', totalCount: 0, groups: [] }
    return
  }

  loading.value = true
  try {
    result.value = await publicApi.search({ q: keyword, limit: 6 })
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.q,
  value => {
    if (typeof value === 'string' && value !== q.value) {
      q.value = value
      search()
    }
  }
)

onMounted(() => {
  if (hasQuery.value) search()
})
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <p class="mono dim">// global grep</p>
      <h1 class="section-title prompt">全站搜索</h1>
      <p class="muted">一次搜索教程、Skill、MCP、公益 API 与论坛讨论。</p>
    </header>

    <div class="search-panel card">
      <SearchBar
        v-model="q"
        placeholder="输入关键词，例如 MCP、RAG、API、提示词..."
        :debounce="350"
        @search="search"
      />
      <button class="btn btn-primary" :disabled="loading || !hasQuery" @click="search">搜索</button>
    </div>

    <StateBlock :loading="loading" :empty="empty" empty-text="没有匹配结果，换个关键词试试。" class="result-state">
      <template #skeleton>
        <div class="result-groups">
          <section v-for="group in 3" :key="group" class="group">
            <Skeleton block height="20px" width="140px" />
            <div class="result-grid">
              <div v-for="item in 3" :key="item" class="card result-card">
                <Skeleton block height="18px" width="70%" />
                <Skeleton block height="14px" />
                <Skeleton block height="14px" width="55%" />
              </div>
            </div>
          </section>
        </div>
      </template>

      <div v-if="!searched && !loading" class="empty-start card">
        <span class="mono">grep -R "keyword" ./ai-info-station</span>
      </div>

      <div v-else-if="result?.groups.length" class="result-groups">
        <div class="summary mono">{{ result.totalCount }} results for "{{ result.query }}"</div>
        <section v-for="group in result.groups" :key="group.type" class="group">
          <div class="group-head">
            <h2>{{ group.label }}</h2>
            <span class="badge badge-unknown">{{ group.items.length }}</span>
          </div>

          <div class="result-grid">
            <RouterLink
              v-for="(item, index) in group.items"
              :key="`${item.type}-${item.id}`"
              :to="item.url"
              class="card result-card rise"
              :style="{ animationDelay: `${Math.min(index * 0.035, 0.25)}s` }"
            >
              <div class="result-top">
                <span class="type-pill mono" :class="typeClass(item.type)">{{ item.type }}</span>
                <span v-if="item.meta" class="muted mono meta">{{ item.meta }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p class="muted desc">{{ item.description || '暂无摘要。' }}</p>
              <div class="result-foot">
                <span v-if="item.category" class="chip">{{ item.category }}</span>
                <span v-for="tag in tagsOf(item.tags).slice(0, 4)" :key="tag" class="tag">{{ tag }}</span>
              </div>
            </RouterLink>
          </div>
        </section>
      </div>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 70px; }
.page-head { margin-bottom: 16px; }
.search-panel {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}
.result-state { margin-top: 24px; }
.empty-start {
  padding: 28px;
  color: var(--text-dim);
  text-align: center;
  overflow: hidden;
}
.result-groups { display: flex; flex-direction: column; gap: 28px; }
.summary { color: var(--text-dim); font-size: 13px; }
.group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.group-head h2 { margin: 0; font-size: 19px; }
.result-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}
.result-card {
  padding: 18px;
  color: var(--text);
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 210px;
}
.result-card:hover { text-decoration: none; }
.result-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.type-pill {
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--primary-dim);
  font-size: 11px;
}
.type-api {
  color: var(--info);
  border-color: color-mix(in srgb, var(--info) 45%, transparent);
  background: color-mix(in srgb, var(--info) 12%, transparent);
}
.type-forum-thread {
  color: var(--warning);
  border-color: color-mix(in srgb, var(--warning) 45%, transparent);
  background: color-mix(in srgb, var(--warning) 12%, transparent);
}
.meta {
  font-size: 11.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.result-card h3 { margin: 0; font-size: 16px; line-height: 1.45; }
.desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.65;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.result-foot { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.result-foot .chip,
.result-foot .tag {
  cursor: default;
  margin: 0;
}
@media (max-width: 640px) {
  .search-panel { grid-template-columns: 1fr; }
  .search-panel .btn { justify-content: center; }
}
</style>

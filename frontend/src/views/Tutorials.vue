<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { Post } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'

const posts = ref<Post[]>([])
const q = ref('')
const loading = ref(true)
const activeCategory = ref('')

const categories = computed(() => {
  const set = new Set<string>()
  for (const p of posts.value) if (p.category) set.add(p.category)
  return [...set].sort()
})

const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase()
  return posts.value.filter(p => {
    if (activeCategory.value && p.category !== activeCategory.value) return false
    if (!kw) return true
    return p.title.toLowerCase().includes(kw) ||
      (p.summary || '').toLowerCase().includes(kw) ||
      (p.tags || '').toLowerCase().includes(kw)
  })
})

const hasFilter = computed(() => !!(q.value || activeCategory.value))

function selectCategory(c: string) {
  activeCategory.value = activeCategory.value === c ? '' : c
}
function reset() {
  q.value = ''
  activeCategory.value = ''
}

onMounted(async () => {
  loading.value = true
  try {
    posts.value = await publicApi.posts()
  } finally {
    loading.value = false
  }
})

function fmt(d: string) {
  return new Date(d).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">相关教程</h1>
      <p class="muted">从入门到进阶的 AI 实战教程。</p>
    </header>

    <div class="toolbar">
      <SearchBar class="grow" v-model="q" placeholder="搜索教程标题、摘要、标签…" />
      <button v-if="hasFilter" class="btn btn-sm" @click="reset">清除筛选 ✕</button>
    </div>

    <div v-if="categories.length" class="chips" role="group" aria-label="分类筛选">
      <button
        class="chip" :class="{ 'chip-active': activeCategory === c }"
        v-for="c in categories" :key="c"
        :aria-pressed="activeCategory === c"
        @click="selectCategory(c)"
      >{{ c }}</button>
    </div>

    <StateBlock :loading="loading" :empty="!filtered.length" empty-text="没有找到教程。" class="block-area">
      <template #skeleton>
        <div class="list">
          <div v-for="i in 5" :key="i" class="card post">
            <div class="post-main">
              <Skeleton block height="18px" width="40%" />
              <Skeleton block height="14px" width="80%" radius="6px" />
            </div>
          </div>
        </div>
      </template>

      <div class="list">
        <RouterLink
          v-for="(p, i) in filtered" :key="p.id"
          :to="`/tutorials/${p.slug}`"
          class="card post rise"
          :style="{ animationDelay: `${Math.min(i * 0.04, 0.4)}s` }"
        >
          <div class="post-main">
            <span v-if="p.category" class="chip cat">{{ p.category }}</span>
            <h3 class="mono">{{ p.title }}</h3>
            <p class="muted">{{ p.summary }}</p>
          </div>
          <span class="muted date mono">{{ fmt(p.createdAt) }}</span>
        </RouterLink>
      </div>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.page-head { margin-bottom: 16px; }
.toolbar { max-width: 560px; }
.chips { margin-top: 14px; }
.block-area { margin-top: 20px; }
.list { display: flex; flex-direction: column; gap: 14px; }
.post {
  padding: 20px; color: var(--text);
  display: flex; justify-content: space-between; align-items: start; gap: 16px;
}
.post:hover { text-decoration: none; }
.cat { margin-bottom: 8px; pointer-events: none; }
.post h3 { margin: 0 0 4px; font-size: 17px; }
.date { white-space: nowrap; font-size: 13px; flex-shrink: 0; }
</style>

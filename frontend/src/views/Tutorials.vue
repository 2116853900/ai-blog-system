<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { Post, ResourceTagSummary } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import { useListView } from '../composables/useListView'

const {
  items, loading, q, activeTag, activeCategory, allCategories,
  isEmpty, hasFilter, load, toggleTag, selectCategory, reset
} = useListView<Post>(publicApi.posts)

const popularTags = ref<ResourceTagSummary[]>([])

async function loadPopularTags() {
  try {
    popularTags.value = await publicApi.postPopularTags({ limit: 12 })
  } catch {
    popularTags.value = []
  }
}

onMounted(loadPopularTags)

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
      <SearchBar class="grow" v-model="q" placeholder="搜索教程标题、摘要、标签…" @search="load" />
      <button v-if="hasFilter" class="btn btn-sm" @click="reset">清除筛选 ✕</button>
    </div>

    <div v-if="allCategories.length" class="chips" role="group" aria-label="分类筛选">
      <button
        class="chip" :class="{ 'chip-active': activeCategory === c }"
        v-for="c in allCategories" :key="c"
        :aria-pressed="activeCategory === c"
        @click="selectCategory(c)"
      >{{ c }}</button>
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

    <StateBlock :loading="loading" :empty="isEmpty" empty-text="没有找到教程。" class="block-area">
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
          v-for="(p, i) in items" :key="p.id"
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
.popular-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
.popular-label { color: var(--text-dim); font-size: 12px; }
.tag-chip {
  border: 1px solid var(--border); background: var(--bg-soft); color: var(--text-soft);
  border-radius: 999px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;
  font: inherit; font-size: 12px; padding: 5px 10px;
}
.tag-chip span { color: var(--text-dim); font-family: var(--font-mono); font-size: 11px; }
.tag-chip:hover, .tag-chip.active { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
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

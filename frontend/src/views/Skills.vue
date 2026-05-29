<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { Skill } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import StarRating from '../components/StarRating.vue'

const items = ref<Skill[]>([])
const loading = ref(false)
const q = ref('')
const activeTag = ref('')

async function load() {
  loading.value = true
  try {
    items.value = await publicApi.skills({
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

onMounted(load)
</script>

<template>
  <div class="container page">
    <h1 class="section-title">✨ AI Skill 推荐</h1>
    <p class="muted">精选实用的 AI 能力与技巧。</p>

    <div class="toolbar">
      <SearchBar v-model="q" placeholder="搜索技能名称、描述、标签…" @search="load" />
      <button class="btn btn-primary" @click="load">搜索</button>
      <button v-if="activeTag" class="btn btn-sm" @click="toggleTag(activeTag)">清除筛选: {{ activeTag }}</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有找到相关内容。</p>

    <div class="grid">
      <div v-for="s in items" :key="s.id" class="card item">
        <div class="item-head">
          <h3>{{ s.name }}</h3>
          <StarRating :level="s.recommendLevel" />
        </div>
        <p class="muted desc">{{ s.description }}</p>
        <div class="item-foot">
          <TagList :tags="s.tags" :active="activeTag" @select="toggleTag" />
          <a v-if="s.link" :href="s.link" target="_blank" rel="noopener" class="btn btn-sm">查看 ↗</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin: 18px 0 24px; flex-wrap: wrap; }
.toolbar > :first-child { flex: 1; min-width: 220px; }
.item { padding: 20px; display: flex; flex-direction: column; }
.item-head { display: flex; justify-content: space-between; align-items: start; gap: 10px; }
.item-head h3 { margin: 0 0 4px; }
.desc { flex: 1; }
.item-foot { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
</style>

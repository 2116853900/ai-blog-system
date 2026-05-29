<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { Post } from '../api/types'
import SearchBar from '../components/SearchBar.vue'

const posts = ref<Post[]>([])
const q = ref('')
const loading = ref(false)

const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase()
  if (!kw) return posts.value
  return posts.value.filter(p =>
    p.title.toLowerCase().includes(kw) ||
    (p.summary || '').toLowerCase().includes(kw) ||
    (p.tags || '').toLowerCase().includes(kw)
  )
})

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
    <h1 class="section-title">📚 相关教程</h1>
    <p class="muted">从入门到进阶的 AI 实战教程。</p>

    <div class="toolbar">
      <SearchBar v-model="q" placeholder="搜索教程标题、摘要、标签…" />
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!filtered.length" class="muted">没有找到教程。</p>

    <div class="list">
      <RouterLink
        v-for="p in filtered"
        :key="p.id"
        :to="`/tutorials/${p.slug}`"
        class="card post"
      >
        <div class="post-main">
          <span v-if="p.category" class="tag">{{ p.category }}</span>
          <h3>{{ p.title }}</h3>
          <p class="muted">{{ p.summary }}</p>
        </div>
        <span class="muted date">{{ fmt(p.createdAt) }}</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.toolbar { margin: 18px 0 24px; max-width: 480px; }
.list { display: flex; flex-direction: column; gap: 14px; }
.post {
  padding: 20px; color: var(--text);
  display: flex; justify-content: space-between; align-items: start; gap: 16px;
}
.post:hover { text-decoration: none; }
.post h3 { margin: 8px 0 4px; }
.date { white-space: nowrap; font-size: 13px; }
</style>

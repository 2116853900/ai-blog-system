<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { Mcp } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import StarRating from '../components/StarRating.vue'

const items = ref<Mcp[]>([])
const loading = ref(false)
const q = ref('')
const activeTag = ref('')
const copied = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    items.value = await publicApi.mcps({
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

async function copyCmd(m: Mcp) {
  if (!m.installCmd) return
  await navigator.clipboard.writeText(m.installCmd)
  copied.value = m.id
  setTimeout(() => (copied.value = null), 1500)
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <h1 class="section-title">🔌 MCP 推荐</h1>
    <p class="muted">模型上下文协议服务器，让 AI 安全连接文件、数据库、API 等外部能力。</p>

    <div class="toolbar">
      <SearchBar v-model="q" placeholder="搜索 MCP 名称、描述、标签…" @search="load" />
      <button class="btn btn-primary" @click="load">搜索</button>
      <button v-if="activeTag" class="btn btn-sm" @click="toggleTag(activeTag)">清除筛选: {{ activeTag }}</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有找到相关内容。</p>

    <div class="grid">
      <div v-for="m in items" :key="m.id" class="card item">
        <div class="item-head">
          <h3>{{ m.name }}</h3>
          <StarRating :level="m.recommendLevel" />
        </div>
        <p class="muted desc">{{ m.description }}</p>

        <div v-if="m.installCmd" class="cmd" @click="copyCmd(m)" :title="'点击复制'">
          <code>{{ m.installCmd }}</code>
          <span class="copy">{{ copied === m.id ? '已复制 ✓' : '复制' }}</span>
        </div>

        <div class="item-foot">
          <TagList :tags="m.tags" :active="activeTag" @select="toggleTag" />
          <a v-if="m.repoUrl" :href="m.repoUrl" target="_blank" rel="noopener" class="btn btn-sm">仓库 ↗</a>
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
.cmd {
  display: flex; justify-content: space-between; align-items: center; gap: 8px;
  background: var(--bg-soft); border-radius: 8px; padding: 8px 10px; margin: 8px 0;
  cursor: pointer; font-size: 13px; overflow: hidden;
}
.cmd code { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.copy { color: var(--primary); white-space: nowrap; }
.item-foot { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 6px; flex-wrap: wrap; }
</style>

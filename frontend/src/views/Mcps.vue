<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { Mcp, ResourceTagSummary } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import StarRating from '../components/StarRating.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import DetailDrawer from '../components/DetailDrawer.vue'
import CommentSection from '../components/CommentSection.vue'
import CopyButton from '../components/CopyButton.vue'
import { useListView } from '../composables/useListView'

const {
  items, loading, q, activeTag, activeCategory, allCategories,
  isEmpty, hasFilter, load, toggleTag, selectCategory, reset
} = useListView<Mcp>(publicApi.mcps)

const selected = ref<Mcp | null>(null)
const popularTags = ref<ResourceTagSummary[]>([])
function open(m: Mcp) { selected.value = m }
function close() { selected.value = null }

async function loadPopularTags() {
  try {
    popularTags.value = await publicApi.mcpPopularTags({ limit: 12 })
  } catch {
    popularTags.value = []
  }
}

onMounted(loadPopularTags)
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">MCP 推荐</h1>
      <p class="muted">Model Context Protocol 服务器，点击查看安装命令与详情。</p>
    </header>

    <div class="toolbar">
      <SearchBar class="grow" v-model="q" placeholder="搜索 MCP 名称、描述、标签…" @search="load" />
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

    <StateBlock :loading="loading" :empty="isEmpty" empty-text="没有匹配的 MCP，换个关键词试试。" class="block-area">
      <template #skeleton>
        <div class="grid">
          <div v-for="i in 6" :key="i" class="card item">
            <Skeleton block height="20px" width="55%" />
            <Skeleton block height="14px" radius="6px" />
            <Skeleton block height="34px" radius="6px" />
          </div>
        </div>
      </template>

      <div class="grid">
        <button
          v-for="(m, i) in items" :key="m.id"
          class="card item is-interactive rise"
          :style="{ animationDelay: `${Math.min(i * 0.04, 0.4)}s` }"
          @click="open(m)"
        >
          <div class="item-head">
            <h3 class="mono">{{ m.name }}</h3>
            <StarRating :level="m.recommendLevel" />
          </div>
          <p class="muted desc">{{ m.description }}</p>
          <div v-if="m.installCmd" class="codeline" @click.stop>
            <code>{{ m.installCmd }}</code>
            <CopyButton :text="m.installCmd" label="复制" success-msg="安装命令已复制" />
          </div>
          <div class="item-foot">
            <TagList :tags="m.tags" :active="activeTag" @select="toggleTag" />
            <span class="open-hint mono">详情 →</span>
          </div>
        </button>
      </div>
    </StateBlock>

    <DetailDrawer :open="!!selected" :title="selected?.name" @close="close">
      <template v-if="selected">
        <div class="d-meta">
          <StarRating :level="selected.recommendLevel" />
          <span v-if="selected.category" class="chip chip-active">{{ selected.category }}</span>
        </div>
        <p class="d-desc">{{ selected.description || '暂无描述。' }}</p>

        <div v-if="selected.installCmd" class="d-field">
          <span class="d-label mono">安装命令</span>
          <div class="codeline">
            <code>{{ selected.installCmd }}</code>
            <CopyButton :text="selected.installCmd" success-msg="安装命令已复制" />
          </div>
        </div>

        <TagList :tags="selected.tags" @select="(t) => { toggleTag(t); close() }" />
        <a v-if="selected.repoUrl" :href="selected.repoUrl" target="_blank" rel="noopener" class="btn btn-primary d-link">
          查看仓库 ↗
        </a>
        <hr class="d-sep" />
        <CommentSection ref-type="MCP" :ref-id="selected.id" />
      </template>
    </DetailDrawer>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; }
.page-head { margin-bottom: 16px; }
.block-area { margin-top: 20px; }
.chips { margin-top: 14px; }
.popular-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
.popular-label { color: var(--text-dim); font-size: 12px; }
.tag-chip {
  border: 1px solid var(--border); background: var(--bg-soft); color: var(--text-soft);
  border-radius: 999px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;
  font: inherit; font-size: 12px; padding: 5px 10px;
}
.tag-chip span { color: var(--text-dim); font-family: var(--font-mono); font-size: 11px; }
.tag-chip:hover, .tag-chip.active { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.item {
  padding: 20px; display: flex; flex-direction: column; gap: 10px;
  text-align: left; width: 100%; cursor: pointer; font: inherit; color: inherit;
}
.item-head { display: flex; justify-content: space-between; align-items: start; gap: 10px; }
.item-head h3 { margin: 0; font-size: 16px; font-weight: 700; }
.desc { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.item-foot { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; }
.open-hint { font-size: 12px; color: var(--text-dim); flex-shrink: 0; }
.item:hover .open-hint { color: var(--primary); }
.d-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.d-desc { line-height: 1.8; margin: 0 0 16px; }
.d-field { margin-bottom: 16px; }
.d-label { display: block; font-size: 12px; color: var(--text-soft); margin-bottom: 6px; }
.d-link { margin-top: 16px; }
.d-sep { border: none; border-top: 1px dashed var(--border-strong); margin: 24px 0; }
</style>

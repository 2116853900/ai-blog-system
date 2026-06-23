<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicApi } from '../api'
import type { ResourceTagSummary, Skill } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import TagList from '../components/TagList.vue'
import CommunityRatingBadge from '../components/CommunityRatingBadge.vue'
import StarRating from '../components/StarRating.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import DetailDrawer from '../components/DetailDrawer.vue'
import CommentSection from '../components/CommentSection.vue'
import ResourceFavoriteButton from '../components/ResourceFavoriteButton.vue'
import ResourceReviewPanel from '../components/ResourceReviewPanel.vue'
import { useListView } from '../composables/useListView'

const {
  items, loading, q, activeTag, activeCategory, allCategories,
  isEmpty, hasFilter, load, toggleTag, selectCategory, reset
} = useListView<Skill>(publicApi.skills)

const selected = ref<Skill | null>(null)
const popularTags = ref<ResourceTagSummary[]>([])
function open(s: Skill) { selected.value = s }
function close() { selected.value = null }

async function loadPopularTags() {
  try {
    popularTags.value = await publicApi.skillPopularTags({ limit: 12 })
  } catch {
    popularTags.value = []
  }
}

onMounted(loadPopularTags)
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">AI Skill 推荐</h1>
      <p class="muted">精选实用的 AI 能力与技巧，点击卡片查看详情与讨论。</p>
    </header>

    <div class="toolbar">
      <SearchBar class="grow" v-model="q" placeholder="搜索技能名称、描述、标签…" @search="load" />
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

    <StateBlock :loading="loading" :empty="isEmpty" empty-text="没有匹配的 Skill，换个关键词试试。" class="block-area">
      <template #skeleton>
        <div class="grid">
          <div v-for="i in 6" :key="i" class="card item">
            <Skeleton block height="20px" width="60%" />
            <Skeleton block height="14px" radius="6px" />
            <Skeleton block height="14px" width="80%" radius="6px" />
          </div>
        </div>
      </template>

      <div class="grid">
        <button
          v-for="(s, i) in items" :key="s.id"
          class="card item is-interactive rise"
          :style="{ animationDelay: `${Math.min(i * 0.04, 0.4)}s` }"
          @click="open(s)"
        >
          <div class="item-head">
            <h3 class="mono">{{ s.name }}</h3>
            <div class="item-ratings">
              <StarRating :level="s.recommendLevel" />
              <CommunityRatingBadge :average-rating="s.averageRating" :review-count="s.reviewCount" />
            </div>
          </div>
          <p class="muted desc">{{ s.description }}</p>
          <div class="item-foot">
            <TagList :tags="s.tags" :active="activeTag" @select="toggleTag" />
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
        <TagList :tags="selected.tags" @select="(t) => { toggleTag(t); close() }" />
        <a v-if="selected.link" :href="selected.link" target="_blank" rel="noopener" class="btn btn-primary d-link">
          访问 Skill ↗
        </a>
        <div class="resource-actions">
          <ResourceFavoriteButton ref-type="SKILL" :ref-id="selected.id" />
          <ResourceReviewPanel ref-type="SKILL" :ref-id="selected.id" />
        </div>
        <hr class="d-sep" />
        <CommentSection ref-type="SKILL" :ref-id="selected.id" />
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
  padding: 20px; display: flex; flex-direction: column; gap: 8px;
  text-align: left; width: 100%; cursor: pointer; font: inherit; color: inherit;
}
.item-ratings { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.item-head { display: flex; justify-content: space-between; align-items: start; gap: 10px; }
.item-head h3 { margin: 0; font-size: 16px; font-weight: 700; }
.desc { flex: 1; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.item-foot { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 6px; flex-wrap: wrap; }
.open-hint { font-size: 12px; color: var(--text-dim); flex-shrink: 0; }
.item:hover .open-hint { color: var(--primary); }
.d-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.d-desc { line-height: 1.8; margin: 0 0 16px; }
.d-link { margin-top: 16px; }
.resource-actions { display: grid; gap: 18px; margin-top: 20px; }
.d-sep { border: none; border-top: 1px dashed var(--border-strong); margin: 24px 0; }
</style>

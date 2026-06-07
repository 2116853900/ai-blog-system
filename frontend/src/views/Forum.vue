<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { forumApi, userApi } from '../api'
import type { ForumCategory, ForumTagSummary, ForumThread, Page, UserProfile } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import { useAuthStore } from '../stores/auth'

type ForumSort = 'latest' | 'newest' | 'popular'
type SolveFilter = 'all' | 'unsolved' | 'solved'

const sortOptions: Array<{ value: ForumSort; label: string }> = [
  { value: 'latest', label: '最近活跃' },
  { value: 'newest', label: '最新发布' },
  { value: 'popular', label: '热门' }
]
const solveFilterOptions: Array<{ value: SolveFilter; label: string }> = [
  { value: 'all', label: '全部状态' },
  { value: 'unsolved', label: '未解决' },
  { value: 'solved', label: '已解决' }
]

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const categories = ref<ForumCategory[]>([])
const popularTags = ref<ForumTagSummary[]>([])
const threads = ref<Page<ForumThread> | null>(null)
const profiles = ref<Record<number, UserProfile>>({})
const loading = ref(true)
const page = ref(0)
const selectedCategoryId = ref<number | undefined>(
  route.query.categoryId ? Number(route.query.categoryId) : undefined
)
const q = ref(typeof route.query.q === 'string' ? route.query.q : '')
const selectedTag = ref(typeof route.query.tag === 'string' ? route.query.tag : '')
const unansweredOnly = ref(route.query.unanswered === 'true')
const solveFilter = ref<SolveFilter>(parseSolveFilter(route.query.solved))
const sort = ref<ForumSort>(parseSort(route.query.sort))

const parents = computed(() => categories.value.filter(c => !c.parentId))
const activeFilters = computed(() => {
  const filters: string[] = []
  if (selectedCategoryId.value) filters.push(`板块：${categoryName(selectedCategoryId.value)}`)
  if (q.value.trim()) filters.push(`搜索：${q.value.trim()}`)
  if (selectedTag.value) filters.push(`标签：${selectedTag.value}`)
  if (unansweredOnly.value) filters.push('只看未回复')
  if (solveFilter.value !== 'all') filters.push(`状态：${solveFilterLabel(solveFilter.value)}`)
  if (sort.value !== 'latest') filters.push(`排序：${sortLabel(sort.value)}`)
  return filters
})

function parseSolveFilter(value: unknown): SolveFilter {
  if (value === 'true') return 'solved'
  if (value === 'false') return 'unsolved'
  return 'all'
}

function solvedParam() {
  if (solveFilter.value === 'solved') return true
  if (solveFilter.value === 'unsolved') return false
  return undefined
}

function solveFilterLabel(value: SolveFilter) {
  return solveFilterOptions.find(option => option.value === value)?.label || '全部状态'
}

function parseSort(value: unknown): ForumSort {
  return value === 'newest' || value === 'popular' ? value : 'latest'
}

function sortLabel(value: ForumSort) {
  return sortOptions.find(option => option.value === value)?.label || '最近活跃'
}

function children(parentId: number) {
  return categories.value.filter(c => c.parentId === parentId)
}

function categoryName(id: number) {
  return categories.value.find(c => c.id === id)?.name || `#${id}`
}

function tagsOf(tags?: string) {
  return (tags || '').split(',').map(t => t.trim()).filter(Boolean)
}

function fmt(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'
}

function authorName(id: number) {
  return profiles.value[id]?.nickname || profiles.value[id]?.username || `用户 ${id}`
}

function threadLink(id: number) {
  return `/forum/threads/${id}`
}

function userLink(id: number) {
  return `/users/${id}`
}

async function loadProfiles(ids: number[]) {
  const missing = [...new Set(ids)].filter(id => id && !profiles.value[id])
  if (!missing.length) return
  const result = await Promise.allSettled(missing.map(id => userApi.profile(id)))
  result.forEach((r, idx) => {
    if (r.status === 'fulfilled') profiles.value[missing[idx]] = r.value
  })
}

async function loadThreads(nextPage = page.value) {
  page.value = nextPage
  loading.value = true
  try {
    threads.value = await forumApi.threads({
      categoryId: selectedCategoryId.value,
      q: q.value.trim() || undefined,
      tag: selectedTag.value || undefined,
      unanswered: unansweredOnly.value || undefined,
      solved: solvedParam(),
      sort: sort.value,
      page: page.value,
      size: 20
    })
    await loadProfiles(threads.value.content.map(t => t.authorId).concat(threads.value.content.map(t => t.lastReplyUserId || 0)))
  } finally {
    loading.value = false
  }
}

async function selectCategory(id?: number) {
  selectedCategoryId.value = id
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function syncQuery() {
  const query: Record<string, string> = {}
  if (selectedCategoryId.value) query.categoryId = String(selectedCategoryId.value)
  if (q.value.trim()) query.q = q.value.trim()
  if (selectedTag.value) query.tag = selectedTag.value
  if (unansweredOnly.value) query.unanswered = 'true'
  if (solveFilter.value !== 'all') query.solved = String(solvedParam())
  if (sort.value !== 'latest') query.sort = sort.value
  await router.replace({ query })
}

async function searchThreads() {
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function setUnansweredOnly(value: boolean) {
  unansweredOnly.value = value
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

function onUnansweredChange(event: Event) {
  void setUnansweredOnly((event.target as HTMLInputElement).checked)
}

async function selectSolveFilter(value: SolveFilter) {
  if (solveFilter.value === value) return
  solveFilter.value = value
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function selectTag(tag: string) {
  selectedTag.value = tag
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function clearTag() {
  selectedTag.value = ''
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function clearAllFilters() {
  selectedCategoryId.value = undefined
  q.value = ''
  selectedTag.value = ''
  unansweredOnly.value = false
  solveFilter.value = 'all'
  sort.value = 'latest'
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function selectSort(value: ForumSort) {
  if (sort.value === value) return
  sort.value = value
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

onMounted(async () => {
  loading.value = true
  try {
    const [categoryResult, tagResult] = await Promise.all([
      forumApi.categories(),
      forumApi.popularThreadTags({ limit: 16 })
    ])
    categories.value = categoryResult
    popularTags.value = tagResult
    await loadThreads(0)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page">
    <header class="forum-hero card">
      <div>
        <p class="mono dim">// ai community</p>
        <h1 class="section-title prompt">AI 论坛</h1>
        <p class="muted">围绕 AI 工具、MCP、Prompt、API 使用经验和项目展示进行讨论。</p>
      </div>
      <RouterLink :to="auth.isLoggedIn() ? '/forum/new' : '/login?redirect=/forum/new'" class="btn btn-primary">
        发新帖
      </RouterLink>
    </header>

    <section class="layout">
      <aside class="card categories">
        <button class="cat-all" :class="{ active: !selectedCategoryId }" @click="selectCategory(undefined)">全部讨论</button>
        <div v-for="p in parents" :key="p.id" class="cat-group">
          <button class="cat-parent" :class="{ active: selectedCategoryId === p.id }" @click="selectCategory(p.id)">
            <span>{{ p.icon || '//' }}</span>
            <strong>{{ p.name }}</strong>
            <small>{{ p.threadCount }}</small>
          </button>
          <button
            v-for="c in children(p.id)"
            :key="c.id"
            class="cat-child"
            :class="{ active: selectedCategoryId === c.id }"
            @click="selectCategory(c.id)"
          >
            <span>{{ c.icon || '>' }}</span>
            {{ c.name }}
            <small>{{ c.threadCount }}</small>
          </button>
        </div>
        <div v-if="popularTags.length" class="popular-tags">
          <p class="popular-tags-title mono">热门标签</p>
          <button
            v-for="item in popularTags"
            :key="item.tag"
            type="button"
            class="popular-tag"
            :class="{ active: selectedTag === item.tag }"
            @click="selectTag(item.tag)"
          >
            <span>{{ item.tag }}</span>
            <small>{{ item.count }}</small>
          </button>
        </div>
      </aside>

      <main class="threads">
        <div class="thread-head">
          <div>
            <h2>{{ selectedCategoryId ? categoryName(selectedCategoryId) : '全部讨论' }}</h2>
            <span class="muted mono">{{ threads?.totalElements || 0 }} threads</span>
          </div>
          <div class="thread-tools">
            <div class="sort-tabs" aria-label="帖子排序">
              <button
                v-for="option in sortOptions"
                :key="option.value"
                type="button"
                :class="{ active: sort === option.value }"
                @click="selectSort(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <label class="filter-toggle">
              <input type="checkbox" :checked="unansweredOnly" @change="onUnansweredChange" />
              只看未回复
            </label>
            <div class="status-tabs" aria-label="解决状态筛选">
              <button
                v-for="option in solveFilterOptions"
                :key="option.value"
                type="button"
                :class="{ active: solveFilter === option.value }"
                @click="selectSolveFilter(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <SearchBar v-model="q" placeholder="搜索标题、正文或标签" @search="searchThreads" />
          </div>
        </div>

        <div v-if="activeFilters.length" class="active-filter">
          <span v-for="filter in activeFilters" :key="filter" class="filter-chip">{{ filter }}</span>
          <button type="button" @click="clearAllFilters">清除全部</button>
        </div>

        <StateBlock :loading="loading" :empty="!threads?.content.length" :empty-text="q || selectedTag || unansweredOnly || solveFilter !== 'all' ? '没有匹配的讨论。' : '暂无帖子，来发布第一条讨论吧。'">
          <template #skeleton>
            <div class="thread-list">
              <div v-for="i in 5" :key="i" class="card thread-card">
                <Skeleton block height="18px" width="55%" />
                <Skeleton block height="14px" width="85%" />
              </div>
            </div>
          </template>

          <div class="thread-list">
            <div
              v-for="(t, i) in threads?.content"
              :key="t.id"
              class="card thread-card rise"
              :style="{ animationDelay: `${Math.min(i * 0.035, 0.35)}s` }"
            >
              <div class="thread-main">
                <div class="thread-top">
                  <span class="chip">{{ categoryName(t.categoryId) }}</span>
                  <span v-if="t.acceptedReplyId" class="solution-chip">已解决</span>
                  <span v-if="t.status !== 'NORMAL'" class="badge badge-unknown">{{ t.status }}</span>
                </div>
                <h3>
                  <RouterLink :to="threadLink(t.id)" class="thread-title-link">{{ t.title }}</RouterLink>
                </h3>
                <div class="tags" v-if="tagsOf(t.tags).length">
                  <button v-for="tag in tagsOf(t.tags)" :key="tag" type="button" class="tag tag-button" @click.prevent.stop="selectTag(tag)">
                    {{ tag }}
                  </button>
                </div>
                <p class="muted meta mono">
                  by <RouterLink :to="userLink(t.authorId)" class="profile-link">{{ authorName(t.authorId) }}</RouterLink>
                  · {{ fmt(t.createdAt) }}
                </p>
              </div>
              <div class="stats mono">
                <span>{{ t.replyCount }} 回复</span>
                <span>{{ t.viewCount }} 浏览</span>
                <span v-if="t.lastReplyUserId">
                  last by <RouterLink :to="userLink(t.lastReplyUserId)" class="profile-link">{{ authorName(t.lastReplyUserId) }}</RouterLink>
                </span>
                <span>last {{ fmt(t.lastReplyAt || t.createdAt) }}</span>
              </div>
            </div>
          </div>
        </StateBlock>

        <div v-if="threads && threads.totalPages > 1" class="pager">
          <button class="btn" :disabled="threads.first" @click="loadThreads(page - 1)">上一页</button>
          <span class="muted mono">{{ threads.number + 1 }} / {{ threads.totalPages }}</span>
          <button class="btn" :disabled="threads.last" @click="loadThreads(page + 1)">下一页</button>
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 70px; }
.forum-hero {
  padding: 26px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  background:
    radial-gradient(circle at 8% 0%, color-mix(in srgb, var(--primary) 16%, transparent), transparent 32%),
    var(--bg-elevated);
}
.layout { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 18px; align-items: start; }
.categories { padding: 14px; position: sticky; top: 82px; }
.cat-group { margin-top: 10px; }
.cat-all, .cat-parent, .cat-child {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--text-soft);
  text-align: left;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--dur) var(--ease), color var(--dur) var(--ease);
}
.cat-all, .cat-parent { padding: 9px 10px; font-family: var(--font-mono); display: flex; align-items: center; gap: 8px; }
.cat-child { padding: 7px 10px 7px 32px; display: flex; gap: 7px; align-items: center; }
.cat-parent small, .cat-child small { margin-left: auto; color: var(--text-dim); }
.cat-all:hover, .cat-parent:hover, .cat-child:hover,
.cat-all.active, .cat-parent.active, .cat-child.active { background: var(--primary-soft); color: var(--primary); }
.popular-tags {
  border-top: 1px solid var(--border);
  margin-top: 14px;
  padding-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.popular-tags-title {
  width: 100%;
  margin: 0 0 2px;
  color: var(--text-dim);
  font-size: 12px;
}
.popular-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  cursor: pointer;
  font-size: 12px;
  padding: 6px 8px;
}
.popular-tag small {
  color: var(--text-dim);
  font-family: var(--font-mono);
}
.popular-tag:hover,
.popular-tag.active {
  border-color: color-mix(in srgb, var(--primary) 45%, var(--border));
  background: var(--primary-soft);
  color: var(--primary);
}
.thread-head { display: grid; grid-template-columns: minmax(0, 1fr) minmax(260px, 360px); gap: 14px; align-items: center; margin-bottom: 12px; }
.thread-head h2 { margin: 0; }
.thread-tools { display: grid; gap: 10px; }
.sort-tabs,
.status-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.sort-tabs button,
.status-tabs button {
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  font-size: 13px;
  padding: 7px 10px;
  white-space: nowrap;
}
.sort-tabs button:hover,
.sort-tabs button.active,
.status-tabs button:hover,
.status-tabs button.active {
  background: var(--primary-soft);
  color: var(--primary);
}
.filter-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-soft);
  font-size: 13px;
}
.filter-toggle input {
  accent-color: var(--primary);
}
.active-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--text-soft);
  font-size: 13px;
}
.filter-chip {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  padding: 5px 8px;
}
.active-filter button {
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  cursor: pointer;
  padding: 5px 8px;
}
.thread-list { display: flex; flex-direction: column; gap: 12px; }
.thread-card { padding: 18px; color: var(--text); display: flex; justify-content: space-between; gap: 20px; }
.thread-card:hover { text-decoration: none; }
.thread-top { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.solution-chip {
  border: 1px solid color-mix(in srgb, #16a34a 42%, var(--border));
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, #16a34a 12%, var(--bg-inset));
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
}
.thread-main h3 { margin: 0 0 8px; font-size: 18px; }
.thread-title-link { color: var(--text); }
.thread-title-link:hover,
.profile-link:hover { color: var(--primary); }
.profile-link {
  color: var(--text-soft);
  text-decoration: none;
}
.tags { margin-bottom: 8px; }
.tag-button {
  border: 1px solid var(--border);
  cursor: pointer;
  font-family: var(--font-mono);
}
.meta { font-size: 12px; margin: 0; }
.stats { color: var(--text-dim); font-size: 12px; display: flex; flex-direction: column; gap: 4px; text-align: right; min-width: 116px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 18px; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .categories { position: static; }
}
@media (max-width: 640px) {
  .forum-hero, .thread-card { flex-direction: column; align-items: stretch; }
  .thread-head { grid-template-columns: 1fr; }
  .sort-tabs, .status-tabs { overflow-x: auto; }
  .stats { text-align: left; flex-direction: row; flex-wrap: wrap; }
}
</style>

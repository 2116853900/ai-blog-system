<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { forumApi, userApi } from '../api'
import type { ForumCategory, ForumThread, Page, UserProfile } from '../api/types'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const categories = ref<ForumCategory[]>([])
const threads = ref<Page<ForumThread> | null>(null)
const profiles = ref<Record<number, UserProfile>>({})
const loading = ref(true)
const page = ref(0)
const selectedCategoryId = ref<number | undefined>(
  route.query.categoryId ? Number(route.query.categoryId) : undefined
)

const parents = computed(() => categories.value.filter(c => !c.parentId))

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
  await router.replace({ query: id ? { categoryId: String(id) } : {} })
  await loadThreads(0)
}

onMounted(async () => {
  loading.value = true
  try {
    categories.value = await forumApi.categories()
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
      </aside>

      <main class="threads">
        <div class="thread-head">
          <h2>{{ selectedCategoryId ? categoryName(selectedCategoryId) : '全部讨论' }}</h2>
          <span class="muted mono">{{ threads?.totalElements || 0 }} threads</span>
        </div>

        <StateBlock :loading="loading" :empty="!threads?.content.length" empty-text="暂无帖子，来发布第一条讨论吧。">
          <template #skeleton>
            <div class="thread-list">
              <div v-for="i in 5" :key="i" class="card thread-card">
                <Skeleton block height="18px" width="55%" />
                <Skeleton block height="14px" width="85%" />
              </div>
            </div>
          </template>

          <div class="thread-list">
            <RouterLink
              v-for="(t, i) in threads?.content"
              :key="t.id"
              :to="`/forum/threads/${t.id}`"
              class="card thread-card rise"
              :style="{ animationDelay: `${Math.min(i * 0.035, 0.35)}s` }"
            >
              <div class="thread-main">
                <div class="thread-top">
                  <span class="chip">{{ categoryName(t.categoryId) }}</span>
                  <span v-if="t.status !== 'NORMAL'" class="badge badge-unknown">{{ t.status }}</span>
                </div>
                <h3>{{ t.title }}</h3>
                <div class="tags" v-if="tagsOf(t.tags).length">
                  <span v-for="tag in tagsOf(t.tags)" :key="tag" class="tag">{{ tag }}</span>
                </div>
                <p class="muted meta mono">by {{ authorName(t.authorId) }} · {{ fmt(t.createdAt) }}</p>
              </div>
              <div class="stats mono">
                <span>{{ t.replyCount }} 回复</span>
                <span>{{ t.viewCount }} 浏览</span>
                <span>last {{ fmt(t.lastReplyAt || t.createdAt) }}</span>
              </div>
            </RouterLink>
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
.thread-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.thread-head h2 { margin: 0; }
.thread-list { display: flex; flex-direction: column; gap: 12px; }
.thread-card { padding: 18px; color: var(--text); display: flex; justify-content: space-between; gap: 20px; }
.thread-card:hover { text-decoration: none; }
.thread-top { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.thread-main h3 { margin: 0 0 8px; font-size: 18px; }
.tags { margin-bottom: 8px; }
.meta { font-size: 12px; margin: 0; }
.stats { color: var(--text-dim); font-size: 12px; display: flex; flex-direction: column; gap: 4px; text-align: right; min-width: 116px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 18px; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .categories { position: static; }
}
@media (max-width: 640px) {
  .forum-hero, .thread-card { flex-direction: column; align-items: stretch; }
  .stats { text-align: left; flex-direction: row; flex-wrap: wrap; }
}
</style>

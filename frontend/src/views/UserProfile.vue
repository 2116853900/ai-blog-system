<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { userApi } from '../api'
import type { ForumReply, ForumThread, Page, UserProfile } from '../api/types'
import Skeleton from '../components/Skeleton.vue'
import StateBlock from '../components/StateBlock.vue'

type ActivityTab = 'threads' | 'replies'

const route = useRoute()
const profile = ref<UserProfile | null>(null)
const threads = ref<Page<ForumThread> | null>(null)
const replies = ref<Page<ForumReply> | null>(null)
const profileLoading = ref(true)
const activityLoading = ref(false)
const error = ref('')
const activeTab = ref<ActivityTab>('threads')

const userId = computed(() => Number(route.params.id))
const activePage = computed(() => activeTab.value === 'threads' ? threads.value : replies.value)
const activeItems = computed(() => activePage.value?.content || [])
const activityTotal = computed(() => (threads.value?.totalElements || 0) + (replies.value?.totalElements || 0))
const displayName = computed(() => profile.value?.nickname || profile.value?.username || '用户')
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())

function fmt(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '-'
}

function fmtMinute(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'
}

function tagsOf(tags?: string) {
  return (tags || '').split(',').map(tag => tag.trim()).filter(Boolean)
}

function preview(markdown?: string) {
  if (!markdown) return '暂无内容摘要。'
  const text = markdown
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[[^\]]+]\([^)]*\)/g, match => match.replace(/\[|\]\([^)]*\)/g, ''))
    .replace(/[>#*_~|-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > 150 ? `${text.slice(0, 147)}...` : text || '暂无内容摘要。'
}

function pageFor(tab: ActivityTab) {
  return tab === 'threads' ? threads.value : replies.value
}

async function loadActivity(tab: ActivityTab, page = 0) {
  activityLoading.value = true
  try {
    if (tab === 'threads') {
      threads.value = await userApi.threads(userId.value, { page, size: 10 })
    } else {
      replies.value = await userApi.replies(userId.value, { page, size: 10 })
    }
  } finally {
    activityLoading.value = false
  }
}

async function load() {
  if (!Number.isFinite(userId.value)) {
    error.value = '用户不存在。'
    profileLoading.value = false
    return
  }

  profileLoading.value = true
  activityLoading.value = true
  error.value = ''
  profile.value = null
  threads.value = null
  replies.value = null
  activeTab.value = 'threads'

  try {
    const [profileData, threadData, replyData] = await Promise.all([
      userApi.profile(userId.value),
      userApi.threads(userId.value, { page: 0, size: 10 }),
      userApi.replies(userId.value, { page: 0, size: 10 })
    ])
    profile.value = profileData
    threads.value = threadData
    replies.value = replyData
  } catch (e: any) {
    error.value = e?.response?.status === 404
      ? '用户不存在。'
      : (e?.response?.data?.message || '用户资料加载失败。')
  } finally {
    profileLoading.value = false
    activityLoading.value = false
  }
}

async function selectTab(tab: ActivityTab) {
  activeTab.value = tab
  if (!pageFor(tab)) {
    await loadActivity(tab, 0)
  }
}

async function turnPage(nextPage: number) {
  await loadActivity(activeTab.value, nextPage)
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="container page">
    <RouterLink to="/forum" class="muted mono">← 返回论坛</RouterLink>

    <StateBlock :loading="profileLoading" :empty="!!error || !profile" :empty-text="error || '用户不存在。'" class="profile-state">
      <template #skeleton>
        <div class="profile-layout">
          <aside class="card profile-card">
            <Skeleton block height="82px" width="82px" radius="18px" />
            <Skeleton block height="24px" width="65%" />
            <Skeleton block height="14px" width="50%" />
            <Skeleton block height="66px" />
          </aside>
          <main class="card activity-card">
            <Skeleton block height="28px" width="180px" />
            <Skeleton block height="90px" />
            <Skeleton block height="90px" />
          </main>
        </div>
      </template>

      <div v-if="profile" class="profile-layout">
        <aside class="card profile-card">
          <img v-if="profile.avatarUrl" class="avatar" :src="profile.avatarUrl" :alt="displayName" />
          <div v-else class="avatar fallback" aria-hidden="true">{{ initials }}</div>

          <div>
            <p class="mono dim">// user</p>
            <h1>{{ displayName }}</h1>
            <p class="muted mono">@{{ profile.username }}</p>
          </div>

          <p class="bio">{{ profile.bio || '这个用户还没有填写简介。' }}</p>

          <div class="stats">
            <span class="badge badge-unknown">{{ profile.role }}</span>
            <span class="badge badge-up">Lv.{{ profile.level || 1 }}</span>
            <span class="muted mono">{{ profile.experiencePoints || 0 }} xp</span>
          </div>

          <dl class="meta-list">
            <div>
              <dt>加入时间</dt>
              <dd>{{ fmt(profile.createdAt) }}</dd>
            </div>
            <div>
              <dt>公开动态</dt>
              <dd>{{ activityTotal }}</dd>
            </div>
          </dl>
        </aside>

        <main class="card activity-card">
          <header class="activity-head">
            <div>
              <p class="mono dim">// activity</p>
              <h2>公开动态</h2>
            </div>
            <span class="muted mono">{{ activityTotal }} items</span>
          </header>

          <div class="tabs" role="tablist" aria-label="公开动态">
            <button class="tab" :class="{ active: activeTab === 'threads' }" @click="selectTab('threads')">
              帖子 {{ threads?.totalElements || 0 }}
            </button>
            <button class="tab" :class="{ active: activeTab === 'replies' }" @click="selectTab('replies')">
              回复 {{ replies?.totalElements || 0 }}
            </button>
          </div>

          <StateBlock
            :loading="activityLoading"
            :empty="activeItems.length === 0"
            :empty-text="activeTab === 'threads' ? '暂无公开帖子。' : '暂无公开回复。'"
            class="activity-state"
          >
            <template #skeleton>
              <div class="activity-list">
                <div v-for="i in 3" :key="i" class="activity-item skeleton-row">
                  <Skeleton block height="18px" width="55%" />
                  <Skeleton block height="14px" />
                  <Skeleton block height="14px" width="35%" />
                </div>
              </div>
            </template>

            <div v-if="activeTab === 'threads'" class="activity-list">
              <RouterLink
                v-for="thread in threads?.content"
                :key="thread.id"
                :to="`/forum/threads/${thread.id}`"
                class="activity-item"
              >
                <div class="item-main">
                  <h3>{{ thread.title }}</h3>
                  <p class="muted">{{ preview(thread.contentMarkdown) }}</p>
                  <div v-if="tagsOf(thread.tags).length" class="tags">
                    <span v-for="tag in tagsOf(thread.tags).slice(0, 5)" :key="tag" class="tag">{{ tag }}</span>
                  </div>
                </div>
                <div class="item-side mono">
                  <span>{{ thread.replyCount }} 回复</span>
                  <span>{{ thread.viewCount }} 浏览</span>
                  <span>{{ fmtMinute(thread.createdAt) }}</span>
                </div>
              </RouterLink>
            </div>

            <div v-else class="activity-list">
              <RouterLink
                v-for="reply in replies?.content"
                :key="reply.id"
                :to="`/forum/threads/${reply.threadId}`"
                class="activity-item"
              >
                <div class="item-main">
                  <h3>回复 #{{ reply.floorNumber }}</h3>
                  <p class="muted">{{ preview(reply.contentMarkdown) }}</p>
                </div>
                <div class="item-side mono">
                  <span>thread {{ reply.threadId }}</span>
                  <span>{{ fmtMinute(reply.createdAt) }}</span>
                </div>
              </RouterLink>
            </div>
          </StateBlock>

          <div v-if="activePage && activePage.totalPages > 1" class="pager">
            <button class="btn" :disabled="activePage.first || activityLoading" @click="turnPage(activePage.number - 1)">上一页</button>
            <span class="muted mono">{{ activePage.number + 1 }} / {{ activePage.totalPages }}</span>
            <button class="btn" :disabled="activePage.last || activityLoading" @click="turnPage(activePage.number + 1)">下一页</button>
          </div>
        </main>
      </div>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 70px; }
.profile-state { margin-top: 16px; }
.profile-layout {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}
.profile-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 82px;
}
.avatar {
  width: 82px;
  height: 82px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid var(--border-strong);
  background: var(--bg-soft);
}
.avatar.fallback {
  display: grid;
  place-items: center;
  color: var(--primary);
  background: var(--primary-soft);
  font-family: var(--font-mono);
  font-size: 34px;
  font-weight: 800;
}
.profile-card h1 { margin: 4px 0 0; font-size: 28px; line-height: 1.2; overflow-wrap: anywhere; }
.profile-card p { margin: 0; }
.bio {
  color: var(--text-soft);
  line-height: 1.75;
  overflow-wrap: anywhere;
}
.stats { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.meta-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-top: 14px;
  border-top: 1px dashed var(--border-strong);
}
.meta-list div { display: flex; justify-content: space-between; gap: 12px; }
.meta-list dt { color: var(--text-dim); font-size: 13px; }
.meta-list dd { margin: 0; color: var(--text); font-family: var(--font-mono); font-size: 13px; }
.activity-card { padding: 24px; }
.activity-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 14px;
  margin-bottom: 16px;
}
.activity-head p, .activity-head h2 { margin: 0; }
.activity-head h2 { font-size: 22px; }
.tabs {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  margin-bottom: 18px;
}
.tab {
  border: 0;
  border-radius: 5px;
  padding: 7px 12px;
  cursor: pointer;
  background: transparent;
  color: var(--text-soft);
  font-family: var(--font-mono);
  font-size: 12px;
}
.tab.active {
  background: var(--primary-soft);
  color: var(--primary);
}
.activity-state { min-height: 220px; }
.activity-list { display: flex; flex-direction: column; gap: 12px; }
.activity-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(104px, auto);
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--text);
  background: var(--bg-inset);
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease);
}
.activity-item:hover {
  border-color: var(--primary-dim);
  transform: translateY(-1px);
  text-decoration: none;
}
.activity-item h3 {
  margin: 0 0 6px;
  font-size: 16px;
  line-height: 1.45;
}
.activity-item p {
  margin: 0;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tags { margin-top: 8px; }
.item-side {
  color: var(--text-dim);
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: right;
}
.skeleton-row { display: flex; flex-direction: column; }
.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}
@media (max-width: 860px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-card { position: static; }
}
@media (max-width: 620px) {
  .activity-card, .profile-card { padding: 18px; }
  .activity-head, .activity-item { grid-template-columns: 1fr; }
  .activity-head { flex-direction: column; }
  .item-side {
    flex-direction: row;
    flex-wrap: wrap;
    text-align: left;
  }
  .tabs { display: flex; }
  .tab { flex: 1; }
}
</style>

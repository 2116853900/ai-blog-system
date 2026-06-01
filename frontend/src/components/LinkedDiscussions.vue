<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { forumApi, userApi } from '../api'
import type { ForumThread, RefType, UserProfile } from '../api/types'
import { useAuthStore } from '../stores/auth'
import Skeleton from './Skeleton.vue'

const props = defineProps<{
  refType: RefType
  refId: number
  sourceTitle: string
}>()

const auth = useAuthStore()
const threads = ref<ForumThread[]>([])
const profiles = ref<Record<number, UserProfile>>({})
const loading = ref(true)
const error = ref('')

const newThreadPath = computed(() => {
  const query = new URLSearchParams({
    linkedRefType: props.refType,
    linkedRefId: String(props.refId),
    sourceTitle: props.sourceTitle
  })
  return `/forum/new?${query.toString()}`
})

const newThreadTo = computed(() => {
  if (auth.isLoggedIn()) return newThreadPath.value
  return `/login?redirect=${encodeURIComponent(newThreadPath.value)}`
})

function authorName(id: number) {
  return profiles.value[id]?.nickname || profiles.value[id]?.username || `用户 ${id}`
}

function fmt(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'
}

async function loadProfiles(ids: number[]) {
  const missing = [...new Set(ids)].filter(id => id && !profiles.value[id])
  if (!missing.length) return
  const result = await Promise.allSettled(missing.map(id => userApi.profile(id)))
  result.forEach((item, index) => {
    if (item.status === 'fulfilled') profiles.value[missing[index]] = item.value
  })
}

async function load() {
  if (!props.refId) return
  loading.value = true
  error.value = ''
  try {
    threads.value = await forumApi.linkedThreads(props.refType, props.refId)
    await loadProfiles(threads.value.flatMap(thread => [thread.authorId, thread.lastReplyUserId || 0]))
  } catch (e: any) {
    error.value = e?.response?.data?.message || '关联讨论加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => [props.refType, props.refId], load)
</script>

<template>
  <section class="linked-discussions">
    <div class="discussions-head">
      <div>
        <p class="mono dim">// forum links</p>
        <h2>相关讨论</h2>
      </div>
      <RouterLink class="btn btn-primary" :to="newThreadTo">
        {{ auth.isLoggedIn() ? '发起讨论' : '登录后讨论' }}
      </RouterLink>
    </div>

    <div v-if="loading" class="discussion-list">
      <div v-for="i in 2" :key="i" class="discussion-card">
        <Skeleton block height="18px" width="65%" />
        <Skeleton block height="14px" width="45%" />
      </div>
    </div>

    <p v-else-if="error" class="err">{{ error }}</p>

    <div v-else-if="threads.length" class="discussion-list">
      <RouterLink
        v-for="thread in threads.slice(0, 5)"
        :key="thread.id"
        class="discussion-card"
        :to="`/forum/threads/${thread.id}`"
      >
        <div>
          <h3>{{ thread.title }}</h3>
          <p class="muted mono">
            {{ authorName(thread.authorId) }} · {{ thread.replyCount }} 回复 · {{ thread.viewCount }} 浏览
          </p>
        </div>
        <span class="muted mono last">last {{ fmt(thread.lastReplyAt || thread.createdAt) }}</span>
      </RouterLink>
    </div>

    <div v-else class="empty-discussions">
      <p class="muted">还没有围绕这个资源的论坛讨论。</p>
      <RouterLink class="mono" :to="newThreadTo">创建第一条讨论 →</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.linked-discussions {
  margin-top: 28px;
}
.discussions-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}
.discussions-head p,
.discussions-head h2 {
  margin: 0;
}
.discussions-head h2 {
  font-size: 20px;
}
.discussion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.discussion-card {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text);
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease);
}
a.discussion-card:hover {
  border-color: var(--primary-dim);
  transform: translateY(-1px);
  text-decoration: none;
}
.discussion-card h3 {
  margin: 0 0 4px;
  font-size: 15px;
  line-height: 1.45;
}
.discussion-card p {
  margin: 0;
  font-size: 12px;
}
.last {
  flex-shrink: 0;
  font-size: 12px;
  align-self: center;
  text-align: right;
}
.empty-discussions {
  padding: 18px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.empty-discussions p {
  margin: 0 0 6px;
}
.err {
  color: var(--danger);
  margin: 0;
}
@media (max-width: 640px) {
  .discussions-head,
  .discussion-card {
    flex-direction: column;
    align-items: stretch;
  }
  .last {
    text-align: left;
  }
}
</style>

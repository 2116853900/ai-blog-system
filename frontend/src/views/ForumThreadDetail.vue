<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { forumApi, userApi } from '../api'
import type {
  ForumCategory, ForumInteraction, ForumReply, ForumThread, Page,
  ReportReasonType, ReportTargetType, UserProfile
} from '../api/types'
import MarkdownView from '../components/MarkdownView.vue'
import StateBlock from '../components/StateBlock.vue'
import Skeleton from '../components/Skeleton.vue'
import { toast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const thread = ref<ForumThread | null>(null)
const replies = ref<Page<ForumReply> | null>(null)
const categories = ref<ForumCategory[]>([])
const profiles = ref<Record<number, UserProfile>>({})
const loading = ref(true)
const replyLoading = ref(false)
const saving = ref(false)
const page = ref(0)
const error = ref('')
const replyToId = ref<number | null>(null)
const form = reactive({ contentMarkdown: '' })
const interaction = ref<ForumInteraction | null>(null)
const interactionSaving = ref(false)
const reportTarget = ref<{ type: ReportTargetType; id: number; label: string } | null>(null)
const reportSaving = ref(false)
const reportForm = reactive<{ reasonType: ReportReasonType; reasonText: string }>({
  reasonType: 'SPAM',
  reasonText: ''
})

const reportReasons: Array<{ value: ReportReasonType; label: string }> = [
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'ABUSE', label: '辱骂攻击' },
  { value: 'PORN', label: '色情低俗' },
  { value: 'POLITICS', label: '敏感内容' },
  { value: 'ILLEGAL', label: '违法违规' },
  { value: 'COPYRIGHT', label: '侵权' },
  { value: 'OTHER', label: '其他' }
]

const threadId = computed(() => Number(route.params.id))
const canManageThread = computed(() => {
  if (!thread.value || !auth.isLoggedIn()) return false
  return auth.userId === thread.value.authorId || auth.isModerator
})

function categoryName(id?: number) {
  return categories.value.find(c => c.id === id)?.name || '未分类'
}

function authorName(id?: number) {
  if (!id) return '匿名用户'
  return profiles.value[id]?.nickname || profiles.value[id]?.username || `用户 ${id}`
}

function fmt(d?: string) {
  return d ? new Date(d).toLocaleString('zh-CN') : '-'
}

function tagsOf(tags?: string) {
  return (tags || '').split(',').map(t => t.trim()).filter(Boolean)
}

async function loadProfiles(ids: Array<number | undefined>) {
  const missing = [...new Set(ids.filter(Boolean) as number[])].filter(id => !profiles.value[id])
  if (!missing.length) return
  const result = await Promise.allSettled(missing.map(id => userApi.profile(id)))
  result.forEach((r, idx) => {
    if (r.status === 'fulfilled') profiles.value[missing[idx]] = r.value
  })
}

async function loadReplies(nextPage = page.value) {
  page.value = nextPage
  replyLoading.value = true
  try {
    replies.value = await forumApi.replies(threadId.value, { page: page.value, size: 30 })
    await loadProfiles(replies.value.content.flatMap(r => [r.authorId, r.replyToUserId]))
  } finally {
    replyLoading.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [categoryData, threadData] = await Promise.all([
      forumApi.categories(),
      forumApi.thread(threadId.value)
    ])
    categories.value = categoryData
    thread.value = threadData
    interaction.value = await forumApi.interaction(threadId.value)
    await loadProfiles([threadData.authorId, threadData.lastReplyUserId])
    await loadReplies(0)
  } catch (e: any) {
    error.value = e?.response?.data?.message || '帖子不存在或加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!thread.value) return
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  interactionSaving.value = true
  try {
    interaction.value = interaction.value?.liked
      ? await forumApi.unlikeThread(thread.value.id)
      : await forumApi.likeThread(thread.value.id)
    thread.value.likeCount = interaction.value.likeCount
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '操作失败')
  } finally {
    interactionSaving.value = false
  }
}

async function toggleFavorite() {
  if (!thread.value) return
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  interactionSaving.value = true
  try {
    interaction.value = interaction.value?.favorited
      ? await forumApi.unfavoriteThread(thread.value.id)
      : await forumApi.favoriteThread(thread.value.id)
    thread.value.favoriteCount = interaction.value.favoriteCount
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '操作失败')
  } finally {
    interactionSaving.value = false
  }
}

function openReport(type: ReportTargetType, id: number, label: string) {
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  reportTarget.value = { type, id, label }
  reportForm.reasonType = 'SPAM'
  reportForm.reasonText = ''
}

function closeReport() {
  reportTarget.value = null
  reportForm.reasonType = 'SPAM'
  reportForm.reasonText = ''
}

async function submitReport() {
  if (!reportTarget.value) return
  reportSaving.value = true
  try {
    await forumApi.report({
      targetType: reportTarget.value.type,
      targetId: reportTarget.value.id,
      reasonType: reportForm.reasonType,
      reasonText: reportForm.reasonText.trim() || undefined
    })
    toast.success('举报已提交')
    closeReport()
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '举报失败')
  } finally {
    reportSaving.value = false
  }
}

async function submitReply() {
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!form.contentMarkdown.trim()) {
    toast.error('回复内容不能为空')
    return
  }
  saving.value = true
  try {
    await forumApi.createReply(threadId.value, { contentMarkdown: form.contentMarkdown.trim(), replyToId: replyToId.value || undefined })
    form.contentMarkdown = ''
    replyToId.value = null
    toast.success('回复已发布')
    await loadReplies(replies.value?.last ? page.value : Math.max(0, (replies.value?.totalPages || 1) - 1))
    if (thread.value) thread.value.replyCount += 1
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '回复失败')
  } finally {
    saving.value = false
  }
}

async function removeReply(reply: ForumReply) {
  if (!confirm(`删除 #${reply.floorNumber} 回复？`)) return
  await forumApi.deleteReply(reply.id)
  toast.success('回复已删除')
  await loadReplies(page.value)
}

function quoteReply(reply: ForumReply) {
  replyToId.value = reply.id
  form.contentMarkdown = `> 回复 #${reply.floorNumber} ${authorName(reply.authorId)}\n\n${form.contentMarkdown}`
}

async function removeThread() {
  if (!thread.value || !confirm(`删除「${thread.value.title}」？`)) return
  await forumApi.deleteThread(thread.value.id)
  toast.success('帖子已删除')
  router.push('/forum')
}

onMounted(load)
</script>

<template>
  <div class="container page">
    <RouterLink to="/forum" class="muted mono">← 返回论坛</RouterLink>

    <StateBlock :loading="loading" :empty="!!error || !thread" :empty-text="error || '帖子不存在。'" class="detail-state">
      <template #skeleton>
        <section class="card thread-card">
          <Skeleton block height="28px" width="60%" />
          <Skeleton block height="16px" width="35%" />
          <Skeleton block height="180px" radius="10px" />
        </section>
      </template>

      <template v-if="thread">
        <article class="card thread-card">
          <header class="thread-head">
            <div>
              <div class="thread-meta">
                <span class="chip chip-active">{{ categoryName(thread.categoryId) }}</span>
                <span v-if="thread.status !== 'NORMAL'" class="badge badge-unknown">{{ thread.status }}</span>
              </div>
              <h1>{{ thread.title }}</h1>
              <p class="muted mono small">
                {{ authorName(thread.authorId) }} · {{ fmt(thread.createdAt) }} · {{ thread.viewCount }} 浏览 · {{ thread.replyCount }} 回复
              </p>
            </div>
            <div class="thread-actions">
              <button class="btn btn-sm" :disabled="interactionSaving" @click="toggleLike">
                {{ interaction?.liked ? '已点赞' : '点赞' }} {{ interaction?.likeCount ?? thread.likeCount }}
              </button>
              <button class="btn btn-sm" :disabled="interactionSaving" @click="toggleFavorite">
                {{ interaction?.favorited ? '已收藏' : '收藏' }} {{ interaction?.favoriteCount ?? thread.favoriteCount }}
              </button>
              <button class="btn btn-sm" @click="openReport('POST', thread.id, `帖子「${thread.title}」`)">举报</button>
              <RouterLink v-if="canManageThread" class="btn btn-sm" :to="`/forum/threads/${thread.id}/edit`">编辑帖子</RouterLink>
              <button v-if="canManageThread" class="btn btn-danger btn-sm" @click="removeThread">删除帖子</button>
            </div>
          </header>

          <div v-if="tagsOf(thread.tags).length" class="tags">
            <span v-for="tag in tagsOf(thread.tags)" :key="tag" class="tag">{{ tag }}</span>
          </div>

          <MarkdownView :source="thread.contentMarkdown" />
        </article>

        <section class="reply-section">
          <div class="reply-head">
            <h2 class="section-title prompt">回复</h2>
            <span class="muted mono">{{ replies?.totalElements || 0 }} replies</span>
          </div>

          <StateBlock :loading="replyLoading" :empty="!replies?.content.length" empty-text="暂无回复。">
            <template #skeleton>
              <div class="reply-list">
                <div v-for="i in 3" :key="i" class="card reply-card">
                  <Skeleton block height="14px" width="30%" />
                  <Skeleton block height="70px" radius="8px" />
                </div>
              </div>
            </template>

            <div class="reply-list">
              <article v-for="r in replies?.content" :key="r.id" class="card reply-card">
                <div class="reply-info">
                  <span class="mono">#{{ r.floorNumber }}</span>
                  <span>{{ authorName(r.authorId) }}</span>
                  <span class="muted">{{ fmt(r.createdAt) }}</span>
                  <button class="btn btn-sm" @click="quoteReply(r)">引用</button>
                  <button class="btn btn-sm" @click="openReport('REPLY', r.id, `#${r.floorNumber} 回复`)">举报</button>
                  <button
                    v-if="auth.userId === r.authorId || auth.isModerator"
                    class="btn btn-sm btn-danger"
                    @click="removeReply(r)"
                  >删除</button>
                </div>
                <MarkdownView :source="r.contentMarkdown" />
              </article>
            </div>
          </StateBlock>

          <div v-if="replies && replies.totalPages > 1" class="pager">
            <button class="btn" :disabled="replies.first" @click="loadReplies(page - 1)">上一页</button>
            <span class="muted mono">{{ replies.number + 1 }} / {{ replies.totalPages }}</span>
            <button class="btn" :disabled="replies.last" @click="loadReplies(page + 1)">下一页</button>
          </div>
        </section>

        <section class="card reply-form">
          <h2>发表回复</h2>
          <template v-if="auth.isLoggedIn()">
            <p v-if="replyToId" class="muted">正在回复 #{{ replyToId }} <button class="btn btn-sm btn-ghost" @click="replyToId = null">取消引用</button></p>
            <textarea class="textarea" v-model="form.contentMarkdown" placeholder="支持 Markdown，写下你的补充或建议…"></textarea>
            <div class="form-foot">
              <span class="muted mono">以 {{ auth.displayName }} 身份回复</span>
              <button class="btn btn-primary" :disabled="saving" @click="submitReply">{{ saving ? '发布中…' : '发布回复' }}</button>
            </div>
          </template>
          <p v-else class="muted">
            <RouterLink :to="`/login?redirect=${route.fullPath}`">登录</RouterLink> 后参与讨论。
          </p>
        </section>
      </template>
    </StateBlock>

    <div v-if="reportTarget" class="modal-mask" @click.self="closeReport">
      <div class="modal card">
        <h2>举报{{ reportTarget.label }}</h2>
        <label>举报原因</label>
        <select v-model="reportForm.reasonType" class="input">
          <option v-for="reason in reportReasons" :key="reason.value" :value="reason.value">{{ reason.label }}</option>
        </select>
        <label>补充说明</label>
        <textarea v-model="reportForm.reasonText" class="textarea" maxlength="1000" placeholder="可补充具体原因，最多 1000 字"></textarea>
        <div class="modal-foot">
          <button class="btn" @click="closeReport">取消</button>
          <button class="btn btn-primary" :disabled="reportSaving" @click="submitReport">{{ reportSaving ? '提交中…' : '提交举报' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 70px; }
.detail-state { margin-top: 16px; }
.thread-card { padding: 26px; margin-top: 14px; }
.thread-head { display: flex; justify-content: space-between; gap: 18px; align-items: start; margin-bottom: 18px; }
.thread-actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.thread-head h1 { margin: 10px 0 6px; font-size: clamp(26px, 4vw, 40px); line-height: 1.25; }
.thread-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.small { font-size: 12px; }
.tags { margin-bottom: 18px; }
.reply-section { margin-top: 28px; }
.reply-head, .form-foot, .pager { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.reply-list { display: flex; flex-direction: column; gap: 12px; }
.reply-card { padding: 18px 20px; }
.reply-info { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; color: var(--text-soft); font-size: 13px; }
.reply-info .btn { margin-left: auto; }
.pager { justify-content: center; margin-top: 18px; }
.reply-form { padding: 22px; margin-top: 24px; }
.reply-form h2 { margin-top: 0; }
.form-foot { margin-top: 12px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { width: min(520px, 100%); max-height: 90vh; overflow-y: auto; padding: 22px; display: flex; flex-direction: column; gap: 10px; }
.modal h2 { margin: 0 0 4px; font-size: 20px; }
.modal label { font-size: 13px; font-weight: 600; color: var(--text-soft); }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
@media (max-width: 640px) {
  .thread-head, .form-foot { flex-direction: column; align-items: stretch; }
  .thread-actions { justify-content: flex-start; }
  .reply-info { flex-wrap: wrap; }
  .reply-info .btn { margin-left: 0; }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { AdminOperationLog, ForumThread, Page, ThreadStatus } from '../../api/types'
import { toast } from '../../composables/useToast'

const statusOptions: Array<{ value: ThreadStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'NORMAL', label: '正常' },
  { value: 'PINNED', label: '置顶' },
  { value: 'FEATURED', label: '精选' },
  { value: 'LOCKED', label: '锁定' },
  { value: 'HIDDEN', label: '已隐藏' },
  { value: 'DELETED', label: '已删除' }
]

const statusLabels: Record<ThreadStatus, string> = {
  NORMAL: '正常',
  PINNED: '置顶',
  FEATURED: '精选',
  LOCKED: '锁定',
  HIDDEN: '已隐藏',
  DELETED: '已删除'
}

const q = ref('')
const author = ref('')
const status = ref<ThreadStatus | ''>('')
const reported = ref('')
const page = ref(0)
const size = 20
const loading = ref(false)
const detailLoading = ref(false)
const pageData = ref<Page<ForumThread> | null>(null)
const selectedIds = ref<number[]>([])
const detail = ref<ForumThread | null>(null)
const logs = ref<AdminOperationLog[]>([])

const threads = computed(() => pageData.value?.content ?? [])
const allSelected = computed(() => threads.value.length > 0 && threads.value.every(t => selectedIds.value.includes(t.id)))

async function load() {
  loading.value = true
  try {
    pageData.value = await adminApi.forumPosts({
      q: q.value.trim() || undefined,
      author: author.value.trim() || undefined,
      status: status.value || undefined,
      reported: reported.value === '' ? undefined : reported.value === 'true',
      page: page.value,
      size
    })
    const ids = new Set(threads.value.map(t => t.id))
    selectedIds.value = selectedIds.value.filter(id => ids.has(id))
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  load()
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : threads.value.map(t => t.id)
}

function statusClass(value: ThreadStatus) {
  if (value === 'HIDDEN') return 'badge-unknown'
  if (value === 'DELETED') return 'badge-down'
  return 'badge-up'
}

function fmt(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function askReason(title: string) {
  const reason = prompt(title)
  return reason === null ? null : reason.trim()
}

async function hide(item: ForumThread) {
  const reason = askReason(`隐藏帖子「${item.title}」的原因`)
  if (reason === null) return
  await adminApi.hideForumPost(item.id, reason)
  toast.success('帖子已隐藏')
  await load()
}

async function restore(item: ForumThread) {
  const reason = askReason(`恢复帖子「${item.title}」的原因`)
  if (reason === null) return
  await adminApi.restoreForumPost(item.id, reason)
  toast.success('帖子已恢复')
  await load()
}

async function remove(item: ForumThread) {
  if (!confirm(`软删除帖子「${item.title}」？`)) return
  const reason = askReason('删除原因')
  if (reason === null) return
  await adminApi.deleteForumPost(item.id, reason)
  toast.success('帖子已删除')
  await load()
}

async function batchHide() {
  if (!selectedIds.value.length) return
  const reason = askReason(`隐藏已选 ${selectedIds.value.length} 个帖子`)
  if (reason === null) return
  const result = await adminApi.batchHideForumPosts(selectedIds.value, reason)
  toast.success(`已隐藏 ${result.affected} 个帖子`)
  selectedIds.value = []
  await load()
}

async function batchDelete() {
  if (!selectedIds.value.length || !confirm(`软删除已选 ${selectedIds.value.length} 个帖子？`)) return
  const reason = askReason('批量删除原因')
  if (reason === null) return
  const result = await adminApi.batchDeleteForumPosts(selectedIds.value, reason)
  toast.success(`已删除 ${result.affected} 个帖子`)
  selectedIds.value = []
  await load()
}

async function openDetail(item: ForumThread) {
  detailLoading.value = true
  detail.value = item
  logs.value = []
  try {
    const [thread, operationLogs] = await Promise.all([
      adminApi.forumPost(item.id),
      adminApi.forumPostLogs(item.id)
    ])
    detail.value = thread
    logs.value = operationLogs
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detail.value = null
  logs.value = []
}

function previousPage() {
  if (!pageData.value?.first) {
    page.value -= 1
    load()
  }
}

function nextPage() {
  if (!pageData.value?.last) {
    page.value += 1
    load()
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="head">
      <div>
        <h1 class="section-title">论坛帖子管理</h1>
        <p class="muted">查看、筛选和处理论坛帖子。</p>
      </div>
    </div>

    <div class="toolbar filters">
      <input v-model="q" class="input grow" placeholder="搜索标题、内容、标签" @keyup.enter="applyFilters" />
      <input v-model="author" class="input" placeholder="作者用户名/昵称" @keyup.enter="applyFilters" />
      <select v-model="status" class="input">
        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model="reported" class="input">
        <option value="">全部举报状态</option>
        <option value="true">有举报</option>
        <option value="false">无举报</option>
      </select>
      <button class="btn btn-primary" @click="applyFilters">筛选</button>
    </div>

    <div class="batch-bar">
      <span class="muted">已选 {{ selectedIds.length }} 项</span>
      <button class="btn btn-sm" :disabled="!selectedIds.length" @click="batchHide">批量隐藏</button>
      <button class="btn btn-sm btn-danger" :disabled="!selectedIds.length" @click="batchDelete">批量删除</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!threads.length" class="muted">没有匹配的帖子。</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th><input type="checkbox" :checked="allSelected" @change="toggleAll" /></th>
            <th>帖子</th>
            <th>作者</th>
            <th>状态</th>
            <th>互动</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in threads" :key="t.id">
            <td><input v-model="selectedIds" type="checkbox" :value="t.id" /></td>
            <td>
              <button class="link-btn" @click="openDetail(t)">{{ t.title }}</button>
              <div class="muted mono">#{{ t.id }}</div>
            </td>
            <td class="mono">#{{ t.authorId }}</td>
            <td><span class="badge" :class="statusClass(t.status)">{{ statusLabels[t.status] }}</span></td>
            <td class="metrics">
              <span>回 {{ t.replyCount }}</span>
              <span>赞 {{ t.likeCount }}</span>
              <span>藏 {{ t.favoriteCount }}</span>
              <span>举 {{ t.reportCount }}</span>
            </td>
            <td>{{ fmt(t.createdAt) }}</td>
            <td class="actions">
              <button class="btn btn-sm" @click="openDetail(t)">详情</button>
              <button v-if="t.status !== 'HIDDEN' && t.status !== 'DELETED'" class="btn btn-sm" @click="hide(t)">隐藏</button>
              <button v-if="t.status === 'HIDDEN' || t.status === 'DELETED'" class="btn btn-sm" @click="restore(t)">恢复</button>
              <button v-if="t.status !== 'DELETED'" class="btn btn-sm btn-danger" @click="remove(t)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="pageData" class="pager">
      <button class="btn btn-sm" :disabled="pageData.first" @click="previousPage">上一页</button>
      <span class="muted">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页，共 {{ pageData.totalElements }} 条</span>
      <button class="btn btn-sm" :disabled="pageData.last" @click="nextPage">下一页</button>
    </div>

    <div v-if="detail" class="modal-mask" @click.self="closeDetail">
      <div class="modal card">
        <div class="modal-head">
          <div>
            <h2>{{ detail.title }}</h2>
            <p class="muted">作者 #{{ detail.authorId }} · {{ fmt(detail.createdAt) }}</p>
          </div>
          <button class="btn btn-sm" @click="closeDetail">关闭</button>
        </div>
        <p v-if="detailLoading" class="muted">加载详情中…</p>
        <div v-else>
          <div class="detail-meta">
            <span class="badge" :class="statusClass(detail.status)">{{ statusLabels[detail.status] }}</span>
            <span>浏览 {{ detail.viewCount }}</span>
            <span>回复 {{ detail.replyCount }}</span>
            <span>点赞 {{ detail.likeCount }}</span>
            <span>收藏 {{ detail.favoriteCount }}</span>
            <span>举报 {{ detail.reportCount }}</span>
          </div>
          <pre class="content-preview">{{ detail.contentMarkdown }}</pre>
          <h3>操作记录</h3>
          <p v-if="!logs.length" class="muted">暂无操作记录。</p>
          <ul v-else class="log-list">
            <li v-for="log in logs" :key="log.id">
              <span class="mono">{{ log.action }}</span>
              <span>{{ log.operatorUsername }}</span>
              <span class="muted">{{ fmt(log.createdAt) }}</span>
              <p v-if="log.detail" class="muted">{{ log.detail }}</p>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 16px; }
.head p { margin: 0; }
.filters { align-items: stretch; }
.filters .input { min-width: 160px; }
.filters select.input { max-width: 180px; }
.batch-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.table { width: 100%; border-collapse: collapse; min-width: 920px; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }
.table tbody tr:last-child td { border-bottom: none; }
.link-btn { border: 0; background: transparent; color: var(--primary); padding: 0; cursor: pointer; font: inherit; text-align: left; }
.link-btn:hover { color: var(--primary-strong); text-decoration: underline; text-underline-offset: 3px; }
.metrics { display: flex; gap: 8px; flex-wrap: wrap; font-family: var(--font-mono); font-size: 12px; color: var(--text-soft); }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { width: min(920px, 100%); max-height: 90vh; overflow-y: auto; padding: 22px; }
.modal-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--border); padding-bottom: 12px; margin-bottom: 14px; }
.modal h2 { margin: 0 0 4px; font-size: 20px; }
.detail-meta { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; color: var(--text-soft); font-size: 13px; }
.content-preview { white-space: pre-wrap; background: var(--bg-inset); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 14px; max-height: 320px; overflow: auto; }
.log-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.log-list li { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; }
.log-list li > span { margin-right: 10px; }
.log-list p { margin: 6px 0 0; }
@media (max-width: 720px) {
  .filters .input, .filters select.input { max-width: none; width: 100%; }
  .modal-head { flex-direction: column; }
}
</style>

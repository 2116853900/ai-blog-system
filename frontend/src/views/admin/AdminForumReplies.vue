<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { AdminOperationLog, ForumReply, Page, ReplyStatus } from '../../api/types'
import { toast } from '../../composables/useToast'

const statusOptions: Array<{ value: ReplyStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'NORMAL', label: '正常' },
  { value: 'HIDDEN', label: '已隐藏' },
  { value: 'DELETED', label: '已删除' }
]

const statusLabels: Record<ReplyStatus, string> = {
  NORMAL: '正常',
  HIDDEN: '已隐藏',
  DELETED: '已删除'
}

const threadId = ref('')
const author = ref('')
const status = ref<ReplyStatus | ''>('')
const reported = ref('')
const page = ref(0)
const size = 20
const loading = ref(false)
const detailLoading = ref(false)
const pageData = ref<Page<ForumReply> | null>(null)
const selectedIds = ref<number[]>([])
const detail = ref<ForumReply | null>(null)
const logs = ref<AdminOperationLog[]>([])

const replies = computed(() => pageData.value?.content ?? [])
const allSelected = computed(() => replies.value.length > 0 && replies.value.every(r => selectedIds.value.includes(r.id)))

async function load() {
  loading.value = true
  try {
    const parsedThreadId = threadId.value.trim() ? Number(threadId.value.trim()) : undefined
    pageData.value = await adminApi.forumReplies({
      threadId: Number.isFinite(parsedThreadId) ? parsedThreadId : undefined,
      author: author.value.trim() || undefined,
      status: status.value || undefined,
      reported: reported.value === '' ? undefined : reported.value === 'true',
      page: page.value,
      size
    })
    const ids = new Set(replies.value.map(r => r.id))
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
  selectedIds.value = allSelected.value ? [] : replies.value.map(r => r.id)
}

function statusClass(value: ReplyStatus) {
  if (value === 'DELETED') return 'badge-down'
  if (value === 'HIDDEN') return 'badge-unknown'
  return 'badge-up'
}

function fmt(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function askReason(title: string) {
  const reason = prompt(title)
  return reason === null ? null : reason.trim()
}

async function hide(item: ForumReply) {
  const reason = askReason(`隐藏回复 #${item.id} 的原因`)
  if (reason === null) return
  await adminApi.hideForumReply(item.id, reason)
  toast.success('回复已隐藏')
  await load()
}

async function restore(item: ForumReply) {
  const reason = askReason(`恢复回复 #${item.id} 的原因`)
  if (reason === null) return
  await adminApi.restoreForumReply(item.id, reason)
  toast.success('回复已恢复')
  await load()
}

async function remove(item: ForumReply) {
  if (!confirm(`软删除回复 #${item.id}？`)) return
  const reason = askReason('删除原因')
  if (reason === null) return
  await adminApi.deleteForumReply(item.id, reason)
  toast.success('回复已删除')
  await load()
}

async function batchHide() {
  if (!selectedIds.value.length) return
  const reason = askReason(`隐藏已选 ${selectedIds.value.length} 条回复`)
  if (reason === null) return
  const result = await adminApi.batchHideForumReplies(selectedIds.value, reason)
  toast.success(`已隐藏 ${result.affected} 条回复`)
  selectedIds.value = []
  await load()
}

async function batchDelete() {
  if (!selectedIds.value.length || !confirm(`软删除已选 ${selectedIds.value.length} 条回复？`)) return
  const reason = askReason('批量删除原因')
  if (reason === null) return
  const result = await adminApi.batchDeleteForumReplies(selectedIds.value, reason)
  toast.success(`已删除 ${result.affected} 条回复`)
  selectedIds.value = []
  await load()
}

async function openDetail(item: ForumReply) {
  detailLoading.value = true
  detail.value = item
  logs.value = []
  try {
    const [reply, operationLogs] = await Promise.all([
      adminApi.forumReply(item.id),
      adminApi.forumReplyLogs(item.id)
    ])
    detail.value = reply
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
        <h1 class="section-title">论坛回复管理</h1>
        <p class="muted">按帖子、作者和状态处理论坛回复。</p>
      </div>
    </div>

    <div class="toolbar filters">
      <input v-model="threadId" class="input" placeholder="帖子 ID" @keyup.enter="applyFilters" />
      <input v-model="author" class="input grow" placeholder="作者用户名/昵称" @keyup.enter="applyFilters" />
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
    <p v-else-if="!replies.length" class="muted">没有匹配的回复。</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th><input type="checkbox" :checked="allSelected" @change="toggleAll" /></th>
            <th>内容</th>
            <th>帖子</th>
            <th>作者</th>
            <th>状态</th>
            <th>互动</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in replies" :key="r.id">
            <td><input v-model="selectedIds" type="checkbox" :value="r.id" /></td>
            <td>
              <button class="link-btn" @click="openDetail(r)">{{ r.contentMarkdown }}</button>
              <div class="muted mono">#{{ r.id }} · {{ r.floorNumber }} 楼</div>
            </td>
            <td class="mono">#{{ r.threadId }}</td>
            <td class="mono">#{{ r.authorId }}</td>
            <td><span class="badge" :class="statusClass(r.status)">{{ statusLabels[r.status] }}</span></td>
            <td class="metrics">
              <span>赞 {{ r.likeCount }}</span>
              <span>举 {{ r.reportCount }}</span>
            </td>
            <td>{{ fmt(r.createdAt) }}</td>
            <td class="actions">
              <button class="btn btn-sm" @click="openDetail(r)">详情</button>
              <button v-if="r.status !== 'HIDDEN' && r.status !== 'DELETED'" class="btn btn-sm" @click="hide(r)">隐藏</button>
              <button v-if="r.status === 'HIDDEN' || r.status === 'DELETED'" class="btn btn-sm" @click="restore(r)">恢复</button>
              <button v-if="r.status !== 'DELETED'" class="btn btn-sm btn-danger" @click="remove(r)">删除</button>
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
            <h2>回复 #{{ detail.id }}</h2>
            <p class="muted">帖子 #{{ detail.threadId }} · 作者 #{{ detail.authorId }} · {{ fmt(detail.createdAt) }}</p>
          </div>
          <button class="btn btn-sm" @click="closeDetail">关闭</button>
        </div>
        <p v-if="detailLoading" class="muted">加载详情中…</p>
        <div v-else>
          <div class="detail-meta">
            <span class="badge" :class="statusClass(detail.status)">{{ statusLabels[detail.status] }}</span>
            <span>{{ detail.floorNumber }} 楼</span>
            <span>点赞 {{ detail.likeCount }}</span>
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
.filters .input { min-width: 150px; }
.filters select.input { max-width: 180px; }
.batch-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.table { width: 100%; border-collapse: collapse; min-width: 980px; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }
.table tbody tr:last-child td { border-bottom: none; }
.link-btn { border: 0; background: transparent; color: var(--primary); padding: 0; cursor: pointer; font: inherit; text-align: left; max-width: 420px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.link-btn:hover { color: var(--primary-strong); text-decoration: underline; text-underline-offset: 3px; }
.metrics { display: flex; gap: 8px; flex-wrap: wrap; font-family: var(--font-mono); font-size: 12px; color: var(--text-soft); }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { width: min(820px, 100%); max-height: 90vh; overflow-y: auto; padding: 22px; }
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

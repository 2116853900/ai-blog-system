<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import type { AdminOperationLog, ContentReport, ContentReportTarget, Page, ReportReasonType, ReportStatus, ReportTargetType } from '../../api/types'
import { toast } from '../../composables/useToast'

const targetOptions: Array<{ value: ReportTargetType | ''; label: string }> = [
  { value: '', label: '全部对象' },
  { value: 'POST', label: '帖子' },
  { value: 'REPLY', label: '回复' },
  { value: 'COMMENT', label: '评论' }
]

const reasonOptions: Array<{ value: ReportReasonType | ''; label: string }> = [
  { value: '', label: '全部原因' },
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'ABUSE', label: '辱骂攻击' },
  { value: 'PORN', label: '色情低俗' },
  { value: 'POLITICS', label: '敏感内容' },
  { value: 'ILLEGAL', label: '违法违规' },
  { value: 'COPYRIGHT', label: '侵权' },
  { value: 'OTHER', label: '其他' }
]

const statusOptions: Array<{ value: ReportStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '举报成立' },
  { value: 'REJECTED', label: '举报不成立' },
  { value: 'CLOSED', label: '已关闭' }
]

const targetLabels: Record<ReportTargetType, string> = { POST: '帖子', REPLY: '回复', COMMENT: '评论' }
const reasonLabels: Record<ReportReasonType, string> = {
  SPAM: '垃圾广告',
  ABUSE: '辱骂攻击',
  PORN: '色情低俗',
  POLITICS: '敏感内容',
  ILLEGAL: '违法违规',
  COPYRIGHT: '侵权',
  OTHER: '其他'
}
const statusLabels: Record<ReportStatus, string> = {
  PENDING: '待审核',
  APPROVED: '举报成立',
  REJECTED: '举报不成立',
  CLOSED: '已关闭'
}

const targetType = ref<ReportTargetType | ''>('')
const reasonType = ref<ReportReasonType | ''>('')
const status = ref<ReportStatus | ''>('PENDING')
const page = ref(0)
const size = 20
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const pageData = ref<Page<ContentReport> | null>(null)
const detail = ref<ContentReport | null>(null)
const logs = ref<AdminOperationLog[]>([])
const currentTarget = ref<ContentReportTarget | null>(null)

const reviewForm = reactive({
  reviewNote: '',
  hideContent: true,
  banTargetAuthor: false,
  banReason: '',
  banEndTime: ''
})

const reports = computed(() => pageData.value?.content ?? [])

async function load() {
  loading.value = true
  try {
    pageData.value = await adminApi.reports({
      targetType: targetType.value || undefined,
      reasonType: reasonType.value || undefined,
      status: status.value || undefined,
      page: page.value,
      size
    })
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  load()
}

function statusClass(value: ReportStatus) {
  if (value === 'APPROVED') return 'badge-up'
  if (value === 'REJECTED') return 'badge-down'
  return 'badge-unknown'
}

function fmt(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function normalizeTime(value: string) {
  if (!value.trim()) return undefined
  const parsed = new Date(value.trim())
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

function resetReviewForm(report?: ContentReport) {
  reviewForm.reviewNote = ''
  reviewForm.hideContent = true
  reviewForm.banTargetAuthor = false
  reviewForm.banReason = report ? `${targetLabels[report.targetType]}举报成立` : ''
  reviewForm.banEndTime = ''
}

async function openDetail(report: ContentReport) {
  detailLoading.value = true
  detail.value = report
  logs.value = []
  currentTarget.value = null
  resetReviewForm(report)
  try {
    const [reportDetail, operationLogs, target] = await Promise.all([
      adminApi.report(report.id),
      adminApi.reportLogs(report.id),
      adminApi.reportTarget(report.id)
    ])
    detail.value = reportDetail
    logs.value = operationLogs
    currentTarget.value = target
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detail.value = null
  logs.value = []
  currentTarget.value = null
  resetReviewForm()
}

async function refreshLogs() {
  if (!detail.value) return
  logs.value = await adminApi.reportLogs(detail.value.id)
}

async function refreshCurrentTarget() {
  if (!detail.value) return
  currentTarget.value = await adminApi.reportTarget(detail.value.id)
}

function reviewBody() {
  return {
    reviewNote: reviewForm.reviewNote.trim() || undefined,
    hideContent: reviewForm.hideContent,
    banTargetAuthor: reviewForm.banTargetAuthor,
    banReason: reviewForm.banReason.trim() || undefined,
    banEndTime: normalizeTime(reviewForm.banEndTime)
  }
}

async function approve() {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await adminApi.approveReport(detail.value.id, reviewBody())
    await refreshLogs()
    await refreshCurrentTarget()
    toast.success('举报已通过')
    await load()
  } finally {
    saving.value = false
  }
}

async function reject() {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await adminApi.rejectReport(detail.value.id, { reviewNote: reviewForm.reviewNote.trim() || undefined })
    await refreshLogs()
    await refreshCurrentTarget()
    toast.success('举报已驳回')
    await load()
  } finally {
    saving.value = false
  }
}

async function closeReport() {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await adminApi.closeReport(detail.value.id, { reviewNote: reviewForm.reviewNote.trim() || undefined })
    await refreshLogs()
    await refreshCurrentTarget()
    toast.success('举报已关闭')
    await load()
  } finally {
    saving.value = false
  }
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
        <h1 class="section-title">举报审核</h1>
        <p class="muted">统一处理帖子、回复和评论举报。</p>
      </div>
    </div>

    <div class="toolbar filters">
      <select v-model="targetType" class="input">
        <option v-for="item in targetOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model="reasonType" class="input">
        <option v-for="item in reasonOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model="status" class="input">
        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <button class="btn btn-primary" @click="applyFilters">筛选</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!reports.length" class="muted">没有匹配的举报。</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>对象</th>
            <th>原因</th>
            <th>状态</th>
            <th>举报人</th>
            <th>被举报作者</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reports" :key="r.id">
            <td>
              <button class="link-btn" @click="openDetail(r)">{{ targetLabels[r.targetType] }} #{{ r.targetId }}</button>
              <div class="muted mono">举报 #{{ r.id }}</div>
            </td>
            <td>{{ reasonLabels[r.reasonType] }}</td>
            <td><span class="badge" :class="statusClass(r.status)">{{ statusLabels[r.status] }}</span></td>
            <td class="mono">#{{ r.reporterId }}</td>
            <td class="mono">{{ r.targetAuthorId ? `#${r.targetAuthorId}` : '-' }}</td>
            <td>{{ fmt(r.createdAt) }}</td>
            <td><button class="btn btn-sm" @click="openDetail(r)">详情</button></td>
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
            <h2>{{ targetLabels[detail.targetType] }} #{{ detail.targetId }}</h2>
            <p class="muted">举报 #{{ detail.id }} · {{ fmt(detail.createdAt) }}</p>
          </div>
          <button class="btn btn-sm" @click="closeDetail">关闭</button>
        </div>

        <p v-if="detailLoading" class="muted">加载详情中…</p>
        <div v-else>
          <div class="detail-meta">
            <span class="badge" :class="statusClass(detail.status)">{{ statusLabels[detail.status] }}</span>
            <span>{{ reasonLabels[detail.reasonType] }}</span>
            <span>举报人 #{{ detail.reporterId }}</span>
            <span>作者 {{ detail.targetAuthorId ? `#${detail.targetAuthorId}` : '-' }}</span>
          </div>

          <div v-if="detail.reasonText" class="note-box">
            <span class="muted">举报说明</span>
            <p>{{ detail.reasonText }}</p>
          </div>

          <h3>内容快照</h3>
          <pre class="content-preview">{{ detail.contentSnapshot }}</pre>

          <h3>当前内容</h3>
          <div v-if="!currentTarget" class="note-box">
            <span class="muted">当前内容加载失败</span>
          </div>
          <div v-else-if="!currentTarget.exists" class="note-box">
            <span class="muted">当前内容不存在或已被删除。</span>
          </div>
          <div v-else class="current-box">
            <div class="detail-meta">
              <span v-if="currentTarget.status">状态 {{ currentTarget.status }}</span>
              <span v-if="currentTarget.authorId">作者 #{{ currentTarget.authorId }}</span>
              <span v-if="currentTarget.authorName">作者 {{ currentTarget.authorName }}</span>
              <span v-if="currentTarget.refType">关联 {{ currentTarget.refType }}{{ currentTarget.refId ? ` #${currentTarget.refId}` : '' }}</span>
              <span v-if="currentTarget.updatedAt">更新 {{ fmt(currentTarget.updatedAt) }}</span>
            </div>
            <strong v-if="currentTarget.title">{{ currentTarget.title }}</strong>
            <pre class="content-preview">{{ currentTarget.content || '-' }}</pre>
          </div>

          <div v-if="detail.status === 'PENDING'" class="review-box">
            <h3>审核操作</h3>
            <label>审核备注</label>
            <textarea v-model="reviewForm.reviewNote" class="textarea" maxlength="1000" placeholder="记录审核依据"></textarea>
            <label class="check-row">
              <input v-model="reviewForm.hideContent" type="checkbox" />
              举报成立时隐藏对应内容
            </label>
            <label v-if="detail.targetAuthorId" class="check-row">
              <input v-model="reviewForm.banTargetAuthor" type="checkbox" />
              举报成立时封禁被举报作者
            </label>
            <div v-if="reviewForm.banTargetAuthor" class="two-cols">
              <div>
                <label>封禁原因</label>
                <input v-model="reviewForm.banReason" class="input" />
              </div>
              <div>
                <label>封禁结束时间</label>
                <input v-model="reviewForm.banEndTime" class="input" placeholder="留空为永久" />
              </div>
            </div>
            <div class="review-actions">
              <button class="btn btn-primary" :disabled="saving" @click="approve">通过</button>
              <button class="btn btn-danger" :disabled="saving" @click="reject">驳回</button>
              <button class="btn" :disabled="saving" @click="closeReport">关闭</button>
            </div>
          </div>

          <div v-else class="note-box">
            <span class="muted">审核结果</span>
            <p>{{ detail.reviewResult || statusLabels[detail.status] }} · {{ detail.reviewerUsername || '-' }} · {{ fmt(detail.reviewedAt) }}</p>
            <p v-if="detail.reviewNote">{{ detail.reviewNote }}</p>
          </div>

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
.filters .input { min-width: 160px; max-width: 190px; }
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.table { width: 100%; border-collapse: collapse; min-width: 900px; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }
.table tbody tr:last-child td { border-bottom: none; }
.link-btn { border: 0; background: transparent; color: var(--primary); padding: 0; cursor: pointer; font: inherit; text-align: left; }
.link-btn:hover { color: var(--primary-strong); text-decoration: underline; text-underline-offset: 3px; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { width: min(920px, 100%); max-height: 90vh; overflow-y: auto; padding: 22px; }
.modal-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--border); padding-bottom: 12px; margin-bottom: 14px; }
.modal h2 { margin: 0 0 4px; font-size: 20px; }
.detail-meta { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; color: var(--text-soft); font-size: 13px; }
.note-box, .review-box { background: var(--bg-inset); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 12px; margin-bottom: 14px; }
.note-box p { margin: 4px 0 0; }
.content-preview { white-space: pre-wrap; background: var(--bg-inset); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 14px; max-height: 320px; overflow: auto; }
.review-box { display: flex; flex-direction: column; gap: 9px; }
.review-box h3 { margin: 0; }
.review-box label { font-size: 13px; font-weight: 600; color: var(--text-soft); }
.check-row { display: flex; align-items: center; gap: 8px; }
.two-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.review-actions { display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.current-box { margin-bottom: 14px; }
.log-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.log-list li { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; }
.log-list li > span { margin-right: 10px; }
.log-list p { margin: 6px 0 0; }
@media (max-width: 720px) {
  .filters .input { max-width: none; width: 100%; }
  .modal-head { flex-direction: column; }
  .two-cols { grid-template-columns: 1fr; }
}
</style>

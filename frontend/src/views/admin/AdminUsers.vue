<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type {
  AdminForumUser,
  AdminOperationLog,
  ContentReport,
  ForumReply,
  ForumThread,
  Page,
  ReplyStatus,
  ReportReasonType,
  ReportStatus,
  ReportTargetType,
  ThreadStatus,
  UserStatus
} from '../../api/types'
import { toast } from '../../composables/useToast'

type DetailTab = 'overview' | 'threads' | 'replies' | 'reports' | 'reported'

const statusOptions: Array<{ value: UserStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'ACTIVE', label: '正常' },
  { value: 'BANNED', label: '已封禁' },
  { value: 'INACTIVE', label: '未激活' }
]

const detailTabs: Array<{ value: DetailTab; label: string }> = [
  { value: 'overview', label: '概览' },
  { value: 'threads', label: '发帖' },
  { value: 'replies', label: '回复' },
  { value: 'reports', label: '提交举报' },
  { value: 'reported', label: '被举报' }
]

const statusLabels: Record<UserStatus, string> = {
  ACTIVE: '正常',
  BANNED: '已封禁',
  INACTIVE: '未激活'
}

const threadStatusLabels: Record<ThreadStatus, string> = {
  NORMAL: '正常',
  PINNED: '置顶',
  FEATURED: '精选',
  LOCKED: '锁定',
  HIDDEN: '已隐藏',
  DELETED: '已删除'
}

const replyStatusLabels: Record<ReplyStatus, string> = {
  NORMAL: '正常',
  HIDDEN: '已隐藏',
  DELETED: '已删除'
}

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
const reportStatusLabels: Record<ReportStatus, string> = {
  PENDING: '待审核',
  APPROVED: '举报成立',
  REJECTED: '举报不成立',
  CLOSED: '已关闭'
}

const q = ref('')
const status = ref<UserStatus | ''>('')
const page = ref(0)
const size = 20
const historySize = 10
const loading = ref(false)
const detailLoading = ref(false)
const historyLoading = ref(false)
const pageData = ref<Page<AdminForumUser> | null>(null)
const detail = ref<AdminForumUser | null>(null)
const logs = ref<AdminOperationLog[]>([])
const detailTab = ref<DetailTab>('overview')

const threadHistory = ref<Page<ForumThread> | null>(null)
const replyHistory = ref<Page<ForumReply> | null>(null)
const submittedReports = ref<Page<ContentReport> | null>(null)
const receivedReports = ref<Page<ContentReport> | null>(null)
const threadPage = ref(0)
const replyPage = ref(0)
const submittedReportPage = ref(0)
const receivedReportPage = ref(0)

const users = computed(() => pageData.value?.content ?? [])
const threads = computed(() => threadHistory.value?.content ?? [])
const replies = computed(() => replyHistory.value?.content ?? [])
const reports = computed(() => submittedReports.value?.content ?? [])
const reported = computed(() => receivedReports.value?.content ?? [])

async function load() {
  loading.value = true
  try {
    pageData.value = await adminApi.forumUsers({
      q: q.value.trim() || undefined,
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

function statusClass(value: UserStatus) {
  if (value === 'BANNED') return 'badge-down'
  if (value === 'INACTIVE') return 'badge-unknown'
  return 'badge-up'
}

function contentStatusClass(value: ThreadStatus | ReplyStatus) {
  if (value === 'HIDDEN' || value === 'DELETED') return 'badge-down'
  if (value === 'LOCKED') return 'badge-unknown'
  return 'badge-up'
}

function reportStatusClass(value: ReportStatus) {
  if (value === 'APPROVED') return 'badge-up'
  if (value === 'REJECTED') return 'badge-down'
  return 'badge-unknown'
}

function fmt(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function normalizeTime(value: string | null) {
  if (!value) return undefined
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

function resetHistory() {
  detailTab.value = 'overview'
  threadHistory.value = null
  replyHistory.value = null
  submittedReports.value = null
  receivedReports.value = null
  threadPage.value = 0
  replyPage.value = 0
  submittedReportPage.value = 0
  receivedReportPage.value = 0
}

async function ban(user: AdminForumUser) {
  const reason = prompt(`封禁用户「${user.username}」的原因`)
  if (reason === null) return
  const until = prompt('封禁结束时间，可留空永久封禁；示例：2026-06-30 18:00')
  if (until === null) return
  await adminApi.banForumUser(user.id, {
    reason: reason.trim(),
    banEndTime: normalizeTime(until.trim())
  })
  toast.success('用户已封禁')
  await load()
  if (detail.value?.id === user.id) await openDetail(user)
}

async function unban(user: AdminForumUser) {
  if (!confirm(`解封用户「${user.username}」？`)) return
  await adminApi.unbanForumUser(user.id)
  toast.success('用户已解封')
  await load()
  if (detail.value?.id === user.id) await openDetail(user)
}

async function openDetail(user: AdminForumUser) {
  detailLoading.value = true
  detail.value = user
  logs.value = []
  resetHistory()
  try {
    const [freshUser, operationLogs] = await Promise.all([
      adminApi.forumUser(user.id),
      adminApi.forumUserLogs(user.id)
    ])
    detail.value = freshUser
    logs.value = operationLogs
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detail.value = null
  logs.value = []
  resetHistory()
}

async function selectDetailTab(tab: DetailTab) {
  detailTab.value = tab
  if (tab !== 'overview') await loadHistory(tab)
}

async function loadHistory(tab: DetailTab) {
  if (!detail.value || tab === 'overview') return
  historyLoading.value = true
  try {
    if (tab === 'threads') {
      threadHistory.value = await adminApi.forumUserThreads(detail.value.id, { page: threadPage.value, size: historySize })
    } else if (tab === 'replies') {
      replyHistory.value = await adminApi.forumUserReplies(detail.value.id, { page: replyPage.value, size: historySize })
    } else if (tab === 'reports') {
      submittedReports.value = await adminApi.forumUserReports(detail.value.id, { page: submittedReportPage.value, size: historySize })
    } else if (tab === 'reported') {
      receivedReports.value = await adminApi.forumUserReported(detail.value.id, { page: receivedReportPage.value, size: historySize })
    }
  } finally {
    historyLoading.value = false
  }
}

function historyData(tab: DetailTab) {
  if (tab === 'threads') return threadHistory.value
  if (tab === 'replies') return replyHistory.value
  if (tab === 'reports') return submittedReports.value
  if (tab === 'reported') return receivedReports.value
  return null
}

function moveHistoryPage(tab: DetailTab, direction: -1 | 1) {
  const data = historyData(tab)
  if (!data || (direction < 0 && data.first) || (direction > 0 && data.last)) return
  if (tab === 'threads') threadPage.value += direction
  if (tab === 'replies') replyPage.value += direction
  if (tab === 'reports') submittedReportPage.value += direction
  if (tab === 'reported') receivedReportPage.value += direction
  loadHistory(tab)
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
        <h1 class="section-title">用户管理</h1>
        <p class="muted">查看论坛用户状态并执行封禁、解封。</p>
      </div>
    </div>

    <div class="toolbar filters">
      <input v-model="q" class="input grow" placeholder="搜索用户名、昵称、邮箱" @keyup.enter="applyFilters" />
      <select v-model="status" class="input">
        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <button class="btn btn-primary" @click="applyFilters">筛选</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!users.length" class="muted">没有匹配的用户。</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>用户</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>封禁结束</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>
              <button class="link-btn" @click="openDetail(u)">{{ u.nickname || u.username }}</button>
              <div class="muted mono">#{{ u.id }} · {{ u.username }}</div>
            </td>
            <td>{{ u.email }}</td>
            <td class="mono">{{ u.role }}</td>
            <td><span class="badge" :class="statusClass(u.status)">{{ statusLabels[u.status] }}</span></td>
            <td>{{ u.status === 'BANNED' ? fmt(u.banEndTime) : '-' }}</td>
            <td>{{ fmt(u.createdAt) }}</td>
            <td class="actions">
              <button class="btn btn-sm" @click="openDetail(u)">详情</button>
              <button v-if="u.status !== 'BANNED'" class="btn btn-sm btn-danger" @click="ban(u)">封禁</button>
              <button v-else class="btn btn-sm" @click="unban(u)">解封</button>
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
            <h2>{{ detail.nickname || detail.username }}</h2>
            <p class="muted">{{ detail.username }} · {{ detail.email }}</p>
          </div>
          <button class="btn btn-sm" @click="closeDetail">关闭</button>
        </div>

        <p v-if="detailLoading" class="muted">加载详情中…</p>
        <div v-else>
          <div class="detail-tabs">
            <button
              v-for="tab in detailTabs"
              :key="tab.value"
              class="tab-btn"
              :class="{ active: detailTab === tab.value }"
              @click="selectDetailTab(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>

          <div v-if="detailTab === 'overview'">
            <div class="detail-grid">
              <div><span class="muted">状态</span><strong><span class="badge" :class="statusClass(detail.status)">{{ statusLabels[detail.status] }}</span></strong></div>
              <div><span class="muted">角色</span><strong class="mono">{{ detail.role }}</strong></div>
              <div><span class="muted">等级</span><strong>{{ detail.level }}</strong></div>
              <div><span class="muted">经验</span><strong>{{ detail.experiencePoints }}</strong></div>
              <div><span class="muted">注册</span><strong>{{ fmt(detail.createdAt) }}</strong></div>
              <div><span class="muted">最后登录</span><strong>{{ fmt(detail.lastLoginAt) }}</strong></div>
            </div>
            <div v-if="detail.status === 'BANNED'" class="ban-box">
              <div><span class="muted">封禁原因</span><p>{{ detail.banReason || '-' }}</p></div>
              <div><span class="muted">封禁时间</span><p>{{ fmt(detail.banStartTime) }} 至 {{ detail.banEndTime ? fmt(detail.banEndTime) : '永久' }}</p></div>
              <div><span class="muted">操作人</span><p>{{ detail.banOperatorUsername || '-' }}</p></div>
            </div>
            <p v-if="detail.bio" class="bio">{{ detail.bio }}</p>
            <div class="detail-actions">
              <button v-if="detail.status !== 'BANNED'" class="btn btn-sm btn-danger" @click="ban(detail)">封禁</button>
              <button v-else class="btn btn-sm" @click="unban(detail)">解封</button>
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

          <div v-else-if="detailTab === 'threads'" class="history-panel">
            <p v-if="historyLoading" class="muted">加载发帖记录中…</p>
            <p v-else-if="!threads.length" class="muted">暂无发帖记录。</p>
            <div v-else class="table-wrap">
              <table class="table history-table">
                <thead>
                  <tr>
                    <th>帖子</th>
                    <th>状态</th>
                    <th>回复</th>
                    <th>举报</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in threads" :key="item.id">
                    <td>
                      <span>{{ item.title }}</span>
                      <div class="muted mono">#{{ item.id }} · 板块 #{{ item.categoryId }}</div>
                    </td>
                    <td><span class="badge" :class="contentStatusClass(item.status)">{{ threadStatusLabels[item.status] }}</span></td>
                    <td>{{ item.replyCount }}</td>
                    <td>{{ item.reportCount }}</td>
                    <td>{{ fmt(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="threadHistory" class="pager compact">
              <button class="btn btn-sm" :disabled="threadHistory.first" @click="moveHistoryPage('threads', -1)">上一页</button>
              <span class="muted">第 {{ threadHistory.number + 1 }} / {{ Math.max(threadHistory.totalPages, 1) }} 页，共 {{ threadHistory.totalElements }} 条</span>
              <button class="btn btn-sm" :disabled="threadHistory.last" @click="moveHistoryPage('threads', 1)">下一页</button>
            </div>
          </div>

          <div v-else-if="detailTab === 'replies'" class="history-panel">
            <p v-if="historyLoading" class="muted">加载回复记录中…</p>
            <p v-else-if="!replies.length" class="muted">暂无回复记录。</p>
            <div v-else class="table-wrap">
              <table class="table history-table">
                <thead>
                  <tr>
                    <th>回复</th>
                    <th>状态</th>
                    <th>举报</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in replies" :key="item.id">
                    <td>
                      <span class="mono">帖子 #{{ item.threadId }} · {{ item.floorNumber }} 楼</span>
                      <p class="muted line-clamp">{{ item.contentMarkdown }}</p>
                    </td>
                    <td><span class="badge" :class="contentStatusClass(item.status)">{{ replyStatusLabels[item.status] }}</span></td>
                    <td>{{ item.reportCount }}</td>
                    <td>{{ fmt(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="replyHistory" class="pager compact">
              <button class="btn btn-sm" :disabled="replyHistory.first" @click="moveHistoryPage('replies', -1)">上一页</button>
              <span class="muted">第 {{ replyHistory.number + 1 }} / {{ Math.max(replyHistory.totalPages, 1) }} 页，共 {{ replyHistory.totalElements }} 条</span>
              <button class="btn btn-sm" :disabled="replyHistory.last" @click="moveHistoryPage('replies', 1)">下一页</button>
            </div>
          </div>

          <div v-else-if="detailTab === 'reports'" class="history-panel">
            <p v-if="historyLoading" class="muted">加载举报记录中…</p>
            <p v-else-if="!reports.length" class="muted">暂无提交举报。</p>
            <div v-else class="table-wrap">
              <table class="table history-table">
                <thead>
                  <tr>
                    <th>对象</th>
                    <th>原因</th>
                    <th>状态</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in reports" :key="item.id">
                    <td>{{ targetLabels[item.targetType] }} #{{ item.targetId }}</td>
                    <td>{{ reasonLabels[item.reasonType] }}</td>
                    <td><span class="badge" :class="reportStatusClass(item.status)">{{ reportStatusLabels[item.status] }}</span></td>
                    <td>{{ fmt(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="submittedReports" class="pager compact">
              <button class="btn btn-sm" :disabled="submittedReports.first" @click="moveHistoryPage('reports', -1)">上一页</button>
              <span class="muted">第 {{ submittedReports.number + 1 }} / {{ Math.max(submittedReports.totalPages, 1) }} 页，共 {{ submittedReports.totalElements }} 条</span>
              <button class="btn btn-sm" :disabled="submittedReports.last" @click="moveHistoryPage('reports', 1)">下一页</button>
            </div>
          </div>

          <div v-else-if="detailTab === 'reported'" class="history-panel">
            <p v-if="historyLoading" class="muted">加载被举报记录中…</p>
            <p v-else-if="!reported.length" class="muted">暂无被举报记录。</p>
            <div v-else class="table-wrap">
              <table class="table history-table">
                <thead>
                  <tr>
                    <th>对象</th>
                    <th>原因</th>
                    <th>状态</th>
                    <th>举报人</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in reported" :key="item.id">
                    <td>{{ targetLabels[item.targetType] }} #{{ item.targetId }}</td>
                    <td>{{ reasonLabels[item.reasonType] }}</td>
                    <td><span class="badge" :class="reportStatusClass(item.status)">{{ reportStatusLabels[item.status] }}</span></td>
                    <td class="mono">#{{ item.reporterId }}</td>
                    <td>{{ fmt(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="receivedReports" class="pager compact">
              <button class="btn btn-sm" :disabled="receivedReports.first" @click="moveHistoryPage('reported', -1)">上一页</button>
              <span class="muted">第 {{ receivedReports.number + 1 }} / {{ Math.max(receivedReports.totalPages, 1) }} 页，共 {{ receivedReports.totalElements }} 条</span>
              <button class="btn btn-sm" :disabled="receivedReports.last" @click="moveHistoryPage('reported', 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 16px; }
.head p { margin: 0; }
.filters { align-items: stretch; }
.filters .input { min-width: 180px; }
.filters select.input { max-width: 180px; }
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.table { width: 100%; border-collapse: collapse; min-width: 900px; }
.history-table { min-width: 720px; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }
.table tbody tr:last-child td { border-bottom: none; }
.link-btn { border: 0; background: transparent; color: var(--primary); padding: 0; cursor: pointer; font: inherit; text-align: left; }
.link-btn:hover { color: var(--primary-strong); text-decoration: underline; text-underline-offset: 3px; }
.actions, .detail-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.pager.compact { margin-top: 12px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { width: min(980px, 100%); max-height: 90vh; overflow-y: auto; padding: 22px; }
.modal-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--border); padding-bottom: 12px; margin-bottom: 14px; }
.modal h2 { margin: 0 0 4px; font-size: 20px; }
.detail-tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 14px; }
.tab-btn { border: 1px solid var(--border); background: var(--bg-inset); color: var(--text-soft); border-radius: var(--radius-sm); padding: 8px 12px; cursor: pointer; font: inherit; }
.tab-btn.active { background: var(--primary-soft); border-color: var(--primary); color: var(--primary); }
.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.detail-grid > div, .ban-box { background: var(--bg-inset); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; }
.detail-grid span, .ban-box span { display: block; font-size: 12px; margin-bottom: 4px; }
.ban-box { display: grid; gap: 8px; margin-bottom: 14px; }
.ban-box p, .bio { margin: 0; }
.bio { background: var(--bg-inset); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; margin-bottom: 14px; }
.detail-actions { margin-bottom: 16px; }
.log-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.log-list li { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; }
.log-list li > span { margin-right: 10px; }
.log-list p { margin: 6px 0 0; }
.history-panel { min-height: 180px; }
.line-clamp { max-width: 520px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin: 6px 0 0; }
@media (max-width: 720px) {
  .filters .input, .filters select.input { max-width: none; width: 100%; }
  .modal-head { flex-direction: column; }
  .detail-grid { grid-template-columns: 1fr; }
  .tab-btn { flex: 1 1 auto; }
}
</style>

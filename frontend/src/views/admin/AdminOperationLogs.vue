<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { AdminOperationLog, Page } from '../../api/types'

const targetOptions = [
  { value: '', label: '全部对象' },
  { value: 'COMMENT', label: '评论' },
  { value: 'CONTENT_REPORT', label: '举报' },
  { value: 'FORUM_THREAD', label: '论坛帖子' },
  { value: 'FORUM_REPLY', label: '论坛回复' },
  { value: 'FORUM_USER', label: '论坛用户' }
]

const targetLabels: Record<string, string> = {
  COMMENT: '评论',
  CONTENT_REPORT: '举报',
  FORUM_THREAD: '论坛帖子',
  FORUM_REPLY: '论坛回复',
  FORUM_USER: '论坛用户'
}

const operatorUsername = ref('')
const action = ref('')
const targetType = ref('')
const targetIdText = ref('')
const createdFrom = ref('')
const createdTo = ref('')
const page = ref(0)
const size = 20
const loading = ref(false)
const pageData = ref<Page<AdminOperationLog> | null>(null)

const logs = computed(() => pageData.value?.content ?? [])

async function load() {
  loading.value = true
  try {
    pageData.value = await adminApi.operationLogs({
      operatorUsername: operatorUsername.value.trim() || undefined,
      action: action.value.trim() || undefined,
      targetType: targetType.value || undefined,
      targetId: normalizedTargetId(),
      createdFrom: normalizeTime(createdFrom.value),
      createdTo: normalizeTime(createdTo.value),
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

function resetFilters() {
  operatorUsername.value = ''
  action.value = ''
  targetType.value = ''
  targetIdText.value = ''
  createdFrom.value = ''
  createdTo.value = ''
  page.value = 0
  load()
}

function normalizedTargetId() {
  const value = Number(targetIdText.value)
  return Number.isInteger(value) && value > 0 ? value : undefined
}

function normalizeTime(value: string) {
  if (!value) return undefined
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

function fmt(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function targetLabel(value: string) {
  return targetLabels[value] || value
}

function hasDetail(log: AdminOperationLog) {
  return Boolean(log.detail && log.detail.trim())
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
        <h1 class="section-title">审计日志</h1>
        <p class="muted">追踪后台审核、治理和用户处置记录。</p>
      </div>
      <button class="btn btn-primary" :disabled="loading" @click="load">刷新</button>
    </div>

    <div class="toolbar filters">
      <input
        v-model="operatorUsername"
        class="input"
        placeholder="操作者"
        @keyup.enter="applyFilters"
      />
      <input
        v-model="action"
        class="input"
        placeholder="动作，如 HIDE_COMMENT"
        @keyup.enter="applyFilters"
      />
      <select v-model="targetType" class="input">
        <option v-for="item in targetOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <input
        v-model="targetIdText"
        class="input target-id"
        inputmode="numeric"
        placeholder="对象 ID"
        @keyup.enter="applyFilters"
      />
      <input v-model="createdFrom" class="input time-input" type="datetime-local" />
      <input v-model="createdTo" class="input time-input" type="datetime-local" />
      <button class="btn btn-primary" @click="applyFilters">筛选</button>
      <button class="btn" @click="resetFilters">重置</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!logs.length" class="muted">没有匹配的审计日志。</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>时间</th>
            <th>操作者</th>
            <th>动作</th>
            <th>对象</th>
            <th>详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td>
              <span>{{ fmt(log.createdAt) }}</span>
              <div class="muted mono">#{{ log.id }}</div>
            </td>
            <td class="mono">{{ log.operatorUsername }}</td>
            <td><span class="action-code">{{ log.action }}</span></td>
            <td>
              <span>{{ targetLabel(log.targetType) }}</span>
              <div class="muted mono">{{ log.targetType }} #{{ log.targetId }}</div>
            </td>
            <td>
              <pre v-if="hasDetail(log)" class="detail">{{ log.detail }}</pre>
              <span v-else class="muted">-</span>
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
  </div>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.head p { margin: 0; }
.filters { align-items: stretch; }
.filters .input { min-width: 150px; max-width: 220px; }
.filters .target-id { max-width: 130px; }
.filters .time-input { min-width: 190px; }
.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}
.table th,
.table td {
  text-align: left;
  padding: 12px;
  border-bottom: 1px solid var(--border);
  vertical-align: top;
}
.table tbody tr:last-child td { border-bottom: none; }
.action-code {
  display: inline-flex;
  padding: 3px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--primary);
  font-family: var(--font-mono);
  font-size: 12px;
  white-space: nowrap;
}
.detail {
  max-width: 520px;
  max-height: 92px;
  margin: 0;
  padding: 8px 10px;
  overflow: auto;
  white-space: pre-wrap;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  font-family: var(--font-sans);
  font-size: 13px;
  line-height: 1.55;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}
@media (max-width: 720px) {
  .head { align-items: stretch; }
  .head .btn { width: 100%; justify-content: center; }
  .filters .input,
  .filters .target-id,
  .filters .time-input {
    max-width: none;
    width: 100%;
  }
}
</style>

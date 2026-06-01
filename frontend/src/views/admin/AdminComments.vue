<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { Comment, CommentStatus } from '../../api/types'
import { toast } from '../../composables/useToast'

const items = ref<Comment[]>([])
const loading = ref(false)
const onlyPending = ref(true)
const status = ref<CommentStatus | ''>('')

const refLabels: Record<string, string> = { POST: '教程', SKILL: 'Skill', MCP: 'MCP', API: 'API站点' }
const statusOptions: Array<{ value: CommentStatus | ''; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'NORMAL', label: '正常' },
  { value: 'HIDDEN', label: '已隐藏' },
  { value: 'DELETED', label: '已删除' }
]
const statusLabels: Record<CommentStatus, string> = {
  NORMAL: '正常',
  HIDDEN: '已隐藏',
  DELETED: '已删除'
}

async function load() {
  loading.value = true
  try {
    items.value = await adminApi.comments({
      pending: onlyPending.value || undefined,
      status: status.value || undefined
    })
  } finally {
    loading.value = false
  }
}

function commentStatus(c: Comment): CommentStatus {
  return c.status ?? 'NORMAL'
}

function statusClass(value: CommentStatus) {
  if (value === 'NORMAL') return 'badge-up'
  if (value === 'HIDDEN') return 'badge-unknown'
  return 'badge-down'
}

async function approve(c: Comment) {
  await adminApi.approveComment(c.id)
  toast.success('评论已通过')
  load()
}

async function hide(c: Comment) {
  await adminApi.hideComment(c.id)
  toast.success('评论已隐藏')
  load()
}

async function restore(c: Comment) {
  await adminApi.restoreComment(c.id)
  toast.success('评论已恢复')
  load()
}

async function remove(c: Comment) {
  if (!confirm('软删除该评论？')) return
  await adminApi.deleteComment(c.id)
  toast.success('评论已软删除')
  load()
}

function fmt(d: string) { return new Date(d).toLocaleString('zh-CN') }

onMounted(load)
</script>

<template>
  <div>
    <div class="head">
      <div>
        <h1 class="section-title">评论审核</h1>
        <p class="muted">审核普通内容评论，并处理隐藏、恢复和软删除。</p>
      </div>
    </div>

    <div class="toolbar filters">
      <label class="filter"><input type="checkbox" v-model="onlyPending" @change="load" /> 只看待审核</label>
      <select v-model="status" class="input" @change="load">
        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <button class="btn btn-primary" @click="load">刷新</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有评论。</p>

    <div v-for="c in items" :key="c.id" class="card comment">
      <div class="c-head">
        <div>
          <b>{{ c.author }}</b>
          <span class="tag">{{ refLabels[c.refType] }} #{{ c.refId }}</span>
          <span class="badge" :class="c.approved ? 'badge-up' : 'badge-unknown'">
            {{ c.approved ? '已通过' : '待审核' }}
          </span>
          <span class="badge" :class="statusClass(commentStatus(c))">{{ statusLabels[commentStatus(c)] }}</span>
        </div>
        <span class="muted">{{ fmt(c.createdAt) }}</span>
      </div>
      <p class="c-body">{{ c.content }}</p>
      <div class="c-foot">
        <button v-if="!c.approved" class="btn btn-sm btn-primary" @click="approve(c)">通过</button>
        <button v-if="commentStatus(c) === 'NORMAL'" class="btn btn-sm" @click="hide(c)">隐藏</button>
        <button v-if="commentStatus(c) === 'HIDDEN' || commentStatus(c) === 'DELETED'" class="btn btn-sm" @click="restore(c)">恢复</button>
        <button v-if="commentStatus(c) !== 'DELETED'" class="btn btn-sm btn-danger" @click="remove(c)">软删除</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 16px; }
.head p { margin: 0; }
.filters { align-items: center; margin-bottom: 16px; }
.filter { display: flex; align-items: center; gap: 6px; font-size: 14px; white-space: nowrap; }
.filters .input { min-width: 160px; max-width: 180px; }
.comment { padding: 16px; margin-bottom: 12px; }
.c-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; font-size: 13px; flex-wrap: wrap; }
.c-head > div { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.c-body { margin: 10px 0; white-space: pre-wrap; }
.c-foot { display: flex; gap: 8px; flex-wrap: wrap; }
@media (max-width: 640px) {
  .filters .input { max-width: none; width: 100%; }
}
</style>

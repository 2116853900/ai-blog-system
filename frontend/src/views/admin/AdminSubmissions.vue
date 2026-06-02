<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { Page, Submission } from '../../api/types'

const loading = ref(false)
const filter = ref<string>('PENDING')
const page = ref(0)
const size = 20
const pageData = ref<Page<Submission> | null>(null)

const items = computed(() => pageData.value?.content ?? [])

const typeLabels: Record<string, string> = { SKILL: 'AI Skill', MCP: 'MCP', API: '公益 API' }
const statusLabels: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }

async function load() {
  loading.value = true
  try {
    pageData.value = await adminApi.submissions({
      status: filter.value || undefined,
      page: page.value,
      size
    })
  }
  finally { loading.value = false }
}

function applyFilter() {
  page.value = 0
  load()
}

async function approve(s: Submission) {
  try {
    await adminApi.approveSubmission(s.id)
    load()
  } catch (e: any) {
    alert(e?.response?.data?.message || '操作失败')
  }
}
async function reject(s: Submission) { await adminApi.rejectSubmission(s.id); load() }
async function remove(s: Submission) {
  if (!confirm('删除该投稿？')) return
  await adminApi.deleteSubmission(s.id); load()
}

function parse(json: string): Record<string, string> {
  try { return JSON.parse(json) } catch { return {} }
}
function fmt(d: string) { return new Date(d).toLocaleString('zh-CN') }

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
      <h1 class="section-title">投稿审核</h1>
      <select class="input filter" v-model="filter" @change="applyFilter">
        <option value="PENDING">待审核</option>
        <option value="APPROVED">已通过</option>
        <option value="REJECTED">已拒绝</option>
        <option value="">全部</option>
      </select>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有投稿。</p>

    <div v-for="s in items" :key="s.id" class="card sub">
      <div class="s-head">
        <div>
          <span class="tag">{{ typeLabels[s.type] }}</span>
          <span class="badge"
            :class="s.status === 'APPROVED' ? 'badge-up' : s.status === 'REJECTED' ? 'badge-down' : 'badge-unknown'">
            {{ statusLabels[s.status] }}
          </span>
        </div>
        <span class="muted">{{ fmt(s.createdAt) }}</span>
      </div>

      <table class="kv">
        <tr v-for="(v, k) in parse(s.payloadJson)" :key="k">
          <td class="k muted">{{ k }}</td><td>{{ v }}</td>
        </tr>
      </table>
      <p v-if="s.contactInfo" class="muted contact">联系方式：{{ s.contactInfo }}</p>

      <div class="s-foot" v-if="s.status === 'PENDING'">
        <button class="btn btn-sm btn-primary" @click="approve(s)">通过并发布</button>
        <button class="btn btn-sm" @click="reject(s)">拒绝</button>
        <button class="btn btn-sm btn-danger" @click="remove(s)">删除</button>
      </div>
      <div class="s-foot" v-else>
        <button class="btn btn-sm btn-danger" @click="remove(s)">删除</button>
      </div>
    </div>

    <div v-if="pageData" class="pager">
      <button class="btn btn-sm" :disabled="pageData.first" @click="previousPage">上一页</button>
      <span class="muted">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页，共 {{ pageData.totalElements }} 条</span>
      <button class="btn btn-sm" :disabled="pageData.last" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.filter { width: auto; }
.sub { padding: 16px; margin-bottom: 14px; }
.s-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; font-size: 13px; }
.s-head > div { display: flex; gap: 8px; align-items: center; }
.kv { width: 100%; border-collapse: collapse; margin: 10px 0; }
.kv td { padding: 5px 8px; border-bottom: 1px solid var(--border); vertical-align: top; }
.kv .k { width: 140px; font-size: 13px; }
.contact { font-size: 13px; }
.s-foot { display: flex; gap: 8px; margin-top: 6px; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { ApiStation, Page } from '../../api/types'
import StatusBadge from '../../components/StatusBadge.vue'

const loading = ref(false)
const editing = ref<Partial<ApiStation> | null>(null)
const checking = ref<number | null>(null)
const page = ref(0)
const size = 20
const pageData = ref<Page<ApiStation> | null>(null)

const items = computed(() => pageData.value?.content ?? [])

async function load() {
  loading.value = true
  try { pageData.value = await adminApi.apiStations({ page: page.value, size }) } finally { loading.value = false }
}

function add() { editing.value = { name: '', baseUrl: '', description: '', supportedModels: '', tags: '' } }
function edit(s: ApiStation) { editing.value = { ...s } }
function cancel() { editing.value = null }

async function save() {
  const e = editing.value!
  if (!e.name || !e.baseUrl) return
  if (e.id) await adminApi.updateApiStation(e.id, e)
  else await adminApi.createApiStation(e)
  editing.value = null
  load()
}

async function remove(s: ApiStation) {
  if (!confirm(`删除「${s.name}」？`)) return
  await adminApi.deleteApiStation(s.id)
  load()
}

async function check(s: ApiStation) {
  checking.value = s.id
  try {
    const updated = await adminApi.checkApiStation(s.id)
    const content = pageData.value?.content
    const i = content?.findIndex(x => x.id === s.id) ?? -1
    if (content && i >= 0) content[i] = updated
  } finally { checking.value = null }
}

async function checkAll() {
  loading.value = true
  try { await adminApi.checkAllApiStations(); await load() } finally { loading.value = false }
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
      <h1 class="section-title">公益 API 站点管理</h1>
      <div class="head-btns">
        <button class="btn" @click="checkAll">检测全部</button>
        <button class="btn btn-primary" @click="add">+ 新增站点</button>
      </div>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有 API 站点。</p>
    <table v-else class="table">
      <thead><tr><th>名称</th><th>地址</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="s in items" :key="s.id">
          <td>{{ s.name }}</td>
          <td><small class="muted">{{ s.baseUrl }}</small></td>
          <td><StatusBadge :status="s.status" :latency-ms="s.latencyMs" /></td>
          <td class="actions">
            <button class="btn btn-sm" @click="check(s)" :disabled="checking === s.id">
              {{ checking === s.id ? '检测中…' : '检测' }}
            </button>
            <button class="btn btn-sm" @click="edit(s)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="remove(s)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="pageData" class="pager">
      <button class="btn btn-sm" :disabled="pageData.first" @click="previousPage">上一页</button>
      <span class="muted">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页，共 {{ pageData.totalElements }} 条</span>
      <button class="btn btn-sm" :disabled="pageData.last" @click="nextPage">下一页</button>
    </div>

    <div v-if="editing" class="modal-mask" @click.self="cancel">
      <div class="modal card">
        <h2>{{ editing.id ? '编辑' : '新增' }} API 站点</h2>
        <label>名称</label><input class="input" v-model="editing.name" />
        <label>API 地址</label><input class="input" v-model="editing.baseUrl" placeholder="https://api.example.com" />
        <label>描述</label><textarea class="textarea" v-model="editing.description"></textarea>
        <label>支持模型（逗号分隔）</label><input class="input" v-model="editing.supportedModels" />
        <label>标签（逗号分隔）</label><input class="input" v-model="editing.tags" />
        <div class="modal-foot">
          <button class="btn" @click="cancel">取消</button>
          <button class="btn btn-primary" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; }
.head-btns { display: flex; gap: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { padding: 24px; width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.modal h2 { margin: 0 0 8px; }
.modal label { font-size: 13px; font-weight: 600; margin-top: 6px; }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { Page, Skill } from '../../api/types'

const loading = ref(false)
const editing = ref<Partial<Skill> | null>(null)
const page = ref(0)
const size = 20
const pageData = ref<Page<Skill> | null>(null)

const items = computed(() => pageData.value?.content ?? [])

async function load() {
  loading.value = true
  try { pageData.value = await adminApi.skills({ page: page.value, size }) } finally { loading.value = false }
}

function add() { editing.value = { name: '', description: '', link: '', tags: '', category: '', recommendLevel: 3 } }
function edit(s: Skill) { editing.value = { ...s } }
function cancel() { editing.value = null }

async function save() {
  const e = editing.value!
  if (!e.name) return
  if (e.id) await adminApi.updateSkill(e.id, e)
  else await adminApi.createSkill(e)
  editing.value = null
  load()
}

async function remove(s: Skill) {
  if (!confirm(`删除「${s.name}」？`)) return
  await adminApi.deleteSkill(s.id)
  load()
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
      <h1 class="section-title">Skill 管理</h1>
      <button class="btn btn-primary" @click="add">+ 新增 Skill</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!items.length" class="muted">没有 Skill。</p>
    <table v-else class="table">
      <thead><tr><th>名称</th><th>分类</th><th>推荐</th><th>标签</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="s in items" :key="s.id">
          <td>{{ s.name }}</td>
          <td>{{ s.category || '-' }}</td>
          <td>{{ s.recommendLevel }}★</td>
          <td><small class="muted">{{ s.tags }}</small></td>
          <td class="actions">
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
        <h2>{{ editing.id ? '编辑' : '新增' }} Skill</h2>
        <label>名称</label><input class="input" v-model="editing.name" />
        <label>描述</label><textarea class="textarea" v-model="editing.description"></textarea>
        <label>链接</label><input class="input" v-model="editing.link" placeholder="https://" />
        <label>分类</label><input class="input" v-model="editing.category" />
        <label>标签（逗号分隔）</label><input class="input" v-model="editing.tags" />
        <label>推荐指数 (1-5)</label>
        <input class="input" type="number" min="1" max="5" v-model.number="editing.recommendLevel" />
        <div class="modal-foot">
          <button class="btn" @click="cancel">取消</button>
          <button class="btn btn-primary" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); }
.actions { display: flex; gap: 6px; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { padding: 24px; width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.modal h2 { margin: 0 0 8px; }
.modal label { font-size: 13px; font-weight: 600; margin-top: 6px; }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
</style>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { Mcp } from '../../api/types'

const items = ref<Mcp[]>([])
const loading = ref(false)
const editing = ref<Partial<Mcp> | null>(null)

async function load() {
  loading.value = true
  try { items.value = await adminApi.mcps() } finally { loading.value = false }
}

function add() { editing.value = { name: '', description: '', repoUrl: '', installCmd: '', tags: '', category: '', recommendLevel: 3 } }
function edit(m: Mcp) { editing.value = { ...m } }
function cancel() { editing.value = null }

async function save() {
  const e = editing.value!
  if (!e.name) return
  if (e.id) await adminApi.updateMcp(e.id, e)
  else await adminApi.createMcp(e)
  editing.value = null
  load()
}

async function remove(m: Mcp) {
  if (!confirm(`删除「${m.name}」？`)) return
  await adminApi.deleteMcp(m.id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="head">
      <h1 class="section-title">MCP 管理</h1>
      <button class="btn btn-primary" @click="add">+ 新增 MCP</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <table v-else class="table">
      <thead><tr><th>名称</th><th>分类</th><th>推荐</th><th>标签</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="m in items" :key="m.id">
          <td>{{ m.name }}</td>
          <td>{{ m.category || '-' }}</td>
          <td>{{ m.recommendLevel }}★</td>
          <td><small class="muted">{{ m.tags }}</small></td>
          <td class="actions">
            <button class="btn btn-sm" @click="edit(m)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="remove(m)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="editing" class="modal-mask" @click.self="cancel">
      <div class="modal card">
        <h2>{{ editing.id ? '编辑' : '新增' }} MCP</h2>
        <label>名称</label><input class="input" v-model="editing.name" />
        <label>描述</label><textarea class="textarea" v-model="editing.description"></textarea>
        <label>仓库地址</label><input class="input" v-model="editing.repoUrl" placeholder="https://github.com/..." />
        <label>安装命令</label><input class="input" v-model="editing.installCmd" placeholder="npx -y ..." />
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
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { padding: 24px; width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.modal h2 { margin: 0 0 8px; }
.modal label { font-size: 13px; font-weight: 600; margin-top: 6px; }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
</style>

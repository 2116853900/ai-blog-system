<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { ForumCategory } from '../../api/types'
import { toast } from '../../composables/useToast'

const items = ref<ForumCategory[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const editing = ref<Partial<ForumCategory> | null>(null)

const parents = computed(() => items.value.filter(c => !c.parentId))
const sorted = computed(() => [...items.value].sort((a, b) => (a.parentId || 0) - (b.parentId || 0) || a.sortOrder - b.sortOrder))

function parentName(id?: number | null) {
  if (!id) return '一级板块'
  return items.value.find(c => c.id === id)?.name || `#${id}`
}

async function load() {
  loading.value = true
  try {
    items.value = await adminApi.forumCategories()
  } finally {
    loading.value = false
  }
}

function add() {
  error.value = ''
  editing.value = {
    name: '',
    slug: '',
    description: '',
    icon: '',
    sortOrder: 0,
    parentId: null,
    active: true,
    threadCount: 0
  }
}

function edit(item: ForumCategory) {
  error.value = ''
  editing.value = { ...item }
}

function cancel() {
  editing.value = null
  error.value = ''
}

async function save() {
  if (!editing.value) return
  if (!editing.value.name?.trim() || !editing.value.slug?.trim()) {
    error.value = '名称和 slug 必填'
    return
  }
  if (editing.value.parentId === editing.value.id) {
    error.value = '父板块不能选择自己'
    return
  }

  saving.value = true
  error.value = ''
  try {
    const body = {
      ...editing.value,
      name: editing.value.name.trim(),
      slug: editing.value.slug.trim(),
      parentId: editing.value.parentId || null,
      sortOrder: Number(editing.value.sortOrder || 0)
    }
    if (body.id) await adminApi.updateForumCategory(body.id, body)
    else await adminApi.createForumCategory(body)
    toast.success('板块已保存')
    editing.value = null
    await load()
  } catch (e: any) {
    error.value = e?.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(item: ForumCategory) {
  if (!confirm(`删除板块「${item.name}」？已有帖子时不建议删除。`)) return
  await adminApi.deleteForumCategory(item.id)
  toast.success('板块已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="head">
      <div>
        <h1 class="section-title">论坛板块管理</h1>
        <p class="muted">维护前台论坛分类、层级、排序和启用状态。</p>
      </div>
      <button class="btn btn-primary" @click="add">+ 新增板块</button>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <table v-else class="table">
      <thead>
        <tr><th>排序</th><th>名称</th><th>Slug</th><th>父级</th><th>帖子数</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="c in sorted" :key="c.id">
          <td class="mono">{{ c.sortOrder }}</td>
          <td><span class="icon">{{ c.icon || '//' }}</span>{{ c.name }}</td>
          <td class="mono muted">{{ c.slug }}</td>
          <td>{{ parentName(c.parentId) }}</td>
          <td>{{ c.threadCount }}</td>
          <td><span class="badge" :class="c.active ? 'badge-up' : 'badge-unknown'">{{ c.active ? '启用' : '停用' }}</span></td>
          <td class="actions">
            <button class="btn btn-sm" @click="edit(c)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="remove(c)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="editing" class="modal-mask" @click.self="cancel">
      <div class="modal card">
        <h2>{{ editing.id ? '编辑' : '新增' }}板块</h2>
        <label>名称</label>
        <input class="input" v-model="editing.name" placeholder="如：AI 开发" />
        <label>Slug</label>
        <input class="input" v-model="editing.slug" placeholder="如：ai-dev" />
        <label>描述</label>
        <textarea class="textarea" v-model="editing.description"></textarea>
        <div class="two-cols">
          <div>
            <label>图标</label>
            <input class="input" v-model="editing.icon" placeholder="如：//" />
          </div>
          <div>
            <label>排序</label>
            <input class="input" type="number" v-model.number="editing.sortOrder" />
          </div>
        </div>
        <label>父板块</label>
        <select class="input" v-model="editing.parentId">
          <option :value="null">一级板块</option>
          <option v-for="p in parents" :key="p.id" :value="p.id" :disabled="p.id === editing.id">{{ p.name }}</option>
        </select>
        <label class="check-row">
          <input type="checkbox" v-model="editing.active" />
          启用板块
        </label>
        <p v-if="error" class="err">{{ error }}</p>
        <div class="modal-foot">
          <button class="btn" @click="cancel">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 16px; }
.head p { margin: 0; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }
.icon { display: inline-flex; width: 28px; color: var(--primary); font-family: var(--font-mono); }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.modal { padding: 24px; width: 100%; max-width: 560px; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.modal h2 { margin: 0 0 8px; }
.modal label { font-size: 13px; font-weight: 600; margin-top: 6px; color: var(--text-soft); }
.two-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.check-row { display: flex; align-items: center; gap: 8px; }
.err { color: var(--danger); margin: 4px 0 0; }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
@media (max-width: 720px) {
  .two-cols { grid-template-columns: 1fr; }
  .table { display: block; overflow-x: auto; }
}
</style>

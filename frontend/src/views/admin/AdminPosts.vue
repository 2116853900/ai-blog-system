<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { adminApi } from '../../api'
import type { Page, Post } from '../../api/types'

const loading = ref(false)
const page = ref(0)
const size = 20
const pageData = ref<Page<Post> | null>(null)

const posts = computed(() => pageData.value?.content ?? [])

async function load() {
  loading.value = true
  try { pageData.value = await adminApi.posts({ page: page.value, size }) } finally { loading.value = false }
}

async function togglePublish(p: Post) {
  await adminApi.publishPost(p.id, !p.published)
  load()
}

async function remove(p: Post) {
  if (!confirm(`确定删除教程「${p.title}」？`)) return
  await adminApi.deletePost(p.id)
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
      <h1 class="section-title">教程管理</h1>
      <RouterLink to="/admin/posts/new" class="btn btn-primary">+ 新建教程</RouterLink>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="!posts.length" class="muted">没有教程。</p>
    <table v-else class="table">
      <thead>
        <tr><th>标题</th><th>分类</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="p in posts" :key="p.id">
          <td>{{ p.title }}<br /><small class="muted">/{{ p.slug }}</small></td>
          <td>{{ p.category || '-' }}</td>
          <td>
            <span class="badge" :class="p.published ? 'badge-up' : 'badge-unknown'">
              {{ p.published ? '已发布' : '草稿' }}
            </span>
          </td>
          <td class="actions">
            <RouterLink :to="`/admin/posts/${p.id}/edit`" class="btn btn-sm">编辑</RouterLink>
            <button class="btn btn-sm" @click="togglePublish(p)">{{ p.published ? '下架' : '发布' }}</button>
            <button class="btn btn-sm btn-danger" @click="remove(p)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="pageData" class="pager">
      <button class="btn btn-sm" :disabled="pageData.first" @click="previousPage">上一页</button>
      <span class="muted">第 {{ pageData.number + 1 }} / {{ Math.max(pageData.totalPages, 1) }} 页，共 {{ pageData.totalElements }} 条</span>
      <button class="btn btn-sm" :disabled="pageData.last" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { text-align: left; padding: 12px; border-bottom: 1px solid var(--border); vertical-align: top; }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
</style>

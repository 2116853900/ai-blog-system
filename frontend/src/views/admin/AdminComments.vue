<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import type { Comment } from '../../api/types'

const items = ref<Comment[]>([])
const loading = ref(false)
const onlyPending = ref(true)

const refLabels: Record<string, string> = { POST: '教程', SKILL: 'Skill', MCP: 'MCP', API: 'API站点' }

async function load() {
  loading.value = true
  try { items.value = await adminApi.comments(onlyPending.value || undefined) }
  finally { loading.value = false }
}

async function approve(c: Comment) { await adminApi.approveComment(c.id); load() }
async function remove(c: Comment) {
  if (!confirm('删除该评论？')) return
  await adminApi.deleteComment(c.id); load()
}

function fmt(d: string) { return new Date(d).toLocaleString('zh-CN') }

onMounted(load)
</script>

<template>
  <div>
    <div class="head">
      <h1 class="section-title">评论审核</h1>
      <label class="filter"><input type="checkbox" v-model="onlyPending" @change="load" /> 只看待审核</label>
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
        </div>
        <span class="muted">{{ fmt(c.createdAt) }}</span>
      </div>
      <p class="c-body">{{ c.content }}</p>
      <div class="c-foot">
        <button v-if="!c.approved" class="btn btn-sm btn-primary" @click="approve(c)">通过</button>
        <button class="btn btn-sm btn-danger" @click="remove(c)">删除</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.filter { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.comment { padding: 16px; margin-bottom: 12px; }
.c-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; font-size: 13px; flex-wrap: wrap; }
.c-head > div { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.c-body { margin: 10px 0; }
.c-foot { display: flex; gap: 8px; }
</style>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { Post } from '../api/types'
import MarkdownView from '../components/MarkdownView.vue'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const post = ref<Post | null>(null)
const notFound = ref(false)
const loading = ref(true)

async function load() {
  loading.value = true
  notFound.value = false
  post.value = null
  try {
    post.value = await publicApi.post(route.params.slug as string)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

function fmt(d?: string) {
  return d ? new Date(d).toLocaleDateString('zh-CN') : ''
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <div class="container page">
    <RouterLink to="/tutorials" class="back">← 返回教程列表</RouterLink>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="notFound" class="muted">教程不存在或未发布。</p>

    <article v-else-if="post">
      <header class="post-header">
        <span v-if="post.category" class="tag">{{ post.category }}</span>
        <h1>{{ post.title }}</h1>
        <p class="muted meta">发布于 {{ fmt(post.createdAt) }}</p>
      </header>

      <MarkdownView :source="post.bodyMarkdown" />

      <CommentSection ref-type="POST" :ref-id="post.id" />
    </article>
  </div>
</template>

<style scoped>
.page { padding: 24px 0 60px; max-width: 820px; }
.back { display: inline-block; margin-bottom: 16px; font-size: 14px; }
.post-header { margin-bottom: 24px; }
.post-header h1 { margin: 10px 0 6px; font-size: 32px; }
.meta { font-size: 14px; }
</style>

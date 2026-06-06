<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { publicApi } from '../api'
import type { Post } from '../api/types'
import MarkdownView from '../components/MarkdownView.vue'
import CommentSection from '../components/CommentSection.vue'
import LinkedDiscussions from '../components/LinkedDiscussions.vue'
import ResourceFavoriteButton from '../components/ResourceFavoriteButton.vue'
import ResourceReviewPanel from '../components/ResourceReviewPanel.vue'
import Skeleton from '../components/Skeleton.vue'

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
    <RouterLink to="/tutorials" class="back mono">← 返回教程列表</RouterLink>

    <div v-if="loading" class="loading">
      <Skeleton block height="34px" width="70%" />
      <Skeleton block height="16px" width="30%" radius="6px" />
      <Skeleton block height="14px" radius="6px" />
      <Skeleton block height="14px" radius="6px" />
      <Skeleton block height="14px" width="85%" radius="6px" />
    </div>

    <div v-else-if="notFound" class="notfound">
      <span class="nf-mark mono" aria-hidden="true">404</span>
      <p class="muted">教程不存在或未发布。</p>
      <RouterLink to="/tutorials" class="btn">返回列表</RouterLink>
    </div>

    <article v-else-if="post">
      <header class="post-header">
        <div class="post-head-row">
          <div class="post-head-main">
            <span v-if="post.category" class="chip chip-active cat">{{ post.category }}</span>
            <h1 class="post-title mono">{{ post.title }}</h1>
            <p class="muted meta mono">发布于 {{ fmt(post.createdAt) }}</p>
          </div>
          <ResourceFavoriteButton ref-type="POST" :ref-id="post.id" />
        </div>
      </header>

      <MarkdownView class="markdown-body" :source="post.bodyMarkdown" />

      <hr class="sep" />
      <LinkedDiscussions ref-type="POST" :ref-id="post.id" :source-title="post.title" />

      <hr class="sep" />
      <CommentSection ref-type="POST" :ref-id="post.id" />
    </article>

    <ResourceReviewPanel v-if="post?.id" ref-type="POST" :ref-id="post.id" />
  </div>
</template>

<style scoped>
.page { padding: 24px 0 60px; max-width: 820px; }
.back { display: inline-block; margin-bottom: 20px; font-size: 13px; }
.loading { display: flex; flex-direction: column; gap: 14px; }
.notfound { display: grid; place-items: center; gap: 14px; padding: 60px 0; text-align: center; }
.nf-mark { font-size: 56px; font-weight: 800; color: var(--primary-dim); }
.post-header { margin-bottom: 28px; }
.post-head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; }
.post-head-main { min-width: 0; }
.cat { margin-bottom: 12px; pointer-events: none; }
.post-title { margin: 0 0 8px; font-size: 32px; font-weight: 800; line-height: 1.25; letter-spacing: -0.01em; }
.meta { font-size: 13px; }
.sep { border: none; border-top: 1px dashed var(--border-strong); margin: 36px 0; }
@media (max-width: 640px) {
  .post-head-row { flex-direction: column; }
}
</style>

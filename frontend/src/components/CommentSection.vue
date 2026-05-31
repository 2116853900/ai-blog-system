<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { publicApi } from '../api'
import type { Comment, RefType } from '../api/types'
import { toast } from '../composables/useToast'
import StateBlock from './StateBlock.vue'

const props = defineProps<{ refType: RefType; refId: number }>()

const comments = ref<Comment[]>([])
const author = ref('')
const content = ref('')
const submitting = ref(false)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    comments.value = await publicApi.comments(props.refType, props.refId)
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!author.value.trim() || !content.value.trim()) {
    toast.error('请填写昵称和评论内容')
    return
  }
  submitting.value = true
  try {
    const res = await publicApi.addComment({
      refType: props.refType,
      refId: props.refId,
      author: author.value.trim(),
      content: content.value.trim()
    })
    toast.success(res.message || '评论已提交，审核后显示')
    content.value = ''
  } catch {
    // 错误已由 http 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

function fmt(d: string) {
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(load)
watch(() => props.refId, load)
</script>

<template>
  <section class="comments">
    <h2 class="c-title mono">// 评论 ({{ comments.length }})</h2>

    <div class="card comment-form">
      <input class="input" v-model="author" placeholder="你的昵称" maxlength="40" aria-label="昵称" />
      <textarea class="textarea" v-model="content" placeholder="友善发言，评论将在审核后显示…" maxlength="2000" aria-label="评论内容"></textarea>
      <div class="form-foot">
        <span class="dim mono">{{ content.length }}/2000</span>
        <button class="btn btn-primary" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '发表评论' }}
        </button>
      </div>
    </div>

    <StateBlock :loading="loading" :empty="!comments.length" empty-text="还没有评论，来抢沙发吧 ～">
      <template #skeleton>
        <div class="muted prompt mono">loading<span class="cursor"></span></div>
      </template>
      <div class="c-list">
        <div v-for="c in comments" :key="c.id" class="card comment">
          <div class="comment-head">
            <b class="mono">{{ c.author }}</b>
            <span class="dim mono c-date">{{ fmt(c.createdAt) }}</span>
          </div>
          <p>{{ c.content }}</p>
        </div>
      </div>
    </StateBlock>
  </section>
</template>

<style scoped>
.comments { margin-top: 12px; }
.c-title { font-size: 16px; font-weight: 700; color: var(--text-soft); margin: 0 0 16px; }
.comment-form { padding: 16px; display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.form-foot { display: flex; justify-content: space-between; align-items: center; }
.c-list { display: flex; flex-direction: column; gap: 12px; }
.comment { padding: 14px 16px; }
.comment-head { display: flex; justify-content: space-between; align-items: center; font-size: 13px; margin-bottom: 6px; gap: 10px; }
.c-date { font-size: 11px; }
.comment p { margin: 0; line-height: 1.7; }
</style>

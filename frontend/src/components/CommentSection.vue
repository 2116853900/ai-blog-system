<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { publicApi } from '../api'
import type { Comment, RefType } from '../api/types'

const props = defineProps<{ refType: RefType; refId: number }>()

const comments = ref<Comment[]>([])
const author = ref('')
const content = ref('')
const msg = ref('')
const submitting = ref(false)

async function load() {
  comments.value = await publicApi.comments(props.refType, props.refId)
}

async function submit() {
  if (!author.value.trim() || !content.value.trim()) {
    msg.value = '请填写昵称和评论内容'
    return
  }
  submitting.value = true
  msg.value = ''
  try {
    const res = await publicApi.addComment({
      refType: props.refType,
      refId: props.refId,
      author: author.value.trim(),
      content: content.value.trim()
    })
    msg.value = res.message || '已提交'
    content.value = ''
  } catch {
    msg.value = '提交失败，请稍后再试'
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
    <h2 class="section-title">💬 评论（{{ comments.length }}）</h2>

    <div class="card comment-form">
      <input class="input" v-model="author" placeholder="你的昵称" maxlength="40" />
      <textarea class="textarea" v-model="content" placeholder="友善发言，评论将在审核后显示…" maxlength="2000"></textarea>
      <div class="form-foot">
        <span class="muted">{{ msg }}</span>
        <button class="btn btn-primary" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '发表评论' }}
        </button>
      </div>
    </div>

    <p v-if="!comments.length" class="muted">还没有评论，来抢沙发吧～</p>
    <div v-for="c in comments" :key="c.id" class="card comment">
      <div class="comment-head">
        <b>{{ c.author }}</b>
        <span class="muted">{{ fmt(c.createdAt) }}</span>
      </div>
      <p>{{ c.content }}</p>
    </div>
  </section>
</template>

<style scoped>
.comments { margin-top: 50px; }
.comment-form { padding: 16px; display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.form-foot { display: flex; justify-content: space-between; align-items: center; }
.comment { padding: 14px 16px; margin-bottom: 12px; }
.comment-head { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
.comment p { margin: 0; }
</style>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { forumApi, publicApi } from '../api'
import type { Comment, RefType, ReportReasonType } from '../api/types'
import { toast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'
import StateBlock from './StateBlock.vue'

const props = defineProps<{ refType: RefType; refId: number }>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const comments = ref<Comment[]>([])
const author = ref('')
const content = ref('')
const submitting = ref(false)
const loading = ref(true)
const reportTarget = ref<Comment | null>(null)
const reportSaving = ref(false)
const reportForm = reactive<{ reasonType: ReportReasonType; reasonText: string }>({
  reasonType: 'SPAM',
  reasonText: ''
})

const reportReasons: Array<{ value: ReportReasonType; label: string }> = [
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'ABUSE', label: '辱骂攻击' },
  { value: 'PORN', label: '色情低俗' },
  { value: 'POLITICS', label: '敏感内容' },
  { value: 'ILLEGAL', label: '违法违规' },
  { value: 'COPYRIGHT', label: '侵权' },
  { value: 'OTHER', label: '其他' }
]

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

function openReport(comment: Comment) {
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  reportTarget.value = comment
  reportForm.reasonType = 'SPAM'
  reportForm.reasonText = ''
}

function closeReport() {
  reportTarget.value = null
  reportForm.reasonType = 'SPAM'
  reportForm.reasonText = ''
}

async function submitReport() {
  if (!reportTarget.value) return
  reportSaving.value = true
  try {
    await forumApi.report({
      targetType: 'COMMENT',
      targetId: reportTarget.value.id,
      reasonType: reportForm.reasonType,
      reasonText: reportForm.reasonText.trim() || undefined
    })
    toast.success('举报已提交')
    closeReport()
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '举报失败')
  } finally {
    reportSaving.value = false
  }
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
            <div class="comment-actions">
              <span class="dim mono c-date">{{ fmt(c.createdAt) }}</span>
              <button class="report-btn" type="button" @click="openReport(c)">举报</button>
            </div>
          </div>
          <p>{{ c.content }}</p>
        </div>
      </div>
    </StateBlock>

    <div v-if="reportTarget" class="modal-mask" @click.self="closeReport">
      <div class="modal card">
        <h2>举报评论</h2>
        <p class="muted quoted">{{ reportTarget.content }}</p>

        <label>举报原因</label>
        <select v-model="reportForm.reasonType" class="input">
          <option v-for="reason in reportReasons" :key="reason.value" :value="reason.value">{{ reason.label }}</option>
        </select>

        <label>补充说明</label>
        <textarea
          v-model="reportForm.reasonText"
          class="textarea"
          maxlength="1000"
          placeholder="可补充具体原因，最多 1000 字"
        ></textarea>

        <div class="modal-foot">
          <button class="btn" type="button" @click="closeReport">取消</button>
          <button class="btn btn-primary" type="button" :disabled="reportSaving" @click="submitReport">
            {{ reportSaving ? '提交中…' : '提交举报' }}
          </button>
        </div>
      </div>
    </div>
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
.comment-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.c-date { font-size: 11px; }
.report-btn {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 11px;
}
.report-btn:hover { color: var(--danger); }
.comment p { margin: 0; line-height: 1.7; }
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}
.modal {
  width: min(520px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.modal h2 { margin: 0; font-size: 20px; }
.modal label { font-size: 13px; font-weight: 600; color: var(--text-soft); }
.quoted {
  margin: 0;
  padding: 10px 12px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
@media (max-width: 560px) {
  .comment-head { align-items: flex-start; flex-direction: column; }
  .comment-actions { justify-content: flex-start; }
  .modal-foot { flex-direction: column; }
}
</style>

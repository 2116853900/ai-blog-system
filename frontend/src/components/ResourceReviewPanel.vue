<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accountApi, publicApi } from '../api'
import type { Page, ResourceFavoriteRefType, ResourceReview, ResourceReviewSummary } from '../api/types'
import { toast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{
  refType: ResourceFavoriteRefType
  refId: number
}>()

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const summary = ref<ResourceReviewSummary | null>(null)
const page = ref<Page<ResourceReview> | null>(null)
const rating = ref(5)
const content = ref('')
const loading = ref(false)
const initialLoading = ref(true)

const reviews = computed(() => page.value?.content ?? [])
const hasMyReview = computed(() => !!summary.value?.myReview)
const reviewCount = computed(() => summary.value?.reviewCount ?? 0)
const averageRating = computed(() => summary.value?.averageRating ?? 0)
const canSubmit = computed(() => auth.isLoggedIn() && rating.value >= 1 && rating.value <= 5)

async function load() {
  if (!props.refId) return
  initialLoading.value = true
  try {
    const [summaryData, reviewPage] = await Promise.all([
      publicApi.resourceReviewSummary(props.refType, props.refId),
      publicApi.resourceReviews(props.refType, props.refId, { page: 0, size: 10 })
    ])
    summary.value = summaryData
    page.value = reviewPage
    if (summaryData.myReview) {
      rating.value = summaryData.myReview.rating
      content.value = summaryData.myReview.content ?? ''
    }
  } catch {
    summary.value = null
    page.value = null
  } finally {
    initialLoading.value = false
  }
}

function requireLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function submit() {
  if (!auth.isLoggedIn()) {
    requireLogin()
    return
  }
  if (!canSubmit.value || loading.value) return
  loading.value = true
  try {
    await accountApi.upsertResourceReview(props.refType, props.refId, {
      rating: rating.value,
      content: content.value.trim() || undefined
    })
    toast.success('评价已保存')
    await load()
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '评价保存失败')
  } finally {
    loading.value = false
  }
}

async function remove() {
  if (!auth.isLoggedIn()) {
    requireLogin()
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    await accountApi.deleteResourceReview(props.refType, props.refId)
    rating.value = 5
    content.value = ''
    toast.success('评价已删除')
    await load()
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '评价删除失败')
  } finally {
    loading.value = false
  }
}

function fmt(d: string) {
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(load)
watch(() => [props.refType, props.refId, auth.token], load)
</script>

<template>
  <section class="card review-panel">
    <div class="review-head">
      <div>
        <h2 class="section-title mono">// 资源评价</h2>
        <p class="muted review-summary">
          {{ reviewCount }} 条评价
          <span v-if="reviewCount"> · 平均 {{ averageRating.toFixed(1) }}/5</span>
        </p>
      </div>
      <div class="score mono">{{ reviewCount ? averageRating.toFixed(1) : '-' }}</div>
    </div>

    <div v-if="auth.isLoggedIn()" class="review-form">
      <label class="field">
        <span class="mono">评分</span>
        <select v-model.number="rating" class="input">
          <option v-for="n in [5, 4, 3, 2, 1]" :key="n" :value="n">{{ n }} 分</option>
        </select>
      </label>
      <label class="field">
        <span class="mono">评价</span>
        <textarea
          v-model="content"
          class="textarea"
          maxlength="1000"
          rows="4"
          placeholder="写下实际体验、适用场景或注意事项"
        ></textarea>
      </label>
      <div class="form-foot">
        <span class="dim mono">{{ content.length }}/1000</span>
        <div class="actions">
          <button class="btn btn-primary" type="button" :disabled="loading || !canSubmit" @click="submit">
            {{ loading ? '保存中' : '保存评价' }}
          </button>
          <button v-if="hasMyReview" class="btn btn-ghost" type="button" :disabled="loading" @click="remove">
            删除
          </button>
        </div>
      </div>
    </div>

    <div v-else class="login-hint">
      <p class="muted">登录后可以发表评价。</p>
      <button class="btn" type="button" @click="requireLogin">去登录</button>
    </div>

    <div class="review-list">
      <p v-if="initialLoading" class="muted mono loading-line">loading<span class="cursor"></span></p>
      <template v-else>
        <article v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-meta">
            <strong class="mono">{{ review.rating }} 分</strong>
            <span class="muted mono">{{ fmt(review.createdAt) }}</span>
          </div>
          <p v-if="review.content">{{ review.content }}</p>
        </article>
      </template>
      <p v-if="!initialLoading && !reviews.length" class="muted empty">暂无评价。</p>
    </div>
  </section>
</template>

<style scoped>
.review-panel { padding: 22px; margin-top: 24px; }
.review-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.review-head .section-title { margin: 0 0 6px; font-size: 18px; }
.review-summary { margin: 0; font-size: 13px; }
.score { font-size: 30px; font-weight: 800; line-height: 1; color: var(--primary); }
.review-form { margin-top: 18px; display: grid; gap: 12px; }
.field { display: grid; gap: 6px; font-size: 13px; color: var(--text-soft); }
.field .textarea { min-height: 96px; }
.form-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.login-hint {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.login-hint p { margin: 0; }
.review-list { margin-top: 20px; display: grid; gap: 12px; }
.review-item { border-top: 1px solid var(--border); padding-top: 12px; }
.review-meta { display: flex; justify-content: space-between; gap: 12px; font-size: 13px; }
.review-item p { margin: 8px 0 0; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
.loading-line,
.empty { margin: 0; font-size: 13px; }
@media (max-width: 560px) {
  .review-head,
  .form-foot,
  .login-hint {
    flex-direction: column;
    align-items: flex-start;
  }
  .actions { justify-content: flex-start; }
}
</style>

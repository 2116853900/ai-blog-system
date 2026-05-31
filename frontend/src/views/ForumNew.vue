<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { forumApi } from '../api'
import type { ForumCategory } from '../api/types'
import MarkdownView from '../components/MarkdownView.vue'
import { toast } from '../composables/useToast'

const router = useRouter()
const categories = ref<ForumCategory[]>([])
const saving = ref(false)
const error = ref('')
const form = reactive({
  categoryId: 0,
  title: '',
  tags: '',
  contentMarkdown: ''
})

const selectableCategories = computed(() =>
  categories.value.filter(c => c.active).sort((a, b) => (a.parentId || 0) - (b.parentId || 0) || a.sortOrder - b.sortOrder)
)

function labelOf(c: ForumCategory) {
  const parent = c.parentId ? categories.value.find(p => p.id === c.parentId)?.name : ''
  return parent ? `${parent} / ${c.name}` : c.name
}

async function submit() {
  error.value = ''
  if (!form.categoryId) {
    error.value = '请选择板块'
    return
  }
  if (!form.title.trim() || !form.contentMarkdown.trim()) {
    error.value = '标题和正文不能为空'
    return
  }
  saving.value = true
  try {
    const thread = await forumApi.createThread({
      categoryId: form.categoryId,
      title: form.title.trim(),
      tags: form.tags.trim() || undefined,
      contentMarkdown: form.contentMarkdown.trim()
    })
    toast.success('帖子已发布')
    router.push(`/forum/threads/${thread.id}`)
  } catch (e: any) {
    error.value = e?.response?.data?.message || '发布失败'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  categories.value = await forumApi.categories()
  form.categoryId = selectableCategories.value[0]?.id || 0
})
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <RouterLink to="/forum" class="muted mono">← 返回论坛</RouterLink>
      <h1 class="section-title prompt">发布新帖</h1>
      <p class="muted">清楚描述问题、经验或项目背景，能显著提高讨论质量。</p>
    </header>

    <form class="editor" @submit.prevent="submit">
      <section class="card form-panel">
        <label class="label">板块</label>
        <select class="input" v-model.number="form.categoryId">
          <option v-for="c in selectableCategories" :key="c.id" :value="c.id">{{ labelOf(c) }}</option>
        </select>

        <label class="label">标题</label>
        <input class="input" v-model="form.title" maxlength="200" placeholder="一句话说明你想讨论什么" />

        <label class="label">标签（逗号分隔）</label>
        <input class="input" v-model="form.tags" placeholder="如：MCP,Prompt,求助" />

        <label class="label">正文 Markdown</label>
        <textarea class="textarea body-input" v-model="form.contentMarkdown" placeholder="写下你的问题、经验或方案…"></textarea>

        <p v-if="error" class="err">{{ error }}</p>
        <div class="actions">
          <RouterLink to="/forum" class="btn">取消</RouterLink>
          <button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? '发布中…' : '发布帖子' }}</button>
        </div>
      </section>

      <section class="card preview">
        <p class="mono dim">// preview</p>
        <h2>{{ form.title || '预览标题' }}</h2>
        <MarkdownView :source="form.contentMarkdown || '正文预览会显示在这里。'" />
      </section>
    </form>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 70px; }
.editor { display: grid; grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr); gap: 18px; align-items: start; }
.form-panel, .preview { padding: 22px; }
.label { display: block; margin: 14px 0 6px; color: var(--text-soft); font-size: 13px; font-weight: 700; font-family: var(--font-mono); }
.label:first-child { margin-top: 0; }
.body-input { min-height: 340px; }
.preview { position: sticky; top: 82px; }
.preview h2 { margin: 0 0 14px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
.err { color: var(--danger); margin: 12px 0 0; }
@media (max-width: 920px) {
  .editor { grid-template-columns: 1fr; }
  .preview { position: static; }
}
</style>

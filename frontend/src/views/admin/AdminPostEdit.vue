<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminApi } from '../../api'
import type { Post } from '../../api/types'
import MarkdownView from '../../components/MarkdownView.vue'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const msg = ref('')
const saving = ref(false)
const showPreview = ref(true)

const form = ref<Partial<Post>>({
  title: '', slug: '', summary: '', bodyMarkdown: '', tags: '', category: '', published: false
})

onMounted(async () => {
  if (isEdit.value) {
    form.value = await adminApi.post(Number(route.params.id))
  }
})

function autoSlug() {
  if (!form.value.slug && form.value.title) {
    form.value.slug = form.value.title.trim().toLowerCase()
      .replace(/[^\w一-龥]+/g, '-').replace(/^-+|-+$/g, '')
  }
}

async function save() {
  if (!form.value.title || !form.value.slug) {
    msg.value = '标题和 slug 不能为空'
    return
  }
  saving.value = true
  msg.value = ''
  try {
    if (isEdit.value) {
      await adminApi.updatePost(Number(route.params.id), form.value)
    } else {
      await adminApi.createPost(form.value)
    }
    router.push('/admin/posts')
  } catch (e: any) {
    msg.value = e?.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <h1 class="section-title">{{ isEdit ? '编辑教程' : '新建教程' }}</h1>

    <div class="meta-row">
      <div class="field"><label>标题</label>
        <input class="input" v-model="form.title" @blur="autoSlug" placeholder="教程标题" />
      </div>
      <div class="field"><label>Slug（URL）</label>
        <input class="input" v-model="form.slug" placeholder="url-friendly-slug" />
      </div>
    </div>
    <div class="meta-row">
      <div class="field"><label>分类</label>
        <input class="input" v-model="form.category" placeholder="如：入门 / 进阶" />
      </div>
      <div class="field"><label>标签（逗号分隔）</label>
        <input class="input" v-model="form.tags" placeholder="入门,提示词" />
      </div>
    </div>
    <div class="field"><label>摘要</label>
      <textarea class="textarea summary" v-model="form.summary" placeholder="一句话简介"></textarea>
    </div>

    <div class="editor-head">
      <label>正文（Markdown）</label>
      <button class="btn btn-sm" @click="showPreview = !showPreview">
        {{ showPreview ? '隐藏预览' : '显示预览' }}
      </button>
    </div>
    <div class="editor" :class="{ split: showPreview }">
      <textarea class="textarea body" v-model="form.bodyMarkdown" placeholder="# 标题&#10;支持 Markdown…"></textarea>
      <div v-if="showPreview" class="preview card">
        <MarkdownView :source="form.bodyMarkdown" />
      </div>
    </div>

    <div class="foot">
      <label class="pub">
        <input type="checkbox" v-model="form.published" /> 立即发布
      </label>
      <span class="err">{{ msg }}</span>
      <div class="spacer"></div>
      <button class="btn" @click="router.push('/admin/posts')">取消</button>
      <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
    </div>
  </div>
</template>

<style scoped>
.meta-row { display: flex; gap: 14px; flex-wrap: wrap; }
.field { flex: 1; min-width: 200px; margin-bottom: 14px; }
.field label, .editor-head label { display: block; font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.summary { min-height: 60px; }
.editor-head { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.editor { display: grid; grid-template-columns: 1fr; gap: 14px; }
.editor.split { grid-template-columns: 1fr 1fr; }
.body { min-height: 420px; font-family: ui-monospace, Menlo, Consolas, monospace; }
.preview { padding: 16px; overflow-y: auto; max-height: 420px; }
.foot { display: flex; align-items: center; gap: 12px; margin-top: 18px; flex-wrap: wrap; }
.pub { display: flex; align-items: center; gap: 6px; }
.spacer { flex: 1; }
.err { color: var(--danger); }
@media (max-width: 720px) { .editor.split { grid-template-columns: 1fr; } }
</style>

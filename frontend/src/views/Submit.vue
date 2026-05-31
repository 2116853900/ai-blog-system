<script setup lang="ts">
import { ref, reactive } from 'vue'
import { publicApi } from '../api'
import type { SubmissionType } from '../api/types'
import { toast } from '../composables/useToast'

const type = ref<SubmissionType>('SKILL')
const contactInfo = ref('')
const submitting = ref(false)
const errorKey = ref('')

const form = reactive<Record<string, string>>({
  name: '', description: '', tags: '', category: '',
  link: '', repoUrl: '', installCmd: '', baseUrl: '', supportedModels: ''
})

const fieldsByType: Record<SubmissionType, { key: string; label: string; placeholder?: string; textarea?: boolean }[]> = {
  SKILL: [
    { key: 'name', label: '名称' },
    { key: 'description', label: '描述', textarea: true },
    { key: 'link', label: '链接', placeholder: 'https://' },
    { key: 'category', label: '分类', placeholder: '如：技巧 / 入门 / 进阶' },
    { key: 'tags', label: '标签', placeholder: '逗号分隔，如：提示词,入门' }
  ],
  MCP: [
    { key: 'name', label: '名称' },
    { key: 'description', label: '描述', textarea: true },
    { key: 'repoUrl', label: '仓库地址', placeholder: 'https://github.com/...' },
    { key: 'installCmd', label: '安装命令', placeholder: 'npx -y ...' },
    { key: 'category', label: '分类', placeholder: '如：官方 / 数据库 / 自动化' },
    { key: 'tags', label: '标签', placeholder: '逗号分隔' }
  ],
  API: [
    { key: 'name', label: '站点名称' },
    { key: 'baseUrl', label: 'API 地址', placeholder: 'https://api.example.com' },
    { key: 'description', label: '描述', textarea: true },
    { key: 'supportedModels', label: '支持模型', placeholder: '逗号分隔，如：gpt-4o,claude-3.5' },
    { key: 'tags', label: '标签', placeholder: '逗号分隔，如：公益,中转' }
  ]
}

const typeLabels: Record<SubmissionType, string> = { SKILL: 'AI Skill', MCP: 'MCP', API: '公益 API 站点' }

async function submit() {
  const fields = fieldsByType[type.value]
  const payload: Record<string, string> = {}
  for (const f of fields) {
    if (form[f.key]) payload[f.key] = form[f.key]
  }
  errorKey.value = ''
  if (!payload.name) {
    errorKey.value = 'name'
    toast.error('请至少填写名称')
    return
  }
  submitting.value = true
  try {
    const res = await publicApi.submit({
      type: type.value,
      payloadJson: JSON.stringify(payload),
      contactInfo: contactInfo.value || undefined
    })
    toast.success(res.message || '投稿成功，感谢分享！')
    for (const k of Object.keys(form)) form[k] = ''
    contactInfo.value = ''
  } catch {
    // 网络/5xx 错误已由 http 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

function switchType(key: SubmissionType) {
  type.value = key
  errorKey.value = ''
}
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <h1 class="section-title prompt">投稿分享</h1>
      <p class="muted">发现好用的 Skill / MCP / 公益 API？分享给大家，审核通过后会展示在站点上。</p>
    </header>

    <form class="card form" @submit.prevent="submit">
      <fieldset class="type-fieldset">
        <legend class="label">投稿类型</legend>
        <div class="type-tabs" role="tablist">
          <button
            v-for="(label, key) in typeLabels"
            :key="key"
            type="button"
            role="tab"
            :aria-selected="type === key"
            class="btn"
            :class="{ 'btn-primary': type === key }"
            @click="switchType(key as SubmissionType)"
          >{{ label }}</button>
        </div>
      </fieldset>

      <div v-for="f in fieldsByType[type]" :key="f.key" class="field">
        <label class="label" :for="`f-${f.key}`">{{ f.label }}<span v-if="f.key === 'name'" class="req" aria-hidden="true"> *</span></label>
        <textarea v-if="f.textarea" :id="`f-${f.key}`" class="textarea" v-model="form[f.key]" :placeholder="f.placeholder"></textarea>
        <input
          v-else :id="`f-${f.key}`" class="input" v-model="form[f.key]"
          :placeholder="f.placeholder"
          :aria-invalid="errorKey === f.key"
        />
        <span v-if="errorKey === f.key" class="field-err">此项为必填</span>
      </div>

      <div class="field">
        <label class="label" for="f-contact">联系方式（可选）</label>
        <input id="f-contact" class="input" v-model="contactInfo" placeholder="邮箱 / 社交账号，便于联系" />
      </div>

      <div class="form-foot">
        <span class="dim mono">→ 审核后展示</span>
        <button class="btn btn-primary" type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : '提交投稿' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; max-width: 640px; }
.page-head { margin-bottom: 4px; }
.form { padding: 24px; margin-top: 18px; }
.type-fieldset { border: none; padding: 0; margin: 0 0 18px; }
.type-tabs { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.field { margin-bottom: 16px; }
.label { display: block; font-size: 13px; margin-bottom: 6px; font-weight: 600; font-family: var(--font-mono); color: var(--text-soft); }
.req { color: var(--danger); }
.field-err { display: block; margin-top: 5px; font-size: 12px; color: var(--danger); }
.form-foot { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 8px; }
</style>

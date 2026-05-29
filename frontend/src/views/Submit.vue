<script setup lang="ts">
import { ref, reactive } from 'vue'
import { publicApi } from '../api'
import type { SubmissionType } from '../api/types'

const type = ref<SubmissionType>('SKILL')
const contactInfo = ref('')
const msg = ref('')
const ok = ref(false)
const submitting = ref(false)

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
  if (!payload.name) {
    msg.value = '请至少填写名称'
    ok.value = false
    return
  }
  submitting.value = true
  msg.value = ''
  try {
    const res = await publicApi.submit({
      type: type.value,
      payloadJson: JSON.stringify(payload),
      contactInfo: contactInfo.value || undefined
    })
    msg.value = res.message || '投稿成功'
    ok.value = true
    for (const k of Object.keys(form)) form[k] = ''
    contactInfo.value = ''
  } catch {
    msg.value = '提交失败，请稍后再试'
    ok.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page">
    <h1 class="section-title">📝 投稿分享</h1>
    <p class="muted">发现好用的 Skill / MCP / 公益 API？分享给大家，审核通过后会展示在站点上。</p>

    <div class="card form">
      <label class="label">投稿类型</label>
      <div class="type-tabs">
        <button
          v-for="(label, key) in typeLabels"
          :key="key"
          class="btn"
          :class="{ 'btn-primary': type === key }"
          @click="type = key as SubmissionType"
        >{{ label }}</button>
      </div>

      <div v-for="f in fieldsByType[type]" :key="f.key" class="field">
        <label class="label">{{ f.label }}</label>
        <textarea v-if="f.textarea" class="textarea" v-model="form[f.key]" :placeholder="f.placeholder"></textarea>
        <input v-else class="input" v-model="form[f.key]" :placeholder="f.placeholder" />
      </div>

      <div class="field">
        <label class="label">联系方式（可选）</label>
        <input class="input" v-model="contactInfo" placeholder="邮箱 / 社交账号，便于联系" />
      </div>

      <div class="form-foot">
        <span :class="ok ? 'ok' : 'err'">{{ msg }}</span>
        <button class="btn btn-primary" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '提交投稿' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 60px; max-width: 640px; }
.form { padding: 24px; margin-top: 18px; }
.type-tabs { display: flex; gap: 8px; margin-bottom: 18px; flex-wrap: wrap; }
.field { margin-bottom: 16px; }
.label { display: block; font-size: 14px; margin-bottom: 6px; font-weight: 600; }
.form-foot { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.ok { color: var(--accent); }
.err { color: var(--danger); }
</style>

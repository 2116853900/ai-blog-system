<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { publicApi } from '../api'
import type { Skill } from '../api/types'
import CommentSection from '../components/CommentSection.vue'
import LinkedDiscussions from '../components/LinkedDiscussions.vue'
import Skeleton from '../components/Skeleton.vue'
import StarRating from '../components/StarRating.vue'
import TagList from '../components/TagList.vue'

const route = useRoute()
const skill = ref<Skill | null>(null)
const loading = ref(true)
const notFound = ref(false)

async function load() {
  loading.value = true
  notFound.value = false
  skill.value = null
  try {
    skill.value = await publicApi.skill(Number(route.params.id))
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div class="container detail-page">
    <RouterLink to="/skills" class="back mono">← 返回 Skill 列表</RouterLink>

    <div v-if="loading" class="loading">
      <Skeleton block height="34px" width="62%" />
      <Skeleton block height="18px" width="180px" radius="6px" />
      <Skeleton block height="14px" />
      <Skeleton block height="14px" width="78%" />
    </div>

    <div v-else-if="notFound" class="notfound">
      <span class="nf-mark mono" aria-hidden="true">404</span>
      <p class="muted">Skill 不存在。</p>
      <RouterLink to="/skills" class="btn">返回列表</RouterLink>
    </div>

    <article v-else-if="skill" class="detail-card card">
      <header class="detail-head">
        <div>
          <span v-if="skill.category" class="chip chip-active">{{ skill.category }}</span>
          <h1 class="mono">{{ skill.name }}</h1>
        </div>
        <StarRating :level="skill.recommendLevel" />
      </header>

      <p class="desc">{{ skill.description || '暂无描述。' }}</p>
      <TagList :tags="skill.tags" />
      <a v-if="skill.link" :href="skill.link" target="_blank" rel="noopener" class="btn btn-primary action">
        访问 Skill ↗
      </a>

      <hr class="sep" />
      <LinkedDiscussions ref-type="SKILL" :ref-id="skill.id" :source-title="skill.name" />

      <hr class="sep" />
      <CommentSection ref-type="SKILL" :ref-id="skill.id" />
    </article>
  </div>
</template>

<style scoped>
.detail-page { padding: 24px 0 70px; max-width: 820px; }
.back { display: inline-block; margin-bottom: 20px; font-size: 13px; }
.loading { display: flex; flex-direction: column; gap: 14px; }
.notfound { display: grid; place-items: center; gap: 14px; padding: 60px 0; text-align: center; }
.nf-mark { font-size: 56px; font-weight: 800; color: var(--primary-dim); }
.detail-card { padding: 26px; }
.detail-head { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; margin-bottom: 16px; }
.detail-head h1 { margin: 10px 0 0; font-size: 30px; line-height: 1.25; }
.desc { margin: 0 0 18px; line-height: 1.85; color: var(--text-soft); }
.action { margin-top: 20px; }
.sep { border: none; border-top: 1px dashed var(--border-strong); margin: 28px 0; }
@media (max-width: 640px) {
  .detail-head { flex-direction: column; }
  .detail-card { padding: 20px; }
}
</style>

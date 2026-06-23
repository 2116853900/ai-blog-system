<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accountApi, publicApi } from '../api'
import type { ResourceFavoriteRefType } from '../api/types'
import { toast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{
  refType: ResourceFavoriteRefType
  refId: number
}>()

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const toggling = ref(false)
const favorited = ref(false)
const favoriteCount = ref(0)

const label = computed(() => {
  if (!auth.isLoggedIn()) return '登录后收藏'
  return favorited.value ? '已收藏' : '收藏'
})

async function load() {
  if (!props.refId) return
  loading.value = true
  try {
    const data = await publicApi.resourceFavoriteInteraction(props.refType, props.refId)
    favorited.value = data.favorited
    favoriteCount.value = data.favoriteCount
  } catch {
    favorited.value = false
    favoriteCount.value = 0
  } finally {
    loading.value = false
  }
}

async function toggle() {
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (toggling.value) return
  toggling.value = true
  try {
    const data = favorited.value
      ? await accountApi.unfavoriteResource(props.refType, props.refId)
      : await accountApi.favoriteResource(props.refType, props.refId)
    favorited.value = data.favorited
    favoriteCount.value = data.favoriteCount
    toast.success(favorited.value ? '已加入收藏' : '已取消收藏')
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '操作失败')
  } finally {
    toggling.value = false
  }
}

onMounted(load)
watch(() => [props.refType, props.refId, auth.token], load)
</script>

<template>
  <button
    type="button"
    class="btn resource-favorite"
    :class="{ active: favorited }"
    :disabled="loading || toggling || !refId"
    :aria-pressed="auth.isLoggedIn() ? favorited : undefined"
    @click="toggle"
  >
    <span class="mark" aria-hidden="true">{{ favorited ? '★' : '☆' }}</span>
    <span>{{ loading ? '加载中' : label }}</span>
    <span class="count mono">{{ favoriteCount }}</span>
  </button>
</template>

<style scoped>
.resource-favorite {
  min-height: 38px;
  white-space: nowrap;
}
.resource-favorite.active {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}
.mark {
  line-height: 1;
  color: var(--primary);
}
.count {
  min-width: 1.5em;
  padding-left: 3px;
  color: var(--text-soft);
  text-align: right;
}
.resource-favorite.active .count {
  color: var(--primary);
}
</style>

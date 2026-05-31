import { computed, onMounted, ref, type Ref } from 'vue'

interface HasMeta {
  tags?: string
  category?: string
}

type Fetcher<T> = (params: { q?: string; tag?: string; category?: string }) => Promise<T[]>

/**
 * 列表视图通用逻辑：加载、防抖搜索（由 SearchBar 触发）、分类 chips、标签筛选。
 * 分类列表从已加载数据派生，避免新增后端接口。
 */
export function useListView<T extends HasMeta>(fetcher: Fetcher<T>) {
  const items = ref<T[]>([]) as Ref<T[]>
  const loading = ref(true)
  const loaded = ref(false)
  const q = ref('')
  const activeTag = ref('')
  const activeCategory = ref('')

  // 全量分类（首次加载后固定，作为筛选 chips）
  const allCategories = ref<string[]>([])

  async function load() {
    loading.value = true
    try {
      items.value = await fetcher({
        q: q.value || undefined,
        tag: activeTag.value || undefined,
        category: activeCategory.value || undefined
      })
      if (!loaded.value) {
        const cats = new Set<string>()
        for (const it of items.value) {
          if (it.category) cats.add(it.category)
        }
        allCategories.value = [...cats].sort()
        loaded.value = true
      }
    } catch {
      items.value = []
    } finally {
      loading.value = false
    }
  }

  function toggleTag(t: string) {
    activeTag.value = activeTag.value === t ? '' : t
    load()
  }

  function selectCategory(c: string) {
    activeCategory.value = activeCategory.value === c ? '' : c
    load()
  }

  function reset() {
    q.value = ''
    activeTag.value = ''
    activeCategory.value = ''
    load()
  }

  const isEmpty = computed(() => !loading.value && items.value.length === 0)
  const hasFilter = computed(() => !!(q.value || activeTag.value || activeCategory.value))

  onMounted(load)

  return {
    items, loading, q, activeTag, activeCategory, allCategories,
    isEmpty, hasFilter, load, toggleTag, selectCategory, reset
  }
}

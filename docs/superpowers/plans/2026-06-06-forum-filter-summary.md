# Forum Filter Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show active forum list filters in one compact summary row and provide a single action to clear them.

**Architecture:** Keep all filter state local to `frontend/src/views/Forum.vue`, reusing existing URL sync and `loadThreads` flow. Add computed summary state, a `clearAllFilters` helper, and replace the tag-only active filter row with a compact active-filter bar that can show category, search, tag, unanswered, and non-default sort together.

**Tech Stack:** Vue 3 Composition API, Vue Router, TypeScript, Vite.

---

## Project Analysis

The forum list now supports category filtering, keyword search, tag filtering, unanswered-only filtering, and sorting. These filters are spread across the sidebar and tool area, with only tag having a visible clear action. Users can end up in a filtered state without a single way to understand or reset every active condition. A compact summary row solves this without adding new backend behavior.

The current worktree contains existing forum list backend and frontend changes. This plan only touches `frontend/src/views/Forum.vue` and this plan document.

## File Structure

- Modify `frontend/src/views/Forum.vue`
  - Add computed active filter labels.
  - Add `sortLabel` helper.
  - Add `clearAllFilters` helper that resets category, search, tag, unanswered, sort, and page.
  - Replace the tag-only active-filter row with a filter summary row.
  - Add scoped CSS for dense filter chips and reset action.

## Task 1: Filter Summary UI

**Files:**
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Add computed filter state**

Add after `parents`:

```ts
const activeFilters = computed(() => {
  const filters: string[] = []
  if (selectedCategoryId.value) filters.push(`板块：${categoryName(selectedCategoryId.value)}`)
  if (q.value.trim()) filters.push(`搜索：${q.value.trim()}`)
  if (selectedTag.value) filters.push(`标签：${selectedTag.value}`)
  if (unansweredOnly.value) filters.push('只看未回复')
  if (sort.value !== 'latest') filters.push(`排序：${sortLabel(sort.value)}`)
  return filters
})
```

Add after `parseSort`:

```ts
function sortLabel(value: ForumSort) {
  return sortOptions.find(option => option.value === value)?.label || '最近活跃'
}
```

- [ ] **Step 2: Add clear helper**

Add after `clearTag`:

```ts
async function clearAllFilters() {
  selectedCategoryId.value = undefined
  q.value = ''
  selectedTag.value = ''
  unansweredOnly.value = false
  sort.value = 'latest'
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}
```

- [ ] **Step 3: Replace tag-only active row**

Replace:

```vue
        <div v-if="selectedTag" class="active-filter">
          <span>标签：{{ selectedTag }}</span>
          <button type="button" @click="clearTag">清除</button>
        </div>
```

with:

```vue
        <div v-if="activeFilters.length" class="active-filter">
          <span v-for="filter in activeFilters" :key="filter" class="filter-chip">{{ filter }}</span>
          <button type="button" @click="clearAllFilters">清除全部</button>
        </div>
```

- [ ] **Step 4: Update styles**

Replace `.active-filter` and `.active-filter button` CSS with:

```css
.active-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--text-soft);
  font-size: 13px;
}
.filter-chip {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  padding: 5px 8px;
}
.active-filter button {
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  cursor: pointer;
  padding: 5px 8px;
}
```

- [ ] **Step 5: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

## Task 2: Regression Verification

- [ ] **Step 1: Run backend forum tests**

Run from `backend`: `mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

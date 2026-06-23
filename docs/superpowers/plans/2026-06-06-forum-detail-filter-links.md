# Forum Detail Filter Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let visitors jump from a forum thread detail page back to the forum list filtered by the current thread's category or tag.

**Architecture:** Reuse the existing `/forum` list route and its `categoryId` and `tag` query support. Keep the change scoped to `ForumThreadDetail.vue` by replacing static category/tag chips with `RouterLink` chips and adding small route helper functions.

**Tech Stack:** Vue 3, Vue Router, TypeScript, Vite.

---

## Project Analysis

The forum list now supports category, tag, unanswered, sort, and active-filter summaries. The thread detail page still renders its category and tags as inert chips, which strands users who discover a useful topic from a detail page. Linking these chips into the existing list filters improves navigation without backend work.

The current worktree has pending forum list enhancements and a popular-tags endpoint. This plan only touches `frontend/src/views/ForumThreadDetail.vue` and this plan document.

## File Structure

- Modify `frontend/src/views/ForumThreadDetail.vue`
  - Add `categoryFilterLink(id)` helper returning `/forum?categoryId=<id>`.
  - Add `tagFilterLink(tag)` helper returning `/forum?tag=<tag>`.
  - Change the category chip in the detail header from `span` to `RouterLink`.
  - Change tag chips below the header from `span` to `RouterLink`.
  - Add a small scoped style to prevent linked chips from underlining.

## Task 1: Detail Filter Links

**Files:**
- Modify: `frontend/src/views/ForumThreadDetail.vue`

- [ ] **Step 1: Add route helpers**

Add after `tagsOf`:

```ts
function categoryFilterLink(id?: number) {
  return id ? `/forum?categoryId=${id}` : '/forum'
}

function tagFilterLink(tag: string) {
  return `/forum?tag=${encodeURIComponent(tag)}`
}
```

- [ ] **Step 2: Link the category chip**

Replace:

```vue
<span class="chip chip-active">{{ categoryName(thread.categoryId) }}</span>
```

with:

```vue
<RouterLink class="chip chip-active filter-link" :to="categoryFilterLink(thread.categoryId)">
  {{ categoryName(thread.categoryId) }}
</RouterLink>
```

- [ ] **Step 3: Link the tag chips**

Replace:

```vue
<span v-for="tag in tagsOf(thread.tags)" :key="tag" class="tag">{{ tag }}</span>
```

with:

```vue
<RouterLink v-for="tag in tagsOf(thread.tags)" :key="tag" class="tag filter-link" :to="tagFilterLink(tag)">
  {{ tag }}
</RouterLink>
```

- [ ] **Step 4: Add link style**

Add scoped CSS:

```css
.filter-link:hover { text-decoration: none; }
```

- [ ] **Step 5: Run frontend build**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

## Task 2: Regression Verification

- [ ] **Step 1: Run backend forum tests**

Run from `backend`:

```bash
mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

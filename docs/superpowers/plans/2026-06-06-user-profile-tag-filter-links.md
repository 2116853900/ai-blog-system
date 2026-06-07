# User Profile Tag Filter Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let visitors click tags in a public user's thread list to jump back to the forum list filtered by that tag.

**Architecture:** Reuse the forum list's existing `tag` query support. In `UserProfile.vue`, change thread activity rows from a whole-row `RouterLink` to a normal container so each row can contain focused links for the thread title and tag chips without nesting anchors.

**Tech Stack:** Vue 3, Vue Router, TypeScript, Vite.

---

## Project Analysis

The forum list and thread detail page now support tag navigation into `/forum?tag=...`. Public user profiles also show each user's recent threads and their tags, but those tags are inert inside an all-row link. Making those tags navigable closes another discovery path while keeping the row layout intact.

The current worktree already contains forum list and detail navigation enhancements. This plan only touches `frontend/src/views/UserProfile.vue` and this plan document.

## File Structure

- Modify `frontend/src/views/UserProfile.vue`
  - Add `threadLink(id)` and `tagFilterLink(tag)` helpers.
  - Change only the `threads` activity rows from `RouterLink` to `div`.
  - Wrap thread titles in `RouterLink`.
  - Render thread tags as `RouterLink` chips to `/forum?tag=<tag>`.
  - Add compact title/filter link styles.

## Task 1: User Profile Thread Tag Links

**Files:**
- Modify: `frontend/src/views/UserProfile.vue`

- [ ] **Step 1: Add route helpers**

Add after `tagsOf`:

```ts
function threadLink(id: number) {
  return `/forum/threads/${id}`
}

function tagFilterLink(tag: string) {
  return `/forum?tag=${encodeURIComponent(tag)}`
}
```

- [ ] **Step 2: Change thread rows to focused links**

Replace the thread activity row root:

```vue
<RouterLink
  v-for="thread in threads?.content"
  :key="thread.id"
  :to="`/forum/threads/${thread.id}`"
  class="activity-item"
>
```

with:

```vue
<div
  v-for="thread in threads?.content"
  :key="thread.id"
  class="activity-item"
>
```

Replace:

```vue
<h3>{{ thread.title }}</h3>
```

with:

```vue
<h3><RouterLink class="thread-title-link" :to="threadLink(thread.id)">{{ thread.title }}</RouterLink></h3>
```

Replace:

```vue
<span v-for="tag in tagsOf(thread.tags).slice(0, 5)" :key="tag" class="tag">{{ tag }}</span>
```

with:

```vue
<RouterLink v-for="tag in tagsOf(thread.tags).slice(0, 5)" :key="tag" class="tag filter-link" :to="tagFilterLink(tag)">
  {{ tag }}
</RouterLink>
```

Close the thread row with `</div>` instead of `</RouterLink>`.

- [ ] **Step 3: Add scoped styles**

Add:

```css
.thread-title-link { color: var(--text); }
.thread-title-link:hover { color: var(--primary); text-decoration: none; }
.filter-link:hover { text-decoration: none; }
```

- [ ] **Step 4: Run frontend build**

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

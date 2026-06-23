# Forum Author Profile Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let visitors jump from the forum thread list to public user profiles for thread authors and last reply users.

**Architecture:** Reuse existing `/users/:id` route and profile preloading already present in `Forum.vue`. Change each thread card from an all-card `RouterLink` to a normal card container with focused links for the title, author, and last reply user so links are valid and do not nest inside one another.

**Tech Stack:** Vue 3, Vue Router, TypeScript, Vite.

---

## Project Analysis

The forum list already calls `loadProfiles` for each thread author and `lastReplyUserId`, but it only renders the author's display name as plain text. The project already has a public user profile route at `/users/:id` and a `UserProfile.vue` page. Adding profile links improves navigation and uses data already loaded by the page.

The current worktree contains previous forum list enhancements: sorting, tag filtering, and unanswered filtering. This plan only touches `frontend/src/views/Forum.vue` and this plan document.

## File Structure

- Modify `frontend/src/views/Forum.vue`
  - Add `threadLink(id)` and `userLink(id)` helper functions.
  - Change the thread list card root from `RouterLink` to a `div`.
  - Wrap each thread title in a `RouterLink` to the thread detail page.
  - Add author profile link in the metadata line.
  - Add last reply user profile link in the stats column when `lastReplyUserId` is present.
  - Add compact link styling that matches the existing forum list density.

## Task 1: Frontend Profile Links

**Files:**
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Add route helpers**

Add after `authorName`:

```ts
function threadLink(id: number) {
  return `/forum/threads/${id}`
}

function userLink(id: number) {
  return `/users/${id}`
}
```

- [ ] **Step 2: Change card root and add links**

Change the thread card root from:

```vue
<RouterLink
  v-for="(t, i) in threads?.content"
  :key="t.id"
  :to="`/forum/threads/${t.id}`"
  class="card thread-card rise"
  :style="{ animationDelay: `${Math.min(i * 0.035, 0.35)}s` }"
>
```

to:

```vue
<div
  v-for="(t, i) in threads?.content"
  :key="t.id"
  class="card thread-card rise"
  :style="{ animationDelay: `${Math.min(i * 0.035, 0.35)}s` }"
>
```

Change title from:

```vue
<h3>{{ t.title }}</h3>
```

to:

```vue
<h3><RouterLink :to="threadLink(t.id)" class="thread-title-link">{{ t.title }}</RouterLink></h3>
```

Change metadata from:

```vue
<p class="muted meta mono">by {{ authorName(t.authorId) }} · {{ fmt(t.createdAt) }}</p>
```

to:

```vue
<p class="muted meta mono">
  by <RouterLink :to="userLink(t.authorId)" class="profile-link">{{ authorName(t.authorId) }}</RouterLink>
  · {{ fmt(t.createdAt) }}
</p>
```

Change stats from:

```vue
<span>last {{ fmt(t.lastReplyAt || t.createdAt) }}</span>
```

to:

```vue
<span v-if="t.lastReplyUserId">
  last by <RouterLink :to="userLink(t.lastReplyUserId)" class="profile-link">{{ authorName(t.lastReplyUserId) }}</RouterLink>
</span>
<span>last {{ fmt(t.lastReplyAt || t.createdAt) }}</span>
```

Close the card root with `</div>` instead of `</RouterLink>`.

- [ ] **Step 3: Add styles**

Add scoped CSS:

```css
.thread-title-link { color: var(--text); }
.thread-title-link:hover,
.profile-link:hover { color: var(--primary); }
.profile-link {
  color: var(--text-soft);
  text-decoration: none;
}
```

- [ ] **Step 4: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

## Task 2: Regression Verification

- [ ] **Step 1: Run backend forum tests**

Run from `backend`: `mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

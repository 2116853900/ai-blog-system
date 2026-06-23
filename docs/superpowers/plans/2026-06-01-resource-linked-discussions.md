# Resource Linked Discussions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add linked forum discussions to tutorial, Skill, MCP, and API station detail pages, with a one-click flow to start a forum thread tied back to the current resource.

**Architecture:** Reuse the existing backend `GET /api/forum/threads/linked` endpoint and the existing `linkedRefType` / `linkedRefId` fields in `ThreadRequest` and `ForumThread`. Add a reusable Vue component that loads linked threads for a resource, renders compact discussion cards, and links authenticated users into `/forum/new` with query parameters. Update `ForumNew.vue` to read those query parameters, prefill the title, include the link fields in create/update payloads, and show the resource association.

**Tech Stack:** Vue 3 Composition API, Vue Router, TypeScript, Vite, existing Spring Boot forum APIs.

---

## File Structure

- Create: `frontend/src/components/LinkedDiscussions.vue`
  - Reusable resource discussion panel for detail pages.
- Modify: `frontend/src/views/ForumNew.vue`
  - Read `linkedRefType`, `linkedRefId`, and `sourceTitle` query params.
  - Include `linkedRefType` and `linkedRefId` in create/update payloads.
  - Preserve existing linked refs when editing a thread.
- Modify: `frontend/src/views/TutorialDetail.vue`
  - Add linked discussions for `POST`.
- Modify: `frontend/src/views/SkillDetail.vue`
  - Add linked discussions for `SKILL`.
- Modify: `frontend/src/views/McpDetail.vue`
  - Add linked discussions for `MCP`.
- Modify: `frontend/src/views/ApiStationDetail.vue`
  - Add linked discussions for `API`.
- Modify: `README.md`
  - Document that resource detail pages show and create linked forum discussions.

---

### Task 1: Reusable Linked Discussions Component

**Files:**
- Create: `frontend/src/components/LinkedDiscussions.vue`

- [x] **Step 1: Create `LinkedDiscussions.vue`**

Create a component with these props:

```ts
const props = defineProps<{
  refType: RefType
  refId: number
  sourceTitle: string
}>()
```

It should:

- call `forumApi.linkedThreads(props.refType, props.refId)` on mount and whenever props change;
- load author profiles with `userApi.profile`;
- render up to 5 linked threads with title, author, reply count, view count, and last activity;
- render an empty state text when no discussions exist;
- render a CTA to `/forum/new?linkedRefType=<type>&linkedRefId=<id>&sourceTitle=<title>`, or `/login?redirect=<encoded forum-new-url>` when not logged in.

- [x] **Step 2: Build frontend to catch type errors**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS after the component compiles.

---

### Task 2: Forum New Linked Thread Flow

**Files:**
- Modify: `frontend/src/views/ForumNew.vue`

- [x] **Step 1: Add linked refs to form state**

Extend the form state:

```ts
const form = reactive({
  categoryId: 0,
  title: '',
  tags: '',
  contentMarkdown: '',
  linkedRefType: undefined as RefType | undefined,
  linkedRefId: undefined as number | undefined
})
```

- [x] **Step 2: Initialize linked refs from route query**

Add helpers:

```ts
function queryString(name: string): string {
  const value = route.query[name]
  return typeof value === 'string' ? value : ''
}

function queryRefType(): RefType | undefined {
  const value = queryString('linkedRefType')
  return ['POST', 'SKILL', 'MCP', 'API'].includes(value) ? value as RefType : undefined
}
```

On create, set `form.linkedRefType`, `form.linkedRefId`, prefill title as `关于「${sourceTitle}」的讨论`, and prefill body with a short prompt if the editor is empty.

- [x] **Step 3: Include linked refs in create/update payload**

Add `linkedRefType` and `linkedRefId` to the body passed to `forumApi.createThread` and `forumApi.updateThread`.

- [x] **Step 4: Preserve linked refs on edit**

When editing, copy `originalThread.linkedRefType` and `originalThread.linkedRefId` into the form.

- [x] **Step 5: Show linked source notice**

Above the title input, show a small notice when `form.linkedRefType && form.linkedRefId`:

```vue
<div class="linked-note">
  <span class="mono">linked {{ form.linkedRefType }} #{{ form.linkedRefId }}</span>
  <span v-if="sourceTitle">「{{ sourceTitle }}」</span>
</div>
```

- [x] **Step 6: Build frontend**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

---

### Task 3: Detail Page Integration

**Files:**
- Modify: `frontend/src/views/TutorialDetail.vue`
- Modify: `frontend/src/views/SkillDetail.vue`
- Modify: `frontend/src/views/McpDetail.vue`
- Modify: `frontend/src/views/ApiStationDetail.vue`

- [x] **Step 1: Import and render the component in each detail page**

For each detail page, import:

```ts
import LinkedDiscussions from '../components/LinkedDiscussions.vue'
```

Then render before comments:

```vue
<LinkedDiscussions ref-type="POST" :ref-id="post.id" :source-title="post.title" />
```

Use `SKILL` / `skill.name`, `MCP` / `mcp.name`, and `API` / `station.name` for the other pages.

- [x] **Step 2: Build frontend**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

---

### Task 4: Documentation and Verification

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README**

Update the feature line to mention “资源关联论坛讨论”. Update the end-to-end verification list with a step: open a resource detail page, click “发起讨论”, publish a thread, and confirm it appears in that resource’s discussion panel.

- [x] **Step 2: Run frontend production build**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

- [x] **Step 3: Run backend tests**

Run from `backend`:

```bash
mvn -q test
```

Expected: PASS. Backend code should not change, but this confirms existing contracts still compile against the project.

---

## Self-Review

Spec coverage:
- Resource detail pages gain a discussion panel in Task 3.
- Thread creation preserves backend `linkedRefType` and `linkedRefId` in Task 2.
- Existing backend linked-thread endpoint is reused in Task 1.
- Documentation and verification are covered in Task 4.

Placeholder scan:
- No TBD, TODO, or unspecified implementation steps remain.

Type consistency:
- Resource types use the existing `RefType` union: `POST`, `SKILL`, `MCP`, `API`.
- Frontend API method is the existing `forumApi.linkedThreads`.
- Route query names are `linkedRefType`, `linkedRefId`, and `sourceTitle`.

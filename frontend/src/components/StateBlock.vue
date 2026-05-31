<script setup lang="ts">
defineProps<{
  loading?: boolean
  empty?: boolean
  emptyText?: string
}>()
</script>

<template>
  <!-- 加载中：显示骨架（由调用方通过 #skeleton slot 提供） -->
  <div v-if="loading" class="state-loading">
    <slot name="skeleton">
      <p class="muted prompt">loading<span class="cursor"></span></p>
    </slot>
  </div>

  <!-- 空状态 -->
  <div v-else-if="empty" class="state-empty" role="status">
    <slot name="empty">
      <div class="empty-card">
        <span class="empty-mark" aria-hidden="true">∅</span>
        <p class="muted">{{ emptyText || '没有找到相关内容。' }}</p>
      </div>
    </slot>
  </div>

  <!-- 正常内容 -->
  <slot v-else />
</template>

<style scoped>
.state-empty { padding: 48px 0; display: grid; place-items: center; }
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 36px 48px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius);
  background: var(--bg-elevated);
}
.empty-mark {
  font-family: var(--font-mono);
  font-size: 40px;
  color: var(--primary-dim);
  line-height: 1;
}
</style>

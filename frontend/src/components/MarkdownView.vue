<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'

const props = defineProps<{ source?: string }>()
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const html = computed(() => md.render(props.source || ''))
</script>

<template>
  <div class="markdown-body" v-html="html"></div>
</template>

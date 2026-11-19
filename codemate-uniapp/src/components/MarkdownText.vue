<template>
  <view class="markdown-text">
    <template v-for="(block, index) in blocks" :key="index">
      <view v-if="block.type === 'code'" class="markdown-text__code">
        <text selectable>{{ block.text }}</text>
      </view>
      <text v-else-if="block.type === 'heading'" class="markdown-text__heading">{{ block.text }}</text>
      <view v-else-if="block.type === 'list'" class="markdown-text__list-item">
        <text class="markdown-text__bullet">•</text>
        <text selectable>{{ block.text }}</text>
      </view>
      <text v-else class="markdown-text__paragraph" selectable>{{ block.text }}</text>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface MarkdownBlock {
  type: 'code' | 'heading' | 'list' | 'paragraph'
  text: string
}

const props = defineProps<{ content: string }>()

const blocks = computed<MarkdownBlock[]>(() => {
  const source = props.content || ''
  const lines = source.split('\n')
  const result: MarkdownBlock[] = []
  let inCode = false
  let codeLines: string[] = []

  const flushCode = () => {
    if (codeLines.length) result.push({ type: 'code', text: codeLines.join('\n') })
    codeLines = []
  }

  lines.forEach((line) => {
    if (line.trim().startsWith('```')) {
      if (inCode) flushCode()
      else codeLines = []
      inCode = !inCode
      return
    }
    if (inCode) {
      codeLines.push(line)
      return
    }
    if (!line.trim()) return
    const heading = line.match(/^#{1,6}\s+(.*)$/)
    if (heading) {
      result.push({ type: 'heading', text: heading[1] })
      return
    }
    const list = line.match(/^\s*(?:[-*+]|\d+\.)\s+(.*)$/)
    if (list) {
      result.push({ type: 'list', text: list[1] })
      return
    }
    result.push({ type: 'paragraph', text: line })
  })

  if (inCode) flushCode()
  return result
})
</script>

<style scoped>
.markdown-text {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  font-size: 27rpx;
  line-height: 1.7;
  word-break: break-word;
}

.markdown-text__heading {
  color: #f8fafc;
  font-size: 32rpx;
  font-weight: 700;
}

.markdown-text__paragraph {
  white-space: pre-wrap;
}

.markdown-text__list-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.markdown-text__bullet {
  color: #93c5fd;
}

.markdown-text__code {
  padding: 20rpx;
  overflow-x: auto;
  border: 1rpx solid #334155;
  border-radius: 16rpx;
  color: #e2e8f0;
  background: #020617;
  font-family: Consolas, monospace;
  font-size: 23rpx;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>

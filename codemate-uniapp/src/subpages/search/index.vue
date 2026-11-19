<template>
  <view class="search-page">
    <input v-model="searchText" class="search-page__input" placeholder="筛选标签" confirm-type="search" />

    <view class="search-selected">
      <view class="search-selected__heading">
        <text>已选标签（{{ selectedTags.length }}）</text>
        <button v-if="selectedTags.length" @click="selectedTags = []">清空</button>
      </view>
      <view class="search-selected__tags">
        <text v-for="tag in selectedTags" :key="tag" @click="toggleTag(tag)">{{ tag }} ×</text>
        <text v-if="!selectedTags.length" class="search-selected__empty">尚未选择标签</text>
      </view>
    </view>

    <view class="search-body">
      <scroll-view class="search-categories" scroll-y>
        <view
          v-for="(category, index) in TAG_CATEGORIES"
          :key="category.text"
          class="search-category"
          :class="{ 'search-category--active': activeCategory === index }"
          @click="activeCategory = index"
        >{{ category.text }}</view>
      </scroll-view>
      <scroll-view class="search-tags" scroll-y>
        <text
          v-for="tag in visibleTags"
          :key="tag.id"
          class="search-tag"
          :class="{ 'search-tag--active': selectedTags.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >{{ tag.text }}</text>
        <view v-if="!visibleTags.length" class="search-tags__empty">没有匹配标签</view>
      </scroll-view>
    </view>

    <button class="search-submit" @click="goResult">
      {{ selectedTags.length ? `查看匹配伙伴 · ${selectedTags.length}` : '查看推荐伙伴' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { TAG_CATEGORIES } from '@/constants/tags'
import { requireLogin } from '@/utils/navigation'

const searchText = ref('')
const selectedTags = ref<string[]>([])
const activeCategory = ref(0)

const visibleTags = computed(() => {
  const category = TAG_CATEGORIES[activeCategory.value]
  const keyword = searchText.value.trim().toLowerCase()
  if (!keyword) return category?.children || []
  return (category?.children || []).filter((tag) => tag.text.toLowerCase().includes(keyword))
})

onLoad(() => requireLogin())

function toggleTag(tag: string) {
  const index = selectedTags.value.indexOf(tag)
  if (index >= 0) selectedTags.value.splice(index, 1)
  else selectedTags.value.push(tag)
}

function goResult() {
  const tags = encodeURIComponent(JSON.stringify(selectedTags.value))
  uni.navigateTo({ url: `/subpages/search/result?tags=${tags}` })
}
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  padding: 28rpx;
  background: #0f172a;
}

.search-page__input {
  height: 88rpx;
  padding: 0 28rpx;
  border-radius: 24rpx;
  color: #f8fafc;
  background: #1e293b;
}

.search-selected {
  margin-top: 24rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #1e293b;
}

.search-selected__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 26rpx;
}

.search-selected__heading button {
  padding: 0 16rpx;
  color: #94a3b8;
  background: transparent;
  font-size: 22rpx;
}

.search-selected__tags {
  display: flex;
  margin-top: 18rpx;
  flex-wrap: wrap;
  gap: 12rpx;
}

.search-selected__tags text {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  color: #bfdbfe;
  background: #172554;
  font-size: 22rpx;
}

.search-selected__tags .search-selected__empty {
  color: #64748b;
  background: transparent;
}

.search-body {
  display: flex;
  height: 650rpx;
  margin-top: 24rpx;
  overflow: hidden;
  border-radius: 24rpx;
  background: #1e293b;
}

.search-categories {
  width: 190rpx;
  height: 100%;
  background: #111c30;
}

.search-category {
  padding: 28rpx 20rpx;
  color: #94a3b8;
  font-size: 25rpx;
}

.search-category--active {
  color: #f8fafc;
  background: #334155;
}

.search-tags {
  display: flex;
  height: 100%;
  padding: 24rpx;
  flex: 1;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 14rpx;
}

.search-tag {
  height: 56rpx;
  padding: 0 20rpx;
  border: 1rpx solid #475569;
  border-radius: 16rpx;
  color: #cbd5e1;
  font-size: 23rpx;
  line-height: 56rpx;
}

.search-tag--active {
  border-color: #93c5fd;
  color: #bfdbfe;
  background: #172554;
}

.search-tags__empty {
  width: 100%;
  padding: 60rpx 0;
  color: #64748b;
  text-align: center;
}

.search-submit {
  margin-top: 28rpx;
  color: #0f172a;
  background: #f8fafc;
  font-size: 27rpx;
}
</style>

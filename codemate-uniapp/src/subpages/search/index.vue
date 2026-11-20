<template>
  <view class="search-page">
    <view class="search-header">
      <AppSearchBox v-model="searchText" placeholder="请输入要搜索的标签" @search="activeCategory = 0" />
      <button class="search-cancel" @click="clearSearch">取消</button>
    </view>
    <view class="search-body">
      <view class="search-panel selected-panel">
        <view class="search-panel-title">
          <view class="search-panel-heading"><text>已选标签</text><text>{{ selectedTags.length ? `已选择 ${selectedTags.length} 项` : '组合标签，缩小匹配范围' }}</text></view>
          <button v-if="selectedTags.length" class="clear-selected-btn" @click="selectedTags = []">清空</button>
        </view>
        <view v-if="selectedTags.length" class="selected-tags"><text v-for="tag in selectedTags" :key="tag" @click="toggleTag(tag)">{{ tag }}<wd-icon name="close" size="13px" /></text></view>
        <view v-else class="search-tip">还没有选择标签，可直接浏览全部伙伴</view>
      </view>
      <view class="search-panel tag-panel">
        <view class="search-panel-title"><text>选择标签</text><text class="tag-count">{{ visibleTags.length }} 个</text></view>
        <view class="tag-tree">
          <scroll-view class="tag-tree__nav" scroll-y>
            <view v-for="(category, index) in TAG_CATEGORIES" :key="category.text" class="tag-category" :class="{ 'tag-category--active': activeCategory === index }" @click="activeCategory = index">{{ category.text }}</view>
          </scroll-view>
          <scroll-view class="tag-tree__content" scroll-y>
            <view v-for="tag in visibleTags" :key="tag.id" class="tag-option" :class="{ 'tag-option--active': selectedTags.includes(tag.id) }" @click="toggleTag(tag.id)">
              <text>{{ tag.text }}</text><wd-icon v-if="selectedTags.includes(tag.id)" name="check" size="16px" />
            </view>
            <view v-if="!visibleTags.length" class="tag-empty">没有匹配标签</view>
          </scroll-view>
        </view>
      </view>
    </view>
    <view class="search-dock"><button class="mall-search-btn" @click="goResult">{{ selectedTags.length ? `查看匹配伙伴 · ${selectedTags.length}` : '查看全部伙伴' }}</button></view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppSearchBox from '@/components/AppSearchBox.vue'
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
function toggleTag(tag: string) { const index = selectedTags.value.indexOf(tag); if (index >= 0) selectedTags.value.splice(index, 1); else selectedTags.value.push(tag) }
function clearSearch() { searchText.value = ''; activeCategory.value = 0 }
function goResult() { const tags = encodeURIComponent(JSON.stringify(selectedTags.value)); uni.navigateTo({ url: `/subpages/search/result?tags=${tags}` }) }
</script>

<style scoped>
.search-page { display: flex; min-height: 100vh; padding-bottom: 94px; flex-direction: column; color: #242321; background: #faf9f6; }
.search-header { display: flex; padding: 14px 16px 8px; align-items: center; gap: 10px; }
.search-header :deep(.partner-search) { height: 48px; margin: 0; border-color: rgba(255,255,255,.9); border-radius: 17px; color: #77736c; background: rgba(255,255,255,.76); box-shadow: 0 10px 28px rgba(39,34,27,.08); }
.search-header :deep(.partner-search input) { color: #242321; }
.search-cancel { flex: 0 0 auto; margin: 0; padding: 0 2px; border: 0; color: #242321; background: transparent; font-size: 14px; font-weight: 600; }
.search-cancel::after { border: 0; }
.search-body { display: flex; min-height: 0; padding: 8px 16px 12px; flex: 1; flex-direction: column; gap: 14px; }
.search-panel { border: 1px solid rgba(255,255,255,.88); border-radius: 22px; background: rgba(255,255,255,.82); box-shadow: 0 12px 34px rgba(39,34,27,.07); backdrop-filter: blur(18px) saturate(1.08); }
.selected-panel { min-height: 96px; padding: 17px 18px; }
.tag-panel { overflow: hidden; }
.search-panel-title { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.tag-panel > .search-panel-title { padding: 17px 18px 13px; font-family: 'Noto Serif SC','Songti SC',serif; font-size: 17px; font-weight: 700; }
.search-panel-heading { display: grid; min-width: 0; gap: 2px; }
.search-panel-heading > text:first-child { font-family: 'Noto Serif SC','Songti SC',serif; font-size: 17px; font-weight: 700; line-height: 24px; }
.search-panel-heading > text:last-child { color: #77736c; font-size: 11px; line-height: 17px; }
.search-tip { margin-top: 8px; color: #77736c; font-size: 12px; line-height: 20px; }
.clear-selected-btn { min-height: 32px; margin: 0; padding: 0 12px; border: 1px solid #e9dfc7; border-radius: 999px; color: #8c6c24; background: #f8f0dd; font-size: 12px; font-weight: 600; line-height: 30px; }
.clear-selected-btn::after { border: 0; }
.tag-count { padding: 4px 9px; border-radius: 999px; color: #8c6c24; background: #f8f0dd; font-family: inherit; font-size: 11px; font-weight: 600; }
.selected-tags { display: flex; margin-top: 12px; flex-wrap: wrap; gap: 7px; }
.selected-tags > text { display: inline-flex; min-height: 30px; padding: 5px 10px; align-items: center; gap: 5px; border: 1px solid #e9dfc7; border-radius: 999px; color: #8c6c24; background: #f8f0dd; font-size: 12px; }
.tag-tree { display: flex; height: 330px; border-top: 1px solid #eae7e0; }
.tag-tree__nav { width: 39%; height: 100%; padding: 8px; border-right: 1px solid #eae7e0; background: #f7f5f0; }
.tag-category { position: relative; margin-bottom: 3px; padding: 13px 12px; border-radius: 12px; color: #77736c; font-size: 13px; line-height: 20px; }
.tag-category--active { color: #242321; background: #fff; font-weight: 700; box-shadow: 0 5px 16px rgba(39,34,27,.06); }
.tag-category--active::before { position: absolute; top: 50%; left: -8px; width: 3px; height: 22px; border-radius: 0 3px 3px 0; background: #bd9226; content: ''; transform: translateY(-50%); }
.tag-tree__content { height: 100%; padding: 8px; flex: 1; background: rgba(255,255,255,.8); }
.tag-option { display: flex; min-height: 46px; margin-bottom: 3px; padding: 10px 14px; align-items: center; justify-content: space-between; border-radius: 12px; color: #242321; font-size: 13px; font-weight: 500; line-height: 20px; }
.tag-option--active { color: #8c6c24; background: #f8f0dd; font-weight: 700; }
.tag-empty { padding: 48px 12px; color: #77736c; font-size: 13px; text-align: center; }
.search-dock { position: fixed; right: 0; bottom: 0; left: 0; z-index: 20; padding: 12px 16px calc(14px + env(safe-area-inset-bottom)); background: linear-gradient(180deg,rgba(250,249,246,0),#faf9f6 24%); }
.mall-search-btn { height: 52px; margin: 0; border: 0; border-radius: 999px; color: #fff; background: #242321; box-shadow: 0 12px 26px rgba(39,34,27,.16); font-size: 14px; font-weight: 700; line-height: 52px; }
.mall-search-btn::after { border: 0; }
</style>

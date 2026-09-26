<template>
  <SubPageLayout title="搜索">
    <div class="search-page">
      <div class="search-header">
        <van-search
            v-model="searchText"
            class="tag-search"
            show-action
            shape="round"
            placeholder="请输入要搜索的标签"
            @search="onSearch"
            @cancel="onCancel"
        />
      </div>
      <div class="search-body">
        <section class="search-panel selected-panel">
          <div class="search-panel-title">
            <div class="search-panel-heading">
              <span>已选标签</span>
              <small>{{ activeIds.length > 0 ? `已选择 ${activeIds.length} 项` : '组合标签，缩小匹配范围' }}</small>
            </div>
            <button v-if="activeIds.length > 0" class="clear-selected-btn" type="button" @click="doClearSelected">
              清空
            </button>
          </div>
          <div v-if="activeIds.length > 0" class="selected-tags">
            <van-tag
                v-for="tag in activeIds"
                :key="tag"
                closeable
                plain
                size="medium"
                type="primary"
                @close="doClose(tag)"
            >
              {{ tag }}
            </van-tag>
          </div>
          <div v-else class="search-tip">还没有选择标签，可直接浏览全部伙伴</div>
        </section>
        <section class="search-panel tag-panel">
          <div class="search-panel-title">
            <span>选择标签</span>
            <span class="tag-count">{{ tagCountText }}</span>
          </div>
          <van-tree-select
              v-if="hasMatchedTags"
              v-model:active-id="activeIds"
              v-model:main-active-index="activeIndex"
              class="tag-tree"
              selected-icon="success"
              :height="treeHeight"
              :items="tagList"
          />
          <van-empty v-else class="tag-empty" description="没有匹配标签"/>
        </section>
      </div>
    </div>

    <template #bottom>
      <van-button class="mall-search-btn" block round type="primary" @click="doSearchResult">
        {{ activeIds.length > 0 ? `查看匹配伙伴 · ${activeIds.length}` : '查看全部伙伴' }}
      </van-button>
    </template>
  </SubPageLayout>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useRoute, useRouter} from "vue-router";
import SubPageLayout from '../components/SubPageLayout.vue';

const router = useRouter()
const route = useRoute()

const searchText = ref('');

const originTagList = [{
  text: '性别',
  children: [
    {text: '男', id: '男'},
    {text: '女', id: '女'},
  ],
},
  {
    text: '年级',
    children: [
      {text: '大一', id: '大一'},
      {text: '大二', id: '大二'},
      {text: '大三', id: '大三'},
      {text: '大四', id: '大四'},
      {text: '大五', id: '大五'},
      {text: '研一', id: '研一'},
      {text: '研二', id: '研二'},
      {text: '研三', id: '研三'},
      {text: '博士', id: '博士'},
    ],
  },
  {
    text: '技术栈',
    children: [
      {text: 'Vue', id: 'Vue'},
      {text: 'React', id: 'React'},
      {text: 'Spring Boot', id: 'Spring Boot'},
      {text: 'MySQL', id: 'MySQL'},
      {text: 'Redis', id: 'Redis'},
      {text: 'Docker', id: 'Docker'},
      {text: 'Git', id: 'Git'},
      {text: 'Linux', id: 'Linux'},
      {text: 'LangChain', id: 'LangChain'},
      {text: 'WebSocket', id: 'WebSocket'},
    ],
  },
  {
    text: '编程语言',
    children: [
      {text: 'Java', id: 'Java'},
      {text: 'Python', id: 'Python'},
      {text: 'JavaScript', id: 'JavaScript'},
      {text: 'TypeScript', id: 'TypeScript'},
      {text: 'C', id: 'C'},
      {text: 'C++', id: 'C++'},
      {text: 'Go', id: 'Go'},
      {text: 'Rust', id: 'Rust'},
      {text: 'SQL', id: 'SQL'},
    ],
  },
  {
    text: '职业方向',
    children: [
      {text: '前端开发', id: '前端开发'},
      {text: '后端开发', id: '后端开发'},
      {text: '全栈开发', id: '全栈开发'},
      {text: '移动端开发', id: '移动端开发'},
      {text: 'AI应用开发', id: 'AI应用开发'},
      {text: '算法工程师', id: '算法工程师'},
      {text: '测试开发', id: '测试开发'},
      {text: '运维开发', id: '运维开发'},
      {text: '数据分析', id: '数据分析'},
      {text: '产品经理', id: '产品经理'},
      {text: 'UI设计', id: 'UI设计'},
    ],
  },
  {
    text: '学习方向',
    children: [
      {text: '前端', id: '前端'},
      {text: '后端', id: '后端'},
      {text: '算法', id: '算法'},
      {text: 'AI', id: 'AI'},
      {text: '大数据', id: '大数据'},
      {text: '网络安全', id: '网络安全'},
      {text: '嵌入式', id: '嵌入式'},
      {text: '游戏开发', id: '游戏开发'},
    ],
  },
  {
    text: '项目经验',
    children: [
      {text: '无项目经验', id: '无项目经验'},
      {text: '课程项目', id: '课程项目'},
      {text: '竞赛项目', id: '竞赛项目'},
      {text: '实习项目', id: '实习项目'},
      {text: '开源项目', id: '开源项目'},
    ],
  },
  {
    text: '协作偏好',
    children: [
      {text: '线上协作', id: '线上协作'},
      {text: '线下协作', id: '线下协作'},
      {text: '长期组队', id: '长期组队'},
      {text: '短期冲刺', id: '短期冲刺'},
      {text: '周末空闲', id: '周末空闲'},
    ],
  },
  {
    text: '能力阶段',
    children: [
      {text: '入门', id: '入门'},
      {text: '进阶', id: '进阶'},
      {text: '熟练', id: '熟练'},
      {text: '可带队', id: '可带队'},
    ],
  },
  {
    text: '目标方向',
    children: [
      {text: '找队友', id: '找队友'},
      {text: '找搭子', id: '找搭子'},
      {text: '刷题', id: '刷题'},
      {text: '做项目', id: '做项目'},
      {text: '备赛', id: '备赛'},
      {text: '求职准备', id: '求职准备'},
    ],
  },
]

const treeHeight = 260;

// 标签列表
const tagList = computed(() => {
  const keyword = searchText.value.trim().toLowerCase();
  if (!keyword) {
    return originTagList;
  }
  return originTagList
      .map(parentTag => {
        const children = parentTag.children.filter(item => {
          return item.text.toLowerCase().includes(keyword) || item.id.toLowerCase().includes(keyword);
        });
        return {
          ...parentTag,
          children,
        };
      })
      .filter(parentTag => parentTag.children.length > 0);
});

const hasMatchedTags = computed(() => tagList.value.length > 0);

const tagCountText = computed(() => {
  const total = tagList.value.reduce((count, item) => count + item.children.length, 0);
  return `${total} 个`;
});

/**
 * 搜索过滤
 * @param val
 */
const onSearch = (val) => {
  searchText.value = val;
  activeIndex.value = 0;
}
const onCancel = () => {
  searchText.value = '';
  activeIndex.value = 0;
};

const availableTagIds = new Set(originTagList.flatMap(group => group.children.map(tag => tag.id)));
const queryTags = Array.isArray(route.query.tags) ? route.query.tags : [route.query.tags];
const restoredTagIds = [...new Set(queryTags
    .filter((tag): tag is string => typeof tag === 'string' && availableTagIds.has(tag)))];

// 已选中的标签：从 URL 恢复，确保结果页返回后仍保留筛选条件
const activeIds = ref<string[]>(restoredTagIds);
const activeIndex = ref(Math.max(0, originTagList.findIndex(group => {
  return group.children.some(tag => restoredTagIds.includes(tag.id));
})));

// 移除标签
const doClose = (tag) => {
  activeIds.value = activeIds.value.filter(item => {
    return item !== tag;
  })
}

const doClearSelected = () => {
  activeIds.value = [];
}

/**
 * 执行搜索
 */
const doSearchResult = async () => {
  const tags = [...activeIds.value];
  const query = tags.length > 0 ? {tags} : {};

  await router.replace({
    path: '/search',
    query,
  });
  await router.push({
    path: '/user/list',
    query,
  })
}

</script>

<style scoped>
.search-page {
  display: flex;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: var(--lab-ink);
  background: var(--lab-paper);
}

.search-header {
  flex: 0 0 auto;
  padding: 14px 16px 8px;
  border: 0;
  background: transparent;
}

.tag-search { box-sizing: border-box; }

.search-page :deep(.van-search) {
  padding: 0;
  background: transparent;
}

.search-page :deep(.van-search__content) {
  box-sizing: border-box;
  height: 48px;
  padding-left: 15px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 17px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 10px 28px rgba(39, 34, 27, 0.08);
  backdrop-filter: blur(18px) saturate(1.12);
  -webkit-backdrop-filter: blur(18px) saturate(1.12);
}

.search-page :deep(.van-search__field) {
  height: 100%;
  padding-top: 0;
  padding-bottom: 0;
}

.search-page :deep(.van-search__field .van-field__body) {
  height: 100%;
  align-items: center;
}

.search-page :deep(.van-search__field .van-field__left-icon) {
  display: flex;
  height: 100%;
  align-items: center;
  line-height: 1;
}

.search-page :deep(.van-search__field .van-field__left-icon .van-icon) {
  display: block;
  line-height: 1;
}

.search-page :deep(.van-search__field .van-field__control) {
  height: 100%;
  padding: 0;
  line-height: 20px;
}

.search-page :deep(.van-search__content:focus-within) {
  border-color: rgba(189, 146, 38, 0.55);
  box-shadow: 0 0 0 2px rgba(189, 146, 38, 0.1), 0 10px 28px rgba(39, 34, 27, 0.08);
}

.search-page :deep(.van-search__action) {
  margin-left: 10px;
  padding: 0 2px;
  color: var(--lab-ink);
  font-size: 14px;
  font-weight: 600;
}

.search-body {
  display: flex;
  box-sizing: border-box;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  padding: 8px 16px 12px;
  overflow: hidden;
}

.search-panel {
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--lab-shadow);
  backdrop-filter: blur(18px) saturate(1.08);
  -webkit-backdrop-filter: blur(18px) saturate(1.08);
}

.selected-panel {
  flex: 0 0 auto;
  min-height: 96px;
  padding: 17px 18px;
}

.tag-panel {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.search-panel-title {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--lab-ink);
}

.tag-panel > .search-panel-title { padding: 17px 18px 13px; }

.search-panel-heading {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.search-panel-heading > span,
.tag-panel .search-panel-title > span:first-child {
  font-family: var(--lab-serif);
  font-size: 17px;
  font-weight: 700;
  line-height: 24px;
}

.search-panel-heading small {
  overflow: hidden;
  color: var(--lab-muted);
  font-size: 11px;
  font-weight: 400;
  line-height: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-tip {
  margin-top: 8px;
  color: var(--lab-muted);
  font-size: 12px;
  line-height: 20px;
}

.clear-selected-btn {
  flex: 0 0 auto;
  box-sizing: border-box;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid #e9dfc7;
  border-radius: 999px;
  color: #8c6c24;
  background: var(--lab-gold-soft);
  font: inherit;
  font-size: 12px;
  font-weight: 600;
}

.clear-selected-btn:active { transform: scale(0.96); }

.tag-count {
  flex: 0 0 auto;
  padding: 4px 9px;
  border-radius: 999px;
  color: #8c6c24;
  background: var(--lab-gold-soft);
  font-size: 11px;
  font-weight: 600;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.selected-tags :deep(.van-tag) {
  box-sizing: border-box;
  max-width: 100%;
  min-height: 30px;
  padding: 5px 10px;
  border-color: #e9dfc7;
  border-radius: 999px;
  color: #8c6c24;
  background: var(--lab-gold-soft);
}

.tag-tree {
  min-height: 0;
  flex: 1;
  height: 100% !important;
  margin-top: 0;
  border-top: 1px solid var(--lab-line);
}

.search-page :deep(.van-tree-select) { background: transparent; }

.search-page :deep(.van-tree-select__nav) {
  width: 39%;
  padding: 8px;
  border-right: 1px solid var(--lab-line);
  background: #f7f5f0;
}

.search-page :deep(.van-tree-select__content) {
  padding: 8px;
  background: rgba(255, 255, 255, 0.8);
}

.search-page :deep(.van-sidebar-item) {
  margin-bottom: 3px;
  padding: 13px 12px;
  border-radius: 12px;
  color: var(--lab-muted);
  background: transparent;
  font-size: 13px;
  line-height: 20px;
}

.search-page :deep(.van-sidebar-item--select) {
  color: var(--lab-ink);
  background: #fff;
  font-weight: 700;
  box-shadow: 0 5px 16px rgba(39, 34, 27, 0.06);
}

.search-page :deep(.van-sidebar-item--select::before) {
  left: -8px;
  width: 3px;
  height: 22px;
  border-radius: 0 3px 3px 0;
  background: var(--lab-gold);
}

.search-page :deep(.van-tree-select__item) {
  display: flex;
  min-height: 46px;
  align-items: center;
  margin-bottom: 3px;
  padding: 10px 34px 10px 14px;
  border-radius: 12px;
  color: var(--lab-ink);
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
}

.search-page :deep(.van-tree-select__item:active) { background: #f7f5f0; }
.search-page :deep(.van-tree-select__item--active) { color: #8c6c24; background: var(--lab-gold-soft); font-weight: 700; }
.search-page :deep(.van-tree-select__selected) { right: 13px; color: var(--lab-gold); }

.tag-empty {
  min-height: 0;
  flex: 1;
}

.mall-search-btn {
  box-sizing: border-box;
  height: 52px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: var(--lab-ink);
  box-shadow: 0 12px 26px rgba(39, 34, 27, 0.16);
  font-size: 14px;
  font-weight: 700;
}

.mall-search-btn:active { transform: scale(0.985); background: #49443f; }

@media (max-height: 700px) {
  .search-header { padding-top: 10px; }
  .search-body { gap: 10px; padding-top: 4px; }
  .selected-panel { min-height: 82px; padding: 13px 16px; }
}
</style>

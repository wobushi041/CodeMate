<template>
  <div class="mall-page search-page">
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
          <span>已选标签</span>
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
        <div v-else class="search-tip">请选择标签</div>
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
    <div class="search-footer">
      <van-button class="mall-search-btn" block round type="primary" @click="doSearchResult">
        搜索
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useRouter} from "vue-router";

const router = useRouter()

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

// 已选中的标签
const activeIds = ref<string[]>([]);
const activeIndex = ref(0);

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
const doSearchResult = () => {
  router.push({
    path: '/user/list',
    query: {
      tags: activeIds.value
    }
  })
}

</script>

<style scoped>
.search-page {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-header {
  flex: 0 0 auto;
  padding: 10px 12px 12px;
  background: rgba(39, 42, 55, 0.72);
  border-bottom: 1px solid var(--border-weak);
}

.tag-search {
  box-sizing: border-box;
}

.search-page :deep(.van-search) {
  padding: 0;
  background: transparent;
}

.search-page :deep(.van-search__content) {
  box-sizing: border-box;
  height: 40px;
  padding-left: 12px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
}

.search-tip {
  color: var(--text-light);
  font-size: 13px;
  line-height: 24px;
}

.search-body {
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px;
  -webkit-overflow-scrolling: touch;
}

.search-panel {
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.selected-panel {
  padding: 14px;
}

.tag-panel {
  margin-top: 12px;
  overflow: hidden;
}

.search-panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-light-1);
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.clear-selected-btn {
  box-sizing: border-box;
  padding: 0;
  color: var(--color-primary);
  background: transparent;
  border: 0;
  font-size: 13px;
  line-height: 22px;
}

.tag-count {
  color: var(--text-light);
  font-size: 12px;
  font-weight: 400;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.selected-tags :deep(.van-tag) {
  box-sizing: border-box;
  max-width: 100%;
  padding: 4px 8px;
  background: rgba(29, 144, 245, 0.12);
  border-color: rgba(29, 144, 245, 0.42);
  border-radius: 8px;
}

.search-page :deep(.van-search__action) {
  color: var(--color-primary);
}

.tag-tree {
  margin-top: 12px;
}

.search-page :deep(.van-tree-select) {
  background: transparent;
}

.search-page :deep(.van-tree-select__nav) {
  width: 34%;
  background: rgba(0, 0, 0, 0.08);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.search-page :deep(.van-tree-select__content) {
  background: rgba(255, 255, 255, 0.025);
}

.search-page :deep(.van-sidebar-item) {
  color: var(--text-light);
  background: transparent;
}

.search-page :deep(.van-sidebar-item--select) {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.055);
}

.search-page :deep(.van-sidebar-item--select::before) {
  background-color: var(--color-primary);
}

.search-page :deep(.van-tree-select__item) {
  color: var(--text-light-1);
  font-weight: 500;
}

.search-page :deep(.van-tree-select__item--active) {
  font-weight: 600;
  color: var(--color-primary);
}

.tag-empty {
  height: 260px;
}

.search-footer {
  box-sizing: border-box;
  flex: 0 0 auto;
  padding: 12px 16px calc(14px + env(safe-area-inset-bottom));
  background: rgba(30, 33, 44, 0.92);
  border-top: 1px solid var(--border-weak);
}

.mall-search-btn {
  box-sizing: border-box;
  height: 44px;
}
</style>

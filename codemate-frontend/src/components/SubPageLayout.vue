<template>
  <div class="mall-page" :class="{ 'mall-page--tabbar': hasTabbar }">
    <header class="team-page-header">
      <button
        v-if="showBack"
        class="team-page-back"
        type="button"
        aria-label="返回"
        @click="handleBack"
      >
        <ArrowLeft :size="24" :stroke-width="2" />
      </button>
      <slot name="headerCenter">
        <h1>{{ title }}</h1>
      </slot>
      <div v-if="$slots.headerRight" class="team-page-header__right">
        <slot name="headerRight" />
      </div>
    </header>

    <main ref="contentRef" class="team-page-content" :class="contentClass">
      <slot />
    </main>

    <div v-if="$slots.bottom" class="team-page-dock" :class="{ 'team-page-dock--tabbar': hasTabbar }">
      <slot name="bottom" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft } from 'lucide-vue-next';

interface SubPageLayoutProps {
  title: string;
  showBack?: boolean;
  backPath?: string;
  hasTabbar?: boolean;
  contentClass?: string;
}

const props = withDefaults(defineProps<SubPageLayoutProps>(), {
  showBack: true,
  backPath: '',
  hasTabbar: false,
  contentClass: '',
});

const emit = defineEmits<{
  (e: 'back'): void;
}>();

const router = useRouter();
const contentRef = ref<HTMLElement | null>(null);

const handleBack = () => {
  emit('back');
  if (props.backPath) {
    router.push(props.backPath);
  } else {
    router.back();
  }
};

const scrollToBottom = async () => {
  await nextTick();
  if (contentRef.value) {
    contentRef.value.scrollTop = contentRef.value.scrollHeight;
  }
};

const scrollTo = (options: ScrollToOptions) => {
  contentRef.value?.scrollTo(options);
};

defineExpose({
  contentRef,
  scrollToBottom,
  scrollTo,
});
</script>

<style scoped>
.mall-page {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: #f8fafc;
  background: #0f172a;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.team-page-header {
  position: relative;
  display: flex;
  min-height: 54px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  padding: 22px 16px 12px;
  background: #0f172a;
}

.team-page-back {
  position: absolute;
  top: calc(50% + 5px);
  left: 8px;
  display: grid;
  width: 44px;
  height: 44px;
  padding: 0;
  transform: translateY(-50%);
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: #f8fafc;
  background: transparent;
  cursor: pointer;
  transition: transform 0.15s ease, background 0.15s ease;
}

.team-page-back:active {
  transform: translateY(-50%) scale(0.92);
  background: rgba(255, 255, 255, 0.08);
}

.team-page-header h1 {
  margin: 0;
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
  line-height: 28px;
  letter-spacing: -0.025em;
  text-align: center;
}

.team-page-header__right {
  position: absolute;
  top: calc(50% + 5px);
  right: 16px;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
}

.team-page-content {
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.team-page-content::-webkit-scrollbar {
  display: none;
}

.team-page-dock {
  flex: 0 0 auto;
  box-sizing: border-box;
  padding: 12px 16px calc(14px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(15, 23, 42, 0), #0f172a 24%);
}

.team-page-dock--tabbar {
  padding-bottom: 14px;
}
</style>

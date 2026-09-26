<template>
  <button
    v-if="trigger"
    class="partner-search partner-search--trigger"
    type="button"
    @click="onActivate"
  >
    <Search :size="20" :stroke-width="1.8" />
    <span>{{ placeholder }}</span>
  </button>
  <label v-else class="partner-search">
    <Search :size="20" :stroke-width="1.8" />
    <input
      :value="modelValue"
      type="search"
      :placeholder="placeholder"
      @input="onInput"
      @keyup.enter.prevent="onSearch"
    />
  </label>
</template>

<script setup lang="ts">
import {Search} from 'lucide-vue-next';

const props = withDefaults(defineProps<{
  modelValue?: string;
  placeholder?: string;
  trigger?: boolean;
}>(), {
  modelValue: '',
  placeholder: '请输入搜索内容',
  trigger: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'search', value: string): void;
  (event: 'activate'): void;
}>();

const onInput = (event: Event) => {
  emit('update:modelValue', (event.target as HTMLInputElement).value);
};

const onSearch = (event: KeyboardEvent) => {
  emit('search', (event.target as HTMLInputElement).value.trim());
};

const onActivate = () => {
  emit('activate');
};
</script>

<style scoped>
.partner-search { display: flex; align-items: center; gap: 12px; width: 100%; height: 48px; box-sizing: border-box; margin-bottom: 24px; padding: 0 16px; border: 1px solid #334155; border-radius: 16px; color: #94a3b8; background: #1e293b; }
.partner-search:focus-within { border-color: #fff; box-shadow: 0 0 0 1px #fff; }
.partner-search--trigger { outline: 0; font: inherit; font-size: 14px; text-align: left; cursor: pointer; }
.partner-search--trigger:focus-visible { border-color: #fff; box-shadow: 0 0 0 1px #fff; }
.partner-search--trigger:active { transform: scale(0.99); }
.partner-search--trigger span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.partner-search input { min-width: 0; flex: 1; border: 0; outline: 0; color: #f8fafc; background: transparent; font: inherit; font-size: 14px; }
.partner-search input::placeholder { color: #94a3b8; }
</style>

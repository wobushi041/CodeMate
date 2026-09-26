<template>
  <article v-if="loading" class="partner-card partner-card--loading" aria-hidden="true">
    <span class="loading-avatar" />
    <div class="loading-lines"><span /><span /><span /></div>
  </article>

  <article v-else-if="user" class="partner-card">
    <div class="partner-avatar-wrap">
      <img
          class="partner-avatar"
          :src="user.avatarUrl || fallbackAvatar"
          :alt="user.username"
          @error="onAvatarError"
      />
    </div>

    <div class="partner-info">
      <div>
        <h3>{{ displayName }}</h3>
        <p>寻找项目队友</p>
        <div class="partner-tags">
          <span v-for="tag in user.tags" :key="tag">{{ tag }}</span>
        </div>
      </div>
      <div class="contact-row">
        <button
            type="button"
            :class="{ 'contact-button--contacted': contacted }"
            :disabled="contacting"
            @click.stop="emit('contact', user)"
        >
          <LoaderCircle v-if="contacting" class="is-spinning" :size="16" />
          <Send v-else-if="!contacted" :size="16" :stroke-width="1.9" />
          <Check v-else :size="16" :stroke-width="2" />
          <span>{{ contacting ? '发起中...' : contacted ? '已联系' : '联系我' }}</span>
        </button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {Check, LoaderCircle, Send} from 'lucide-vue-next';
import type {UserType} from '../models/user';

type PartnerCardUser = Omit<UserType, 'tags'> & {tags: string[]};

const props = withDefaults(defineProps<{
  user?: PartnerCardUser;
  contacted?: boolean;
  contacting?: boolean;
  loading?: boolean;
}>(), {
  contacted: false,
  contacting: false,
  loading: false,
});

const emit = defineEmits<{
  contact: [user: PartnerCardUser];
}>();

const fallbackAvatar = 'https://api.dicebear.com/8.x/initials/svg?seed=Code&backgroundColor=334155&fontFamily=Arial';
const displayName = computed(() => {
  if (!props.user) return '';
  return props.user.planetCode ? `${props.user.username}(${props.user.planetCode})` : props.user.username;
});

const onAvatarError = (event: Event) => {
  const image = event.target as HTMLImageElement;
  if (image.src !== fallbackAvatar) image.src = fallbackAvatar;
};
</script>

<style scoped>
.partner-card {
  display: flex;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 112px;
  box-sizing: border-box;
  gap: 16px;
  margin-right: auto;
  margin-left: auto;
  padding: 16px;
  overflow: hidden;
  border: 1px solid transparent;
  border-radius: 16px;
  background: #1e293b;
  transition: transform .15s ease, box-shadow .15s ease;
}

.partner-avatar-wrap { position: relative; width: 80px; height: 80px; flex: 0 0 auto; }
.partner-avatar { display: block; width: 80px; height: 80px; border-radius: 12px; object-fit: cover; background: #334155; }
.partner-info { display: flex; width: 0; min-width: 0; flex: 1 1 0; flex-direction: column; justify-content: space-between; }
.partner-info > div { min-width: 0; }
.partner-info h3 { width: 100%; max-width: 100%; margin: 0; overflow: hidden; color: #f8fafc; font-size: 16px; font-weight: 600; line-height: 20px; text-overflow: ellipsis; white-space: nowrap; }
.partner-info p { margin: 4px 0 0; color: #cbd5e1; font-size: 12px; line-height: 16px; }
.partner-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.partner-tags span { padding: 4px 12px; border-radius: 999px; color: #cbd5e1; background: #334155; font-size: 12px; line-height: 16px; }
.contact-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.contact-row button { display: inline-flex; max-width: 100%; box-sizing: border-box; align-items: center; gap: 8px; padding: 8px 20px; border: 0; border-radius: 999px; color: #030712; background: #fff; font: inherit; font-size: 14px; font-weight: 600; line-height: 20px; box-shadow: 0 4px 8px rgba(2,6,23,.18); transition: transform .15s ease, background .15s ease; }
.contact-row button:active { transform: scale(.95); background: #f4f4f5; }
.contact-row button.contact-button--contacted { color: #86efac; background: rgba(34, 197, 94, 0.14); border-color: rgba(74, 222, 128, 0.3); }
.contact-row button:disabled { opacity: 0.65; cursor: not-allowed; }
.partner-card--loading { min-height: 144px; }
.loading-avatar { width: 80px; height: 80px; flex: 0 0 80px; border-radius: 12px; background: #334155; animation: loading-pulse 1.4s ease-in-out infinite; }
.loading-lines { display: grid; flex: 1; align-content: start; gap: 10px; padding-top: 4px; }
.loading-lines span { display: block; height: 10px; border-radius: 999px; background: #334155; animation: loading-pulse 1.4s ease-in-out infinite; }
.loading-lines span:nth-child(2) { width: 72%; }
.loading-lines span:nth-child(3) { width: 48%; }
.is-spinning { animation: spin 0.8s linear infinite; }

@keyframes loading-pulse { 0%,100% { opacity: .5; } 50% { opacity: 1; } }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>

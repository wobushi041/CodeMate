<template>
  <view class="match-page">
    <view class="match-header">
      <view class="match-header__title">伙伴匹配</view>
      <view class="match-header__subtitle">基于技术标签寻找更合适的伙伴</view>
      <view class="match-header__description">每一次匹配，都可能是一次优秀合作的开始</view>
    </view>

    <view class="match-main">
      <view class="match-stage">
        <view class="match-list-badge">
          <view class="match-list-badge__icon"><wd-icon name="usergroup" size="20px" /></view>
          <text class="match-list-badge__text">用户匹配列表</text>
        </view>

        <template v-if="currentPartner">
          <view class="match-avatar-section">
            <view class="match-avatar-glow" />
            <image class="match-avatar" :src="currentPartner.avatarUrl || fallbackAvatar" mode="aspectFill" />
            <view class="match-avatar-ring" />
          </view>
          <view class="match-queue-state">
            <view class="match-queue-state__icon"><wd-icon name="time" size="20px" /></view>
            <view class="match-queue-state__title">已生成 {{ users.length }} 位伙伴候选</view>
            <view class="match-queue-state__hint">通过“上一位”和“下一位”查看所有匹配结果</view>
          </view>
          <view class="match-single-card">
            <PartnerCard :user="currentPartner" :contacted="contactedIds.includes(Number(currentPartner.id))" :contacting="contacting" @contact="contactUser" />
          </view>
        </template>

        <view v-else class="match-empty-slot">
          <view class="match-empty-slot__circle"><wd-icon name="refresh" :class="{ 'is-spinning': loading }" size="44px" /></view>
          <view class="match-empty-slot__title">{{ loading ? '正在为你匹配伙伴' : '开始寻找你的技术搭档' }}</view>
          <view class="match-empty-slot__subtitle">{{ loading ? '正在分析技术标签与匹配度，请稍候' : '点击下方按钮，生成一份可浏览的匹配用户列表' }}</view>
          <button v-if="!loading" class="match-empty-slot__button" @click="loadUsers"><wd-icon name="heart" size="16px" />立即匹配</button>
          <view v-else class="match-loading-line"><wd-icon name="refresh" class="is-spinning" size="16px" />匹配请求处理中...</view>
        </view>
      </view>

      <view class="match-controls">
        <view class="match-controls__card">
          <button class="match-control match-control--secondary" :disabled="!users.length" @click="previousPartner"><wd-icon name="arrow-right" class="icon-back" size="20px" /><text>上一位</text></button>
          <button class="match-control match-control--secondary" :disabled="!users.length" @click="nextPartner"><text>下一位</text><wd-icon name="arrow-right" size="20px" /></button>
          <button class="match-control match-control--primary" :disabled="loading" @click="loadUsers"><wd-icon name="refresh" :class="{ 'is-spinning': loading }" size="20px" /><text>{{ loading ? '匹配中' : '重新匹配' }}</text></button>
        </view>
        <view class="match-progress">
          <view class="match-progress__icon"><wd-icon name="filter1" size="20px" /></view>
          <text class="match-progress__text">候选用户 <text class="match-progress__number">{{ users.length }}</text> 位</text>
          <text class="match-progress__number match-progress__index">{{ currentPartner ? `${currentIndex + 1}/${users.length}` : '0/0' }}</text>
        </view>
      </view>
    </view>
    <AppTabbar active="match" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabbar from '@/components/AppTabbar.vue'
import PartnerCard from '@/components/PartnerCard.vue'
import { startPrivateChat } from '@/api/chat'
import { getMatchedUsers } from '@/api/partner'
import type { UserType } from '@/types/api'
import { getContactedUserIds, markUserContacted } from '@/utils/contacted'
import { requireLogin } from '@/utils/navigation'

const users = ref<UserType[]>([])
const currentIndex = ref(0)
const loading = ref(false)
const loaded = ref(false)
const contacting = ref(false)
const contactedIds = ref<number[]>(getContactedUserIds())
const currentPartner = computed(() => users.value[currentIndex.value] || null)
const fallbackAvatar = 'https://api.dicebear.com/8.x/initials/svg?seed=Code&backgroundColor=334155&fontFamily=Arial'

onShow(() => {
  uni.hideTabBar({ animation: false })
  if (!requireLogin()) return
  contactedIds.value = getContactedUserIds()
  if (!loaded.value) loadUsers()
})
async function loadUsers() {
  if (loading.value) return
  loading.value = true
  try { users.value = await getMatchedUsers(20); currentIndex.value = 0; loaded.value = true } catch { /* 请求层统一提示 */ } finally { loading.value = false }
}
function nextPartner() { currentIndex.value = users.value.length ? (currentIndex.value + 1) % users.value.length : 0 }
function previousPartner() { currentIndex.value = users.value.length ? (currentIndex.value - 1 + users.value.length) % users.value.length : 0 }
async function contactUser(user: UserType) {
  if (contacting.value) return
  contacting.value = true
  try {
    const session = await startPrivateChat(Number(user.id))
    markUserContacted(user.id)
    contactedIds.value = getContactedUserIds()
    const name = session.targetUser?.userName || session.targetUser?.username || user.username || '编程伙伴'
    const avatar = session.targetUser?.avatarUrl || user.avatarUrl || ''
    uni.navigateTo({ url: `/subpages/chat/private?sessionId=${session.sessionId}&targetUserId=${user.id}&targetUsername=${encodeURIComponent(name)}&targetAvatarUrl=${encodeURIComponent(avatar)}&isOnline=${session.isTargetOnline ? 1 : 0}` })
  } catch { /* 请求层统一提示 */ } finally { contacting.value = false }
}
</script>

<style scoped>
.match-page { width: 100%; min-height: 100vh; padding: calc(30px + var(--status-bar-height)) 16px calc(28px + 64px + env(safe-area-inset-bottom)); color: #242321; background: #faf9f6; }
.match-header { margin-bottom: 24px; text-align: center; }
.match-header__title { color: #242321; font-size: 30px; font-weight: 700; line-height: 38px; letter-spacing: -.03em; }
.match-header__subtitle { margin-top: 8px; color: #66635d; font-size: 14px; line-height: 22px; }
.match-header__description { margin-top: 4px; color: #85817a; font-size: 12px; line-height: 18px; }
.match-main { display: flex; flex-direction: column; gap: 18px; }
.match-stage { position: relative; min-height: 405px; padding: 22px 16px 20px; overflow: hidden; border: 1px solid rgba(100,116,139,.42); border-radius: 26px; background: linear-gradient(135deg,#ffffff 0%,#ffffff 48%,#263449 100%); box-shadow: 0 18px 45px rgba(2,6,23,.18); }
.match-stage::after { position: absolute; right: -72px; bottom: -92px; width: 220px; height: 220px; border: 1px solid rgba(148,163,184,.08); border-radius: 50%; content: ''; pointer-events: none; }
.match-list-badge { position: absolute; top: 18px; left: 18px; z-index: 3; display: flex; align-items: center; gap: 9px; color: #242321; }
.match-list-badge__icon { display: grid; width: 32px; height: 32px; place-items: center; border: 1px solid rgba(255,255,255,.22); border-radius: 50%; color: #242321; background: rgba(255,255,255,.18); }
.match-list-badge__text { font-size: 13px; font-weight: 600; letter-spacing: .02em; }
.match-avatar-section { position: relative; display: flex; width: 100%; height: 166px; align-items: center; justify-content: center; }
.match-avatar-glow { position: absolute; top: 25px; left: 50%; width: 170px; height: 170px; border-radius: 50%; background: rgba(96,165,250,.14); filter: blur(20px); transform: translateX(-50%); }
.match-avatar { position: relative; z-index: 1; width: 138px; height: 138px; border: 5px solid #242321; border-radius: 50%; background: #eae7e0; box-shadow: 0 10px 24px rgba(2,6,23,.28); }
.match-avatar-ring { position: absolute; top: 50%; left: 50%; width: 148px; height: 148px; border: 1px solid rgba(255,255,255,.26); border-radius: 50%; transform: translate(-50%,-50%); }
.match-queue-state { display: flex; margin-top: 5px; flex-direction: column; align-items: center; text-align: center; }
.match-queue-state__icon { display: grid; width: 42px; height: 42px; margin-bottom: 10px; place-items: center; border: 1px solid rgba(148,163,184,.16); border-radius: 50%; color: #66635d; background: rgba(148,163,184,.1); }
.match-queue-state__title { color: #66635d; font-size: 13px; font-weight: 500; line-height: 20px; }
.match-queue-state__hint { margin-top: 4px; color: #85817a; font-size: 11px; line-height: 18px; }
.match-single-card { position: relative; z-index: 2; margin-top: 18px; border-radius: 16px; }
.match-empty-slot { display: flex; min-height: 359px; padding: 44px 12px 16px; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.match-empty-slot__circle { display: grid; width: 86px; height: 86px; margin-bottom: 18px; place-items: center; border: 1px solid rgba(148,163,184,.16); border-radius: 50%; color: #85817a; background: rgba(148,163,184,.08); }
.match-empty-slot__title { color: #242321; font-size: 18px; font-weight: 600; line-height: 26px; }
.match-empty-slot__subtitle { max-width: 250px; margin-top: 8px; color: #85817a; font-size: 12px; line-height: 20px; }
.match-empty-slot__button { display: flex; min-width: 142px; margin: 20px 0 0; padding: 11px 20px; align-items: center; justify-content: center; gap: 8px; border: 0; border-radius: 999px; color: #242321; background: #fff; font-size: 14px; font-weight: 600; }
.match-empty-slot__button::after { border: 0; }
.match-loading-line { display: flex; margin-top: 20px; align-items: center; gap: 7px; color: #bd9226; font-size: 12px; }
.match-controls { display: flex; flex-direction: column; gap: 12px; }
.match-controls__card { display: flex; width: 100%; gap: 10px; padding: 14px; border: 1px solid rgba(100,116,139,.32); border-radius: 22px; background: #ffffff; }
.match-control { display: flex; min-width: 0; min-height: 46px; margin: 0; padding: 10px 8px; flex: 1 1 0; align-items: center; justify-content: center; gap: 8px; border: 0; border-radius: 999px; font-size: 14px; font-weight: 600; line-height: 20px; white-space: nowrap; }
.match-control::after { border: 0; }
.match-control--primary { flex: 1.35 1 0; color: #242321; background: #fff; }
.match-control--secondary { color: #242321; background: #eae7e0; }
.match-control:disabled { color: #85817a; background: rgba(51,65,85,.45); opacity: .7; }
.icon-back { transform: rotate(180deg); }
.match-progress { display: flex; min-height: 46px; align-items: center; gap: 9px; padding: 0 18px; border: 1px solid rgba(100,116,139,.32); border-radius: 999px; color: #66635d; background: #ffffff; }
.match-progress__icon { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 50%; color: #66635d; background: #eae7e0; }
.match-progress__text { flex: 1; font-size: 12px; }
.match-progress__number { color: #242321; font-size: 16px; font-weight: 600; }
.match-progress__index { font-size: 12px; }
.is-spinning { animation: spin .8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.match-header__title { color: #242321; font-size: 30px; }
.match-header__subtitle, .match-header__description { color: #77736c; font-size: 13px; }
.match-stage, .match-controls__card, .match-progress { border-color: #eae7e0; background: #fff; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.match-list-badge__icon, .match-queue-state__icon, .match-empty-slot__circle, .match-progress__icon { border-color: #eae7e0; color: #bd9226; background: #f8f0dd; }
.match-avatar { border-color: #f3f0e9; background: #eeeae2; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.match-queue-state__title, .match-empty-slot__title { color: #242321; }
.match-queue-state__hint, .match-empty-slot__subtitle { color: #77736c; }
.match-empty-slot__button, .match-control--primary { color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.match-control--secondary { border: 1px solid #eae7e0; color: #242321; background: #fff; }
.match-control:disabled { color: #85817a; background: #f1efea; }
.match-progress__text { color: #77736c; }
.match-progress__number { color: #242321; }
</style>

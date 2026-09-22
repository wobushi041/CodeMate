<template>
  <div ref="wrapperRef" class="team-datetime-picker">
    <!-- 触发输入按钮 -->
    <button
      type="button"
      class="datetime-trigger"
      :class="{ 'is-active': showCalendar, 'is-disabled': disabled }"
      :disabled="disabled"
      @click="toggleCalendar"
    >
      <span :class="{ 'placeholder-text': !formattedDisplay }">
        {{ formattedDisplay || placeholder }}
      </span>
      <CalendarDays :size="20" class="trigger-icon" />
    </button>

    <!-- 原位展开的日历 + 快捷时间选择面板 -->
    <transition name="picker-fade">
      <div v-if="showCalendar" class="calendar-panel">
        <!-- 日历头部（月份切换） -->
        <div class="calendar-header">
          <button type="button" class="calendar-nav-btn" aria-label="上个月" @click="prevMonth">
            <ChevronLeft :size="20" />
          </button>
          <div class="calendar-title">{{ formatMonthYear(currentDate) }}</div>
          <button type="button" class="calendar-nav-btn" aria-label="下个月" @click="nextMonth">
            <ChevronRight :size="20" />
          </button>
        </div>

        <!-- 日历星期表头与日期网格 -->
        <div class="calendar-grid">
          <div v-for="day in weekDays" :key="day" class="calendar-weekday">
            {{ day }}
          </div>
          <button
            v-for="date in calendarDays"
            :key="date.key"
            type="button"
            class="calendar-day-btn"
            :class="{
              'calendar-day--muted': date.isPreviousMonth || date.isNextMonth || date.isPast,
              'calendar-day--active': date.isSelected,
              'calendar-day--today': date.isToday && !date.isSelected,
            }"
            :disabled="date.isPreviousMonth || date.isNextMonth || date.isPast"
            @click="selectDate(date)"
          >
            <span>{{ date.day }}</span>
          </button>
        </div>

        <!-- 时间选择滚条区（时/分 双滚条） -->
        <div class="calendar-time-section">
          <div class="calendar-time-header">
            <span class="calendar-time-label">选择时间</span>
            <span class="calendar-time-preview">{{ selectedHour }}:{{ selectedMinute }}</span>
          </div>

          <!-- 滚轮选择器容器（中间贯通的白色圆角胶囊与时/分滚轮） -->
          <div class="time-picker-roller">
            <div class="roller-highlight-capsule" />

            <!-- 时 列 -->
            <div class="roller-column">
              <div
                ref="hourWheelRef"
                class="wheel-scroll-track"
                @scroll="handleHourScroll"
              >
                <button
                  v-for="(h, idx) in loopHoursList"
                  :key="`h-${idx}`"
                  type="button"
                  class="wheel-item"
                  :class="getHourItemClass(idx)"
                  @click="selectHour(h, idx)"
                >
                  {{ h }}
                </button>
              </div>
            </div>

            <!-- 分 列 -->
            <div class="roller-column">
              <div
                ref="minuteWheelRef"
                class="wheel-scroll-track"
                @scroll="handleMinuteScroll"
              >
                <button
                  v-for="(m, idx) in loopMinutesList"
                  :key="`m-${idx}`"
                  type="button"
                  class="wheel-item"
                  :class="getMinuteItemClass(idx)"
                  @click="selectMinute(m, idx)"
                >
                  {{ m }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮栏 -->
        <div class="calendar-actions">
          <button type="button" class="calendar-btn-cancel" @click="showCalendar = false">
            取消
          </button>
          <button type="button" class="calendar-btn-confirm" @click="confirmDateTime">
            确认
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-vue-next';

interface Props {
  modelValue?: Date | string | null;
  placeholder?: string;
  disabled?: boolean;
  minDate?: Date;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  placeholder: '点击选择过期时间',
  disabled: false,
  minDate: () => new Date(),
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: Date | null): void;
  (e: 'change', value: Date | null): void;
}>();

const wrapperRef = ref<HTMLElement | null>(null);
const showCalendar = ref(false);
const currentDate = ref(new Date());
const selectedDateObj = ref<Date | null>(null);
const selectedHour = ref('12');
const selectedMinute = ref('00');
const hourWheelRef = ref<HTMLElement | null>(null);
const minuteWheelRef = ref<HTMLElement | null>(null);

const LOOP_CYCLES = 9;
const MIDDLE_CYCLE = 4;

const loopHoursList = Array.from({ length: LOOP_CYCLES * 24 }, (_, i) => String(i % 24).padStart(2, '0'));
const loopMinutesList = Array.from({ length: LOOP_CYCLES * 60 }, (_, i) => String(i % 60).padStart(2, '0'));

const ITEM_HEIGHT = 26;

const currentHourCenterIndex = ref(MIDDLE_CYCLE * 24 + 12);
const currentMinuteCenterIndex = ref(MIDDLE_CYCLE * 60);

const getHourItemClass = (idx: number) => {
  const diff = Math.abs(idx - currentHourCenterIndex.value);
  if (diff === 0) return 'wheel-item--active';
  if (diff === 1) return 'wheel-item--near';
  if (diff === 2) return 'wheel-item--far';
  return 'wheel-item--hidden';
};

const getMinuteItemClass = (idx: number) => {
  const diff = Math.abs(idx - currentMinuteCenterIndex.value);
  if (diff === 0) return 'wheel-item--active';
  if (diff === 1) return 'wheel-item--near';
  if (diff === 2) return 'wheel-item--far';
  return 'wheel-item--hidden';
};

let isManualScrollingHour = false;
let isManualScrollingMinute = false;
let hourScrollTimer: number | null = null;
let minScrollTimer: number | null = null;

const weekDays = ['日', '一', '二', '三', '四', '五', '六'];

// 解析 modelValue
const parseDate = (val: unknown): Date | null => {
  if (!val) return null;
  if (val instanceof Date) {
    return Number.isNaN(val.getTime()) ? null : val;
  }
  const parsed = new Date(String(val));
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

// 格式化展示文本
const formattedDisplay = computed(() => {
  const d = parseDate(props.modelValue);
  if (!d) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
});

const formatMonthYear = (date: Date) => {
  return `${date.getFullYear()}年 ${date.getMonth() + 1}月`;
};

const scrollToSelected = () => {
  nextTick(() => {
    setTimeout(() => {
      const h = parseInt(selectedHour.value || '12', 10);
      const m = parseInt(selectedMinute.value || '0', 10);
      const validH = Number.isNaN(h) ? 12 : ((h % 24) + 24) % 24;
      const validM = Number.isNaN(m) ? 0 : ((m % 60) + 60) % 60;
      const targetHourIdx = MIDDLE_CYCLE * 24 + validH;
      const targetMinIdx = MIDDLE_CYCLE * 60 + validM;

      currentHourCenterIndex.value = targetHourIdx;
      currentMinuteCenterIndex.value = targetMinIdx;

      if (hourWheelRef.value) {
        hourWheelRef.value.scrollTop = targetHourIdx * ITEM_HEIGHT;
      }
      if (minuteWheelRef.value) {
        minuteWheelRef.value.scrollTop = targetMinIdx * ITEM_HEIGHT;
      }
    }, 50);
  });
};

// 同步初始状态
const syncFromModel = () => {
  const d = parseDate(props.modelValue);
  const pad = (n: number) => String(n).padStart(2, '0');
  if (d) {
    currentDate.value = new Date(d);
    selectedDateObj.value = new Date(d);
    selectedHour.value = pad(d.getHours());
    selectedMinute.value = pad(d.getMinutes());
  } else {
    // 默认选中明天中午 12:00
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    currentDate.value = new Date(tomorrow);
    selectedDateObj.value = new Date(tomorrow);
    selectedHour.value = '12';
    selectedMinute.value = '00';
  }
};

watch(() => props.modelValue, syncFromModel, { immediate: true });

const toggleCalendar = () => {
  if (props.disabled) return;
  if (!showCalendar.value) {
    syncFromModel();
    showCalendar.value = true;
    scrollToSelected();
  } else {
    showCalendar.value = false;
  }
};

// 42 格日历网格计算
const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear();
  const month = currentDate.value.getMonth();
  const firstDayOfMonth = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const daysInPrevMonth = new Date(year, month, 0).getDate();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const days = [];

  // 上月占位天数
  for (let i = firstDayOfMonth - 1; i >= 0; i--) {
    days.push({
      day: daysInPrevMonth - i,
      isPreviousMonth: true,
      isNextMonth: false,
      isPast: true,
      isToday: false,
      isSelected: false,
      key: `prev-${i}`,
    });
  }

  // 本月天数
  for (let i = 1; i <= daysInMonth; i++) {
    const dateObj = new Date(year, month, i);
    dateObj.setHours(0, 0, 0, 0);
    const isPast = props.minDate ? dateObj.getTime() < today.getTime() : false;
    const isToday = dateObj.getTime() === today.getTime();
    const isSelected = selectedDateObj.value
      ? new Date(selectedDateObj.value).setHours(0, 0, 0, 0) === dateObj.getTime()
      : false;

    days.push({
      day: i,
      dateObj,
      isPreviousMonth: false,
      isNextMonth: false,
      isPast,
      isToday,
      isSelected,
      key: `curr-${i}`,
    });
  }

  // 下月占位天数（补齐 42 格保证高度稳定）
  const remainingSlots = 42 - days.length;
  for (let i = 1; i <= remainingSlots; i++) {
    days.push({
      day: i,
      isPreviousMonth: false,
      isNextMonth: true,
      isPast: false,
      isToday: false,
      isSelected: false,
      key: `next-${i}`,
    });
  }

  return days;
});

const prevMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1, 1);
};

const nextMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 1);
};

const selectDate = (dateItem: any) => {
  if (dateItem.isPast || dateItem.isPreviousMonth || dateItem.isNextMonth) return;
  selectedDateObj.value = new Date(dateItem.dateObj);
};

const handleHourScroll = () => {
  if (!hourWheelRef.value) return;
  const rawIndex = Math.round(hourWheelRef.value.scrollTop / ITEM_HEIGHT);
  currentHourCenterIndex.value = rawIndex;

  if (isManualScrollingHour) return;
  if (hourScrollTimer) window.clearTimeout(hourScrollTimer);
  hourScrollTimer = window.setTimeout(() => {
    if (!hourWheelRef.value) return;
    const hourMod = ((rawIndex % 24) + 24) % 24;
    selectedHour.value = String(hourMod).padStart(2, '0');

    // 轮回无感归位：当滚动停止且偏离中间循环段（小于第2轮或大于第6轮）时，静默瞬移回第4轮
    if (rawIndex < 2 * 24 || rawIndex > 6 * 24) {
      const normalized = hourMod + MIDDLE_CYCLE * 24;
      currentHourCenterIndex.value = normalized;
      hourWheelRef.value.scrollTop = normalized * ITEM_HEIGHT;
    }
  }, 70);
};

const handleMinuteScroll = () => {
  if (!minuteWheelRef.value) return;
  const rawIndex = Math.round(minuteWheelRef.value.scrollTop / ITEM_HEIGHT);
  currentMinuteCenterIndex.value = rawIndex;

  if (isManualScrollingMinute) return;
  if (minScrollTimer) window.clearTimeout(minScrollTimer);
  minScrollTimer = window.setTimeout(() => {
    if (!minuteWheelRef.value) return;
    const minMod = ((rawIndex % 60) + 60) % 60;
    selectedMinute.value = String(minMod).padStart(2, '0');

    // 轮回无感归位：当滚动停止且偏离中间循环段时，静默瞬移回第4轮
    if (rawIndex < 2 * 60 || rawIndex > 6 * 60) {
      const normalized = minMod + MIDDLE_CYCLE * 60;
      currentMinuteCenterIndex.value = normalized;
      minuteWheelRef.value.scrollTop = normalized * ITEM_HEIGHT;
    }
  }, 70);
};

const selectHour = (h: string, globalIdx: number) => {
  selectedHour.value = h;
  currentHourCenterIndex.value = globalIdx;
  if (hourWheelRef.value) {
    isManualScrollingHour = true;
    hourWheelRef.value.scrollTo({
      top: globalIdx * ITEM_HEIGHT,
      behavior: 'smooth',
    });
    setTimeout(() => {
      isManualScrollingHour = false;
      if (globalIdx < 2 * 24 || globalIdx > 6 * 24) {
        const normalized = (((globalIdx % 24) + 24) % 24) + MIDDLE_CYCLE * 24;
        currentHourCenterIndex.value = normalized;
        if (hourWheelRef.value) {
          hourWheelRef.value.scrollTop = normalized * ITEM_HEIGHT;
        }
      }
    }, 280);
  }
};

const selectMinute = (m: string, globalIdx: number) => {
  selectedMinute.value = m;
  currentMinuteCenterIndex.value = globalIdx;
  if (minuteWheelRef.value) {
    isManualScrollingMinute = true;
    minuteWheelRef.value.scrollTo({
      top: globalIdx * ITEM_HEIGHT,
      behavior: 'smooth',
    });
    setTimeout(() => {
      isManualScrollingMinute = false;
      if (globalIdx < 2 * 60 || globalIdx > 6 * 60) {
        const normalized = (((globalIdx % 60) + 60) % 60) + MIDDLE_CYCLE * 60;
        currentMinuteCenterIndex.value = normalized;
        if (minuteWheelRef.value) {
          minuteWheelRef.value.scrollTop = normalized * ITEM_HEIGHT;
        }
      }
    }, 280);
  }
};

const confirmDateTime = () => {
  if (!selectedDateObj.value) return;

  const hours = parseInt(selectedHour.value || '0', 10);
  const minutes = parseInt(selectedMinute.value || '0', 10);
  const finalDateTime = new Date(selectedDateObj.value);
  finalDateTime.setHours(hours, minutes, 0, 0);

  emit('update:modelValue', finalDateTime);
  emit('change', finalDateTime);
  showCalendar.value = false;
};

// 点击外部关闭
const handleDocumentClick = (e: MouseEvent) => {
  if (!showCalendar.value) return;
  if (wrapperRef.value && !wrapperRef.value.contains(e.target as Node)) {
    showCalendar.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleDocumentClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick);
  if (hourScrollTimer) window.clearTimeout(hourScrollTimer);
  if (minScrollTimer) window.clearTimeout(minScrollTimer);
});
</script>

<style scoped>
.team-datetime-picker {
  position: relative;
  width: 100%;
}

.datetime-trigger {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 48px;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  outline: none;
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.65);
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.datetime-trigger:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.2);
}

.datetime-trigger.is-active,
.datetime-trigger:focus {
  border-color: #ffffff;
  box-shadow: 0 0 0 1px #ffffff;
}

.datetime-trigger.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.placeholder-text {
  color: #94a3b8;
}

.trigger-icon {
  flex: 0 0 auto;
  color: #94a3b8;
  transition: color 0.18s ease;
}

.datetime-trigger:hover .trigger-icon,
.datetime-trigger.is-active .trigger-icon {
  color: #ffffff;
}

/* 展开面板（玻璃拟态高质感设计） */
.calendar-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 60;
  box-sizing: border-box;
  padding: 18px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(26, 36, 54, 0.96);
  box-shadow: 0 22px 45px rgba(2, 6, 23, 0.6);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.calendar-title {
  color: #f8fafc;
  font-size: 14px;
  font-weight: 600;
}

.calendar-nav-btn {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  color: #94a3b8;
  background: transparent;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.calendar-nav-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
  text-align: center;
  font-size: 12px;
}

.calendar-weekday {
  padding-bottom: 8px;
  color: #94a3b8;
  font-weight: 500;
}

.calendar-day-btn {
  display: flex;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  color: #f8fafc;
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.calendar-day-btn:not(:disabled):hover {
  background: rgba(255, 255, 255, 0.1);
}

.calendar-day--muted {
  color: #94a3b8;
  opacity: 0.35;
  cursor: not-allowed;
}

.calendar-day--active {
  color: #030712 !important;
  background: #ffffff !important;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.25);
}

.calendar-day--today {
  border: 1px solid rgba(255, 255, 255, 0.35);
}

/* 时间滚条区 */
.calendar-time-section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.calendar-time-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 0 4px;
}

.calendar-time-label {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
}

.calendar-time-preview {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.05);
  font-size: 13px;
  font-weight: 600;
  font-family: monospace;
}

/* 滚轮选择器（高度进一步收窄至 130px，宽度收窄至 220px 居中，单行 26px） */
.time-picker-roller {
  position: relative;
  display: flex;
  width: 100%;
  max-width: 220px;
  height: 130px;
  margin: 0 auto;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.6);
  overflow: hidden;
  user-select: none;
}

/* 贯穿两列的白色高亮圆角胶囊（高度收窄至 26px，圆角 7px） */
.roller-highlight-capsule {
  position: absolute;
  top: 52px;
  left: 8px;
  right: 8px;
  height: 26px;
  border-radius: 7px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.22);
  pointer-events: none;
  z-index: 1;
}

.roller-column {
  position: relative;
  flex: 1;
  height: 100%;
  z-index: 2;
}

.wheel-scroll-track {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 52px 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scroll-snap-type: y mandatory;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.wheel-scroll-track::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.wheel-item {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 26px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  cursor: pointer;
  scroll-snap-align: center;
  scroll-snap-stop: normal;
  transition: all 0.2s cubic-bezier(0.25, 1, 0.5, 1);
  user-select: none;
}

/* 选中项（在白色胶囊上方，高亮鲜艳蓝色） */
.wheel-item--active {
  color: #2563eb !important;
  font-size: 16px;
  font-weight: 700;
  transform: scale(1);
  opacity: 1;
}

/* 临近项（上下第 1 格） */
.wheel-item--near {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
  transform: scale(0.92);
  opacity: 0.72;
}

/* 上下第 2 格（渐淡小字） */
.wheel-item--far {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 400;
  transform: scale(0.82);
  opacity: 0.35;
}

/* 更远距离 */
.wheel-item--hidden {
  color: #64748b;
  font-size: 10px;
  font-weight: 400;
  transform: scale(0.75);
  opacity: 0.12;
}

/* 底部操作 */
.calendar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.calendar-btn-cancel {
  padding: 7px 18px;
  border: 0;
  border-radius: 999px;
  color: #f8fafc;
  background: #334155;
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.calendar-btn-cancel:hover {
  background: #475569;
}

.calendar-btn-confirm {
  padding: 7px 18px;
  border: 0;
  border-radius: 999px;
  color: #030712;
  background: #ffffff;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.25);
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.calendar-btn-confirm:hover {
  background: #f4f4f5;
}

/* 动画过渡 */
.picker-fade-enter-active,
.picker-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.picker-fade-enter-from,
.picker-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>

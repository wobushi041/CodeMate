<template>
  <div id="teamAddPage" class="mall-page team-add-page">
    <van-form class="team-add-form" @submit="onSubmit">
      <van-cell-group inset class="mall-form-group">
        <van-field
            v-model="addTeamData.name"
            name="name"
            label="队伍名"
            placeholder="请输入队伍名"
            :rules="[{ required: true, message: '请输入队伍名' }]"
        />
        <van-field
            v-model="addTeamData.description"
            rows="4"
            autosize
            label="队伍描述"
            type="textarea"
            placeholder="请输入队伍描述"
        />
        <van-field
            is-link
            readonly
            name="datetimePicker"
            label="过期时间"
            placeholder="点击选择过期时间"
            :model-value="expireTimeText"
            @click="showPicker = true"
        />
        <van-field name="stepper" label="最大人数">
          <template #input>
            <van-stepper v-model="addTeamData.maxNum" max="10" min="3"/>
          </template>
        </van-field>
        <van-field name="radio" label="队伍状态">
          <template #input>
            <van-radio-group v-model="addTeamData.status" direction="horizontal">
              <van-radio name="0">公开</van-radio>
              <van-radio name="1">私有</van-radio>
              <van-radio name="2">加密</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
            v-if="Number(addTeamData.status) === 2"
            v-model="addTeamData.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入队伍密码"
            :rules="[{ required: true, message: '请填写密码' }]"
        />
      </van-cell-group>
      <div class="team-add-actions">
        <van-button round block type="primary" native-type="submit" :loading="submitting">
          提交
        </van-button>
      </div>
    </van-form>
    <van-popup v-model:show="showPicker" round position="bottom" class="team-picker-popup">
      <van-datetime-picker
          v-model="addTeamData.expireTime"
          type="datetime"
          title="请选择过期时间"
          :min-date="minDate"
          @confirm="onExpireTimeConfirm"
          @cancel="showPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">

import {useRouter} from "vue-router";
import {computed, ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";

type TeamAddForm = {
  name: string;
  description: string;
  expireTime: Date | null;
  maxNum: number;
  password: string;
  status: string;
};

const router = useRouter();
// 展示日期选择器
const showPicker = ref(false);
const submitting = ref(false);

const initFormData: TeamAddForm = {
  "name": "",
  "description": "",
  "expireTime": null,
  "maxNum": 3,
  "password": "",
  "status": '0',
}

const minDate = new Date();

// 需要用户填写的表单数据
const addTeamData = ref({...initFormData})

const expireTimeText = computed(() => {
  if (!addTeamData.value.expireTime) {
    return '';
  }
  return formatDateTime(addTeamData.value.expireTime);
});

const formatDateTime = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

const onExpireTimeConfirm = (value: Date) => {
  addTeamData.value.expireTime = value;
  showPicker.value = false;
}

// 提交
const onSubmit = async () => {
  if (submitting.value) {
    return;
  }
  const postData = {
    ...addTeamData.value,
    status: Number(addTeamData.value.status)
  }
  submitting.value = true;
  try {
    const res = await myAxios.post("/team/add", postData);
    if (res?.code === 0 && res.data){
      Toast.success('添加成功');
      router.push({
        path: '/team',
        replace: true,
      });
    } else {
      Toast.fail(res?.description || '添加失败');
    }
  } catch (e) {
    Toast.fail('添加失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.team-add-page {
  height: 100%;
  min-height: 0;
  padding: 12px 0 0;
  overflow: hidden;
}

.team-add-form {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.mall-form-group {
  box-sizing: border-box;
  flex: 0 1 auto;
  min-height: 0;
  max-height: calc(100% - 88px);
  margin: 0 12px;
  border: 1px solid var(--border-weak);
  border-radius: 12px;
  overflow-y: auto;
  overflow-x: hidden;
}

.mall-form-group :deep(.van-field__control) {
  color: var(--text-main);
}

.mall-form-group :deep(.van-field__label) {
  color: var(--text-light);
}

.team-add-actions {
  box-sizing: border-box;
  flex: 0 0 auto;
  margin-top: auto;
  padding: 14px 16px 18px;
}

.team-picker-popup {
  overflow: hidden;
  background: var(--bg-panel);
}

.team-picker-popup :deep(.van-picker) {
  background: var(--bg-panel);
}

.team-picker-popup :deep(.van-picker__toolbar) {
  background: var(--bg-panel);
  border-bottom: 1px solid var(--border-weak);
}

.team-picker-popup :deep(.van-picker__title) {
  color: var(--text-main);
  font-weight: 600;
}

.team-picker-popup :deep(.van-picker__columns) {
  background: var(--bg-panel);
}

.team-picker-popup :deep(.van-picker-column__item) {
  color: var(--text-light);
}

.team-picker-popup :deep(.van-picker-column__item--selected) {
  color: var(--text-main);
  font-weight: 600;
}

.team-picker-popup :deep(.van-picker__mask) {
  background-image:
    linear-gradient(180deg, rgba(50, 54, 68, 0.96), rgba(50, 54, 68, 0.32)),
    linear-gradient(0deg, rgba(50, 54, 68, 0.96), rgba(50, 54, 68, 0.32));
}

.team-picker-popup :deep(.van-hairline-unset--top-bottom::after) {
  border-color: var(--border-strong);
}
</style>

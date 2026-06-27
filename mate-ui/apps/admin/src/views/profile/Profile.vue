<template>
  <MatePageCard :title="t('profile.title')" :description="t('profile.description')">
    <!-- User header -->
    <div class="profile-header">
      <div class="profile-avatar">
        {{ userInitial }}
      </div>
      <div class="profile-identity">
        <span class="profile-name">{{ auth.user?.realName || auth.user?.username || t('profile.guest') }}</span>
        <span class="profile-email">{{ auth.user?.email || auth.user?.mobile || '' }}</span>
      </div>
      <div class="profile-roles">
        <el-tag v-for="r in auth.roleCodes" :key="r" size="small">{{ r }}</el-tag>
      </div>
    </div>

    <el-divider />

    <!-- Edit form -->
    <h3 class="form-title">{{ t('profile.basicInfo') }}</h3>
    <el-form :model="form" label-position="top">
      <el-form-item :label="t('login.username')">
        <el-input :model-value="auth.user?.username" disabled />
      </el-form-item>
      <el-form-item :label="t('profile.realName')">
        <el-input v-model="form.realName" />
      </el-form-item>
      <el-form-item :label="t('login.mobile')">
        <el-input v-model="form.mobile" />
      </el-form-item>
      <el-form-item :label="t('user.email')">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <h3 class="form-title">{{ t('profile.changePassword') }}</h3>
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
      <el-form-item :label="t('profile.oldPassword')" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="t('profile.newPassword')" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="t('profile.confirmPassword')" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">
          {{ t('profile.changePassword') }}
        </el-button>
      </el-form-item>
    </el-form>
  </MatePageCard>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from 'element-plus'
import { useAuthStore, adminApi } from '@matecloud/core'
import { MatePageCard, MateMessage } from '@matecloud/ui'

const { t } = useI18n()
const auth = useAuthStore()

const userInitial = computed(() =>
  (auth.user?.realName || auth.user?.username || 'G').charAt(0).toUpperCase(),
)

// ---- Basic info ----
const form = reactive({
  realName: auth.user?.realName || '',
  mobile: auth.user?.mobile || '',
  email: auth.user?.email || '',
})
const saving = ref(false)

async function handleSave() {
  if (!auth.user) return
  saving.value = true
  try {
    // 个人中心针对当前登录的 mate_admin(不是 mate_user),走 /admin/admins/profile。
    await adminApi.adminUpdateMyProfile({
      realName: form.realName,
      mobile: form.mobile,
      email: form.email,
    })
    MateMessage.success(t('common.success'))
    await auth.fetchUserInfo()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  } finally {
    saving.value = false
  }
}

// ---- Change password ----
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changingPwd = ref(false)

const pwdRules = {
  oldPassword: [{ required: true, message: t('profile.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('profile.newPasswordRequired'), trigger: 'blur' },
    { min: 6, max: 64, message: t('register.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('profile.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (_: any, value: string, callback: (err?: Error) => void) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error(t('profile.passwordMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    // 改 mate_admin 的密码(不是 mate_user)。走 /admin/admins/password,
    // 后端会用当前 session 的 loginId 找 mate_admin、验证旧密码、更新。
    await adminApi.adminChangePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    MateMessage.success(t('common.success'))
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdFormRef.value?.resetFields()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--mc-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 900;
  flex-shrink: 0;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--mc-text-primary);
}

.profile-email {
  font-size: 13px;
  color: var(--mc-text-muted);
}

.profile-roles {
  display: flex;
  gap: 6px;
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 20px;
  color: var(--mc-text-primary);
}
</style>

<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
    <!-- Global toast + modal surfaces (MateMessage / MateMessageBox). Mounted
         once here so they are always present and in the app's reactivity tree —
         appear instantly and survive route changes. -->
    <MateMessageHost />
    <MateMessageBoxHost />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElConfigProvider } from 'element-plus'
import { MateMessageHost, MateMessageBoxHost } from '@matecloud/ui'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import i18n from '@/i18n'

// Drive Element Plus's built-in text (pagination "共 N 条 / N条/页", date-picker,
// table empty, upload, etc.) from the SAME global locale ref that setLocale()
// mutates. Reading i18n.global.locale.value directly — instead of a
// component-scoped useI18n() composer — avoids any scope mismatch where the app
// language switches but EP components stay stuck on the default (English).
const globalLocale = i18n.global.locale as unknown as { value: string }
const elementLocale = computed(() => (globalLocale.value === 'en-US' ? en : zhCn))
</script>

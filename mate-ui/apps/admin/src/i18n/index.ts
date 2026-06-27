import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('mate_locale') || 'zh-CN',
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export default i18n

export function setLocale(locale: 'zh-CN' | 'en-US') {
  ;(i18n.global.locale as any).value = locale
  localStorage.setItem('mate_locale', locale)
  document.querySelector('html')?.setAttribute('lang', locale)
}

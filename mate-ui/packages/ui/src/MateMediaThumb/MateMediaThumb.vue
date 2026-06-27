<template>
  <div class="mt-thumb">
    <!-- 媒体基层:视频用首帧封面,其它用图片 -->
    <video
      v-if="type === 'video'"
      :src="posterSrc"
      class="mt-media"
      :style="{ visibility: state === 'ready' ? 'visible' : 'hidden' }"
      muted
      playsinline
      preload="metadata"
      @loadeddata="onLoaded"
      @error="onError"
    />
    <img
      v-else
      :src="src"
      class="mt-media"
      :style="{ visibility: state === 'ready' ? 'visible' : 'hidden' }"
      loading="lazy"
      :alt="alt"
      @load="onLoaded"
      @error="onError"
    />

    <!-- 加载骨架 -->
    <div v-if="state === 'loading'" class="mt-skel"></div>

    <!-- 失败兜底 -->
    <div v-else-if="state === 'error'" class="mt-broken">
      <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" /><path d="m3 16 5-5 4 4 3-3 6 6" /><circle cx="9" cy="9" r="1.6" /></svg>
      <span>{{ errText }}</span>
    </div>

    <!-- 视频播放角标 -->
    <div v-if="type === 'video' && state === 'ready'" class="mt-play">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="#fff"><path d="M8 5v14l11-7z" /></svg>
    </div>

    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateMediaThumb — media thumbnail with loading skeleton, graceful error
 * fallback and first-frame poster for videos. Replaces ad-hoc {@code <img>}
 * usage that only hides on error. Children (e.g. action buttons) render on top
 * via the default slot. The fallback label is bilingual by default.
 */
const props = withDefaults(defineProps<{
  src: string
  /** image | video | doc — video shows a first-frame poster + play badge. */
  type?: string
  alt?: string
  errorText?: string
}>(), {
  type: 'image',
  alt: '',
})

const { t } = useI18n()
const errText = computed(() => props.errorText ?? t('uikit.loadFailed'))
const state = ref<'loading' | 'ready' | 'error'>('loading')

/** Append a media fragment so the browser paints the first frame as poster. */
const posterSrc = computed(() => (props.src.includes('#') ? props.src : `${props.src}#t=0.1`))

function onLoaded() { state.value = 'ready' }
function onError() { state.value = 'error' }

watch(() => props.src, () => { state.value = 'loading' })
</script>

<style scoped>
.mt-thumb { position: relative; width: 100%; height: 100%; min-height: 100px; background: #0d0d0f; overflow: hidden; }
.mt-media { width: 100%; height: 100%; object-fit: cover; display: block; }
.mt-skel { position: absolute; inset: 0; background: linear-gradient(100deg, #eef1f5 30%, #f7f9fb 50%, #eef1f5 70%); background-size: 200% 100%; animation: mt-sh 1.2s infinite; }
@keyframes mt-sh { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.mt-broken { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; background: var(--el-fill-color-light, #f5f6f8); color: var(--el-text-color-disabled, #9aa3af); font-size: 12px; }
.mt-play { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 40px; height: 40px; display: grid; place-items: center; background: rgba(0, 0, 0, .45); border-radius: 50%; padding-left: 2px; pointer-events: none; }
</style>

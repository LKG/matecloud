<template>
  <Teleport to="body">
    <transition name="mv-fade">
      <div v-if="modelValue" class="mv-mask" @click.self="close" @wheel.prevent="onWheel">
        <!-- 图片 -->
        <img
          v-if="current && current.type === 'image'"
          :src="current.url"
          :alt="current.name"
          class="mv-img"
          :class="{ grab: scale > 1, grabbing: dragging }"
          :style="imgStyle"
          draggable="false"
          @mousedown="onDown"
          @click.stop
          @dblclick="toggleZoom"
        />
        <!-- 视频 -->
        <div v-else-if="current && current.type === 'video'" class="mv-stage" @click.stop>
          <video
            ref="videoRef"
            class="mv-video"
            :src="current.url"
            controls
            autoplay
            playsinline
            preload="metadata"
          />
        </div>

        <!-- 文件名 + 序号 -->
        <div class="mv-caption" @click.stop>
          <span class="nm">{{ current?.name }}</span>
          <span v-if="items.length > 1" class="cnt">{{ idx + 1 }} / {{ items.length }}</span>
        </div>

        <!-- 关闭 -->
        <button class="mv-close" :title="L.close" @click.stop="close">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
        </button>

        <!-- 左右切换 -->
        <button v-if="items.length > 1" class="mv-nav prev" :title="L.prev" @click.stop="prev">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6" /></svg>
        </button>
        <button v-if="items.length > 1" class="mv-nav next" :title="L.next" @click.stop="next">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6" /></svg>
        </button>

        <!-- 工具栏 -->
        <div class="mv-toolbar" @click.stop>
          <template v-if="current && current.type === 'image'">
            <button class="mv-btn" :title="L.zoomOut" @click="zoomOut">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.9"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3M8 11h6" /></svg>
            </button>
            <span class="mv-scale">{{ Math.round(scale * 100) }}%</span>
            <button class="mv-btn" :title="L.zoomIn" @click="zoomIn">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.9"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3M11 8v6M8 11h6" /></svg>
            </button>
            <span class="mv-sep"></span>
            <button class="mv-btn" :title="L.rotate" @click="rotateCw">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 12a9 9 0 1 1-3-6.7L21 8" /><path d="M21 3v5h-5" /></svg>
            </button>
            <button class="mv-btn" :title="L.reset" @click="reset">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7" /></svg>
            </button>
            <span class="mv-sep"></span>
          </template>
          <button class="mv-btn" :title="L.download" @click="download">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><path d="M7 10l5 5 5-5" /><path d="M12 15V3" /></svg>
          </button>
          <button class="mv-btn" :title="L.openNewTab" @click="openNewTab">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></svg>
          </button>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateMediaViewer — full-screen image / video lightbox.
 * <p>
 * One viewer for both media kinds: images get zoom / rotate / drag-pan, videos
 * get native playback; both share the dark overlay, prev/next navigation,
 * download + open-in-new-tab, and keyboard shortcuts (Esc / ← / → / ±).
 * Labels are bilingual by default (uikit.* i18n keys); each can be overridden
 * via the matching prop.
 */
export interface MediaItem {
  type: 'image' | 'video'
  url: string
  name?: string
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  items: MediaItem[]
  initialIndex?: number
  closeText?: string
  prevText?: string
  nextText?: string
  zoomInText?: string
  zoomOutText?: string
  rotateText?: string
  resetText?: string
  downloadText?: string
  openNewTabText?: string
}>(), {
  initialIndex: 0,
})

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const { t } = useI18n()
const L = computed(() => ({
  close: props.closeText ?? t('uikit.close'),
  prev: props.prevText ?? t('uikit.prev'),
  next: props.nextText ?? t('uikit.next'),
  zoomIn: props.zoomInText ?? t('uikit.zoomIn'),
  zoomOut: props.zoomOutText ?? t('uikit.zoomOut'),
  rotate: props.rotateText ?? t('uikit.rotate'),
  reset: props.resetText ?? t('uikit.reset'),
  download: props.downloadText ?? t('uikit.download'),
  openNewTab: props.openNewTabText ?? t('uikit.openNewTab'),
}))

const idx = ref(0)
const scale = ref(1)
const rotate = ref(0)
const pos = reactive({ x: 0, y: 0 })
const dragging = ref(false)
const dragStart = { x: 0, y: 0 }
const videoRef = ref<HTMLVideoElement>()

const current = computed<MediaItem | undefined>(() => props.items[idx.value])
const imgStyle = computed(() => ({
  transform: `translate(${pos.x}px, ${pos.y}px) scale(${scale.value}) rotate(${rotate.value}deg)`,
  transition: dragging.value ? 'none' : 'transform .2s ease',
}))

function reset() { scale.value = 1; rotate.value = 0; pos.x = 0; pos.y = 0 }
function zoomIn() { scale.value = Math.min(scale.value * 1.2, 15) }
function zoomOut() { scale.value = Math.max(scale.value / 1.2, 0.5); if (scale.value <= 1) { pos.x = 0; pos.y = 0 } }
function toggleZoom() { scale.value > 1 ? reset() : (scale.value = 2) }
function rotateCw() { rotate.value += 90 }
function onWheel(e: WheelEvent) { if (current.value?.type === 'image') e.deltaY < 0 ? zoomIn() : zoomOut() }

function onDown(e: MouseEvent) {
  if (scale.value <= 1) return
  dragging.value = true
  dragStart.x = e.clientX - pos.x
  dragStart.y = e.clientY - pos.y
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}
function onMove(e: MouseEvent) { if (dragging.value) { pos.x = e.clientX - dragStart.x; pos.y = e.clientY - dragStart.y } }
function onUp() { dragging.value = false; window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp) }

function stopVideo() {
  const v = videoRef.value
  if (v) { try { v.pause(); v.currentTime = 0 } catch { /* ignore */ } }
}
function prev() { stopVideo(); idx.value = (idx.value - 1 + props.items.length) % props.items.length }
function next() { stopVideo(); idx.value = (idx.value + 1) % props.items.length }
function close() { emit('update:modelValue', false) }

function download() {
  const m = current.value
  if (!m) return
  const a = document.createElement('a')
  a.href = m.url
  a.download = m.name || 'file'
  a.target = '_blank'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
function openNewTab() { if (current.value) window.open(current.value.url, '_blank', 'noopener') }

function onKey(e: KeyboardEvent) {
  if (!props.modelValue) return
  switch (e.key) {
    case 'Escape': close(); break
    case 'ArrowLeft': if (props.items.length > 1) prev(); break
    case 'ArrowRight': if (props.items.length > 1) next(); break
    case '+': case '=': zoomIn(); break
    case '-': zoomOut(); break
  }
}

watch(() => props.modelValue, (open) => {
  if (open) {
    idx.value = Math.min(Math.max(props.initialIndex, 0), Math.max(props.items.length - 1, 0))
    reset()
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
  } else {
    document.removeEventListener('keydown', onKey)
    document.body.style.overflow = ''
    stopVideo()
    onUp()
  }
})
watch(idx, () => { reset(); stopVideo() })

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
  onUp()
})
</script>

<style scoped>
.mv-mask { position: fixed; inset: 0; z-index: 3000; background: rgba(0, 0, 0, .84); display: flex; align-items: center; justify-content: center; overflow: hidden; user-select: none; }
.mv-img { max-width: 86vw; max-height: 82vh; object-fit: contain; box-shadow: 0 8px 40px rgba(0, 0, 0, .5); cursor: zoom-in; will-change: transform; }
.mv-img.grab { cursor: grab; } .mv-img.grabbing { cursor: grabbing; }
.mv-stage { max-width: 86vw; max-height: 82vh; display: flex; }
.mv-video { max-width: 86vw; max-height: 82vh; border-radius: 8px; background: #000; box-shadow: 0 8px 40px rgba(0, 0, 0, .5); outline: none; }

.mv-caption { position: absolute; top: 18px; left: 50%; transform: translateX(-50%); display: flex; align-items: center; gap: 12px; max-width: 60vw; color: #fff; font-size: 13px; }
.mv-caption .nm { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mv-caption .cnt { color: rgba(255, 255, 255, .65); flex: none; }

.mv-close { position: absolute; top: 16px; right: 18px; width: 38px; height: 38px; border: none; border-radius: 10px; background: rgba(255, 255, 255, .12); color: #fff; display: grid; place-items: center; cursor: pointer; transition: background .15s; }
.mv-close:hover { background: rgba(255, 255, 255, .24); }

.mv-nav { position: absolute; top: 50%; transform: translateY(-50%); width: 46px; height: 46px; border: none; border-radius: 50%; background: rgba(255, 255, 255, .1); color: #fff; display: grid; place-items: center; cursor: pointer; transition: background .15s; }
.mv-nav:hover { background: rgba(255, 255, 255, .22); }
.mv-nav.prev { left: 24px; } .mv-nav.next { right: 24px; }

.mv-toolbar { position: absolute; bottom: 26px; left: 50%; transform: translateX(-50%); display: flex; align-items: center; gap: 4px; padding: 7px 12px; border-radius: 12px; background: rgba(28, 28, 30, .78); backdrop-filter: blur(8px); box-shadow: 0 6px 24px rgba(0, 0, 0, .4); }
.mv-btn { width: 34px; height: 34px; border: none; border-radius: 8px; background: transparent; color: rgba(255, 255, 255, .82); display: grid; place-items: center; cursor: pointer; transition: background .15s, color .15s; }
.mv-btn:hover { background: rgba(255, 255, 255, .14); color: #fff; }
.mv-scale { min-width: 46px; text-align: center; color: rgba(255, 255, 255, .82); font-size: 12px; font-variant-numeric: tabular-nums; }
.mv-sep { width: 1px; height: 18px; background: rgba(255, 255, 255, .18); margin: 0 4px; }

.mv-fade-enter-active, .mv-fade-leave-active { transition: opacity .18s ease; }
.mv-fade-enter-from, .mv-fade-leave-to { opacity: 0; }
</style>

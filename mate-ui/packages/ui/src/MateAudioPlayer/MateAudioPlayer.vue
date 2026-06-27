<template>
  <div class="map">
    <button type="button" class="map-play" :aria-label="playing ? t('uikit.pause') : t('uikit.play')" @click="toggle">
      <svg v-if="!playing" viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M8 5v14l11-7z" /></svg>
      <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M6 5h4v14H6zM14 5h4v14h-4z" /></svg>
    </button>

    <div class="map-track" @click="seek">
      <div class="map-fill" :style="{ width: `${progress}%` }"></div>
      <span class="map-thumb" :style="{ left: `${progress}%` }"></span>
    </div>

    <span class="map-time">{{ fmt(currentTime) }} / {{ fmt(duration) }}</span>

    <audio
      ref="audioRef"
      :src="src"
      preload="metadata"
      @timeupdate="onTime"
      @loadedmetadata="onMeta"
      @ended="onEnded"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateAudioPlayer — compact audio player (play/pause + seekable track + time).
 * For an "audio" material type or any inline audio preview. Control labels are
 * bilingual by default.
 */
const props = defineProps<{ src: string; name?: string }>()

const { t } = useI18n()

const audioRef = ref<HTMLAudioElement>()
const playing = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const progress = ref(0)

function toggle() {
  const a = audioRef.value
  if (!a) return
  if (a.paused) { a.play(); playing.value = true }
  else { a.pause(); playing.value = false }
}
function onTime() {
  const a = audioRef.value
  if (!a) return
  currentTime.value = a.currentTime
  progress.value = a.duration ? (a.currentTime / a.duration) * 100 : 0
}
function onMeta() {
  const a = audioRef.value
  if (a) duration.value = a.duration || 0
}
function onEnded() { playing.value = false; progress.value = 0; currentTime.value = 0 }
function seek(e: MouseEvent) {
  const a = audioRef.value
  if (!a || !a.duration) return
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const ratio = Math.min(Math.max((e.clientX - rect.left) / rect.width, 0), 1)
  a.currentTime = ratio * a.duration
}
function fmt(s: number) {
  if (!Number.isFinite(s)) return '00:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}
// expose name to satisfy unused-prop lint while documenting intent
void props.name
</script>

<style scoped>
.map { display: flex; align-items: center; gap: 12px; width: 100%; max-width: 440px; background: #fff; border: 1px solid var(--el-border-color); border-radius: 12px; padding: 9px 14px; }
.map-play { flex: none; width: 36px; height: 36px; border: none; border-radius: 50%; background: var(--el-color-primary); display: grid; place-items: center; cursor: pointer; }
.map-play:hover { filter: brightness(.95); }
.map-track { position: relative; flex: 1; height: 6px; border-radius: 4px; background: var(--el-fill-color); cursor: pointer; }
.map-fill { position: absolute; left: 0; top: 0; height: 100%; border-radius: 4px; background: var(--el-color-primary); }
.map-thumb { position: absolute; top: 50%; width: 12px; height: 12px; border-radius: 50%; background: #fff; border: 2px solid var(--el-color-primary); transform: translate(-50%, -50%); }
.map-time { flex: none; font-size: 12px; color: var(--el-text-color-secondary); font-variant-numeric: tabular-nums; }
</style>

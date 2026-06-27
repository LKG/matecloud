<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="380px"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    align-center
    destroy-on-close
    class="captcha-dialog"
    @closed="onDialogClosed"
  >
    <div class="slider-captcha" :class="{ success: state === 'success', fail: state === 'fail' }">
      <!-- Image area: fixed 310x155 to match captcha-plus native resolution -->
      <div class="captcha-image-wrap" ref="imageWrapRef">
        <img
          v-if="challenge"
          :src="'data:image/png;base64,' + challenge.originalImageBase64"
          class="captcha-bg"
          draggable="false"
          alt=""
        />
        <div v-else class="captcha-placeholder">
          <span class="captcha-loading-text">加载中...</span>
        </div>

        <!-- Puzzle piece (same height as bg, auto width) -->
        <img
          v-if="challenge"
          :src="'data:image/png;base64,' + challenge.jigsawImageBase64"
          class="captcha-piece"
          :style="{ left: moveBlockLeft + 'px' }"
          draggable="false"
          alt=""
        />

        <!-- Success / fail overlay -->
        <Transition name="fade">
          <div v-if="state === 'success'" class="captcha-overlay success-overlay">
            <span class="overlay-icon">&#10003;</span>
            <span class="overlay-text">验证成功</span>
          </div>
          <div v-else-if="state === 'fail'" class="captcha-overlay fail-overlay">
            <span class="overlay-icon">&#10007;</span>
            <span class="overlay-text">验证失败，请重试</span>
          </div>
        </Transition>

        <!-- Refresh button -->
        <button class="refresh-btn" title="刷新" @click.stop="loadChallenge">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10"></polyline>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
          </svg>
        </button>
      </div>

      <!-- Slider track (same 310px width as image) -->
      <div class="slider-track" ref="trackRef">
        <div class="slider-bar" :style="{ width: moveBlockLeft + 'px' }" />
        <div
          class="slider-thumb"
          :style="{ left: moveBlockLeft + 'px' }"
          @mousedown.prevent="startDrag"
          @touchstart.prevent="startDrag"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        <span class="slider-hint" v-if="state !== 'success'">拖动滑块完成拼图</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CryptoJS from 'crypto-js'
import { authApi } from '../api/modules/auth'
import type { CaptchaChallenge } from '../types/user'

// ── Props ─────────────────────────────────────────────────────────────────
withDefaults(defineProps<{
  title?: string
}>(), {
  title: '请完成安全验证',
})

// ── Constants ─────────────────────────────────────────────────────────────
// captcha-plus generates images at 310x155
const IMG_W = 310
const IMG_H = 155
const CAPTCHA_TYPE = 'blockPuzzle'

const emit = defineEmits<{
  (e: 'verified', token: string): void
  (e: 'cancel'): void
}>()

// ── State ─────────────────────────────────────────────────────────────────
const visible = ref(false)
type CaptchaState = 'loading' | 'idle' | 'dragging' | 'verifying' | 'success' | 'fail'
const state = ref<CaptchaState>('loading')
const challenge = ref<CaptchaChallenge | null>(null)

// moveBlockLeft: pixel position of the puzzle piece / slider thumb (0..~260)
const moveBlockLeft = ref(0)

const trackRef = ref<HTMLElement | null>(null)
const imageWrapRef = ref<HTMLElement | null>(null)

let startX = 0       // mousedown clientX
let startLeft = 0    // moveBlockLeft at drag start
let verified = false

// ── Open / close ──────────────────────────────────────────────────────────
function open() {
  verified = false
  visible.value = true
  resetState()
  setTimeout(() => loadChallenge(), 100)
}

function onDialogClosed() {
  removeDragListeners()
  if (!verified) emit('cancel')
}

function resetState() {
  state.value = 'loading'
  challenge.value = null
  moveBlockLeft.value = 0
}

// ── Challenge loading ─────────────────────────────────────────────────────
async function loadChallenge() {
  resetState()
  try {
    const res = await authApi.getCaptchaChallenge(CAPTCHA_TYPE)
    challenge.value = (res as any).repData ?? (res as any).data ?? res
    state.value = 'idle'
  } catch {
    state.value = 'fail'
  }
}

// ── AES-ECB encryption (matches the captcha backend's expected format) ────
function aesEncrypt(word: string, keyWord: string): string {
  const key = CryptoJS.enc.Utf8.parse(keyWord)
  const srcs = CryptoJS.enc.Utf8.parse(word)
  return CryptoJS.AES.encrypt(srcs, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  }).toString()
}

// ── Drag handlers ─────────────────────────────────────────────────────────
function startDrag(e: MouseEvent | TouchEvent) {
  if (state.value !== 'idle') return
  state.value = 'dragging'
  startX = 'touches' in e ? e.touches[0].clientX : e.clientX
  startLeft = moveBlockLeft.value

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', endDrag)
}

function onDrag(e: MouseEvent | TouchEvent) {
  if (state.value !== 'dragging') return
  if ('cancelable' in e && e.cancelable) e.preventDefault()

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const delta = clientX - startX
  const barW = imageWrapRef.value?.clientWidth ?? IMG_W

  let newLeft = startLeft + delta
  // Clamp: 0 .. barW - ~47 (piece width ≈ 47px)
  if (newLeft < 0) newLeft = 0
  if (newLeft > barW - 47) newLeft = barW - 47

  moveBlockLeft.value = newLeft
}

async function endDrag() {
  removeDragListeners()
  if (state.value !== 'dragging') return
  state.value = 'verifying'

  const ch = challenge.value
  if (!ch) { state.value = 'fail'; return }

  // Normalize pixel position to 310px original image coordinate
  const barW = imageWrapRef.value?.clientWidth ?? IMG_W
  const moveLeftDistance = (moveBlockLeft.value * 310) / barW

  const pointJson = ch.secretKey
    ? aesEncrypt(JSON.stringify({ x: moveLeftDistance, y: 5.0 }), ch.secretKey)
    : JSON.stringify({ x: moveLeftDistance, y: 5.0 })

  try {
    const res = await authApi.checkCaptcha({
      captchaType: CAPTCHA_TYPE,
      token: ch.token,
      pointJson,
    })

    const repCode = (res as any).repCode ?? (res as any).data?.repCode
    if (repCode === '0000') {
      // Build captchaVerification token:
      // AES( token + "---" + pointJson, secretKey )
      // This is what the backend's verification() method expects.
      const captchaVerification = aesEncrypt(
        ch.token + '---' + JSON.stringify({ x: moveLeftDistance, y: 5.0 }),
        ch.secretKey
      )

      state.value = 'success'
      verified = true
      setTimeout(() => {
        visible.value = false
        emit('verified', captchaVerification)
      }, 600)
    } else {
      throw new Error('verification failed')
    }
  } catch {
    state.value = 'fail'
    setTimeout(() => loadChallenge(), 1200)
  }
}

function removeDragListeners() {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)
}

defineExpose({ open })
</script>

<style scoped>
.slider-captcha {
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
}

/* ── Image area: fixed 310x155, centered ── */
.captcha-image-wrap {
  position: relative;
  width: 310px;
  height: 155px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--mc-fill, #f0f2f5);
  border: 1px solid var(--mc-border, #e4e7ed);
}

.captcha-bg {
  width: 310px;
  height: 155px;
  display: block;
  pointer-events: none;
}

.captcha-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-loading-text {
  font-size: 13px;
  color: var(--mc-text-muted, #909399);
}

.captcha-piece {
  position: absolute;
  top: 0;
  left: 0;
  height: 155px;
  width: auto;
  pointer-events: none;
  filter: drop-shadow(0 2px 6px rgba(0,0,0,0.35));
}

/* Overlays */
.captcha-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
}
.success-overlay { background: rgba(103, 194, 58, 0.82); }
.fail-overlay    { background: rgba(245, 108, 108, 0.82); }
.overlay-icon  { font-size: 32px; color: #fff; line-height: 1; }
.overlay-text  { font-size: 14px; font-weight: 600; color: #fff; }

/* Refresh button */
.refresh-btn {
  position: absolute;
  top: 6px; right: 6px;
  width: 28px; height: 28px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.30);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  padding: 5px;
  transition: background 0.2s;
}
.refresh-btn:hover { background: rgba(0,0,0,0.50); }
.refresh-btn svg { width: 14px; height: 14px; }

/* ── Slider track: same 310px width as image ── */
.slider-track {
  position: relative;
  width: 310px;
  height: 40px;
  margin-top: 10px;
  border-radius: 20px;
  background: var(--mc-fill, #f0f2f5);
  border: 1px solid var(--mc-border, #e4e7ed);
  overflow: hidden;
}

.slider-bar {
  position: absolute;
  top: 0; left: 0; bottom: 0;
  background: linear-gradient(90deg, rgba(var(--mc-primary-rgb, 64, 158, 255), 0.25), rgba(var(--mc-primary-rgb, 64, 158, 255), 0.12));
  pointer-events: none;
}

.slider-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--mc-text-muted, #909399);
  pointer-events: none;
}
.success .slider-hint { display: none; }

.slider-thumb {
  position: absolute;
  top: 0; bottom: 0;
  width: 40px;
  background: var(--mc-primary, #409eff);
  border-radius: 20px;
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 2px 8px rgba(var(--mc-primary-rgb, 64, 158, 255), 0.4);
  transition: background 0.2s, box-shadow 0.2s;
  z-index: 2;
}
.slider-thumb:active { cursor: grabbing; }
.slider-thumb svg { width: 20px; height: 20px; }

.success .slider-thumb { background: #67c23a; box-shadow: 0 2px 8px rgba(103, 194, 58, 0.4); }
.fail    .slider-thumb { background: #f56c6c; box-shadow: 0 2px 8px rgba(245, 108, 108, 0.4); }

/* Transitions */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<style>
/* Dialog overrides */
.captcha-dialog .el-dialog__header {
  padding: 16px 20px 12px;
  margin: 0;
}
.captcha-dialog .el-dialog__body {
  padding: 0 20px 20px;
}
</style>

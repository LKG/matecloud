<template>
  <div class="gallery">
    <header class="gh">
      <div>
        <h1>Base 组件画廊</h1>
        <p>RFC-053 #1 · MateButton / MateTag / MateInput · CVA 变体 + 组件语义 Token</p>
      </div>
      <MateButton variant="secondary" size="sm" @click="system.toggleDark()">
        <template #icon><component :is="system.isDark ? IconSun : IconMoon" :size="14" /></template>
        {{ system.isDark ? '亮色模式' : '暗色模式' }}
      </MateButton>
    </header>

    <!-- MateButton -->
    <section class="card">
      <h2>MateButton</h2>
      <div class="row"><span class="lbl">variant</span>
        <MateButton variant="primary">Primary</MateButton>
        <MateButton variant="secondary">Secondary</MateButton>
        <MateButton variant="ghost">Ghost</MateButton>
        <MateButton variant="danger">Danger</MateButton>
        <MateButton variant="secondary" disabled>Disabled</MateButton>
      </div>
      <div class="row"><span class="lbl">size</span>
        <MateButton size="sm">sm</MateButton>
        <MateButton size="md">md</MateButton>
        <MateButton size="lg">lg</MateButton>
      </div>
      <div class="row"><span class="lbl">state</span>
        <MateButton :loading="true">Loading</MateButton>
        <MateButton><template #icon><Plus :size="14" /></template>带图标</MateButton>
        <MateButton variant="danger"><template #icon><Trash2 :size="14" /></template>删除</MateButton>
      </div>
      <div class="row"><span class="lbl">block</span>
        <div style="flex:1"><MateButton block>整宽按钮 block</MateButton></div>
      </div>
    </section>

    <!-- MateTag -->
    <section class="card">
      <h2>MateTag</h2>
      <div class="row"><span class="lbl">variant</span>
        <MateTag variant="default">default</MateTag>
        <MateTag variant="primary">primary</MateTag>
        <MateTag variant="success" dot>启用</MateTag>
        <MateTag variant="warning" dot>冻结</MateTag>
        <MateTag variant="danger" dot>删除</MateTag>
        <MateTag variant="info">info</MateTag>
      </div>
      <div class="row"><span class="lbl">size</span>
        <MateTag variant="primary" size="sm">sm</MateTag>
        <MateTag variant="primary" size="md">md</MateTag>
      </div>
    </section>

    <!-- MateInput -->
    <section class="card">
      <h2>MateInput</h2>
      <div class="grid">
        <div class="field"><span class="lbl">默认 + 前缀图标</span>
          <MateInput v-model="v1" placeholder="搜索用户、角色…" clearable>
            <template #prefix><Search :size="15" /></template>
          </MateInput>
          <small>值：{{ v1 || '(空)' }}</small>
        </div>
        <div class="field"><span class="lbl">large</span>
          <MateInput v-model="v2" size="lg" placeholder="大尺寸" />
        </div>
        <div class="field"><span class="lbl">disabled</span>
          <MateInput model-value="不可编辑" disabled />
        </div>
        <div class="field"><span class="lbl">destructive(校验失败)</span>
          <MateInput v-model="v3" destructive />
          <small class="err">邮箱格式不正确</small>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Trash2, Search, Sun as IconSun, Moon as IconMoon } from 'lucide-vue-next'
import { MateButton, MateTag, MateInput } from '@matecloud/ui'
import { useSystemStore } from '@/stores/system'

defineOptions({ name: 'ComponentsGallery' })

const system = useSystemStore()
const v1 = ref('')
const v2 = ref('')
const v3 = ref('admin@')
</script>

<style scoped>
.gallery { max-width: 920px; margin: 0 auto; padding: 28px 24px 80px; }
.gh { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22px; }
.gh h1 { font-size: 20px; font-weight: 800; margin: 0 0 4px; color: var(--mc-text-primary); }
.gh p { margin: 0; color: var(--mc-text-muted); font-size: 13px; }
.card {
  background: var(--mc-card-bg);
  border: 1px solid var(--mc-card-border);
  border-radius: var(--mc-radius-lg);
  box-shadow: var(--mc-card-shadow);
  padding: 20px 22px;
  margin-bottom: 18px;
}
.card h2 { font-size: 15px; margin: 0 0 16px; color: var(--mc-text-primary); }
.row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.row:last-child { margin-bottom: 0; }
.lbl { width: 110px; flex-shrink: 0; font-size: 11px; font-weight: 600; text-transform: uppercase;
  letter-spacing: .4px; color: var(--mc-text-muted); }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field small { font-size: 11px; color: var(--mc-text-muted); }
.field small.err { color: var(--mc-danger); }
</style>

<template>
  <span class="ftype" :class="[cls, size === 'sm' ? 'sm' : '']">{{ label }}</span>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{ ext: string; size?: 'sm' }>()

const FAMILY: Record<string, string[]> = {
  pdf: ['pdf'],
  doc: ['doc', 'docx'],
  xls: ['xls', 'xlsx', 'csv'],
  ppt: ['ppt', 'pptx'],
  zip: ['zip', 'rar', '7z'],
  txt: ['txt', 'md'],
}

const cls = computed(() => {
  const e = (props.ext || '').toLowerCase()
  for (const k of Object.keys(FAMILY)) {
    if (FAMILY[k].includes(e)) return k
  }
  return 'gen'
})
const label = computed(() => (props.ext || '?').slice(0, 4).toUpperCase())
</script>

<style scoped>
.ftype {
  width: 54px; height: 66px; border-radius: 7px; position: relative;
  display: grid; place-items: end center; color: #fff; font-weight: 800;
  font-size: 12px; letter-spacing: .5px; padding-bottom: 8px;
  box-shadow: 0 2px 6px rgba(16, 24, 40, .12); flex: none;
}
.ftype::before {
  content: ""; position: absolute; top: 0; right: 0;
  border-width: 0 14px 14px 0; border-style: solid;
  border-color: rgba(255, 255, 255, .5) #fff transparent transparent; border-top-right-radius: 7px;
}
.ftype.sm { width: 32px; height: 40px; font-size: 9px; padding-bottom: 4px; }
.ftype.sm::before { border-width: 0 9px 9px 0; }
.ftype.pdf { background: #E5484D; }
.ftype.doc { background: #155AEF; }
.ftype.xls { background: #17B26A; }
.ftype.ppt { background: #F79009; }
.ftype.zip { background: #667085; }
.ftype.txt { background: #7A5AF8; }
.ftype.gen { background: #98A2B2; }
</style>

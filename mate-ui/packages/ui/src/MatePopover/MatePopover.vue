<template>
  <el-popover>
    <template v-if="$slots.reference" #reference>
      <slot name="reference" />
    </template>
    <slot />
  </el-popover>
</template>

<script setup lang="ts">
/**
 * MatePopover — faithful pass-through wrapper around {@code el-popover}.
 * Props/attrs auto-inherit to the el-popover root; the `#reference` trigger and
 * the default (content) slot are forwarded. Behaviour identical to el-popover.
 */
</script>

<template>
  <el-tooltip>
    <slot />
    <template v-if="$slots.content" #content>
      <slot name="content" />
    </template>
  </el-tooltip>
</template>

<script setup lang="ts">
/**
 * MateTooltip — faithful pass-through wrapper around {@code el-tooltip}.
 * All props/attrs auto-inherit to the single el-tooltip root; the trigger
 * (default slot) and `#content` slot are forwarded. Behaviour is identical to
 * el-tooltip — the wrapper exists so the app consumes one Mate* surface and we
 * get a single place to restyle tooltips later.
 *
 *   <MateTooltip content="提示" placement="top"><button>?</button></MateTooltip>
 */
</script>

<template>
  <el-dropdown>
    <slot />
    <template #dropdown>
      <slot name="dropdown" />
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
/**
 * MateDropdown — faithful pass-through wrapper around {@code el-dropdown}.
 * Props/attrs (incl. @command) auto-inherit to the el-dropdown root; the
 * trigger (default slot) and the `#dropdown` menu slot are forwarded. Use with
 * el-dropdown-menu / el-dropdown-item inside `#dropdown`, exactly like el-dropdown.
 */
</script>

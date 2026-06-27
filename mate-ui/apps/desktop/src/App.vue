<script setup lang="ts">
import { MateMessageHost, MateMessageBoxHost } from '@matecloud/ui'
</script>

<template>
  <router-view />
  <!-- Global toast + modal surfaces (MateMessage / MateMessageBox). -->
  <MateMessageHost />
  <MateMessageBoxHost />
</template>

<style>
html, body, #app {
  margin: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f5f7fa;
}
</style>

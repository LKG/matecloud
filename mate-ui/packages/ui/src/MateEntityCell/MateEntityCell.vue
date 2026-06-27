<template>
  <div class="mc-entity-cell">
    <div v-if="hasVisual" class="mc-entity-cell__visual">
      <component :is="icon" v-if="icon" :size="iconSize" />
      <span v-else-if="avatar">{{ avatarInitial }}</span>
    </div>
    <div class="mc-entity-cell__info">
      <span class="mc-entity-cell__name">{{ name }}</span>
      <span v-if="$slots.sub || sub" class="mc-entity-cell__sub" :class="{ 'is-mono': subMono }">
        <slot name="sub">{{ sub }}</slot>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'

/**
 * MateEntityCell — a compact "identity card" cell used inside table rows.
 *
 * Replaces the three near-identical {@code .user-cell}/{@code .admin-cell}/
 * {@code .role-cell} blocks previously copy-pasted across UserList, AdminList
 * and RoleList. Each rendered a 34px gradient box (avatar initial OR lucide
 * icon) on the left and a two-line name+sub block on the right.
 *
 * Usage:
 *   <MateEntityCell :name="row.realName" :sub="row.username" avatar />
 *   <MateEntityCell :name="row.roleName" :sub="row.roleKey" :icon="Shield" sub-mono />
 */
const props = withDefaults(defineProps<{
  /** Primary text (first line) */
  name: string
  /** Secondary text (second line) — can also be provided via #sub slot */
  sub?: string
  /**
   * When true, renders a gradient tile showing {@code name}'s first letter.
   * Mutually preferred over {@code icon} when both are set.
   */
  avatar?: boolean
  /** Lucide (or any Vue) component to render inside the gradient tile. */
  icon?: Component
  /** Icon size inside the tile (defaults to 14 so it visually matches avatar). */
  iconSize?: number
  /** Render sub text in monospace font (useful for IDs, keys, handles). */
  subMono?: boolean
}>(), {
  avatar: false,
  iconSize: 14,
  subMono: false,
})

const hasVisual = computed(() => props.avatar || !!props.icon)
const avatarInitial = computed(() =>
  (props.name || '?').trim().charAt(0).toUpperCase(),
)
</script>

<style scoped>
.mc-entity-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.mc-entity-cell__visual {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--mc-primary), var(--mc-primary-hover));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 4px 10px -3px rgba(var(--mc-primary-rgb), 0.35);
}

.mc-entity-cell__info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.mc-entity-cell__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mc-entity-cell__sub {
  font-size: 12px;
  color: var(--mc-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mc-entity-cell__sub.is-mono {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
</style>

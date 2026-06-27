// MateCloud UI — global styles + components
import './styles/element-override.css'
import './styles/action-btn.css'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/github-dark.css'
import './base/base.css'

export { default as MateTable } from './MateTable/MateTable.vue'
export type { MateColumn } from './MateTable/types'

export { default as MateForm } from './MateForm/MateForm.vue'
export type { FormSchema } from './MateForm/types'

export { default as MatePageCard } from './MatePageCard/MatePageCard.vue'
export { default as MatePane } from './MatePane/MatePane.vue'
export { default as MateInfoBanner } from './MateInfoBanner/MateInfoBanner.vue'
export { default as MateSettingsRow } from './MateSettingsRow/MateSettingsRow.vue'
export { default as MateSearchBar } from './MateSearchBar/MateSearchBar.vue'
export { default as MateBadge } from './MateBadge/MateBadge.vue'
export { mapStatus } from './MateBadge/status'
export type { BadgeType } from './MateBadge/status'
export { default as MateListView } from './MateListView/MateListView.vue'
export type { ListColumn } from './MateListView/types'

// ---- Added in RFC-047 (UI componentization pass) ----
export { default as MatePagination } from './MatePagination/MatePagination.vue'
export { default as MateEntityCell } from './MateEntityCell/MateEntityCell.vue'
export { default as MateDialog } from './MateDialog/MateDialog.vue'
export { default as MateDrawer } from './MateDrawer/MateDrawer.vue'
export { default as MateEmpty } from './MateEmpty/MateEmpty.vue'

// ---- Media / interaction components (componentization pass) ----
export { default as MateMediaViewer } from './MateMediaViewer/MateMediaViewer.vue'
export type { MediaItem } from './MateMediaViewer/MateMediaViewer.vue'
export { default as MateMediaThumb } from './MateMediaThumb/MateMediaThumb.vue'
export { default as MateDocViewer } from './MateDocViewer/MateDocViewer.vue'
export { default as MateInlineConfirm } from './MateInlineConfirm/MateInlineConfirm.vue'
export { default as MateTooltip } from './MateTooltip/MateTooltip.vue'
export { default as MateDropdown } from './MateDropdown/MateDropdown.vue'
export { default as MatePopover } from './MatePopover/MatePopover.vue'
export { default as MateCopyButton } from './MateCopyButton/MateCopyButton.vue'
export { default as MateSkeleton } from './MateSkeleton/MateSkeleton.vue'
export { default as MateTagInput } from './MateTagInput/MateTagInput.vue'
export { default as MateAudioPlayer } from './MateAudioPlayer/MateAudioPlayer.vue'
export { default as MateDropzone } from './MateDropzone/MateDropzone.vue'
export { default as MateUploadZone } from './MateUploadZone/MateUploadZone.vue'
export type { UploadItem } from './MateUploadZone/MateUploadZone.vue'
export { MateStatTile } from './MateStatTile'

// ---- Added for RFC-048 (Excel import/export reusable) ----
export { default as MateImportExport } from './MateImportExport/MateImportExport.vue'

// ---- RFC-048 G1 (batch ops bar) ----
export { default as MateBatchBar } from './MateBatchBar/MateBatchBar.vue'

// ---- RFC-053 #1: base atoms + CVA variants ----
export { default as MateButton } from './base/button/MateButton.vue'
export { buttonVariants } from './base/button/buttonVariants'
export type { ButtonVariantProps } from './base/button/buttonVariants'

export { default as MateTag } from './base/tag/MateTag.vue'
export { tagVariants } from './base/tag/tagVariants'
export type { TagVariantProps } from './base/tag/tagVariants'

export { default as MateInput } from './base/input/MateInput.vue'
export { inputVariants } from './base/input/inputVariants'
export type { InputVariantProps } from './base/input/inputVariants'

// ---- mate-ai (AI assistant suite — Chat / Agents / MCP / Models / Tools) ----
export { default as AiAgentBadge }   from './AiAgentBadge/AiAgentBadge.vue'
export { default as AiStatusDot }    from './AiStatusDot/AiStatusDot.vue'
export { default as AiChatMessage }  from './AiChatMessage/AiChatMessage.vue'
export { default as AiSessionItem }  from './AiSessionItem/AiSessionItem.vue'
export { default as AiProviderCard } from './AiProviderCard/AiProviderCard.vue'
export { default as AiToolCallChip } from './AiToolCallChip/AiToolCallChip.vue'

// ---- 共享 Markdown 渲染内核 ----
export { renderMarkdown, escapeHtml, preprocessLatex } from './markdown/render'

export { default as MateAsync } from './MateAsync/MateAsync.vue'

// ---- MateMessage (toast) — drop-in replacement for ElMessage ----
export { MateMessage } from './MateMessage/message'
export { default as MateMessageHost } from './MateMessage/MateMessageHost.vue'
export type {
  MateMessageType,
  MateMessagePlacement,
  MateMessageOptions,
  MateMessageHandle,
} from './MateMessage/message'

// ---- MateMessageBox (modal confirm/alert/prompt) — drop-in for ElMessageBox ----
export { MateMessageBox } from './MateMessage/messageBox'
export { default as MateMessageBoxHost } from './MateMessage/MateMessageBoxHost.vue'
export type { MateBoxType, MateMessageBoxOptions } from './MateMessage/messageBox'

<template>
  <div class="ai-chat">
    <!-- Left: sessions rail -->
    <aside class="ai-chat__sidebar">
      <div class="ai-chat__sidebar-head">
        <el-button type="primary" size="small" :icon="Plus" @click="newConversation">
          {{ t('ai.chat.newSession') }}
        </el-button>
      </div>
      <el-input
        v-model="search"
        size="small"
        :placeholder="t('ai.chat.searchPlaceholder')"
        :prefix-icon="Search"
        clearable
        @input="reloadConversations"
        style="margin-bottom: 8px"
      />
      <div class="ai-chat__sessions" v-loading="loadingList">
        <AiSessionItem
          v-for="c in conversations"
          :key="c.id"
          :title="c.title"
          :active="c.id === activeId"
          :agent-label="c.agentCode || ''"
          :time="formatRelative(c.lastActiveAt || c.createdAt)"
          :message-count="c.messageCount"
          :model="c.model"
          @select="loadConversation(c.id)"
        />
        <MateEmpty v-if="!conversations.length && !loadingList" :description="t('ai.chat.sessionsEmpty')" />
      </div>
    </aside>

    <!-- Right: chat panel -->
    <section class="ai-chat__main">
      <header class="ai-chat__head">
        <div class="ai-chat__title">
          <span>{{ detail?.header.title || t('ai.chat.placeholderTitle') }}</span>
          <AiAgentBadge v-if="detail?.header.agentCode"
                        :code="detail.header.agentCode"
                        :label="detail.header.agentCode"
                        size="sm" />
        </div>
        <div v-if="detail" class="ai-chat__actions">
          <el-button text size="small" :icon="EditPen" @click="renameVisible = true">
            {{ t('ai.chat.rename') }}
          </el-button>
          <el-button text size="small" :icon="Delete" type="danger" @click="deleteCurrent">
            {{ t('ai.chat.delete') }}
          </el-button>
        </div>
      </header>

      <div ref="messagesRef" class="ai-chat__messages" v-loading="loadingDetail">
        <MateEmpty v-if="!detail" :description="t('ai.chat.emptyDetail')" />
        <template v-else>
          <AiChatMessage
            v-for="m in detail.messages"
            :key="m.id"
            :role="m.role.toLowerCase() as any"
            :content="m.content"
            :time="formatTime(m.createdAt)"
            :latency-ms="m.latencyMs"
          />
          <AiChatMessage v-if="streaming" role="assistant" :content="streamingContent">
            <template #meta-extra>
              <AiStatusDot state="streaming" :label="t('ai.chat.generating')" />
            </template>
          </AiChatMessage>
        </template>
      </div>

      <footer class="ai-chat__compose" v-if="detail">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          :placeholder="t('ai.chat.composerPlaceholder')"
          @keydown.enter.exact.prevent="send"
        />
        <div class="ai-chat__compose-bar">
          <span class="ai-chat__hint">{{ t('ai.chat.charCount', { n: input.length }) }}</span>
          <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="send">
            {{ t('ai.chat.send') }}
          </el-button>
        </div>
      </footer>
    </section>

    <!-- New / rename dialog -->
    <MateDialog v-model="newVisible" :title="t('ai.chat.newSessionTitle')" width="480px">
      <el-form label-position="top" :model="newForm">
        <el-form-item :label="t('ai.chat.titleLabel')">
          <el-input v-model="newForm.title" :placeholder="t('ai.chat.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.chat.agentLabel')">
          <el-select v-model="newForm.agentCode" :placeholder="t('ai.chat.agentPlaceholder')">
            <el-option v-for="a in agents" :key="a.id"
                       :label="a.name + ' (' + a.code + ')'"
                       :value="a.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ai.chat.systemPromptLabel')">
          <el-input v-model="newForm.systemPrompt" type="textarea" :rows="3"
                    :placeholder="t('ai.chat.systemPromptPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newVisible = false">{{ t('ai.common.cancel') }}</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreate">
          {{ t('ai.chat.create') }}
        </el-button>
      </template>
    </MateDialog>

    <MateDialog v-model="renameVisible" :title="t('ai.chat.renameTitle')" width="420px">
      <el-input v-model="renameTitle" :placeholder="t('ai.chat.renamePlaceholder')" />
      <template #footer>
        <el-button @click="renameVisible = false">{{ t('ai.common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmRename">{{ t('ai.common.save') }}</el-button>
      </template>
    </MateDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Search, EditPen, Delete } from '@element-plus/icons-vue'
import { aiApi, type ConversationView, type ConversationDetail, type AgentView } from '@matecloud/core'
import { AiAgentBadge, AiChatMessage, AiSessionItem, AiStatusDot, MateDialog, MateEmpty, MateMessage, MateMessageBox } from '@matecloud/ui'

defineOptions({ name: 'AiChatView' })

const { t, locale } = useI18n()

const search = ref('')
const loadingList = ref(false)
const conversations = ref<ConversationView[]>([])
const activeId = ref<string | null>(null)
const detail = ref<ConversationDetail | null>(null)
const loadingDetail = ref(false)
const agents = ref<AgentView[]>([])

const input = ref('')
const sending = ref(false)
const streaming = ref(false)
const streamingContent = ref('')
const messagesRef = ref<HTMLElement | null>(null)

const newVisible = ref(false)
const creating = ref(false)
const newForm = reactive({ title: '', agentCode: '', systemPrompt: '' })

const renameVisible = ref(false)
const renameTitle = ref('')

async function reloadConversations() {
  loadingList.value = true
  try {
    const res: any = await aiApi.conversationPage({
      pageNum: 1, pageSize: 50, keyword: search.value || undefined,
    })
    conversations.value = res.data?.list || []
  } finally {
    loadingList.value = false
  }
}

async function loadConversation(id: string) {
  activeId.value = id
  loadingDetail.value = true
  try {
    const res: any = await aiApi.conversationDetail(id)
    detail.value = res.data
    await nextTick()
    scrollToBottom()
  } finally {
    loadingDetail.value = false
  }
}

async function loadAgents() {
  try {
    const res: any = await aiApi.agentListEnabled()
    agents.value = res.data || []
  } catch { agents.value = [] }
}

function newConversation() {
  Object.assign(newForm, { title: '', agentCode: '', systemPrompt: '' })
  newVisible.value = true
}

async function confirmCreate() {
  creating.value = true
  try {
    const res: any = await aiApi.conversationCreate({
      title: newForm.title || undefined,
      agentCode: newForm.agentCode || undefined,
      systemPrompt: newForm.systemPrompt || undefined,
    })
    newVisible.value = false
    await reloadConversations()
    if (res.data?.id) loadConversation(res.data.id)
  } finally {
    creating.value = false
  }
}

async function confirmRename() {
  if (!detail.value || !renameTitle.value.trim()) return
  await aiApi.conversationUpdate(detail.value.header.id, { title: renameTitle.value.trim() })
  renameVisible.value = false
  await reloadConversations()
  await loadConversation(detail.value.header.id)
}

async function deleteCurrent() {
  if (!detail.value) return
  await MateMessageBox.confirm(t('ai.chat.deleteConfirm'), t('ai.common.confirmTitle'), { type: 'warning' })
  await aiApi.conversationDelete(detail.value.header.id)
  MateMessage.success(t('ai.common.deleted'))
  detail.value = null
  activeId.value = null
  reloadConversations()
}

async function send() {
  if (!detail.value || !input.value.trim()) return
  const id = detail.value.header.id
  const msg = input.value.trim()
  input.value = ''
  sending.value = true
  // optimistic user message
  detail.value.messages.push({
    id: 'tmp-' + Date.now(),
    role: 'USER',
    content: msg,
    createdAt: new Date().toISOString(),
  })
  await nextTick(); scrollToBottom()
  try {
    const res: any = await aiApi.sendMessage(id, msg)
    if (res.data) detail.value.messages.push(res.data)
    await nextTick(); scrollToBottom()
    reloadConversations()
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  const el = messagesRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function formatRelative(iso?: string): string {
  if (!iso) return ''
  const tt = new Date(iso).getTime()
  const diff = Date.now() - tt
  if (diff < 60_000) return t('ai.chat.justNow')
  if (diff < 3_600_000) return t('ai.chat.minutesAgo', { n: Math.floor(diff / 60_000) })
  if (diff < 86_400_000) return t('ai.chat.hoursAgo', { n: Math.floor(diff / 3_600_000) })
  const loc = String(locale.value).startsWith('zh') ? 'zh-CN' : 'en-US'
  return new Date(iso).toLocaleDateString(loc)
}

function formatTime(iso?: string): string {
  if (!iso) return ''
  const loc = String(locale.value).startsWith('zh') ? 'zh-CN' : 'en-US'
  return new Date(iso).toLocaleTimeString(loc, { hour: '2-digit', minute: '2-digit' })
}

// expose for streaming UI even though not used in MVP path
void streaming.value
void streamingContent.value

onMounted(() => {
  reloadConversations()
  loadAgents()
})
</script>

<style scoped>
.ai-chat {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 0;
  height: calc(100vh - 96px);
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: var(--mc-radius-xl, 16px);
  box-shadow: var(--mc-shadow-soft);
  overflow: hidden;
}

.ai-chat__sidebar {
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  border-right: 1px solid var(--mc-border-light);
  background: var(--mc-bg-soft);
  min-height: 0;
}
.ai-chat__sidebar-head { margin-bottom: 12px; }
.ai-chat__sessions {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ai-chat__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: var(--mc-bg-elevated);
}
.ai-chat__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--mc-divider-subtle);
}
.ai-chat__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--mc-text-primary);
}
.ai-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.ai-chat__compose {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--mc-divider-subtle);
  background: var(--mc-bg-elevated);
}
.ai-chat__compose-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.ai-chat__hint {
  font-size: 12px;
  color: var(--mc-text-muted);
}
</style>
